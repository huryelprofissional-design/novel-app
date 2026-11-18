package com.huryel.novely.data

import com.huryel.novely.NovelyApp

/**
 * Os efeitos de escrita saíram do Novely. Os símbolos que já estavam nos textos viram texto comum
 * (mesma regra do PC, web/limpeza.js). As obras do autor são limpas uma vez, com uma versão guardada antes;
 * obras de outras pessoas são limpas na hora de ler.
 */
object Limpeza {
    private fun campos(s: String) = s.split(Regex("(?<!\\|)\\|(?!\\|)")).map { it.trim() }.filter { it.isNotEmpty() }
    private val RX_INLINE = Regex("\\{\\$?[a-zçãéíóúâêô]+[+-]?(?::[a-zçãéíóúâêô]+)?:\\s([^{}]*)\\}")
    private val PARES = listOf("!!" to "!!", "\\^\\^" to "\\^\\^", "\\(\\(" to "\\)\\)", "\\|\\|" to "\\|\\|", "%%" to "%%", "~~" to "~~", "<<" to ">>")
        .map { (a, b) -> Regex("$a(.+?)$b") }
    private val TELEPATIA = Regex("(?<!\\{)\\{([^{}\\n]+)\\}(?!\\})")
    private val RANK = Regex("\\[(SSS|SS|S|A|B|C|D|E|F)\\+?\\]")
    private val NOTA = Regex("^(N\\.\\s?A\\.|Nota do autor)\\s*:?\\s*", RegexOption.IGNORE_CASE)

    fun limparTrecho(s: String): String {
        // os efeitos novos (Fx.kt) ficam; só os símbolos antigos somem
        var t = RX_INLINE.replace(s) { m -> if (Fx.conhecido(m.value.drop(1).substringBefore(':').trimStart('$'))) m.value else m.groupValues[1] }
        PARES.forEach { rx -> t = rx.replace(t) { it.groupValues[1] } }
        t = TELEPATIA.replace(t) { m -> if (':' in m.groupValues[1] && Fx.conhecido(m.groupValues[1].substringBefore(':'))) m.value else m.groupValues[1] }
        return RANK.replace(t) { it.groupValues[1] }
    }

    /** Uma linha: o texto comum, ou null quando a linha só existia para o efeito. */
    fun limparLinha(l0: String): String? {
        val l = l0.trim(); if (l.isEmpty()) return l0
        if (Fx.lerLinha(l) != null) return l0
        if (l.startsWith("~") && l.length in 2..23 && ' ' !in l) return null
        fun juntar(x: String) = campos(x).map { limparTrecho(it.replace(Regex("^\\[[xX✓ ]?]\\s*"), "")) }.filter { it.isNotEmpty() }.joinToString(". ").ifEmpty { null }
        return when {
            (l.startsWith("[#") || l.startsWith("[$")) && l.length > 2 && !l[2].isWhitespace() && l.endsWith("]") -> l.indexOf(' ').let { if (it < 0) null else juntar(l.substring(it + 1, l.length - 1)) }
            l.startsWith("[[") && l.endsWith("]]") -> juntar(l.substring(2, l.length - 2))
            l.startsWith("[!") && l.endsWith("!]") && l.length > 4 -> limparTrecho(l.substring(2, l.length - 2).trim())
            l.startsWith("{{") && l.endsWith("}}") -> juntar(l.substring(2, l.length - 2))
            l.startsWith("[+") && l.endsWith("]") -> campos(l.substring(2, l.length - 1)).firstOrNull()
            l.startsWith("::") -> campos(l.drop(2)).joinToString(", ").ifEmpty { null }
            l.startsWith("##") && !l.startsWith("###") -> limparTrecho(l.trimStart('#').trim())
            l.startsWith("@@") -> null
            NOTA.containsMatchIn(l) -> limparTrecho(l.replace(NOTA, ""))
            l.startsWith(">>>") -> limparTrecho(l.drop(3).trim())
            l.startsWith(">>") -> limparTrecho(l.drop(2).trim())
            l.startsWith("> ") -> limparTrecho(l.drop(2).trim())
            l.startsWith("==") -> limparTrecho(l.drop(2).trim())
            else -> l0.replace(l, limparTrecho(l))
        }
    }

    fun limparTexto(t: String): String = t.lines().mapNotNull { limparLinha(it) }.joinToString("\n")

    /** Limpa uma vez cada obra do autor (guarda uma versão antes de mudar um capítulo). */
    suspend fun limparObrasUmaVez() {
        val app = NovelyApp.instancia; val repo = app.repo
        for (o in repo.dao.todasObras().filter { it.minha && !it.trancada }) {
            val marca = "efeitosLimpos/${o.id}"
            if (app.prefs.jaContado(marca)) continue
            try {
                for (c in repo.dao.capitulosAgora(o.id)) {
                    val t = repo.lerTexto(c.id); val novo = limparTexto(t)
                    if (novo != t) { repo.textos.gravarVersao(c.id, t, "Antes de tirar os efeitos"); repo.gravarTexto(c, novo) }
                }
                app.prefs.marcarContado(marca)
            } catch (_: Exception) { /* tenta na próxima vez */ }
        }
    }
}
