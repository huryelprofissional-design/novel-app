package com.huryel.novely.data

/**
 * Estúdio de IA: monta prompts completos com o contexto da obra (sinopse, estilo do autor, fichas,
 * linha do tempo, resumos e o capítulo) para colar no ChatGPT, Gemini, Claude etc.
 * A resposta volta para o app no formato do Novely (travessão nas falas, * * * nas cenas, @Personagem e &Lugar).
 * O mesmo texto existe no site do PC (web/ia.js) — mudar os dois juntos.
 */
object Prompts {
    data class Tipo(val id: String, val nome: String, val icone: String, val desc: String, val pedeIdeia: Boolean = false, val dicaIdeia: String = "", val usaCap: Boolean = true)

    val TIPOS = listOf(
        Tipo("CONTINUAR", "Próximo capítulo", "✍", "Escreve o capítulo seguinte no seu estilo, com gancho no fim", true, "O que deve acontecer? (opcional)"),
        Tipo("CENA", "Escrever uma cena", "🎬", "Uma cena específica a partir da sua ideia", true, "Ex.: Seo-jun enfrenta o valentão no vestiário"),
        Tipo("REVISAR", "Revisar capítulo", "🔍", "Corrige erros e ritmo sem mudar seu estilo"),
        Tipo("DIALOGOS", "Melhorar diálogos", "💬", "Falas mais naturais e cada personagem com voz própria"),
        Tipo("GANCHO", "Final com gancho", "🪝", "3 versões mais fortes para o fim do capítulo"),
        Tipo("REVIRAVOLTAS", "Ideias de reviravolta", "🌀", "5 viradas coerentes com as pistas que já existem", true, "Algum rumo que você quer? (opcional)"),
        Tipo("PERSONAGEM", "Criar personagens", "☺", "Vem pronto no formato @Nome, descrição", true, "Ex.: um rival da mesma academia", usaCap = false),
        Tipo("MUNDO", "Lugar, seita ou sistema", "⌂", "Lugares e regras do mundo no formato &Lugar", true, "Ex.: o sistema de cultivo / a capital", usaCap = false),
        Tipo("TECNICAS", "Técnicas e golpes", "⚔", "10 nomes de técnicas coerentes com o poder da história", true, "Para quem? (opcional)", usaCap = false),
        Tipo("CONSISTENCIA", "Checar furos", "🧩", "Procura contradições entre fichas, linha do tempo e texto"),
        Tipo("RESUMOS", "Resumir capítulos", "📝", "Gera os resumos do “Anteriormente…” de todos os capítulos", usaCap = false),
        Tipo("SINOPSE", "Sinopse e título", "📣", "3 sinopses que prendem, 5 títulos e tags", usaCap = false),
        Tipo("TITULOS", "Título do capítulo", "🏷", "5 títulos para o capítulo"),
    )

    val TONS = listOf("Manter o da obra", "Mais sombrio", "Mais leve", "Épico", "Romântico", "Tenso / suspense", "Engraçado")
    val TAMANHOS = listOf(1500, 3000, 5000, 8000)
    val PONTOS = listOf("Manter o da obra", "1ª pessoa", "3ª pessoa")

    data class Contexto(
        val obra: Obra, val caps: List<Capitulo>, val fichas: List<Ficha>, val eventos: List<Evento>,
        val capAtual: Capitulo?, val textoAtual: String, val textosSemResumo: List<Pair<Capitulo, String>> = emptyList(),
    )
    data class Opcoes(val tipo: String, val ideia: String = "", val tom: String = TONS[0], val tamanho: Int = 3000, val ponto: String = PONTOS[0], val quantos: Int = 3)

    const val FORMATO = """REGRAS DE FORMATO (o texto vai direto para o app Novely):
- Cada fala em um parágrafo próprio, começando com travessão (—). Ex.: — Você veio — disse ela.
- Troca de cena: uma linha sozinha com * * *
- Itálico com *assim* e negrito com **assim**. Nada de títulos em markdown além do título do capítulo.
- Personagem NOVO, na primeira vez que aparece: @Nome, descrição curta (ex.: @Kang Min-ho, treinador de quarenta anos com o nariz quebrado duas vezes, …). O app cria a ficha sozinho.
- Lugar NOVO: &Nome, descrição curta (ex.: &Academia Taesik, um porão úmido que cheira a borracha…).
- Nome com palavra minúscula no meio: @[Seo-yeon do clã Baek]. Apelidos entre parênteses: @Lin Feng (Feng, Herdeiro), …
- Personagens e lugares que JÁ existem: escreva só o nome, sem @ nem &.
- Responda só com o texto pedido, sem comentários antes ou depois."""

    private fun linhaFicha(f: Ficha) = buildString {
        append("• ").append(f.nome)
        if (f.apelidos.isNotBlank()) append(" (").append(f.apelidos).append(")")
        append(" — ").append(when (f.tipo) { TipoFicha.LUGAR -> "lugar"; TipoFicha.OUTRO -> "outro"; else -> "personagem" })
        listOf("idade" to f.idade, "facção" to f.faccao).filter { it.second.isNotBlank() }.forEach { (k, v) -> append(", $k: $v") }
        if (f.descricao.isNotBlank()) append(". ").append(f.descricao.trim())
        if (f.aparencia.isNotBlank()) append(" Aparência: ").append(f.aparencia.trim())
        if (f.poder.isNotBlank()) append(" Poder: ").append(f.poder.trim())
        if (f.relacoes.isNotBlank()) append(" Relações: ").append(f.relacoes.trim())
    }.take(520)

    /** Uns parágrafos de narração do autor para a IA copiar o jeito de escrever. */
    fun amostraEstilo(texto: String): String {
        val pars = Marcas.limpar(texto).lines().map { it.trim() }.filter { it.length > 120 && !it.startsWith("—") && !it.startsWith("#") }
        if (pars.isEmpty()) return ""
        val meio = pars.size / 2
        return (listOf(pars.first()) + pars.subList(meio, minOf(pars.size, meio + 2))).distinct().joinToString("\n\n").take(2600)
    }

    fun montar(c: Contexto, op: Opcoes): String {
        val o = c.obra
        val numero = c.caps.count { Texto.tipoEspecial(it.titulo) == null }
        val sb = StringBuilder()
        sb.appendLine("Você é coautor e editor experiente de webnovels em português do Brasil. Vou te passar o contexto da minha obra.").appendLine()
        sb.appendLine("=== OBRA ===").appendLine("Título: ${o.titulo}")
        if (o.generos.isNotBlank()) sb.appendLine("Gêneros: ${o.generos.replace(",", ", ")}")
        if (o.classificacao != "LIVRE") sb.appendLine("Classificação: +${o.classificacao}")
        if (o.descricao.isNotBlank()) sb.appendLine("Sinopse: ${o.descricao.trim()}")
        sb.appendLine("Capítulos até agora: ${c.caps.size}").appendLine()
        val base = c.textoAtual.ifEmpty { "" }
        val estilo = amostraEstilo(base)
        if (estilo.isNotEmpty() && op.tipo in setOf("CONTINUAR", "CENA", "GANCHO", "DIALOGOS", "REVISAR")) {
            sb.appendLine("=== MEU ESTILO (imite o ritmo, o vocabulário e o tamanho dos parágrafos) ===").appendLine(estilo).appendLine()
        }
        if (c.fichas.isNotEmpty()) {
            sb.appendLine("=== PERSONAGENS E LUGARES QUE JÁ EXISTEM ===")
            c.fichas.take(70).forEach { sb.appendLine(linhaFicha(it)) }
            sb.appendLine()
        }
        if (c.eventos.isNotEmpty()) {
            sb.appendLine("=== LINHA DO TEMPO ===")
            c.eventos.take(60).forEach { e -> sb.appendLine("• ${if (e.quando.isNotBlank()) "[${e.quando}] " else ""}${e.titulo}${if (e.descricao.isNotBlank()) " — ${e.descricao}" else ""}") }
            sb.appendLine()
        }
        val resumos = c.caps.filter { it.resumo.isNotBlank() }
        if (resumos.isNotEmpty() && op.tipo != "RESUMOS") {
            sb.appendLine("=== O QUE JÁ ACONTECEU (resumo por capítulo) ===")
            resumos.takeLast(60).forEach { sb.appendLine("${it.titulo}: ${it.resumo.trim()}") }
            sb.appendLine()
        }
        val capTitulo = c.capAtual?.titulo ?: ""
        if (op.tipo != "RESUMOS" && op.tipo != "PERSONAGEM" && op.tipo != "MUNDO" && op.tipo != "TECNICAS" && op.tipo != "SINOPSE" && base.isNotBlank()) {
            val t = Marcas.limpar(base).trim()
            val corte = if (t.length > 30_000) "[…início cortado…]\n" + t.takeLast(30_000) else t
            sb.appendLine(if (op.tipo == "CONTINUAR") "=== ÚLTIMO CAPÍTULO: $capTitulo ===" else "=== CAPÍTULO: $capTitulo ===").appendLine(corte).appendLine()
        }
        if (op.tipo == "RESUMOS") {
            sb.appendLine("=== CAPÍTULOS PARA RESUMIR ===")
            var total = 0
            for ((cap, txt) in c.textosSemResumo) {
                val t = Marcas.limpar(txt).trim().let { if (it.length > 9000) it.take(4500) + "\n[…]\n" + it.takeLast(4500) else it }
                if (total + t.length > 60_000) break
                total += t.length
                sb.appendLine("--- ${cap.titulo} ---").appendLine(t).appendLine()
            }
        }
        val tom = if (op.tom == TONS[0]) "" else " Tom: ${op.tom.lowercase()}."
        val pov = if (op.ponto == PONTOS[0]) "" else " Narre em ${op.ponto}."
        val ideia = op.ideia.trim()
        sb.appendLine("=== TAREFA ===")
        sb.appendLine(when (op.tipo) {
            "CONTINUAR" -> "Escreva o Capítulo ${numero + 1}, continuando exatamente de onde o último parou." +
                (if (ideia.isNotEmpty()) " Neste capítulo: $ideia." else " Avance a história de forma coerente com o que já aconteceu.") +
                " Tamanho: cerca de ${op.tamanho} palavras.$tom$pov Mantenha a personalidade e o jeito de falar de cada personagem. Mostre em vez de explicar. Termine com um gancho que deixe o leitor querendo o próximo. Comece com uma linha só com o título: Capítulo ${numero + 1} — (título)."
            "CENA" -> "Escreva esta cena: ${ideia.ifEmpty { "(descreva a cena)" }}. Tamanho: cerca de ${op.tamanho} palavras.$tom$pov Use os personagens e o mundo acima, com coerência com o capítulo atual. Foque em ação, emoção e detalhes dos sentidos."
            "REVISAR" -> "Revise o capítulo acima: ortografia, gramática, pontuação, repetições de palavras, frases confusas e ritmo. NÃO mude o enredo, os nomes nem o meu estilo. Devolva o capítulo inteiro já corrigido. Depois do capítulo, escreva uma linha com === e liste as 5 mudanças mais importantes que você fez."
            "DIALOGOS" -> "Reescreva o capítulo acima melhorando só os diálogos: falas mais naturais, subtexto, cada personagem com uma voz própria e reconhecível, menos explicação dentro das falas e tags de fala mais variadas. Mantenha o enredo e a narração. Devolva o capítulo inteiro."
            "GANCHO" -> "Escreva 3 versões diferentes para o final do capítulo acima (os últimos 3 a 6 parágrafos), cada uma com um gancho mais forte: revelação, perigo iminente ou pergunta sem resposta. Separe as versões com * * * e dê um nome curto a cada uma antes."
            "REVIRAVOLTAS" -> "Proponha 5 reviravoltas para os próximos capítulos, coerentes com tudo acima.${if (ideia.isNotEmpty()) " Direção desejada: $ideia." else ""} Para cada uma: (1) o que acontece; (2) por que faz sentido — quais pistas já existem no texto; (3) em qual capítulo plantar uma pista nova antes da revelação; (4) o impacto em cada personagem principal. Evite clichês óbvios."
            "PERSONAGEM" -> "Crie ${op.quantos} personagem(ns) novo(s) que caibam nesta história${if (ideia.isNotEmpty()) ": $ideia" else ""}. Dê nomes coerentes com o mundo. Responda SOMENTE com uma linha por personagem, neste formato exato:\n@Nome (apelidos), descrição de 3 a 5 frases com idade, aparência, personalidade, desejo/medo, poder ou habilidade e a relação com personagens que já existem."
            "MUNDO" -> "Crie ${ideia.ifEmpty { "lugares importantes para a história" }}, coerente com o que já existe. Primeiro, uma linha por lugar no formato exato: &Nome, descrição de 2 a 4 frases (aparência, clima, quem manda, por que importa para a trama). Depois, se for um sistema (de poder, cultivo, magia, castas…), explique as regras, níveis e limites em tópicos curtos."
            "TECNICAS" -> "Crie 10 técnicas, golpes ou habilidades${if (ideia.isNotEmpty()) " para $ideia" else ""}, coerentes com o sistema de poder e o tom da história. Os nomes devem soar bem em português (evite tradução literal estranha). Formato: Nome da técnica — efeito em 1 frase — custo ou limite."
            "CONSISTENCIA" -> "Procure furos e contradições entre as fichas, a linha do tempo, os resumos e o capítulo acima: idades, aparência, nomes trocados, poderes que mudam, quem sabe o quê, datas e lugares. Liste cada problema com: o trecho, o que contradiz e a correção sugerida. Se não achar nada grave, diga o que pode confundir o leitor."
            "RESUMOS" -> "Para cada capítulo acima, escreva um resumo de 2 a 4 frases, no passado, com os acontecimentos que importam para a continuidade (sem opinião). Responda SOMENTE neste formato, uma linha por capítulo, com o título exatamente igual:\nTítulo do capítulo: resumo"
            "SINOPSE" -> "Escreva: (1) 3 opções de sinopse de até 600 caracteres que deixem o leitor curioso sem spoilers, cada uma com um ângulo diferente (personagem, mistério, conflito); (2) 5 opções de título; (3) 8 tags curtas em minúsculas."
            else -> "Sugira 5 títulos curtos e marcantes para o capítulo acima, sem spoiler do final, cada um com uma frase explicando a ideia."
        })
        if (op.tipo in setOf("CONTINUAR", "CENA", "REVISAR", "DIALOGOS", "GANCHO", "PERSONAGEM", "MUNDO")) sb.appendLine().appendLine(FORMATO)
        return sb.toString().trim()
    }

    /** "Título do capítulo: resumo" → resumos casados pelo título. */
    fun lerResumos(resposta: String, caps: List<Capitulo>): Map<Long, String> {
        val saida = HashMap<Long, String>()
        resposta.lines().map { it.trim().trimStart('-', '•', '*', ' ') }.filter { ':' in it }.forEach { l ->
            val t = l.substringBefore(':').trim().trim('*'); val r = l.substringAfter(':').trim()
            if (r.length < 15) return@forEach
            val cap = caps.firstOrNull { Texto.normalizar(it.titulo) == Texto.normalizar(t) }
                ?: caps.firstOrNull { Texto.normalizar(t).contains(Texto.normalizar(it.titulo)) && it.titulo.length > 5 }
            if (cap != null) saida[cap.id] = r
        }
        return saida
    }
}
