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
            if (par.trimStart().startsWith("[#") || par.trimStart().startsWith("@@")) return@forEach // efeitos do Mega Pack não criam fichas
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
    /** Tira os símbolos de efeito (!!, ^^, ((, )), [[, ]], >>, linhas ~chuva) para prévias e cartões. */
    fun limparEfeitos(s: String): String = s.lineSequence().filterNot { val l = it.trim(); l.startsWith("~") && l.length <= 24 && ' ' !in l }
        .map { var l = it.trim().removePrefix(">>>").removePrefix(">>").removePrefix("> ").removePrefix("==").removePrefix("##").removePrefix("::").removePrefix("@@").trimStart('#')
            if (l.startsWith("[+") && l.endsWith("]")) l = l.substring(2, l.length - 1)
            if (l.startsWith("[!") && l.endsWith("!]")) l = l.substring(2, l.length - 2)
            listOf("!!", "^^", "((", "))", "[[", "]]", "||", "%%", "<<", ">>", "{{", "}}", "~~", "{", "}").fold(l) { a, m -> a.replace(m, "") } }.joinToString("\n")

    private val RX_FX_INLINE = Regex("\\{[a-zçãéíóúâêô]+[+-]?(?::[a-zçãéíóúâêô]+)?:\\s([^}]*)\\}")
    private fun campos(s: String) = s.split(Regex("(?<!\\|)\\|(?!\\|)")).map { it.trim() }.filter { it.isNotEmpty() }

    /** Tira os símbolos de efeito do trecho e deixa só o texto: "{fogo+:azul: chamas}" → "chamas", "!!Corre!!" → "Corre". */
    fun semEfeitoNoTrecho(s: String): String {
        var t = RX_FX_INLINE.replace(s) { it.groupValues[1] }
        listOf("!!", "^^", "((", "))", "||", "%%", "~~").forEach { t = t.replace(it, "") }
        t = Regex("<<(.*?)>>").replace(t) { it.groupValues[1] }
        t = Regex("(?<!\\{)\\{([^{}]+)\\}(?!\\})").replace(t) { it.groupValues[1] } // {telepatia}
        return t
    }

    /**
     * "Remover efeito" de uma linha inteira: o bloco vira parágrafo comum e o texto fica.
     * "[#sistema:vermelho Você subiu | HP 30/100]" → "Você subiu. HP 30/100"; "[!Aviso!]" → "Aviso".
     */
    fun semEfeito(linha: String): String {
        val l = linha.trim()
        fun juntar(x: String) = campos(x).map { semEfeitoNoTrecho(it.removePrefix("[x]").removePrefix("[ ]").trim()) }.filter { it.isNotEmpty() }.joinToString(". ")
        return when {
            l.startsWith("[#") && l.endsWith("]") -> juntar(l.substring(2, l.length - 1).substringAfter(' ', ""))
            l.startsWith("[[") && l.endsWith("]]") -> juntar(l.substring(2, l.length - 2))
            l.startsWith("[!") && l.endsWith("!]") -> semEfeitoNoTrecho(l.substring(2, l.length - 2).trim())
            l.startsWith("{{") && l.endsWith("}}") -> juntar(l.substring(2, l.length - 2))
            l.startsWith("[+") && l.endsWith("]") -> campos(l.substring(2, l.length - 1)).firstOrNull().orEmpty()
            l.startsWith("::") -> campos(l.drop(2)).joinToString(", ")
            l.startsWith("##") && !l.startsWith("###") -> semEfeitoNoTrecho(l.trimStart('#').trim())
            l.startsWith(">>>") -> semEfeitoNoTrecho(l.drop(3).trim())
            l.startsWith(">>") -> semEfeitoNoTrecho(l.drop(2).trim())
            l.startsWith("> ") -> semEfeitoNoTrecho(l.drop(2).trim())
            l.startsWith("==") -> semEfeitoNoTrecho(l.drop(2).trim())
            else -> semEfeitoNoTrecho(l)
        }
    }

    /** O trecho do jeito que o painel de efeitos escreve: em linha "{fogo+:azul: texto}" ou parágrafo "[#sistema-:verde texto]". */
    fun escreverEfeito(nome: String, cor: String?, forca: Int, texto: String, bloco: Boolean): String {
        val cab = nome + (if (forca > 0) "+" else if (forca < 0) "-" else "") + (cor?.takeIf { it != "roxo" || !bloco }?.let { ":$it" } ?: "")
        return if (bloco) "[#$cab ${texto.trim()}]" else "{$cab: $texto}"
    }

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

    /**
     * Partes do nome que podem valer sozinhas: "Kang Seo-jun" → "Seo-jun" (e "Kang", se ninguém mais tiver Kang).
     * Uma parte só entra se aparece em UMA ficha só — assim dois personagens do mesmo clã não se misturam.
     */
    fun partesUnicas(nomes: Map<String, Long>): Map<String, Long> {
        val contagem = HashMap<String, MutableSet<Long>>()
        nomes.forEach { (nome, id) ->
            val partes = nome.trim().split(Regex("\\s+"))
            if (partes.size < 2) return@forEach
            partes.filter { it.length >= 3 && !Regex("^$LIGA$", RegexOption.IGNORE_CASE).matches(it) && !naoEhNome(it) }.forEach { contagem.getOrPut(it.lowercase()) { HashSet() }.add(id) }
        }
        val saida = HashMap<String, Long>()
        nomes.forEach { (nome, _) ->
            nome.trim().split(Regex("\\s+")).forEach { parte ->
                val ids = contagem[parte.lowercase()] ?: return@forEach
                if (ids.size == 1 && nomes.keys.none { it.equals(parte, true) }) saida[parte] = ids.first()
            }
        }
        return saida
    }

    /**
     * Palavras que nunca valem sozinhas como nome, mesmo fazendo parte de um ("Seu" de "Seu Cristóvão",
     * "Dona" de "Dona Benta"): títulos, tratamentos e palavras comuns que aparecem com maiúscula no começo da frase.
     */
    val NAO_NOME: Set<String> = setOf(
        "seu", "sua", "seus", "suas", "dona", "dom", "senhor", "senhora", "sr", "sra", "srta", "sinhá", "sinhô", "nhô", "nhá", "dr", "dra", "doutor", "doutora",
        "mestre", "mestra", "rei", "rainha", "príncipe", "princesa", "lorde", "lady", "sir", "madame", "dama", "duque", "duquesa", "conde", "condessa", "barão", "baronesa",
        "capitão", "general", "coronel", "sargento", "tenente", "comandante", "chefe", "líder", "padre", "frei", "irmã", "irmão", "santo", "santa", "são",
        "tio", "tia", "avô", "avó", "vovô", "vovó", "pai", "mãe", "velho", "velha", "jovem", "pequeno", "pequena", "grande", "menino", "menina", "moço", "moça",
        "professor", "professora", "jovem-mestre", "anciã", "ancião", "imperador", "imperatriz", "deus", "deusa", "lord", "miss", "mister",
        "para", "como", "mais", "mas", "que", "ele", "ela", "eles", "elas", "isso", "isto", "este", "esta", "esse", "essa", "aquele", "aquela", "pelo", "pela",
        "uma", "com", "sem", "por", "sobre", "entre", "até", "ainda", "também", "muito", "quando", "onde", "porque", "então", "depois", "antes", "sempre",
        "nunca", "nada", "tudo", "cada", "outro", "outra", "mesmo", "mesma", "dele", "dela", "meu", "minha", "você", "não", "sim", "bem", "qual", "quem",
        "noite", "dia", "sol", "lua", "rosa", "flor", "pedra", "lobo", "corvo", "anjo", "morte", "vida", "sombra", "luz",
    )
    fun naoEhNome(palavra: String) = palavra.trim().lowercase() in NAO_NOME

    /** O nome começa uma frase? (antes dele só aspas/travessão/espaço desde o início da linha ou de um . ! ? …) */
    fun inicioDeFrase(texto: CharSequence, inicio: Int): Boolean {
        var i = inicio - 1
        while (i >= 0 && (texto[i].isWhitespace() || texto[i] in "—–-―\"“”«»'‘’([*_")) i--
        return i < 0 || texto[i] in ".!?…:"
    }

    /**
     * Confiança (0–100) de que a palavra achada no texto é mesmo a ficha.
     * Nome completo ou apelido escrito pelo autor: 100. Parte única do nome no meio da frase: 70 (sublinhado leve).
     * Parte do nome no começo da frase: 45 (pode ser só a palavra com maiúscula). Termo que o autor marcou como "não é": 0.
     */
    fun confianca(texto: CharSequence, inicio: Int, termo: String, cadastrado: Boolean, ignorado: Boolean = false): Int = when {
        ignorado -> 0
        cadastrado -> if (' ' !in termo.trim() && naoEhNome(termo) && inicioDeFrase(texto, inicio)) 45 else 100
        naoEhNome(termo) -> 0
        inicioDeFrase(texto, inicio) -> 45
        else -> 70
    }
    const val LIMIAR_FORTE = 80
    const val LIMIAR_LEVE = 50

    /**
     * Nomes próprios no texto que ainda não têm ficha: palavras com maiúscula no MEIO da frase
     * (depois de minúscula ou vírgula), contadas. Ex.: "… disse Seo-jun …" → Seo-jun.
     */
    fun nomesSemFicha(texto: String, existentes: Collection<String>, minimo: Int = 3): List<Pair<String, Int>> {
        val ja = existentes.map { it.lowercase() }.toSet()
        val rx = Regex("(?<=[\\p{Ll},;:)”]\\s|—\\s)(\\p{Lu}[\\p{L}'’]+(?:-[\\p{L}'’]+)*(?:\\s\\p{Lu}[\\p{L}'’]+(?:-[\\p{L}'’]+)*)?)")
        val cont = HashMap<String, Int>()
        limpar(texto).split('\n').forEach { l -> rx.findAll(l).forEach { m -> cont[m.value] = (cont[m.value] ?: 0) + 1 } }
        // junta "Kang Seo-jun" e "Seo-jun": se a forma longa existe, conta para ela também
        return cont.filter { (n, c) -> c >= minimo && n.lowercase() !in ja && n.split(' ').none { it.lowercase() in ja } && n.lowercase() !in COMUNS }
            .toList().sortedByDescending { it.second }.take(40)
    }
    private val COMUNS = setOf("deus", "senhor", "senhora", "mestre", "rei", "rainha", "príncipe", "princesa", "dona", "dom", "não", "sim", "então")

    /** Regex para achar nomes de fichas no texto do leitor (hífen e apóstrofo contam como parte do nome). */
    /**
     * Um nome para a regra de destaque. Palavra solta (sobrenome/parte do nome, apelido de uma palavra) não vale
     * quando vem colada a outro nome com maiúscula: em "Gwon Jang-ho", o "Gwon" é de outra pessoa, não do Gwon Mu-hyeok.
     */
    fun alternativa(nome: String): String = Regex.escape(nome) + if (' ' in nome.trim()) "" else "(?!\\s\\p{Lu}\\p{Ll})"

    /** Nome de ficha com epíteto ("Gwon Mu-hyeok, o Demônio do Céu do Norte") → nome e apelido separados. */
    fun separarEpiteto(nome: String): Pair<String, String?> {
        val i = nome.indexOf(',')
        if (i <= 0) return nome.trim() to null
        val resto = nome.substring(i + 1).trim().replace(Regex("^(o|a|os|as)\\s+", RegexOption.IGNORE_CASE), "").trim()
        return nome.substring(0, i).trim() to resto.ifEmpty { null }
    }

    fun regexNomes(nomes: Collection<String>): Regex? {
        val l = nomes.map { it.trim() }.filter { it.length >= 2 }.distinct().sortedByDescending { it.length }
        if (l.isEmpty()) return null
        return Regex("(?<![\\p{L}\\p{N}'’-])(" + l.joinToString("|") { alternativa(it) } + ")(?![\\p{L}\\p{N}]|[-'’]\\p{L})")
    }
}
