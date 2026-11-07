package com.huryel.novely.data

/**
 * Campos de texto livre da ficha, uma linha por item:
 *  - extras:    "Arma: Espada Rúnica"
 *  - condições: "1: vivo" · "12: ferido" · "30: morto"  (aceita também "cap 12: ferido")
 *  - segredos:  "15: é filho do rei"                    (o leitor só vê a partir do capítulo 15)
 */
object CamposFicha {
    fun extras(s: String): List<Pair<String, String>> = s.lines().mapNotNull { l ->
        val i = l.indexOf(':'); if (i <= 0) null else l.substring(0, i).trim().takeIf { it.isNotEmpty() }?.let { it to l.substring(i + 1).trim() }
    }.filter { it.second.isNotEmpty() }

    private val RX_CAP = Regex("^\\s*(?:cap(?:[ií]tulo)?\\.?\\s*)?(\\d{1,4})\\s*[:—–-]\\s*(.+)$", RegexOption.IGNORE_CASE)
    /** "12: ferido" → (12, "ferido"), em ordem de capítulo. */
    fun porCapitulo(s: String): List<Pair<Int, String>> = s.lines().mapNotNull { RX_CAP.find(it)?.let { m -> m.groupValues[1].toInt() to m.groupValues[2].trim() } }.sortedBy { it.first }

    /** Condição valendo no capítulo [cap] (a última anotada até ele). */
    fun condicaoEm(s: String, cap: Int): String? = porCapitulo(s).lastOrNull { it.first <= cap }?.second

    /** Segredos já revelados no capítulo [cap] e quantos ainda faltam. */
    fun segredosAte(s: String, cap: Int): Pair<List<Pair<Int, String>>, Int> { val t = porCapitulo(s); return t.filter { it.first <= cap } to t.count { it.first > cap } }
}

/** Marcadores e grifos presos ao texto: se o autor inseriu ou tirou parágrafos, acha de novo o parágrafo pelo trecho guardado. */
object Ancora {
    fun ancorar(planos: List<String>, bloco: Int, trecho: String): Int {
        val chave = trecho.trim().take(60)
        if (chave.length < 8) return bloco
        if (bloco in planos.indices && planos[bloco].trim().startsWith(chave)) return bloco
        val achados = planos.indices.filter { planos[it].contains(chave) }
        return achados.minByOrNull { kotlin.math.abs(it - bloco) } ?: bloco
    }
}
