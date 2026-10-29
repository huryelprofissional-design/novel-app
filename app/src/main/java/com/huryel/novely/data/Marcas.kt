package com.huryel.novely.data

/**
 * Personagens e lugares escritos direto no texto:
 *   @Lin Feng (Lin, Herdeiro), o último herdeiro do clã Yun, entrou…   → define/acrescenta à ficha do personagem
 *   Vinha de &Hwanseo, o reino das montanhas…                           → define o lugar
 *   — perguntou @Lin Feng.                                             → só menciona (cria a ficha se não existir)
 * É "definição" quando o nome vem seguido de vírgula, travessão, dois-pontos ou (apelidos).
 * No leitor e nas exportações as marcas somem: fica só "Lin Feng, o último herdeiro…".
 */
object Marcas {
    data class Marca(val lugar: Boolean, val nome: String, val apelidos: List<String>, val definicao: Boolean)

    private const val PALAVRA = "[\\p{L}\\p{N}'’-]*"
    private const val NOME = "\\p{L}$PALAVRA(?:\\s(?:(?:d[aeo]s?|von|van)\\s)?\\p{Lu}$PALAVRA){0,4}"
    val REGRA = Regex("(?<![\\p{L}\\p{N}@&])([@&])($NOME)(\\s*\\(([^)\\n]{1,80})\\))?(\\s*[,—–:])?")

    /** Marcas de um parágrafo, com o parágrafo limpo (para virar trecho da ficha quando for definição). */
    fun encontrar(texto: String): List<Pair<Marca, String?>> {
        if ('@' !in texto && '&' !in texto) return emptyList()
        val saida = ArrayList<Pair<Marca, String?>>()
        texto.split('\n').forEach { par ->
            if ('@' !in par && '&' !in par) return@forEach
            val limpo by lazy { limpar(par).trim() }
            REGRA.findAll(par).forEach { m ->
                val apelidos = m.groupValues[4].split(',').map { it.trim() }.filter { it.isNotEmpty() }
                val def = m.groupValues[5].isNotBlank() || apelidos.isNotEmpty()
                saida.add(Marca(m.groupValues[1] == "&", m.groupValues[2].trim(), apelidos, def) to (if (def) limpo else null))
            }
        }
        return saida
    }

    /** Tira as marcas para leitura: "@Lin Feng (Lin), o" → "Lin Feng, o". */
    fun limpar(s: String): String {
        if ('@' !in s && '&' !in s) return s
        return REGRA.replace(s) { m -> m.groupValues[2] + m.groupValues[5] }
    }

    /** Palavra com @ ou & sendo digitada logo antes do cursor (para sugerir nomes). */
    fun digitando(textoAteCursor: String): Pair<Char, String>? {
        val m = Regex("(?:^|[\\s(“\"—–-])([@&])([\\p{L}\\p{N}'’ ]{0,30})$").find(textoAteCursor) ?: return null
        val termo = m.groupValues[2]
        if (termo.endsWith("  ") || termo.count { it == ' ' } > 3) return null
        return m.groupValues[1][0] to termo
    }
}
