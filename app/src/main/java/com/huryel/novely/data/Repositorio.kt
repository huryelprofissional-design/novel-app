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

    suspend fun novoCapitulo(obraId: Long, titulo: String, volumeId: Long?): Long {
        val id = dao.inserirCapitulo(
            Capitulo(obraId = obraId, volumeId = volumeId, titulo = titulo.trim().ifEmpty { "Novo capítulo" },
                ordem = dao.maiorOrdemCapitulo(obraId) + 1)
        )
        textos.gravar(id, "")
        dao.tocarObra(obraId, System.currentTimeMillis())
        return id
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

/** Preferências simples (tema etc.) guardadas no aparelho. */
class Preferencias(ctx: Context) {
    private val sp = ctx.getSharedPreferences("novely_prefs", Context.MODE_PRIVATE)
    private val _tema = MutableStateFlow(sp.getString("tema", "OLED") ?: "OLED")
    val tema: StateFlow<String> = _tema
    fun definirTema(nome: String) { sp.edit().putString("tema", nome).apply(); _tema.value = nome }

    private val _tamanho = MutableStateFlow(sp.getFloat("tamanho", 18f))
    val tamanho: StateFlow<Float> = _tamanho
    fun definirTamanho(v: Float) { sp.edit().putFloat("tamanho", v).apply(); _tamanho.value = v }
}
