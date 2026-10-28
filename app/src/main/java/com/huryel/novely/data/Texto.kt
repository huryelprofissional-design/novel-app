package com.huryel.novely.data

/**
 * Funções puras de texto (sem Android) — testadas em app/src/test com textos sintéticos.
 */
object Texto {

    private val regexCapitulo = Regex(
        """^\s*(cap[ií]tulo|chapter|cap\.?|ch\.?|parte|part)\s*[0-9ivxlcdm]+\b.{0,100}$""",
        RegexOption.IGNORE_CASE,
    )
    private val regexEspecial = Regex("""^\s*(pr[oó]logo|ep[ií]logo|prologue|epilogue|interl[uú]dio|interlude)\b.{0,100}$""", RegexOption.IGNORE_CASE)
    private val regexMarkdown = Regex("""^\s*#{1,3}\s+\S.{0,120}$""")

    /** A linha parece um título de capítulo? */
    fun ehTituloCapitulo(linha: String, prefixoPersonalizado: String? = null): Boolean {
        val l = linha.trim()
        if (l.isEmpty() || l.length > 130) return false
        if (prefixoPersonalizado != null) return l.startsWith(prefixoPersonalizado, ignoreCase = true)
        return regexCapitulo.matches(l) || regexEspecial.matches(l) || regexMarkdown.matches(l)
    }

    /** "PRÓLOGO" / "EPÍLOGO" / "INTERLÚDIO" quando o título é especial; senão null. */
    fun tipoEspecial(titulo: String): String? {
        val t = normalizar(titulo.trim())
        return when {
            t.startsWith("prologo") || t.startsWith("prologue") -> "PRÓLOGO"
            t.startsWith("epilogo") || t.startsWith("epilogue") -> "EPÍLOGO"
            t.startsWith("interludio") || t.startsWith("interlude") -> "INTERLÚDIO"
            else -> null
        }
    }

    /** Remove o travessão/hífen solto no fim: "Prólogo —" vira "Prólogo". */
    fun tituloLimpo(linha: String) = limparTitulo(linha).trimEnd('—', '–', '-', ':', ' ').ifEmpty { limparTitulo(linha) }

    fun limparTitulo(linha: String) = linha.trim().trimStart('#').trim().take(120)

    fun contarPalavras(texto: CharSequence): Int {
        var n = 0
        var dentro = false
        for (ch in texto) {
            val letra = ch.isLetterOrDigit()
            if (letra && !dentro) n++
            dentro = letra
        }
        return n
    }

    /**
     * Divide um texto grande em pedaços de ~[alvo] caracteres, sempre em quebras de linha,
     * de forma que juntar os pedaços devolva EXATAMENTE o texto original.
     */
    fun dividirEmPedacos(texto: String, alvo: Int = 4000): List<String> {
        if (texto.length <= alvo) return listOf(texto)
        val saida = ArrayList<String>(texto.length / alvo + 1)
        var inicio = 0
        while (inicio < texto.length) {
            var fim = (inicio + alvo).coerceAtMost(texto.length)
            if (fim < texto.length) {
                val nl = texto.indexOf('\n', fim)
                fim = if (nl == -1 || nl - fim > alvo * 2) {
                    // linha gigante sem quebra: corta em fim de frase ou espaço
                    val limite = (inicio + alvo * 2).coerceAtMost(texto.length)
                    var corte = -1
                    for (i in fim until limite) { val c = texto[i]; if (c == '.' || c == '!' || c == '?') { corte = i + 1; break } }
                    if (corte == -1) corte = texto.lastIndexOf(' ', limite).takeIf { it > inicio } ?: limite
                    corte
                } else nl + 1
            }
            saida.add(texto.substring(inicio, fim))
            inicio = fim
        }
        return saida
    }

    /** Remove acentos e caixa para busca ("Coração" encontra "coracao"). */
    fun normalizar(s: CharSequence): String {
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(semAcento(c.lowercaseChar()))
        return sb.toString()
    }

    fun semAcento(c: Char): Char = when (c) {
        'á', 'à', 'â', 'ã', 'ä' -> 'a'
        'é', 'è', 'ê', 'ë' -> 'e'
        'í', 'ì', 'î', 'ï' -> 'i'
        'ó', 'ò', 'ô', 'õ', 'ö' -> 'o'
        'ú', 'ù', 'û', 'ü' -> 'u'
        'ç' -> 'c'
        'ñ' -> 'n'
        else -> c
    }

    private val regexTags = Regex("<[^>]+>")
    private val regexEspacos = Regex("[ \\t\\u00A0]+")
    private val regexEntidadeNum = Regex("&#(x?)([0-9a-fA-F]+);")

    /** Converte XHTML de EPUB em Markdown simples do Novely (parágrafos, títulos, itálico, negrito, cenas). */
    fun htmlParaMarkdown(htmlBruto: String): String {
        var h = htmlBruto
        val corpo = Regex("<body[^>]*>(.*)</body>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(h)
        if (corpo != null) h = corpo.groupValues[1]
        h = h.replace(Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        h = h.replace(Regex("[\\r\\n]+"), " ")
        h = h.replace(Regex("<h[1-3][^>]*>", RegexOption.IGNORE_CASE), "\n\n# ")
        h = h.replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), "\n\n")
        h = h.replace(Regex("<hr[^>]*>", RegexOption.IGNORE_CASE), "\n\n* * *\n\n")
        h = h.replace(Regex("<br[^>]*>", RegexOption.IGNORE_CASE), "\n")
        h = h.replace(Regex("</(p|div|li|blockquote|tr)>", RegexOption.IGNORE_CASE), "\n\n")
        h = h.replace(Regex("</?(em|i)(\\s[^>]*)?>", RegexOption.IGNORE_CASE), "*")
        h = h.replace(Regex("</?(strong|b)(\\s[^>]*)?>", RegexOption.IGNORE_CASE), "**")
        h = regexTags.replace(h, "")
        h = decodificarEntidades(h)
        val linhas = h.split('\n').map { regexEspacos.replace(it, " ").trim() }
        val sb = StringBuilder()
        var vazias = 0
        for (l in linhas) {
            val limpa = if (l == "*" || l == "**" || l == "* *") "" else l
            if (limpa.isEmpty()) { vazias++; continue }
            if (sb.isNotEmpty()) sb.append(if (vazias > 0) "\n\n" else "\n")
            sb.append(limpa)
            vazias = 0
        }
        return sb.toString()
    }

    fun decodificarEntidades(s: String): String {
        var r = s.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'").replace("&#39;", "'")
            .replace("&mdash;", "—").replace("&ndash;", "–").replace("&hellip;", "…")
            .replace("&ldquo;", "“").replace("&rdquo;", "”").replace("&lsquo;", "‘").replace("&rsquo;", "’")
        r = regexEntidadeNum.replace(r) { m ->
            val n = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isNotEmpty()) 16 else 10)
            if (n != null && n in 1..0x10FFFF) String(Character.toChars(n)) else ""
        }
        return r.replace("&amp;", "&")
    }

    fun escaparXml(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /** Markdown do Novely → XHTML (para exportar EPUB). */
    fun markdownParaXhtml(md: String): String {
        val sb = StringBuilder()
        for (bruta in md.lineSequence()) {
            val l = bruta.trim()
            when {
                l.isEmpty() -> {}
                l == "* * *" || l == "***" || l == "---" || l == "◆" -> sb.append("<hr/>\n")
                l.startsWith("#") -> sb.append("<h2>").append(inline(escaparXml(l.trimStart('#').trim()))).append("</h2>\n")
                else -> sb.append("<p>").append(inline(escaparXml(l))).append("</p>\n")
            }
        }
        return sb.toString()
    }

    private fun inline(s: String): String {
        var r = Regex("\\*\\*(.+?)\\*\\*").replace(s) { "<strong>${it.groupValues[1]}</strong>" }
        r = Regex("\\*(.+?)\\*").replace(r) { "<em>${it.groupValues[1]}</em>" }
        return r
    }
}
