package com.huryel.novely.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Repositorio(ctx: Context) {
    val banco = BancoNovely.criar(ctx)
    val dao = banco.dao()
    val textos = ArquivosTexto(ctx)
    val imagens = ArquivosImagem(ctx)

    private val cores = listOf(0xFF3B2A4A, 0xFF1F3B3A, 0xFF4A2420, 0xFF23283F, 0xFF3D3320, 0xFF2B3A22)

    fun resumos() = dao.resumos()
    fun obra(id: Long) = dao.obra(id)
    fun volumes(obraId: Long) = dao.volumes(obraId)
    fun capitulos(obraId: Long) = dao.capitulos(obraId)
    fun capitulo(id: Long) = dao.capitulo(id)

    suspend fun criarObra(titulo: String, descricao: String, status: String): Long =
        dao.inserirObra(
            Obra(titulo = titulo.trim().ifEmpty { "Sem título" }, descricao = descricao.trim(), status = status,
                corBase = cores[(System.nanoTime() % cores.size).toInt().let { if (it < 0) -it else it }])
        )

    suspend fun salvarObra(obra: Obra) = dao.atualizarObra(obra.copy(atualizadaEm = System.currentTimeMillis()))

    suspend fun excluirObra(obra: Obra) {
        val caps = dao.capitulosAgora(obra.id)
        dao.excluirObra(obra) // capítulos e volumes saem junto (cascade)
        caps.forEach { textos.apagar(it.id) }
        imagens.apagar(obra.capa); imagens.apagar(obra.banner)
    }

    /** tipo = "capa" ou "banner" */
    suspend fun trocarImagem(obraId: Long, tipo: String, uri: Uri): Boolean {
        val caminho = imagens.importar(uri, if (tipo == "banner") 2000 else 1200) ?: return false
        val o = dao.obraAgora(obraId) ?: return false
        if (tipo == "banner") {
            imagens.apagar(o.banner)
            salvarObra(o.copy(banner = caminho, bannerX = 0.5f, bannerY = 0.5f, bannerZoom = 1f))
        } else {
            imagens.apagar(o.capa)
            salvarObra(o.copy(capa = caminho, capaX = 0.5f, capaY = 0.5f, capaZoom = 1f))
        }
        return true
    }

    suspend fun removerImagem(obraId: Long, tipo: String) {
        val o = dao.obraAgora(obraId) ?: return
        if (tipo == "banner") { imagens.apagar(o.banner); salvarObra(o.copy(banner = null)) }
        else { imagens.apagar(o.capa); salvarObra(o.copy(capa = null)) }
    }

    suspend fun ajustarImagem(obraId: Long, tipo: String, x: Float, y: Float, zoom: Float) {
        val o = dao.obraAgora(obraId) ?: return
        salvarObra(if (tipo == "banner") o.copy(bannerX = x, bannerY = y, bannerZoom = zoom) else o.copy(capaX = x, capaY = y, capaZoom = zoom))
    }

    suspend fun novoVolume(obraId: Long, titulo: String): Long =
        dao.inserirVolume(Volume(obraId = obraId, titulo = titulo.trim().ifEmpty { "Volume" }, ordem = dao.maiorOrdemVolume(obraId) + 1))

    suspend fun renomearVolume(v: Volume, titulo: String) = dao.atualizarVolume(v.copy(titulo = titulo.trim().ifEmpty { v.titulo }))

    suspend fun excluirVolume(v: Volume) = banco.withTransaction {
        dao.capitulosAgora(v.obraId).filter { it.volumeId == v.id }.forEach { dao.atualizarCapitulo(it.copy(volumeId = null)) }
        dao.excluirVolume(v)
    }

    suspend fun novoCapitulo(obraId: Long, titulo: String, volumeId: Long?, emBranco: Boolean = false): Long {
        val id = dao.inserirCapitulo(
            Capitulo(obraId = obraId, volumeId = volumeId, titulo = if (emBranco) titulo.trim() else titulo.trim().ifEmpty { "Novo capítulo" },
                ordem = dao.maiorOrdemCapitulo(obraId) + 1)
        )
        textos.gravar(id, "")
        dao.tocarObra(obraId, System.currentTimeMillis())
        return id
    }

    /**
     * Divide um capítulo: o texto antes do título fica no capítulo atual; o título vira um capítulo
     * novo logo depois dele, com o texto que vinha abaixo. Retorna o id do capítulo novo.
     */
    suspend fun dividirCapitulo(c: Capitulo, textoAntes: String, titulo: String, textoDepois: String): Long {
        val lista = dao.capitulosAgora(c.obraId)
        val i = lista.indexOfFirst { it.id == c.id }
        val prox = lista.getOrNull(i + 1)
        val ordem = if (prox != null) (c.ordem + prox.ordem) / 2 else c.ordem + 1
        gravarTexto(c, textoAntes)
        val id = dao.inserirCapitulo(Capitulo(obraId = c.obraId, volumeId = c.volumeId, titulo = titulo, ordem = ordem))
        textos.gravar(id, textoDepois)
        dao.atualizarPalavras(id, Texto.contarPalavras(textoDepois), System.currentTimeMillis())
        return id
    }

    /**
     * Procura linhas de título ("Capítulo 3 — …", "Prólogo", "# …") que estejam sozinhas depois de uma
     * linha vazia (ou no começo) e separa cada uma em um capítulo próprio, logo após o atual.
     * Retorna quantos capítulos novos foram criados.
     */
    suspend fun separarPorTitulos(c: Capitulo, texto: String): Int {
        data class Parte(val titulo: String?, val sb: StringBuilder)
        val partes = arrayListOf(Parte(null, StringBuilder()))
        var anteriorVazia = true
        texto.lineSequence().forEach { linha ->
            if (anteriorVazia && Texto.ehTituloCapitulo(linha)) partes.add(Parte(Texto.tituloLimpo(linha), StringBuilder()))
            else partes.last().sb.append(linha).append('\n')
            anteriorVazia = linha.isBlank()
        }
        if (partes.size == 1) return 0
        var atual = c
        val primeira = partes[0]
        var resto = partes.drop(1)
        if (primeira.sb.isBlank()) {
            atual = c.copy(titulo = resto[0].titulo!!)
            salvarCapitulo(atual)
            gravarTexto(atual, resto[0].sb.toString().trim('\n') + "\n")
            resto = resto.drop(1)
        } else gravarTexto(atual, primeira.sb.toString().trimEnd() + "\n")
        val lista = dao.capitulosAgora(c.obraId)
        val prox = lista.getOrNull(lista.indexOfFirst { it.id == c.id } + 1)
        val passo = if (prox != null) (prox.ordem - c.ordem) / (resto.size + 1) else 1.0
        resto.forEachIndexed { k, pt ->
            val id = dao.inserirCapitulo(Capitulo(obraId = c.obraId, volumeId = c.volumeId, titulo = pt.titulo!!, ordem = c.ordem + passo * (k + 1)))
            val t = pt.sb.toString().trim('\n') + "\n"
            textos.gravar(id, t)
            dao.atualizarPalavras(id, Texto.contarPalavras(t), System.currentTimeMillis())
        }
        return resto.size + if (primeira.sb.isBlank()) 0 else 0
    }

    suspend fun salvarCapitulo(c: Capitulo) = dao.atualizarCapitulo(c.copy(atualizadoEm = System.currentTimeMillis()))

    suspend fun excluirCapitulo(c: Capitulo) {
        dao.excluirCapitulo(c)
        textos.apagar(c.id)
    }

    /** Move um capítulo uma posição para cima (-1) ou para baixo (+1), trocando só as duas ordens. */
    suspend fun moverCapitulo(c: Capitulo, direcao: Int) = banco.withTransaction {
        val lista = dao.capitulosAgora(c.obraId)
        val i = lista.indexOfFirst { it.id == c.id }
        val j = i + direcao
        if (i < 0 || j !in lista.indices) return@withTransaction
        val outro = lista[j]
        dao.atualizarCapitulo(lista[i].copy(ordem = outro.ordem))
        dao.atualizarCapitulo(outro.copy(ordem = lista[i].ordem))
    }

    suspend fun lerTexto(capId: Long) = textos.ler(capId)

    suspend fun gravarTexto(c: Capitulo, texto: String) {
        textos.guardarVersaoSePreciso(c.id)
        textos.gravar(c.id, texto)
        val agora = System.currentTimeMillis()
        dao.atualizarPalavras(c.id, ArquivosTexto.contarPalavras(texto), agora)
        dao.tocarObra(c.obraId, agora)
    }

    suspend fun salvarPosicao(c: Capitulo, progresso: Float, bloco: Int, desloc: Int) {
        dao.salvarPosicao(c.id, progresso, bloco, desloc)
        dao.marcarLeitura(c.obraId, c.id, System.currentTimeMillis())
    }
}

/** Preferências guardadas no aparelho (tema, leitor, voz). */
class Preferencias(ctx: Context) {
    private val sp = ctx.getSharedPreferences("novely_prefs", Context.MODE_PRIVATE)

    inner class Pref<T>(private val chave: String, padrao: T, ler: (String, T) -> T, private val gravar: (String, T) -> Unit) {
        private val fluxo = MutableStateFlow(ler(chave, padrao))
        val estado: StateFlow<T> = fluxo
        val valor get() = fluxo.value
        fun definir(v: T) { gravar(chave, v); fluxo.value = v }
    }
    private fun texto(k: String, p: String) = Pref(k, p, { c, d -> sp.getString(c, d) ?: d }, { c, v -> sp.edit().putString(c, v).apply() })
    private fun real(k: String, p: Float) = Pref(k, p, { c, d -> sp.getFloat(c, d) }, { c, v -> sp.edit().putFloat(c, v).apply() })
    private fun bool(k: String, p: Boolean) = Pref(k, p, { c, d -> sp.getBoolean(c, d) }, { c, v -> sp.edit().putBoolean(c, v).apply() })
    private fun longo(k: String, p: Long) = Pref(k, p, { c, d -> sp.getLong(c, d) }, { c, v -> sp.edit().putLong(c, v).apply() })

    val temaP = texto("tema", "OLED")
    val tamanhoP = real("tamanho", 18f)
    val entrelinhas = real("entrelinhas", 1.7f)
    val margem = real("margem", 24f)
    val fonte = texto("fonte", "SERIFA") // SERIFA | SEM_SERIFA
    val destacarFalas = bool("falas", true)
    val modoPaginas = bool("paginas", false)
    val telaLigada = bool("telaLigada", true)
    val velocidadeVoz = real("voz", 1.0f)
    val narrador = texto("narrador", "")
    val pastaBackup = texto("pastaBackup", "")
    val ultimoBackup = longo("ultimoBackup", 0L)
    val backupAuto = bool("backupAuto", false)

    // atalhos usados pelo resto do app
    val tema: StateFlow<String> get() = temaP.estado
    fun definirTema(nome: String) = temaP.definir(nome)
    val tamanho: StateFlow<Float> get() = tamanhoP.estado
    fun definirTamanho(v: Float) = tamanhoP.definir(v)
}
