package com.huryel.novely.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class ObraNoBackup(val indice: Int, val titulo: String, val capitulos: Int, val palavras: Long, val jaExiste: Boolean, val incluir: Boolean)

sealed interface EstadoBackup {
    data object Ocioso : EstadoBackup
    data class Trabalhando(val mensagem: String, val progresso: Float? = null) : EstadoBackup
    data class PreviaRestauracao(val arquivo: File, val criadoEm: Long, val obras: List<ObraNoBackup>) : EstadoBackup
    data class Feito(val mensagem: String) : EstadoBackup
    data class Erro(val mensagem: String) : EstadoBackup
}

/**
 * Backup portátil: um arquivo .novely (ZIP comum) com manifest.json + textos/(id).md + imagens.
 * Pode ser aberto por qualquer programa de ZIP; os textos são Markdown puro.
 * Restaurar NUNCA sobrescreve: cada obra entra como obra nova (duplicatas vêm desmarcadas).
 */
class Backup(private val ctx: Context, private val repo: Repositorio, private val prefs: Preferencias) {
    private val _estado = MutableStateFlow<EstadoBackup>(EstadoBackup.Ocioso)
    val estado: StateFlow<EstadoBackup> = _estado
    fun limpar() { _estado.value = EstadoBackup.Ocioso }

    companion object {
        const val FORMATO = "novely-backup"
        const val VERSAO = 1
        fun nomeArquivo(prefixo: String = "novely-backup") =
            "$prefixo-${SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())}.novely"
    }

    // ───────── Criar ─────────

    suspend fun exportarBackup(destino: Uri, obraId: Long? = null) = withContext(Dispatchers.IO) {
        try {
            _estado.value = EstadoBackup.Trabalhando("Preparando backup…")
            val n = ctx.contentResolver.openOutputStream(destino, "wt")?.use { escreverBackup(it, obraId) }
                ?: throw IllegalStateException("Não consegui criar o arquivo")
            if (obraId == null) prefs.ultimoBackup.definir(System.currentTimeMillis())
            _estado.value = EstadoBackup.Feito("Backup salvo: $n obra(s).")
        } catch (e: Exception) {
            _estado.value = EstadoBackup.Erro("O backup falhou: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** Escreve o ZIP direto no destino, arquivo por arquivo (nunca monta a biblioteca na memória). */
    /**
     * Publicar: gera um .novely de uma obra para outras pessoas lerem.
     * Vai trancado (só leitura), sem notas, sem progresso e (por padrão) sem rascunhos.
     */
    suspend fun publicar(obraId: Long, autor: String, incluirRascunhos: Boolean): File = withContext(Dispatchers.IO) {
        val o = repo.dao.obraAgora(obraId) ?: throw IllegalStateException("Obra não encontrada")
        val dir = File(ctx.cacheDir, "compartilhar").apply { mkdirs() }
        dir.listFiles()?.filter { it.name.endsWith(".novely") }?.forEach { it.delete() }
        val nome = o.titulo.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().ifEmpty { "obra" }.take(60)
        val f = File(dir, "$nome.novely")
        f.outputStream().use { escreverBackup(it, obraId, Publicacao(autor.trim(), incluirRascunhos)) }
        f
    }

    private class Publicacao(val autor: String, val rascunhos: Boolean)

    private suspend fun escreverBackup(saida: OutputStream, soObra: Long?, pub: Publicacao? = null): Int {
        val obras = repo.dao.todasObras().filter { soObra == null || it.id == soObra }
        val manifesto = JSONObject().put("formato", FORMATO).put("versao", VERSAO).put("criadoEm", System.currentTimeMillis())
        val arr = JSONArray()
        ZipOutputStream(saida.buffered(64 * 1024)).use { zip ->
            obras.forEachIndexed { oi, o ->
                _estado.value = EstadoBackup.Trabalhando("Salvando “${o.titulo}”…", oi.toFloat() / obras.size.coerceAtLeast(1))
                val jo = JSONObject()
                    .put("titulo", o.titulo).put("descricao", o.descricao).put("status", if (pub != null) StatusObra.LENDO else o.status)
                    .put("minha", if (pub != null) false else o.minha).put("trancada", pub != null || o.trancada)
                    .put("autor", pub?.autor ?: o.autor)
                    .put("capaX", o.capaX.toDouble()).put("capaY", o.capaY.toDouble()).put("capaZoom", o.capaZoom.toDouble())
                    .put("bannerX", o.bannerX.toDouble()).put("bannerY", o.bannerY.toDouble()).put("bannerZoom", o.bannerZoom.toDouble())
                    .put("corBase", o.corBase).put("criadaEm", o.criadaEm).put("atualizadaEm", o.atualizadaEm)
                fun imagem(caminho: String?, nome: String): String? {
                    val f = caminho?.let { File(it) }?.takeIf { it.exists() } ?: return null
                    val entrada = "imagens/${o.id}-$nome.jpg"
                    zip.putNextEntry(ZipEntry(entrada)); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                    return entrada
                }
                imagem(o.capa, "capa")?.let { jo.put("capa", it) }
                imagem(o.banner, "banner")?.let { jo.put("banner", it) }
                val vols = JSONArray()
                repo.dao.volumesAgora(o.id).forEach { v -> vols.put(JSONObject().put("id", v.id).put("titulo", v.titulo).put("ordem", v.ordem)) }
                jo.put("volumes", vols)
                val caps = JSONArray()
                repo.dao.capitulosAgora(o.id).filter { pub == null || pub.rascunhos || !it.rascunho }.forEach { c ->
                    val entrada = "textos/${c.id}.md"
                    val f = repo.textos.arquivo(c.id)
                    val crc = CRC32()
                    zip.putNextEntry(ZipEntry(entrada))
                    if (f.exists()) f.inputStream().use { ins ->
                        val buf = ByteArray(64 * 1024)
                        while (true) { val r = ins.read(buf); if (r <= 0) break; zip.write(buf, 0, r); crc.update(buf, 0, r) }
                    }
                    zip.closeEntry()
                    caps.put(JSONObject().put("volumeId", c.volumeId ?: JSONObject.NULL).put("titulo", c.titulo).put("ordem", c.ordem)
                        .put("rascunho", if (pub != null) false else c.rascunho).put("palavras", c.palavras)
                        .put("progresso", if (pub != null) 0.0 else c.progresso.toDouble())
                        .put("posicaoBloco", if (pub != null) 0 else c.posicaoBloco).put("posicaoDeslocamento", if (pub != null) 0 else c.posicaoDeslocamento)
                        .put("atualizadoEm", c.atualizadoEm).put("resumo", c.resumo).put("notas", if (pub != null) "" else c.notas).put("arquivo", entrada).put("crc", crc.value))
                }
                jo.put("capitulos", caps)
                val fs = JSONArray()
                repo.dao.fichasAgora(o.id).forEach { f ->
                    val jf = JSONObject().put("tipo", f.tipo).put("nome", f.nome).put("descricao", f.descricao)
                    imagem(f.imagem, "ficha-${f.id}")?.let { jf.put("imagem", it) }
                    fs.put(jf)
                }
                jo.put("fichas", fs)
                arr.put(jo)
            }
            manifesto.put("obras", arr)
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifesto.toString(1).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return obras.size
    }

    // ───────── Restaurar ─────────

    suspend fun analisarRestauracao(origem: Uri) = withContext(Dispatchers.IO) {
        try {
            _estado.value = EstadoBackup.Trabalhando("Lendo backup…")
            val copia = File(ctx.cacheDir, "restaurar.novely")
            ctx.contentResolver.openInputStream(origem)!!.use { ins -> copia.outputStream().use { ins.copyTo(it, 64 * 1024) } }
            val m = lerManifesto(copia)
            val existentes = repo.dao.todasObras().map { it.titulo.trim().lowercase() to repo.dao.contarCapitulos(it.id) }.toSet()
            val obras = m.getJSONArray("obras")
            val lista = (0 until obras.length()).map { i ->
                val o = obras.getJSONObject(i)
                val caps = o.getJSONArray("capitulos")
                var palavras = 0L
                for (k in 0 until caps.length()) palavras += caps.getJSONObject(k).optLong("palavras")
                val dup = (o.getString("titulo").trim().lowercase() to caps.length()) in existentes
                ObraNoBackup(i, o.getString("titulo"), caps.length(), palavras, dup, !dup)
            }
            _estado.value = EstadoBackup.PreviaRestauracao(copia, m.optLong("criadoEm"), lista)
        } catch (e: Exception) {
            _estado.value = EstadoBackup.Erro(e.message ?: "Arquivo de backup inválido ou corrompido.")
        }
    }

    private fun lerManifesto(f: File): JSONObject {
        val zip = try { ZipFile(f) } catch (e: Exception) { throw IllegalStateException("Esse arquivo não é um backup do Novely (ou está corrompido).") }
        zip.use {
            val e = it.getEntry("manifest.json") ?: throw IllegalStateException("Backup sem manifest.json — arquivo incompleto ou de outro app.")
            val m = JSONObject(it.getInputStream(e).use { s -> s.readBytes().toString(Charsets.UTF_8) })
            if (m.optString("formato") != FORMATO) throw IllegalStateException("Esse arquivo não é um backup do Novely.")
            if (m.optInt("versao") > VERSAO) throw IllegalStateException("Backup feito por uma versão mais nova do Novely. Atualize o app.")
            return m
        }
    }

    fun alternar(i: Int) {
        val e = _estado.value as? EstadoBackup.PreviaRestauracao ?: return
        _estado.value = e.copy(obras = e.obras.map { if (it.indice == i) it.copy(incluir = !it.incluir) else it })
    }

    suspend fun restaurar() = withContext(Dispatchers.IO) {
        val e = _estado.value as? EstadoBackup.PreviaRestauracao ?: return@withContext
        val escolhidas = e.obras.filter { it.incluir }.map { it.indice }.toSet()
        var problemas = 0
        var feitas = 0
        try {
            val m = lerManifesto(e.arquivo)
            val obras = m.getJSONArray("obras")
            ZipFile(e.arquivo).use { zip ->
                for (i in 0 until obras.length()) {
                    if (i !in escolhidas) continue
                    val o = obras.getJSONObject(i)
                    _estado.value = EstadoBackup.Trabalhando("Restaurando “${o.getString("titulo")}”…", feitas.toFloat() / escolhidas.size)
                    fun imagem(chave: String): String? {
                        val nome = o.optString(chave, "").ifEmpty { return null }
                        val ent = zip.getEntry(nome) ?: return null
                        val f = File(File(ctx.filesDir, "imagens").apply { mkdirs() }, "${java.util.UUID.randomUUID()}.jpg")
                        zip.getInputStream(ent).use { ins -> f.outputStream().use { ins.copyTo(it) } }
                        return f.absolutePath
                    }
                    repo.banco.withTransaction {
                        val obraId = repo.dao.inserirObra(Obra(
                            titulo = o.getString("titulo"), descricao = o.optString("descricao"), status = o.optString("status", StatusObra.LENDO),
                            minha = !o.optBoolean("trancada", false) && o.optBoolean("minha", o.optString("status", StatusObra.LENDO) != StatusObra.LENDO),
                            trancada = o.optBoolean("trancada", false), autor = o.optString("autor", ""),
                            capa = imagem("capa"), capaX = o.optDouble("capaX", .5).toFloat(), capaY = o.optDouble("capaY", .5).toFloat(), capaZoom = o.optDouble("capaZoom", 1.0).toFloat(),
                            banner = imagem("banner"), bannerX = o.optDouble("bannerX", .5).toFloat(), bannerY = o.optDouble("bannerY", .5).toFloat(), bannerZoom = o.optDouble("bannerZoom", 1.0).toFloat(),
                            corBase = o.optLong("corBase", 0xFF3B2A4A), criadaEm = o.optLong("criadaEm", System.currentTimeMillis()),
                        ))
                        val mapaVol = HashMap<Long, Long>()
                        val vols = o.optJSONArray("volumes") ?: JSONArray()
                        for (k in 0 until vols.length()) {
                            val v = vols.getJSONObject(k)
                            mapaVol[v.getLong("id")] = repo.dao.inserirVolume(Volume(obraId = obraId, titulo = v.getString("titulo"), ordem = v.getDouble("ordem")))
                        }
                        val fichas = o.optJSONArray("fichas") ?: JSONArray()
                        for (k in 0 until fichas.length()) {
                            val jf = fichas.getJSONObject(k)
                            var img: String? = null
                            jf.optString("imagem", "").takeIf { it.isNotEmpty() }?.let { nome ->
                                zip.getEntry(nome)?.let { ent ->
                                    val f = File(File(ctx.filesDir, "imagens").apply { mkdirs() }, "${java.util.UUID.randomUUID()}.jpg")
                                    zip.getInputStream(ent).use { ins -> f.outputStream().use { ins.copyTo(it) } }
                                    img = f.absolutePath
                                }
                            }
                            repo.dao.inserirFicha(Ficha(obraId = obraId, tipo = jf.optString("tipo", TipoFicha.PERSONAGEM), nome = jf.optString("nome"),
                                descricao = jf.optString("descricao"), imagem = img))
                        }
                        val caps = o.getJSONArray("capitulos")
                        for (k in 0 until caps.length()) {
                            val c = caps.getJSONObject(k)
                            val volAntigo = if (c.isNull("volumeId")) null else c.getLong("volumeId")
                            val id = repo.dao.inserirCapitulo(Capitulo(
                                obraId = obraId, volumeId = volAntigo?.let { mapaVol[it] }, titulo = c.getString("titulo"), ordem = c.getDouble("ordem"),
                                rascunho = c.optBoolean("rascunho"), palavras = c.optInt("palavras"), progresso = c.optDouble("progresso", 0.0).toFloat(),
                                posicaoBloco = c.optInt("posicaoBloco"), posicaoDeslocamento = c.optInt("posicaoDeslocamento"),
                                atualizadoEm = c.optLong("atualizadoEm", System.currentTimeMillis()), resumo = c.optString("resumo", ""), notas = c.optString("notas", ""),
                            ))
                            val ent = zip.getEntry(c.getString("arquivo"))
                            val destino = repo.textos.arquivo(id)
                            if (ent == null) { problemas++; destino.writeText("") ; continue }
                            val crc = CRC32()
                            zip.getInputStream(ent).use { ins ->
                                destino.outputStream().use { out ->
                                    val buf = ByteArray(64 * 1024)
                                    while (true) { val r = ins.read(buf); if (r <= 0) break; out.write(buf, 0, r); crc.update(buf, 0, r) }
                                }
                            }
                            if (c.has("crc") && c.getLong("crc") != crc.value) problemas++
                        }
                    }
                    feitas++
                }
            }
            e.arquivo.delete()
            _estado.value = EstadoBackup.Feito(
                "$feitas obra(s) restaurada(s)." + if (problemas > 0) " Atenção: $problemas capítulo(s) vieram incompletos ou com erro de verificação." else " Tudo conferido."
            )
        } catch (ex: Exception) {
            _estado.value = EstadoBackup.Erro("A restauração parou: ${ex.message}. As obras já restauradas foram mantidas.")
        }
    }

    // ───────── Exportar obra como TXT / Markdown / EPUB ─────────

    suspend fun exportarTexto(obraId: Long, destino: Uri) = withContext(Dispatchers.IO) {
        try {
            val o = repo.dao.obraAgora(obraId) ?: return@withContext
            _estado.value = EstadoBackup.Trabalhando("Exportando “${o.titulo}”…")
            ctx.contentResolver.openOutputStream(destino, "wt")!!.bufferedWriter(Charsets.UTF_8).use { w ->
                w.write("# ${o.titulo}\n\n")
                if (o.descricao.isNotBlank()) w.write(o.descricao + "\n\n")
                repo.dao.capitulosAgora(obraId).forEach { c ->
                    w.write("\n# ${c.titulo}\n\n")
                    val f = repo.textos.arquivo(c.id)
                    if (f.exists()) f.bufferedReader(Charsets.UTF_8).use { it.copyTo(w) }
                    w.write("\n")
                }
            }
            _estado.value = EstadoBackup.Feito("“${o.titulo}” exportada.")
        } catch (e: Exception) {
            _estado.value = EstadoBackup.Erro("A exportação falhou: ${e.message}")
        }
    }

    suspend fun exportarEpub(obraId: Long, destino: Uri) = withContext(Dispatchers.IO) {
        try {
            val o = repo.dao.obraAgora(obraId) ?: return@withContext
            val caps = repo.dao.capitulosAgora(obraId)
            _estado.value = EstadoBackup.Trabalhando("Gerando EPUB de “${o.titulo}”…")
            val uid = "urn:uuid:" + java.util.UUID.nameUUIDFromBytes("novely-${o.id}-${o.criadaEm}".toByteArray())
            val t = Texto.escaparXml(o.titulo)
            ctx.contentResolver.openOutputStream(destino, "wt")!!.use { saida ->
                ZipOutputStream(saida.buffered()).use { zip ->
                    // "mimetype" tem que ser o primeiro arquivo e sem compressão
                    val mt = "application/epub+zip".toByteArray()
                    val crc = CRC32().apply { update(mt) }
                    zip.putNextEntry(ZipEntry("mimetype").apply { method = ZipEntry.STORED; size = mt.size.toLong(); compressedSize = mt.size.toLong(); this.crc = crc.value })
                    zip.write(mt); zip.closeEntry()
                    fun txt(nome: String, conteudo: String) { zip.putNextEntry(ZipEntry(nome)); zip.write(conteudo.toByteArray(Charsets.UTF_8)); zip.closeEntry() }
                    txt("META-INF/container.xml", """<?xml version="1.0" encoding="UTF-8"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
                    val temCapa = o.capa?.let { File(it).exists() } == true
                    if (temCapa) { zip.putNextEntry(ZipEntry("OEBPS/capa.jpg")); File(o.capa!!).inputStream().use { it.copyTo(zip) }; zip.closeEntry() }
                    caps.forEachIndexed { i, c ->
                        val corpo = Texto.markdownParaXhtml(repo.textos.ler(c.id))
                        txt("OEBPS/cap$i.xhtml", """<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml" xml:lang="pt-BR"><head><title>${Texto.escaparXml(c.titulo)}</title></head><body><h1>${Texto.escaparXml(c.titulo)}</h1>
$corpo</body></html>""")
                    }
                    val manifest = StringBuilder(); val spine = StringBuilder(); val nav = StringBuilder()
                    caps.forEachIndexed { i, c ->
                        manifest.append("""<item id="c$i" href="cap$i.xhtml" media-type="application/xhtml+xml"/>""")
                        spine.append("""<itemref idref="c$i"/>""")
                        nav.append("""<li><a href="cap$i.xhtml">${Texto.escaparXml(c.titulo)}</a></li>""")
                    }
                    if (temCapa) manifest.append("""<item id="capa" href="capa.jpg" media-type="image/jpeg" properties="cover-image"/>""")
                    txt("OEBPS/nav.xhtml", """<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>$t</title></head><body><nav epub:type="toc"><h1>Sumário</h1><ol>$nav</ol></nav></body></html>""")
                    val agora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date())
                    txt("OEBPS/content.opf", """<?xml version="1.0" encoding="UTF-8"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="uid">$uid</dc:identifier><dc:title>$t</dc:title><dc:language>pt-BR</dc:language><meta property="dcterms:modified">$agora</meta>${if (temCapa) "<meta name=\"cover\" content=\"capa\"/>" else ""}</metadata><manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>$manifest</manifest><spine>$spine</spine></package>""")
                }
            }
            _estado.value = EstadoBackup.Feito("EPUB de “${o.titulo}” criado.")
        } catch (e: Exception) {
            _estado.value = EstadoBackup.Erro("Não consegui gerar o EPUB: ${e.message}")
        }
    }

    // ───────── Backup automático numa pasta escolhida ─────────

    /** Chamado ao abrir o app: se passou 1 dia, grava um backup na pasta e mantém só os 5 mais recentes. */
    suspend fun backupAutomaticoSePreciso() = withContext(Dispatchers.IO) {
        if (!prefs.backupAuto.valor) return@withContext
        val pasta = prefs.pastaBackup.valor.ifEmpty { return@withContext }
        if (System.currentTimeMillis() - prefs.ultimoBackup.valor < 24 * 3600_000L) return@withContext
        if (repo.dao.todasObras().isEmpty()) return@withContext
        try {
            val arvore = Uri.parse(pasta)
            val raiz = DocumentsContract.buildDocumentUriUsingTree(arvore, DocumentsContract.getTreeDocumentId(arvore))
            val novo = DocumentsContract.createDocument(ctx.contentResolver, raiz, "application/zip", nomeArquivo("novely-auto")) ?: return@withContext
            ctx.contentResolver.openOutputStream(novo, "wt")?.use { escreverBackup(it, null) }
            prefs.ultimoBackup.definir(System.currentTimeMillis())
            _estado.value = EstadoBackup.Ocioso
            // apaga os automáticos antigos (mantém 5)
            val filhos = DocumentsContract.buildChildDocumentsUriUsingTree(arvore, DocumentsContract.getTreeDocumentId(arvore))
            val antigos = ArrayList<Pair<String, String>>()
            ctx.contentResolver.query(filhos, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                while (c.moveToNext()) { val nome = c.getString(1) ?: continue; if (nome.startsWith("novely-auto-")) antigos.add(c.getString(0) to nome) }
            }
            antigos.sortedByDescending { it.second }.drop(5).forEach {
                DocumentsContract.deleteDocument(ctx.contentResolver, DocumentsContract.buildDocumentUriUsingTree(arvore, it.first))
            }
        } catch (_: Exception) {
            // silencioso: a tela de backup mostra a data do último backup feito
        }
    }
}
