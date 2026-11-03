package com.huryel.novely.data

/**
 * Bíblia viva: lê os capítulos no próprio aparelho (sem IA, sem internet) e monta
 * quem aparece onde, quem fala, fatos (idade, olhos, cabelo, ferimentos, mortes, nível de poder,
 * juramentos), com quem cada personagem mais divide cena, e aponta possíveis furos de continuidade.
 *
 * Tudo por regras de texto em português: é um assistente, não um juiz. Cada alerta mostra a frase
 * para o autor decidir, e pode ser ignorado.
 */
object Biblia {
    data class Cap(val id: Long, val titulo: String, val texto: String)

    enum class Tipo(val nome: String) {
        IDADE("Idade"), OLHOS("Olhos"), CABELO("Cabelo"), FERIMENTO("Ferimento"), MORTE("Morte"),
        PODER("Nível"), JURAMENTO("Juramento"), LUGAR("Esteve em"),
    }
    data class Fato(val fichaId: Long, val cap: Int, val tipo: Tipo, val valor: String, val frase: String)

    enum class Nivel { FURO, ATENCAO, DICA }
    data class Alerta(
        val nivel: Nivel, val titulo: String, val detalhe: String, val cap: Int, val fichaId: Long?, val frase: String, val chave: String,
    )
    data class Relacao(val a: Long, val b: Long, val cenas: Int, val primeiro: Int, val ultimo: Int, val tipo: String, val porCap: IntArray = IntArray(0))
    data class Retrato(
        val ficha: Ficha, val porCap: IntArray, val falas: Int, val fatos: List<Fato>, val lugares: List<Pair<Long, Int>>,
    ) {
        val total get() = porCap.sum()
        val capsComEle get() = porCap.count { it > 0 }
        val primeiro get() = porCap.indexOfFirst { it > 0 }
        val ultimo get() = porCap.indexOfLast { it > 0 }
    }
    data class Resultado(val caps: List<Cap>, val retratos: List<Retrato>, val relacoes: List<Relacao>, val alertas: List<Alerta>) {
        fun ficha(id: Long) = retratos.firstOrNull { it.ficha.id == id }?.ficha
    }

    // ───────── vocabulário ─────────
    private const val L = "\\p{L}\\p{N}"
    private val FRASES = Regex("(?<=[.!?…])[\"”»]?\\s+")
    private val VERBO_FALA = "disse|diz|perguntou|pergunta|respondeu|responde|gritou|grita|murmurou|sussurrou|falou|fala|retrucou|rosnou|riu|completou|" +
        "continuou|explicou|insistiu|berrou|resmungou|exclamou|declarou|rebateu|admitiu|repetiu|chamou|cortou|provocou|zombou|suspirou|ordenou|avisou"
    private val RX_VERBO_FALA = Regex("\\b($VERBO_FALA)\\b")

    private val NUMEROS = mapOf(
        "um" to 1, "uma" to 1, "dois" to 2, "duas" to 2, "três" to 3, "tres" to 3, "quatro" to 4, "cinco" to 5, "seis" to 6, "sete" to 7, "oito" to 8,
        "nove" to 9, "dez" to 10, "onze" to 11, "doze" to 12, "treze" to 13, "catorze" to 14, "quatorze" to 14, "quinze" to 15, "dezesseis" to 16,
        "dezessete" to 17, "dezoito" to 18, "dezenove" to 19, "vinte" to 20, "trinta" to 30, "quarenta" to 40, "cinquenta" to 50, "sessenta" to 60,
        "setenta" to 70, "oitenta" to 80, "noventa" to 90, "cem" to 100,
    )
    private val NUM = "\\d{1,3}|(?:${NUMEROS.keys.joinToString("|")})(?:\\s+e\\s+(?:${NUMEROS.keys.joinToString("|")}))?"
    private val PESSOA = "garot[oa]|menin[oa]|homem|mulher|rapaz|moça|jovem|velh[oa]|senhor[a]?|criança|adolescente|estudante|guerreir[oa]|" +
        "filh[oa]|irmã[o]?|lutador[a]?|soldado|aluno|aluna|monge|princes[a]|príncipe|herdeir[oa]|órfã[o]?"
    private val RX_IDADE = listOf(
        Regex("(?iu)\\b($NUM)\\s+anos\\s+de\\s+idade"),
        Regex("(?iu)\\b(?:tinha|tem|tenho|completou|completara|completava|fez|fizera|faria|completaria)\\s+(?:apenas\\s+|só\\s+|quase\\s+)?($NUM)\\s+anos\\b(?!\\s+de\\s+(?!idade))"),
        Regex("(?iu)\\b(?:$PESSOA)\\s+de\\s+($NUM)\\s+anos\\b"),
        Regex("(?u),\\s+de\\s+($NUM)\\s+anos\\s*[,.]"),
    )
    private val CORES = mapOf(
        "azul" to "azul", "azuis" to "azul", "verde" to "verde", "verdes" to "verde", "castanho" to "castanho", "castanhos" to "castanho",
        "castanha" to "castanho", "preto" to "preto", "pretos" to "preto", "preta" to "preto", "negro" to "preto", "negros" to "preto",
        "cinza" to "cinza", "cinzas" to "cinza", "cinzento" to "cinza", "cinzentos" to "cinza", "dourado" to "dourado", "dourados" to "dourado",
        "vermelho" to "vermelho", "vermelhos" to "vermelho", "rubros" to "vermelho", "carmesim" to "vermelho", "escarlate" to "vermelho",
        "roxo" to "roxo", "roxos" to "roxo", "violeta" to "roxo", "violetas" to "roxo", "âmbar" to "âmbar", "mel" to "âmbar",
        "prateado" to "prateado", "prateados" to "prateado", "branco" to "branco", "brancos" to "branco", "loiro" to "loiro", "loiros" to "loiro",
        "louro" to "loiro", "louros" to "loiro", "ruivo" to "ruivo", "ruivos" to "ruivo", "rosa" to "rosa", "amarelo" to "dourado", "amarelos" to "dourado",
    )
    private val COR = CORES.keys.sortedByDescending { it.length }.joinToString("|")
    private val RX_OLHOS = Regex("(?iu)\\bolhos?\\s+(?:[$L]+\\s+){0,2}?($COR)\\b")
    private val RX_CABELO = Regex("(?iu)\\b(?:cabelos?|fios|mechas|franja)\\s+(?:[$L]+\\s+){0,2}?($COR)\\b")
    private val RX_MUDA = Regex("(?iu)\\b(ficaram|ficou|tornaram|tornou|mudaram|mudou|acenderam|acendeu|lentes?|disfar\\w*|tingi\\w*|pint\\w*|transform\\w*|despert\\w*)\\b")

    private val RX_MORTE = Regex("(?iu)\\b(morreu|morrera|faleceu|foi mort[oa]|foi assassinad[oa]|est(?:á|ava) mort[oa]|caiu mort[oa]|sem vida|cadáver d[eoa]|enterraram|funeral d[eoa]|túmulo d[eoa]|deu o último suspiro)\\b")
    private val RX_NAO_MORTE = Regex("(?iu)\\b(não|nunca|quase|fingi\\w*|como se|se |talvez|achou que|achavam que|pensou que|parecia|teria|poderia|ia |vai )\\s*(?:[$L]+\\s+){0,2}$")
    private val RX_FERIDO = Regex("(?iu)\\b((?:braço|perna|costela|costelas|nariz|mão|ombro|tornozelo|pulso|mandíbula|clavícula|joelho|dedos?)\\s+(?:[$L]+\\s+){0,2}?(?:quebrad[oa]s?|fraturad[oa]s?|deslocad[oa]|esmagad[oa]s?|partid[oa]s?)|perdeu o (?:braço|olho|movimento)|ficou cego|ferid[oa] gravemente|gravemente ferid[oa]|em coma)\\b")
    private val RX_LUTA = Regex("(?iu)\\b(socou|soco|chutou|golpeou|lutou|lutava|atacou|saltou|correu|esquivou|bloqueou|agarrou|arremessou|derrubou)\\b")
    private val RX_CURA = Regex("(?iu)\\b(cur(?:ou|ado|ada|ar|ou-se)|cicatriz(?:ou|ado)|recuper|regener|tala|gesso|hospital|enfermaria|curandeir|poção|semanas depois|meses depois|dias depois)")
    private val RX_PODER = Regex("(?u)\\b(?:[Nn]ível|[Rr]ank|[Cc]lasse|[Gg]rau|[Ee]stágio|[Pp]atamar|[Íí]ndice|[Cc]amada|[Rr]eino)\\s+(?:de\\s+)?(S{1,3}|[A-F][+-]?|\\d{1,3})\\b")
    private val RX_JURA = Regex("(?iu)\\b(eu juro|juro que|eu prometo|prometo que|prometeu que|jurou que|um dia eu vou|vou te matar|vou matar|vingança|vingar)\\b")

    private val RX_SEPARADOR = Regex("^(\\*\\s*){3,}$|^[◆◇~\\-—=_]{3,}$|^#.*")
    private val PRE_CABELO = listOf("cabel", "fios", "mecha", "franja")
    private val PRE_MORTE = listOf("mort", "morre", "falec", "assassin", "sem vida", "cadáver", "enterr", "funeral", "túmulo", "suspiro")
    private val PRE_FERIDO = listOf("quebrad", "fratur", "desloc", "esmag", "partid", "perdeu o", "cego", "ferid", "coma")
    private val PRE_PODER = listOf("nível", "rank", "classe", "grau", "estágio", "patamar", "índice", "camada", "reino")
    private val PRE_JURA = listOf("jur", "prometo", "prometeu", "vou ", "vingan", "vingar")

    private val FAMILIA = Regex("(?u)\\b(irmã[o]?|irmãos|pai|mãe|filh[oa]|avô|avó|ti[oa]|prim[oa]|família|clã)\\b")
    private val ROMANCE = Regex("(?u)\\b(beij\\w*|cor(?:ou|ando)|coração acelerou|apaixon\\w*|amor|abraç\\w*|mãos dadas|ciúmes?)\\b")
    private val RIVAL = Regex("(?u)\\b(inimig\\w*|rival|odi\\w*|socou|golpe\\w*|atacou|lut\\w*|derrot\\w*|desprez\\w*|ameaç\\w*)\\b")
    private val ALIADO = Regex("(?u)\\b(amig\\w*|junt[oa]s|obrigad[oa]|ajud\\w*|trein\\w*|confi\\w*|protege\\w*|mestre|discípul\\w*|parceir\\w*)\\b")

    private fun numero(s: String): Int? {
        s.trim().toIntOrNull()?.let { return it }
        return s.lowercase().split(Regex("\\s+e\\s+")).sumOf { NUMEROS[it.trim()] ?: return null }
    }
    private fun ordemRank(v: String): Int? = v.toIntOrNull() ?: when {
        v.matches(Regex("S{1,3}")) -> 10 + v.length
        v[0] in 'A'..'F' -> ('F' - v[0]) * 3 + 1 + when { v.endsWith("+") -> 1; v.endsWith("-") -> -1; else -> 0 }
        else -> null
    }

    /** Nomes e apelidos → ficha, com as partes únicas ("Seo-jun" de "Kang Seo-jun"). */
    private fun variantes(fichas: List<Ficha>): Map<String, Long> {
        val m = HashMap<String, Long>()
        fichas.forEach { f ->
            m[f.nome.trim()] = f.id
            f.apelidos.split(',').map { it.trim() }.filter { it.length >= 2 && it[0].isUpperCase() }.forEach { m.putIfAbsent(it, f.id) }
        }
        Marcas.partesUnicas(fichas.associate { it.nome.trim() to it.id }).forEach { (p, id) -> m.putIfAbsent(p, id) }
        return m.filterKeys { it.length >= 2 }
    }

    private class Mencao(val id: Long, val ini: Int, val fim: Int)

    fun analisar(caps: List<Cap>, fichas: List<Ficha>, ignorados: Set<String> = emptySet(), obraId: Long = 0): Resultado {
        val nomes = variantes(fichas)
        val porId = fichas.associateBy { it.id }
        val pessoas = fichas.filter { it.tipo == TipoFicha.PERSONAGEM }.map { it.id }.toSet()
        val n = caps.size
        val contagem = HashMap<Long, IntArray>()
        val falas = HashMap<Long, Int>()
        val falasPorCap = HashMap<Long, MutableSet<Int>>()
        val fatos = ArrayList<Fato>()
        val lugares = HashMap<Long, HashMap<Long, Int>>()
        data class Par(val porCap: IntArray, var cenas: Int = 0, var primeiro: Int = -1, var ultimo: Int = -1, var fam: Int = 0, var rom: Int = 0, var riv: Int = 0, var ali: Int = 0)
        val pares = HashMap<Pair<Long, Long>, Par>()
        val alertas = ArrayList<Alerta>()
        if (nomes.isEmpty()) return Resultado(caps, emptyList(), emptyList(), emptyList())

        val alt = nomes.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        // só testa a partir de letras maiúsculas (nomes próprios): bem mais rápido em textos longos
        val soMaiusculas = nomes.keys.all { it[0].isUpperCase() }
        val rx = Regex((if (soMaiusculas) "(?=\\p{Lu})" else "") + "(?<![$L'’-])(?:$alt)(?![$L'’-])")
        val achaNome = { s: String -> rx.findAll(s).mapNotNull { m -> nomes[m.value]?.let { Mencao(it, m.range.first, m.range.last + 1) } }.toList() }
        // o personagem mais perto de uma posição da frase (para atribuir um fato)
        fun maisPerto(ms: List<Mencao>, pos: Int, max: Int = 70): Long? =
            ms.filter { it.id in pessoas }.minByOrNull { if (pos >= it.fim) pos - it.fim else if (pos < it.ini) it.ini - pos else 0 }
                ?.takeIf { (if (pos >= it.fim) pos - it.fim else if (pos < it.ini) it.ini - pos else 0) <= max }?.id

        val tokensNome = HashMap<String, Int>() // para achar erros de digitação
        val rxToken = Regex("(?=\\p{Lu})(?<=[\\p{Ll},;:)”]\\s|—\\s)\\p{Lu}[\\p{L}'’]+(?:-[\\p{L}'’]+)*")

        caps.forEachIndexed { ci, cap ->
            val texto = Marcas.limpar(cap.texto)
            var cena = ArrayList<Long>()
            val cenas = ArrayList<List<Long>>()
            val paragrafosCena = ArrayList<String>()
            val textosCena = ArrayList<String>()
            fun fecharCena() { if (cena.isNotEmpty()) { cenas.add(cena.distinct()); textosCena.add(paragrafosCena.joinToString("\n")) }; cena = ArrayList(); paragrafosCena.clear() }
            for (linha in texto.split('\n')) {
                val par = linha.trim()
                if (par.isEmpty()) continue
                if ((par[0] == '*' || par[0] == '#' || par.length <= 12) && RX_SEPARADOR.matches(par)) { fecharCena(); continue }
                paragrafosCena.add(par)
                val ms = achaNome(par)
                ms.forEach { m -> contagem.getOrPut(m.id) { IntArray(n) }[ci]++; cena.add(m.id) }
                rxToken.findAll(par).forEach { t -> if (t.value.length >= 4 && t.value !in nomes) tokensNome[t.value] = (tokensNome[t.value] ?: 0) + 1 }
                // quem fala: "— … — disse Fulano" / "Fulano disse"
                if (par.startsWith("—") || par.startsWith("-")) {
                    RX_VERBO_FALA.findAll(par).forEach { v ->
                        val depois = ms.firstOrNull { it.ini >= v.range.last && it.ini - v.range.last <= 25 && it.id in pessoas }
                        val antes = ms.lastOrNull { it.fim <= v.range.first && v.range.first - it.fim <= 12 && it.id in pessoas }
                        (depois ?: antes)?.let { falas[it.id] = (falas[it.id] ?: 0) + 1; falasPorCap.getOrPut(it.id) { HashSet() }.add(ci) }
                    }
                }
                if (ms.isEmpty()) continue
                var iniFrase = 0
                for (frase in par.split(FRASES)) {
                    val desloc = par.indexOf(frase, iniFrase).coerceAtLeast(iniFrase); iniFrase = desloc + frase.length
                    if (frase.length < 8) continue
                    // nomes da frase vêm do parágrafo (sem procurar de novo)
                    val fm = ms.filter { it.ini >= desloc && it.fim <= desloc + frase.length }.map { Mencao(it.id, it.ini - desloc, it.fim - desloc) }
                    if (fm.isEmpty()) continue
                    val curta = frase.trim().take(260)
                    val min = frase.lowercase()
                    // filtros baratos antes das regras caras
                    if ("anos" in min) for (r in RX_IDADE) r.findAll(frase).forEach { m ->
                        val idade = numero(m.groupValues[1]) ?: return@forEach
                        if (idade !in 1..150) return@forEach
                        maisPerto(fm, m.range.first)?.let { fatos.add(Fato(it, ci, Tipo.IDADE, "$idade anos", curta)) }
                    }
                    for ((rxCor, tipo) in listOf(RX_OLHOS to Tipo.OLHOS, RX_CABELO to Tipo.CABELO)) if (if (tipo == Tipo.OLHOS) "olho" in min else PRE_CABELO.any { it in min }) rxCor.findAll(frase).forEach { m ->
                        val mudou = RX_MUDA.containsMatchIn(frase.substring(maxOf(0, m.range.first - 40), minOf(frase.length, m.range.last + 30)))
                        val cor = CORES[m.groupValues[1].lowercase()] ?: return@forEach
                        maisPerto(fm, m.range.first, 60)?.let { fatos.add(Fato(it, ci, tipo, if (mudou) "$cor (mudou)" else cor, curta)) }
                    }
                    if (PRE_MORTE.any { it in min }) RX_MORTE.findAll(frase).forEach { m ->
                        if (RX_NAO_MORTE.containsMatchIn(frase.substring(maxOf(0, m.range.first - 30), m.range.first))) return@forEach
                        maisPerto(fm, m.range.first, 50)?.let { fatos.add(Fato(it, ci, Tipo.MORTE, "morreu", curta)) }
                    }
                    if (PRE_FERIDO.any { it in min }) RX_FERIDO.findAll(frase).forEach { m -> maisPerto(fm, m.range.first, 60)?.let { fatos.add(Fato(it, ci, Tipo.FERIMENTO, m.value.lowercase(), curta)) } }
                    if (PRE_PODER.any { it in min }) RX_PODER.findAll(frase).forEach { m -> maisPerto(fm, m.range.first, 60)?.let { fatos.add(Fato(it, ci, Tipo.PODER, m.value, curta)) } }
                    if (PRE_JURA.any { it in min }) RX_JURA.find(frase)?.let { m -> maisPerto(fm, m.range.first, 80)?.let { fatos.add(Fato(it, ci, Tipo.JURAMENTO, m.value.lowercase(), curta)) } }
                    // pessoa + lugar na mesma frase
                    val ps = fm.filter { it.id in pessoas }.map { it.id }.distinct()
                    fm.filter { porId[it.id]?.tipo == TipoFicha.LUGAR }.map { it.id }.distinct().forEach { lg ->
                        ps.forEach { pe -> val l = lugares.getOrPut(pe) { HashMap() }; l[lg] = (l[lg] ?: 0) + 1 }
                    }
                }
            }
            fecharCena()
            cenas.forEachIndexed { k, ids ->
                val ps = ids.filter { it in pessoas }.sorted()
                if (ps.size < 2) return@forEachIndexed
                // cena pequena: as palavras da cena falam dessas pessoas (contadas uma vez por cena)
                val pal = if (ps.size <= 4) textosCena[k].lowercase().let { tx -> intArrayOf(FAMILIA.findAll(tx).count(), ROMANCE.findAll(tx).count(), RIVAL.findAll(tx).count(), ALIADO.findAll(tx).count()) } else null
                for (i in ps.indices) for (j in i + 1 until ps.size) {
                    val p = pares.getOrPut(ps[i] to ps[j]) { Par(IntArray(n)) }
                    p.cenas++; p.porCap[ci]++; if (p.primeiro < 0) p.primeiro = ci; p.ultimo = ci
                    if (pal != null) { p.fam += pal[0]; p.rom += pal[1]; p.riv += pal[2]; p.ali += pal[3] }
                }
            }
        }

        // ───────── alertas ─────────
        fun cap(i: Int) = "cap. ${i + 1}"
        fun nome(id: Long) = porId[id]?.nome ?: "?"
        fun alerta(nivel: Nivel, titulo: String, detalhe: String, ci: Int, ficha: Long?, frase: String, chave: String) {
            val k = "$obraId|$chave"
            if (k !in ignorados) alertas.add(Alerta(nivel, titulo, detalhe, ci, ficha, frase, k))
        }
        val porFicha = fatos.groupBy { it.fichaId }
        for ((id, fs) in porFicha) {
            val f = porId[id] ?: continue
            // idade
            val idades = fs.filter { it.tipo == Tipo.IDADE }
            val daFicha = Regex("\\d{1,3}").find(f.idade)?.value?.toIntOrNull()
            if (daFicha != null) idades.firstOrNull { it.valor.substringBefore(' ').toInt() != daFicha }?.let {
                alerta(Nivel.ATENCAO, "Idade de ${f.nome} diferente da ficha", "A ficha diz $daFicha; o ${cap(it.cap)} diz ${it.valor}. Se o tempo passou, atualize a ficha.", it.cap, id, it.frase, "idade|$id|${it.valor}")
            }
            idades.zipWithNext().firstOrNull { (a, b) -> b.valor.substringBefore(' ').toInt() < a.valor.substringBefore(' ').toInt() }?.let { (a, b) ->
                alerta(Nivel.FURO, "${f.nome} ficou mais novo", "${a.valor} no ${cap(a.cap)}, ${b.valor} no ${cap(b.cap)}.", b.cap, id, b.frase, "idade-|$id|${b.valor}")
            }
            // olhos e cabelo: cores diferentes sem explicação
            for (tipo in listOf(Tipo.OLHOS, Tipo.CABELO)) {
                val cores = fs.filter { it.tipo == tipo && !it.valor.endsWith("(mudou)") }
                val daFichaCor = (if (tipo == Tipo.OLHOS) RX_OLHOS else RX_CABELO).find(f.aparencia + " " + f.descricao)?.let { CORES[it.groupValues[1].lowercase()] }
                val base = daFichaCor ?: cores.groupingBy { it.valor }.eachCount().maxByOrNull { it.value }?.key ?: continue
                cores.firstOrNull { it.valor != base }?.let {
                    val parte = if (tipo == Tipo.OLHOS) "olhos" else "cabelo"
                    alerta(Nivel.ATENCAO, "${f.nome}: $parte ${it.valor} ou $base?", (if (daFichaCor != null) "A ficha diz $base" else "Na maior parte do texto é $base") +
                        "; no ${cap(it.cap)} aparece ${it.valor}. Se mudou de propósito (poder, disfarce), ignore.", it.cap, id, it.frase, "cor|$id|$tipo|${it.valor}")
                }
            }
            // fala depois de morrer
            fs.firstOrNull { it.tipo == Tipo.MORTE }?.let { m ->
                val depois = falasPorCap[id]?.filter { it > m.cap }?.minOrNull()
                if (depois != null) alerta(Nivel.FURO, "${f.nome} fala depois de morrer", "Morreu no ${cap(m.cap)} e fala de novo no ${cap(depois)}. Se é lembrança, flashback ou ele voltou, ignore.",
                    depois, id, m.frase, "morte|$id|$depois")
            }
            // lutando machucado
            fs.filter { it.tipo == Tipo.FERIMENTO && !it.valor.startsWith("perdeu") }.forEach { fer ->
                val ate = minOf(n - 1, fer.cap + 1)
                for (ci in fer.cap..ate) {
                    val t = Marcas.limpar(caps[ci].texto)
                    val ini = if (ci == fer.cap) t.indexOf(fer.frase.take(40)).coerceAtLeast(0) + fer.frase.length else 0
                    val resto = t.substring(minOf(ini, t.length))
                    val cura = RX_CURA.find(resto)?.range?.first ?: Int.MAX_VALUE
                    var pos = 0
                    var luta: String? = null
                    for (fr in resto.split(FRASES)) {
                        if (pos >= cura) break
                        if (RX_LUTA.containsMatchIn(fr) && rx.findAll(fr).any { nomes[it.value] == id }) { luta = fr; break }
                        pos += fr.length + 1
                    }
                    if (luta != null) {
                        alerta(Nivel.DICA, "${f.nome} luta com ${fer.valor}?", "Machucou no ${cap(fer.cap)} e luta no ${cap(ci)} sem cura ou passagem de tempo no meio.",
                            ci, id, luta.trim().take(260), "ferido|$id|${fer.cap}|$ci")
                        break
                    }
                }
            }
            // nível de poder que cai
            fs.filter { it.tipo == Tipo.PODER }.mapNotNull { fa -> RX_PODER.find(fa.valor)?.groupValues?.get(1)?.let { v -> ordemRank(v)?.let { fa to it } } }
                .zipWithNext().firstOrNull { (a, b) -> b.second < a.second && b.first.cap >= a.first.cap }?.let { (a, b) ->
                    alerta(Nivel.ATENCAO, "Nível de ${f.nome} caiu", "“${a.first.valor}” no ${cap(a.first.cap)}, “${b.first.valor}” no ${cap(b.first.cap)}. Se ele perdeu poder de propósito, ignore.",
                        b.first.cap, id, b.first.frase, "poder|$id|${b.first.valor}")
                }
        }
        // personagem importante que sumiu
        if (n >= 8) contagem.forEach { (id, pc) ->
            if (id !in pessoas || porFicha[id]?.any { it.tipo == Tipo.MORTE } == true) return@forEach
            val total = pc.sum(); val ultimo = pc.indexOfLast { it > 0 }; val caps3 = pc.count { it > 0 }
            if (total >= 15 && caps3 >= 3 && n - 1 - ultimo >= 5)
                alerta(Nivel.DICA, "${nome(id)} sumiu", "Apareceu em $caps3 capítulos e não aparece há ${n - 1 - ultimo} (desde o ${cap(ultimo)}). O leitor pode perguntar dele.", ultimo, id, "", "sumiu|$id|$ultimo")
        }
        // nome escrito errado: "Seo-jum" quando existe "Seo-jun"
        tokensNome.forEach { (tok, q) ->
            if (q > 4) return@forEach
            val parecido = nomes.keys.firstOrNull { it.length >= 4 && !it.contains(' ') && distancia(it, tok) == 1 && (nomes[it]?.let { i -> contagem[i]?.sum() } ?: 0) >= q * 5 } ?: return@forEach
            val ci = caps.indexOfFirst { Regex("(?<![$L])${Regex.escape(tok)}(?![$L])").containsMatchIn(it.texto) }.coerceAtLeast(0)
            val frase = Marcas.limpar(caps[ci].texto).split(FRASES).firstOrNull { it.contains(tok) }?.trim()?.take(260) ?: ""
            alerta(Nivel.ATENCAO, "“$tok” ou “$parecido”?", "“$tok” aparece $q vez(es) e é quase igual a “$parecido”. Pode ser erro de digitação.", ci, nomes[parecido], frase, "grafia|$tok")
        }

        val retratos = fichas.map { f ->
            Retrato(f, contagem[f.id] ?: IntArray(n), falas[f.id] ?: 0, porFicha[f.id].orEmpty(),
                lugares[f.id].orEmpty().entries.sortedByDescending { it.value }.take(4).map { it.key to it.value })
        }.sortedByDescending { it.total }
        val relacoes = pares.filter { it.value.cenas >= 2 }.map { (k, p) ->
            val tipos = listOf("família" to p.fam * 1.4, "romance" to p.rom * 1.6, "rivais" to p.riv * 0.8, "aliados" to p.ali * 1.0)
            val melhor = tipos.maxByOrNull { it.second }!!
            Relacao(k.first, k.second, p.cenas, p.primeiro, p.ultimo, if (melhor.second >= 1.2 * p.cenas.coerceAtMost(5)) melhor.first else "convivem", p.porCap)
        }.sortedByDescending { it.cenas }
        val ordem = alertas.sortedWith(compareBy<Alerta> { it.nivel.ordinal }.thenBy { it.cap })
        return Resultado(caps, retratos, relacoes, ordem)
    }

    fun distancia(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 1) return 2
        val x = a.lowercase(); val y = b.lowercase()
        var ant = IntArray(y.length + 1) { it }
        for (i in 1..x.length) {
            val cur = IntArray(y.length + 1); cur[0] = i
            for (j in 1..y.length) cur[j] = minOf(ant[j] + 1, cur[j - 1] + 1, ant[j - 1] + if (x[i - 1] == y[j - 1]) 0 else 1)
            ant = cur
        }
        return ant[y.length]
    }
}
