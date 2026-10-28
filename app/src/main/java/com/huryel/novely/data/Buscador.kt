package com.huryel.novely.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield

data class Achado(
    val item: ItemIndice,
    val ocorrencia: Int,   // n-ésima ocorrência dentro do capítulo (para pular direto até ela)
    val trecho: String,
    val inicio: Int,       // posição do termo dentro do trecho
    val tamanho: Int,
    val noTitulo: Boolean = false,
)

data class ProgressoBusca(val achados: List<Achado>, val lidos: Int, val total: Int, val terminou: Boolean)

/**
 * Busca local sem índice separado: lê um capítulo por vez do armazenamento, compara sem acentos
 * e sem maiúsculas, e entrega resultados aos poucos. Escolhido porque não duplica o texto da
 * biblioteca em disco e nunca fica desatualizado depois de uma edição; nos testes de desempenho,
 * varrer 100 mil palavras leva poucos milissegundos.
 */
class Buscador(private val repo: Repositorio) {

    fun buscar(consulta: String, obraId: Long?, soTitulos: Boolean, limite: Int = 400): Flow<ProgressoBusca> = flow {
        val q = Texto.normalizar(consulta.trim())
        if (q.length < 2) { emit(ProgressoBusca(emptyList(), 0, 0, true)); return@flow }
        val itens = repo.dao.indiceBusca().filter { obraId == null || it.obraId == obraId }
        val achados = ArrayList<Achado>()
        var ultimoEnvio = 0L
        for ((n, item) in itens.withIndex()) {
            val tituloN = Texto.normalizar(item.capTitulo)
            val ti = tituloN.indexOf(q)
            if (ti >= 0) achados.add(Achado(item, 0, item.capTitulo, ti, q.length, noTitulo = true))
            if (!soTitulos) {
                val texto = repo.textos.ler(item.capId)
                val norm = Texto.normalizar(texto)
                var i = norm.indexOf(q)
                var oc = 0
                while (i >= 0 && oc < 30 && achados.size < limite) {
                    val ini = (i - 70).coerceAtLeast(0).let { s -> texto.lastIndexOf(' ', s).takeIf { it in (s - 20)..s } ?: s }
                    val fim = (i + q.length + 90).coerceAtMost(texto.length)
                    val bruto = texto.substring(ini, fim)
                    // tira marcas de formatação e quebras, mantendo a posição do termo correta
                    val sb = StringBuilder(); var posTermo = -1
                    for (k in bruto.indices) {
                        if (ini + k == i) posTermo = sb.length
                        val ch = bruto[k]
                        when (ch) { '*', '#' -> {}; '\n' -> sb.append(' '); else -> sb.append(ch) }
                    }
                    val prefixo = if (ini > 0) "…" else ""
                    achados.add(Achado(item, oc, prefixo + sb.toString().trim() + if (fim < texto.length) "…" else "",
                        (posTermo.coerceAtLeast(0) + prefixo.length - (sb.length - sb.trimStart().length)).coerceAtLeast(0), q.length))
                    oc++
                    i = norm.indexOf(q, i + q.length)
                }
            }
            if (achados.size >= limite) { emit(ProgressoBusca(achados.toList(), n + 1, itens.size, true)); return@flow }
            val agora = System.currentTimeMillis()
            if (agora - ultimoEnvio > 150) { emit(ProgressoBusca(achados.toList(), n + 1, itens.size, false)); ultimoEnvio = agora }
            yield()
        }
        emit(ProgressoBusca(achados.toList(), itens.size, itens.size, true))
    }.flowOn(Dispatchers.IO)
}
