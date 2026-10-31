package com.huryel.novely.data

/**
 * Personagens e lugares escritos direto no texto:
 *   @Lin Feng (Lin, Herdeiro), o último herdeiro do clã Yun, entrou…   → define/acrescenta à ficha do personagem
 *   @Lin Feng - o último herdeiro…  /  @Lin Feng — …  /  @Lin Feng: …  → também é definição
 *   @[Seo-yeon do clã Baek], …                                         → nome com qualquer palavra (entre colchetes)
 *   Vinha de &Hwanseo, o reino das montanhas…                           → define o lugar
 *   — perguntou @Lin Feng.                                             → só menciona (cria a ficha se não existir)
 *
 * Para não cortar nem misturar nomes:
 *  - nomes que JÁ existem nas fichas (e apelidos) têm prioridade: "@Lin Feng Entrou" vira "Lin Feng";
 *  - hífen e apóstrofo fazem parte do nome (Ha-eun, D'Ávila); " - " com espaços é separador;
 *  - colchetes deixam o autor escrever exatamente o nome que quiser.
 * No leitor e nas exportações as marcas somem: fica só "Lin Feng, o último herdeiro…".
 */
object Marcas {
    data class Marca(val lugar: Boolean, val nome: String, val apelidos: List<String>, val definicao: Boolean)

    private const val PALAVRA = "[\\p{L}\\p{N}'’]*(?:-[\\p{L}\\p{N}'’]+)*"
    private const val LIGA = "(?:d[aeo]s?|de|di|del|della|du|la|le|van|von|der|den|bin|ibn|al)"
    private const val NOME = "\\p{L}$PALAVRA(?:\\s(?:$LIGA\\s)?\\p{Lu}$PALAVRA){0,4}"
    private const val DEPOIS = "(\\s*\\(([^)\\n]{1,80})\\))?(\\s*[,—–:;]|\\s+-(?=\\s))?"
    private const val ANTES = "(?<![\\p{L}\\p{N}@&])([@&])"

    /** Grupos: 1 símbolo · 2 nome entre colchetes · 3 nome conhecido · 4 nome comum · 5 (apelidos) · 6 apelidos · 7 separador */
    private fun regra(conhecidos: Collection<String>): Regex {
        val alt = conhecidos.map { it.trim() }.filter { it.length >= 2 }.distinct().sortedByDescending { it.length }
        val conhecido = if (alt.isEmpty()) "(?!)" else alt.joinToString("|") { Regex.escape(it) }
        return Regex("$ANTES(?:\\[([^\\]\\n]{1,60})\\]|((?iu:$conhecido))(?![\\p{L}\\p{N}'’-])|($NOME))$DEPOIS")
    }
    private val REGRA_BASE = regra(emptyList())

    private fun nomeDe(m: MatchResult) = (m.groups[2] ?: m.groups[3] ?: m.groups[4])!!.value.trim()
    private fun faixaNome(m: MatchResult) = (m.groups[2] ?: m.groups[3] ?: m.groups[4])!!.range

    /** Marcas de um texto, com o parágrafo limpo (vira trecho da ficha quando for definição). [conhecidos] = nomes e apelidos das fichas. */
    fun encontrar(texto: String, conhecidos: Collection<String> = emptyList()): List<Pair<Marca, String?>> {
        if ('@' !in texto && '&' !in texto) return emptyList()
        val rx = if (conhecidos.isEmpty()) REGRA_BASE else regra(conhecidos)
        val porMinusculo = conhecidos.associateBy { it.trim().lowercase() }
        val saida = ArrayList<Pair<Marca, String?>>()
        texto.split('\n').forEach { par ->
            if ('@' !in par && '&' !in par) return@forEach
            val limpo by lazy { limpar(par).trim() }
            rx.findAll(par).forEach { m ->
                var nome = nomeDe(m)
                // conhecido: usa a grafia da ficha
                if (m.groups[3] != null) nome = porMinusculo[nome.lowercase()]?.trim() ?: nome
                // nome comum: corrige maiúsculas acidentais no fim ("@Lin Feng Entrou" sem ficha ainda fica como veio)
                val apelidos = m.groupValues[6].split(',').map { it.trim() }.filter { it.isNotEmpty() }
                val def = m.groupValues[7].isNotBlank() || apelidos.isNotEmpty()
                if (nome.isNotEmpty()) saida.add(Marca(m.groupValues[1] == "&", nome, apelidos, def) to (if (def) limpo else null))
            }
        }
        return saida
    }

    /** Tira as marcas para leitura: "@Lin Feng (Lin), o" → "Lin Feng, o"; "@[Seo-yeon do clã], x" → "Seo-yeon do clã, x". */
    fun limpar(s: String): String {
        if ('@' !in s && '&' !in s) return s
        return REGRA_BASE.replace(s) { m -> nomeDe(m) + m.groupValues[7] }
    }

    /** Partes para colorir no editor: (é lugar, do símbolo até o fim do nome, faixa dos apelidos). */
    fun faixas(t: String): List<Triple<Boolean, IntRange, IntRange?>> {
        if ('@' !in t && '&' !in t) return emptyList()
        return REGRA_BASE.findAll(t).map { m ->
            val fim = faixaNome(m).last + if (m.groups[2] != null) 2 else 1
            Triple(m.groupValues[1] == "&", m.range.first until fim, m.groups[5]?.range)
        }.toList()
    }

    /** Como escrever a marca de um nome: com colchetes se o nome tiver palavra que o app não pegaria sozinho. */
    fun marcar(simbolo: Char, nome: String): String =
        if (Regex("^$NOME$").matches(nome.trim())) "$simbolo${nome.trim()}" else "$simbolo[${nome.trim()}]"

    /** Palavra com @ ou & sendo digitada logo antes do cursor (para sugerir nomes). */
    fun digitando(textoAteCursor: String): Pair<Char, String>? {
        val m = Regex("(?:^|[\\s(“\"—–-])([@&])\\[?([\\p{L}\\p{N}'’ -]{0,30})$").find(textoAteCursor) ?: return null
        val termo = m.groupValues[2]
        if (termo.endsWith("  ") || termo.count { it == ' ' } > 3) return null
        return m.groupValues[1][0] to termo
    }

    /** Regex para achar nomes de fichas no texto do leitor (hífen e apóstrofo contam como parte do nome). */
    fun regexNomes(nomes: Collection<String>): Regex? {
        val l = nomes.map { it.trim() }.filter { it.length >= 2 }.distinct().sortedByDescending { it.length }
        if (l.isEmpty()) return null
        return Regex("(?<![\\p{L}\\p{N}'’-])(" + l.joinToString("|") { Regex.escape(it) } + ")(?![\\p{L}\\p{N}]|[-'’]\\p{L})")
    }
}
