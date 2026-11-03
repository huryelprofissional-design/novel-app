package com.huryel.novely.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedWriter
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.UUID
import java.util.zip.ZipFile

data class CapImport(val titulo: String, val palavras: Int, val arquivo: File, val incluir: Boolean = true)

data class PreviaImport(
    val uri: Uri,
    val nomeArquivo: String,
    val tipo: String, // TXT | EPUB | DOCX
    val titulo: String,
    val capitulos: List<CapImport>,
    val modo: String, // AUTO | UM | PREFIXO
    val prefixo: String = "",
    val avisos: List<String> = emptyList(),
)

sealed interface EstadoImport {
    data object Ocioso : EstadoImport
    data class Lendo(val mensagem: String) : EstadoImport
    data class Previa(val p: PreviaImport) : EstadoImport
    data class Salvando(val feito: Int, val total: Int) : EstadoImport
    data class Concluido(val obraId: Long, val capitulos: Int) : EstadoImport
    data class Erro(val mensagem: String) : EstadoImport
}

/**
 * Importação em duas etapas: (1) lê o arquivo em modo streaming e grava cada capítulo detectado
 * num arquivo temporário — nunca guarda o livro inteiro na memória; (2) depois que você confere,
 * cria a obra e MOVE esses arquivos para a biblioteca.
 */
class Importador(private val ctx: Context, private val repo: Repositorio) {
    private val _estado = MutableStateFlow<EstadoImport>(EstadoImport.Ocioso)
    val estado: StateFlow<EstadoImport> = _estado

    private val pastaTemp get() = File(ctx.cacheDir, "importacao").apply { mkdirs() }

    fun limpar() {
        pastaTemp.deleteRecursively()
        _estado.value = EstadoImport.Ocioso
    }

    fun nomeDe(uri: Uri): String =
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment ?: "arquivo"

    suspend fun analisar(uri: Uri, modo: String = "AUTO", prefixo: String = "") = withContext(Dispatchers.IO) {
        try {
            pastaTemp.deleteRecursively()
            val nome = nomeDe(uri)
            val tipoMime = ctx.contentResolver.getType(uri) ?: ""
            val ehEpub = nome.endsWith(".epub", true) || tipoMime.contains("epub")
            val ehDocx = nome.endsWith(".docx", true) || tipoMime.contains("wordprocessingml")
            _estado.value = EstadoImport.Lendo("Lendo $nome…")
            val previa = when {
                ehEpub -> lerEpub(uri, nome)
                ehDocx -> lerTxt(Uri.fromFile(docxParaTexto(uri)), nome, modo, prefixo).copy(uri = uri, tipo = "DOCX")
                else -> lerTxt(uri, nome, modo, prefixo)
            }
            if (previa.capitulos.isEmpty()) {
                _estado.value = EstadoImport.Erro("Não encontrei texto nesse arquivo. Ele pode estar vazio, protegido (DRM) ou em um formato diferente.")
            } else _estado.value = EstadoImport.Previa(previa)
        } catch (e: OutOfMemoryError) {
            _estado.value = EstadoImport.Erro("O arquivo é grande demais para a memória deste aparelho.")
        } catch (e: SecurityException) {
            _estado.value = EstadoImport.Erro("O Android não deu permissão para ler esse arquivo. Tente escolhê-lo de novo.")
        } catch (e: Exception) {
            _estado.value = EstadoImport.Erro("Não consegui ler o arquivo: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    fun alternar(i: Int) {
        val e = _estado.value as? EstadoImport.Previa ?: return
        val caps = e.p.capitulos.toMutableList()
        caps[i] = caps[i].copy(incluir = !caps[i].incluir)
        _estado.value = EstadoImport.Previa(e.p.copy(capitulos = caps))
    }

    fun renomearObra(t: String) {
        val e = _estado.value as? EstadoImport.Previa ?: return
        _estado.value = EstadoImport.Previa(e.p.copy(titulo = t))
    }

    // ───────── TXT ─────────

    private fun detectarCharset(uri: Uri): Charset {
        val amostra = ByteArray(256 * 1024)
        val n = ctx.contentResolver.openInputStream(uri)?.use { lerAte(it, amostra) } ?: 0
        var lim = n
        // não cortar um caractere UTF-8 no meio
        while (lim > 0 && lim > n - 4 && (amostra[lim - 1].toInt() and 0xC0) == 0x80) lim--
        if (lim > 0 && (amostra[lim - 1].toInt() and 0xC0) == 0xC0) lim--
        return try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(amostra, 0, lim))
            Charsets.UTF_8
        } catch (e: Exception) {
            Charset.forName("windows-1252")
        }
    }

    private fun lerAte(ins: InputStream, buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val r = ins.read(buf, total, buf.size - total)
            if (r <= 0) break
            total += r
        }
        return total
    }

    /** Word (.docx): pega o texto dos parágrafos, com itálico/negrito, e salva como TXT temporário. */
    private fun docxParaTexto(uri: Uri): File {
        var xml = ""
        ctx.contentResolver.openInputStream(uri)!!.use { ins ->
            java.util.zip.ZipInputStream(ins).use { z ->
                var e = z.nextEntry
                while (e != null) { if (e.name == "word/document.xml") { xml = z.readBytes().toString(Charsets.UTF_8); break }; e = z.nextEntry }
            }
        }
        if (xml.isEmpty()) error("não achei o texto dentro do Word")
        val saida = File(pastaTemp.apply { mkdirs() }, "word.txt")
        saida.writeText(textoDoWord(xml))
        return saida
    }

    /**
     * Importa de um link: Google Docs ("qualquer pessoa com o link"), ou link direto de .txt/.epub/.docx.
     */
    suspend fun analisarLink(link: String) = withContext(Dispatchers.IO) {
        try {
            pastaTemp.deleteRecursively(); pastaTemp.mkdirs()
            val docs = Regex("docs\\.google\\.com/document/(?:u/\\d+/)?d/([\\w-]+)").find(link)?.groupValues?.get(1)
            val url = if (docs != null) "https://docs.google.com/document/d/$docs/export?format=docx" else link.trim()
            val nome = if (docs != null) "Documento do Google.docx" else link.substringAfterLast('/').substringBefore('?').ifBlank { "link.txt" }
            _estado.value = EstadoImport.Lendo("Baixando…")
            val con = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            con.instanceFollowRedirects = true; con.connectTimeout = 20000; con.readTimeout = 60000
            if (con.responseCode !in 200..299) error("o link respondeu ${con.responseCode}")
            val tipo = con.contentType ?: ""
            if (tipo.contains("html")) error(if (docs != null) "o documento não está aberto. No Google Docs: Compartilhar › Qualquer pessoa com o link." else "esse link é uma página, não um arquivo (.txt, .epub ou .docx)")
            val f = File(pastaTemp, nome)
            con.inputStream.use { i -> f.outputStream().use { i.copyTo(it) } }
            val destinoFinal = File(ctx.cacheDir, "link-" + nome).also { f.copyTo(it, overwrite = true) }
            analisar(Uri.fromFile(destinoFinal))
        } catch (e: Exception) {
            _estado.value = EstadoImport.Erro("Não consegui baixar: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun lerTxt(uri: Uri, nome: String, modo: String, prefixo: String): PreviaImport {
        val charset = detectarCharset(uri)
        val caps = ArrayList<CapImport>()
        var tituloAtual: String? = null
        var escritor: BufferedWriter? = null
        var arquivoAtual: File? = null
        var palavras = 0
        var linhasLidas = 0L

        fun fechar() {
            val w = escritor ?: return
            w.close()
            if (palavras > 0 || tituloAtual != null) {
                caps.add(CapImport(tituloAtual ?: "Início", palavras, arquivoAtual!!))
            } else arquivoAtual?.delete()
            escritor = null
        }
        fun abrir(titulo: String?) {
            fechar()
            tituloAtual = titulo
            palavras = 0
            arquivoAtual = File(pastaTemp, "${caps.size}-${UUID.randomUUID()}.md")
            escritor = arquivoAtual!!.bufferedWriter(Charsets.UTF_8)
        }

        abrir(null)
        ctx.contentResolver.openInputStream(uri)!!.bufferedReader(charset).useLines { linhas ->
            linhas.forEach { bruta ->
                val linha = bruta.removePrefix("﻿")
                linhasLidas++
                if (linhasLidas % 20000 == 0L) _estado.value = EstadoImport.Lendo("Lendo $nome… ${caps.size} capítulos até agora")
                val ehTitulo = when (modo) {
                    "UM" -> false
                    "PREFIXO" -> prefixo.isNotBlank() && Texto.ehTituloCapitulo(linha, prefixo.trim())
                    else -> Texto.ehTituloCapitulo(linha)
                }
                if (ehTitulo) abrir(Texto.limparTitulo(linha))
                else {
                    escritor!!.write(linha); escritor!!.write("\n")
                    palavras += Texto.contarPalavras(linha)
                }
            }
        }
        fechar()
        if (modo == "UM" && caps.size == 1) caps[0] = caps[0].copy(titulo = "Capítulo 1")
        val tituloObra = nome.substringBeforeLast('.').replace('_', ' ').trim()
        return PreviaImport(uri, nome, "TXT", tituloObra, caps, modo, prefixo, avisos(caps, modo))
    }

    private fun avisos(caps: List<CapImport>, modo: String): List<String> {
        val a = ArrayList<String>()
        if (modo == "AUTO" && caps.size <= 1) a.add("Não encontrei títulos como “Capítulo 1”. Tudo ficou num capítulo só — use “Dividir por…” se o arquivo usa outro padrão.")
        val gigantes = caps.count { it.palavras > 80_000 }
        if (gigantes > 0) a.add("$gigantes capítulo(s) com mais de 80 mil palavras: confira se a divisão está certa.")
        val vazios = caps.count { it.palavras < 30 }
        if (vazios > 0) a.add("$vazios capítulo(s) quase vazios (podem ser títulos repetidos ou sumário). Desmarque se não quiser.")
        return a
    }

    // ───────── EPUB ─────────

    private fun lerEpub(uri: Uri, nome: String): PreviaImport {
        // ZipFile precisa de acesso aleatório: copia para o cache (streaming, sem carregar na memória).
        val copia = File(pastaTemp, "livro.epub")
        ctx.contentResolver.openInputStream(uri)!!.use { ins -> copia.outputStream().use { ins.copyTo(it, 64 * 1024) } }
        val caps = ArrayList<CapImport>()
        val avisos = ArrayList<String>()
        var tituloLivro = nome.substringBeforeLast('.')
        ZipFile(copia).use { zip ->
            if (zip.getEntry("META-INF/encryption.xml") != null) avisos.add("Este EPUB parece ter proteção (DRM). Partes podem sair ilegíveis.")
            val container = zip.getEntry("META-INF/container.xml") ?: throw IllegalStateException("EPUB sem container.xml (arquivo corrompido?)")
            val caminhoOpf = zip.getInputStream(container).use { achaAtributo(it, "rootfile", "full-path") }
                ?: throw IllegalStateException("EPUB sem arquivo OPF")
            val base = caminhoOpf.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
            val manifesto = HashMap<String, String>()
            val espinha = ArrayList<String>()
            zip.getInputStream(zip.getEntry(caminhoOpf) ?: throw IllegalStateException("OPF não encontrado")).use { ins ->
                val p = Xml.newPullParser()
                p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                p.setInput(ins, null)
                var noTitulo = false
                while (p.next() != XmlPullParser.END_DOCUMENT) {
                    when (p.eventType) {
                        XmlPullParser.START_TAG -> {
                            val tag = p.name.substringAfter(':')
                            when (tag) {
                                "item" -> {
                                    val id = p.getAttributeValue(null, "id"); val href = p.getAttributeValue(null, "href")
                                    if (id != null && href != null) manifesto[id] = href
                                }
                                "itemref" -> p.getAttributeValue(null, "idref")?.let { espinha.add(it) }
                                "title" -> noTitulo = true
                            }
                        }
                        XmlPullParser.TEXT -> if (noTitulo && p.text.isNotBlank()) { tituloLivro = p.text.trim(); noTitulo = false }
                        XmlPullParser.END_TAG -> if (p.name.substringAfter(':') == "title") noTitulo = false
                    }
                }
            }
            espinha.forEachIndexed { i, idref ->
                val href = manifesto[idref] ?: return@forEachIndexed
                val caminho = normalizarCaminho(base + java.net.URLDecoder.decode(href.substringBefore('#'), "UTF-8"))
                val entrada = zip.getEntry(caminho) ?: return@forEachIndexed
                if (i % 10 == 0) _estado.value = EstadoImport.Lendo("Lendo $nome… parte ${i + 1} de ${espinha.size}")
                val html = zip.getInputStream(entrada).use { it.readBytes().toString(Charsets.UTF_8) }
                val md = Texto.htmlParaMarkdown(html)
                val palavras = Texto.contarPalavras(md)
                if (palavras == 0) return@forEachIndexed
                val primeiraLinha = md.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
                val titulo = if (primeiraLinha.startsWith("#")) Texto.limparTitulo(primeiraLinha) else "Parte ${caps.size + 1}"
                val corpo = if (primeiraLinha.startsWith("#")) md.substringAfter(primeiraLinha).trimStart() else md
                val f = File(pastaTemp, "${caps.size}-${UUID.randomUUID()}.md")
                f.writeText(corpo, Charsets.UTF_8)
                caps.add(CapImport(titulo, Texto.contarPalavras(corpo), f, incluir = palavras >= 30))
            }
        }
        copia.delete()
        avisos.addAll(avisos(caps, "EPUB").filterNot { it.startsWith("Não encontrei") })
        if (caps.any { !it.incluir }) avisos.add("Páginas muito curtas (capa, sumário) vieram desmarcadas.")
        return PreviaImport(uri, nome, "EPUB", tituloLivro, caps, "EPUB", avisos = avisos)
    }

    private fun normalizarCaminho(c: String): String {
        val partes = ArrayList<String>()
        c.split('/').forEach { if (it == "..") { if (partes.isNotEmpty()) partes.removeAt(partes.size - 1) } else if (it != "." && it.isNotEmpty()) partes.add(it) }
        return partes.joinToString("/")
    }

    private fun achaAtributo(ins: InputStream, tag: String, atributo: String): String? {
        val p = Xml.newPullParser()
        p.setInput(ins, null)
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG && p.name.substringAfter(':') == tag) return p.getAttributeValue(null, atributo)
        }
        return null
    }

    // ───────── Confirmar ─────────

    /** obraDestino = null cria uma obra nova; senão acrescenta os capítulos no fim dela. */
    suspend fun confirmar(obraDestino: Long?) = withContext(Dispatchers.IO) {
        val e = _estado.value as? EstadoImport.Previa ?: return@withContext
        val caps = e.p.capitulos.filter { it.incluir }
        try {
            val obraId = obraDestino ?: repo.criarObra(e.p.titulo, "Importada de ${e.p.nomeArquivo}", StatusObra.LENDO)
            var ordem = repo.dao.maiorOrdemCapitulo(obraId)
            caps.chunked(100).forEachIndexed { lote, grupo ->
                repo.banco.withTransaction {
                    grupo.forEach { c ->
                        ordem += 1
                        val id = repo.dao.inserirCapitulo(Capitulo(obraId = obraId, titulo = c.titulo, ordem = ordem, rascunho = false, palavras = c.palavras))
                        val destino = repo.textos.arquivo(id)
                        if (!c.arquivo.renameTo(destino)) { c.arquivo.copyTo(destino, overwrite = true); c.arquivo.delete() }
                    }
                }
                _estado.value = EstadoImport.Salvando(minOf((lote + 1) * 100, caps.size), caps.size)
            }
            repo.dao.tocarObra(obraId, System.currentTimeMillis())
            pastaTemp.deleteRecursively()
            _estado.value = EstadoImport.Concluido(obraId, caps.size)
        } catch (ex: Exception) {
            _estado.value = EstadoImport.Erro("A importação parou no meio: ${ex.message}. Os capítulos já salvos ficaram na obra.")
        }
    }
}

/** Texto de um Word (word/document.xml): um parágrafo por linha, com *itálico* e **negrito**; títulos viram “# ”. */
fun textoDoWord(xml: String): String {
    fun desfazer(t: String) = t.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
    val rxRun = Regex("<w:r[ >].*?</w:r>", RegexOption.DOT_MATCHES_ALL)
    val rxT = Regex("<w:t(?: [^>]*)?>(.*?)</w:t>|<w:tab/>|<w:br/>", RegexOption.DOT_MATCHES_ALL)
    val rxI = Regex("<w:i(?: w:val=\"(?:1|true|on)\")?\\s*/>")
    val rxB = Regex("<w:b(?: w:val=\"(?:1|true|on)\")?\\s*/>")
    val rxTitulo = Regex("<w:pStyle w:val=\"(Heading|T[ií]tulo|Title)")
    val w = StringBuilder()
    for (par in xml.split("</w:p>")) {
        val titulo = rxTitulo.containsMatchIn(par)
        // pedaços com o mesmo estilo seguidos viram um só (o Word quebra o texto em vários pedaços)
        val pedacos = ArrayList<Triple<StringBuilder, Boolean, Boolean>>()
        rxRun.findAll(par).forEach { r ->
            val run = r.value
            val texto = rxT.findAll(run).joinToString("") { m -> when (m.value) { "<w:tab/>" -> " "; "<w:br/>" -> " "; else -> desfazer(m.groupValues[1]) } }
            if (texto.isEmpty()) return@forEach
            val i = !titulo && rxI.containsMatchIn(run); val b = !titulo && rxB.containsMatchIn(run)
            val ult = pedacos.lastOrNull()
            if (ult != null && ult.second == i && ult.third == b) ult.first.append(texto) else pedacos.add(Triple(StringBuilder(texto), i, b))
        }
        val linha = pedacos.joinToString("") { (t, i, b) ->
            val x = t.toString(); val m = if (b) "**" else if (i) "*" else ""
            if (m.isEmpty() || x.isBlank()) x else { val ini = x.length - x.trimStart().length; val fim = x.trimEnd().length; x.substring(0, ini) + m + x.substring(ini, fim) + m + x.substring(fim) }
        }.trim()
        if (linha.isNotEmpty()) w.append(if (titulo && !Texto.ehTituloCapitulo(linha)) "# $linha" else linha).append("\n\n")
    }
    return w.toString()
}
