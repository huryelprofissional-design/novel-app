package com.huryel.novely.data

import java.text.Normalizer

/**
 * Efeitos de escrita (versão 2026-11-18, escolhidos pelo autor na página "Efeitos do zero").
 * Três jeitos de escrever (o nome aceita acento ou não, maiúscula ou não):
 *   {nome: texto}            dentro da frase (vira estilo) ou sozinho na linha (vira efeito animado). "— {sussurro: fica}" também.
 *   [nome | campo | campo]   linha sozinha: janelas, fichas, barras, chat, carimbos…
 *   ::nome resto             linha sozinha: coisa da cena (som, vibração, partículas, pausa…)
 * A tabela completa está em cofre/50-Recursos/Efeitos.md e no Maestro.
 */
enum class FxTipo {
    LETRAS, FRASE, DIGITAR, DECIFRAR, TRADUZ, CORRIGE, ECO, MULTIDAO, JANELA, FICHA, CONTAR, BARRA, CHAT, NOTIF, CARTA,
    SELO, CONQUISTA, QUADROS, DIVIDE, ESCOLHA, PULSO, LIGACAO, TERMINAL, OBITO, LEGENDA, ABERTURA, CREDITOS, CORTE, RACHA,
    GOTAS, RELOGIO, PARTICULAS, TINTA, VINHETA, RESPIRO, ONDAS, PAUSA,
}

/** modo = variação dentro do tipo; rotulo = título fixo (janela, barra, contador…); padrao = texto quando o autor não escreve nada. */
data class FxDef(val tipo: FxTipo, val modo: String = "", val rotulo: String = "", val padrao: String = "")

/** Um efeito encontrado numa linha. */
data class FxUso(val nome: String, val def: FxDef, val campos: List<String>)

object Fx {
    fun chave(s: String): String = Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace(Regex("\\s+"), " ")

    private fun l(m: String) = FxDef(FxTipo.LETRAS, m)
    private fun f(m: String) = FxDef(FxTipo.FRASE, m)
    private fun p(m: String) = FxDef(FxTipo.PARTICULAS, m)

    val CATALOGO: Map<String, FxDef> = linkedMapOf(
        // voz e fala
        "sussurro" to f("sussurro"), "treme" to l("tremer"), "eco" to FxDef(FxTipo.ECO), "duas" to f("duplo"),
        "multidao" to FxDef(FxTipo.MULTIDAO), "traduz" to FxDef(FxTipo.TRADUZ), "bebado" to f("balancar"),
        "gagueja" to FxDef(FxTipo.DIGITAR), "canta" to p("notas"), "comando" to l("dourar"), "crianca" to l("pulo"),
        // mente
        "flutua" to l("subir"), "intrusa" to f("intrusa"), "telepatia" to f("aura"), "memoria" to f("sepia"),
        "corrige" to FxDef(FxTipo.CORRIGE), "conta" to FxDef(FxTipo.DIGITAR), "plano" to FxDef(FxTipo.QUADROS),
        "panico" to f("panico"), "divide" to FxDef(FxTipo.DIVIDE), "escolha" to FxDef(FxTipo.ESCOLHA),
        "lida" to FxDef(FxTipo.DECIFRAR), "loucura" to l("espalhar"), "ideia" to f("flash"), "tontura" to f("duplo"),
        // sistema
        "sistema" to FxDef(FxTipo.JANELA, "azul", "SISTEMA"), "status" to FxDef(FxTipo.FICHA),
        "nivel" to FxDef(FxTipo.CONTAR, "par", "nível"), "alerta" to FxDef(FxTipo.JANELA, "vermelho", "PERIGO"),
        "missao" to FxDef(FxTipo.JANELA, "azul", "MISSÃO"), "habilidade" to FxDef(FxTipo.CONQUISTA, rotulo = "Habilidade nova"),
        "vida" to FxDef(FxTipo.BARRA, "vermelho", "VIDA"), "tempo" to FxDef(FxTipo.CONTAR, "desce", "segundos"),
        "rank" to FxDef(FxTipo.SELO, "ouro", "RANK "), "loja" to FxDef(FxTipo.JANELA, "ouro", "LOJA"),
        "defeito" to FxDef(FxTipo.DECIFRAR), "constelacao" to FxDef(FxTipo.JANELA, "roxo", "CONSTELAÇÃO"),
        "inventario" to FxDef(FxTipo.QUADROS), "recompensa" to p("moedas"),
        "penalidade" to FxDef(FxTipo.JANELA, "vermelho", "PENALIDADE"), "xp" to FxDef(FxTipo.CONTAR, "sobe", "XP"),
        "mana" to FxDef(FxTipo.BARRA, "azul", "MANA"), "titulo" to FxDef(FxTipo.SELO, "roxo"),
        // luta
        "corte" to FxDef(FxTipo.CORTE), "tecnica" to l("zoom"), "quebra" to FxDef(FxTipo.RACHA),
        "desmaio" to FxDef(FxTipo.VINHETA, "preto"), "final" to l("zoom"), "dano" to FxDef(FxTipo.CONTAR, "sobe", "de dano"),
        "pressao" to f("esmaga"),
        // magia
        "feitico" to p("faiscas"), "runa" to l("dourar"), "portal" to FxDef(FxTipo.TINTA, "roxo"), "cura" to p("vagalumes"),
        "maldicao" to FxDef(FxTipo.TINTA, "vinho"), "invoca" to f("crescer"), "teleporte" to l("juntar"),
        "barreira" to f("aura"), "gelo" to l("congelar"), "fogo" to p("brasas"), "profecia" to FxDef(FxTipo.DIGITAR, "italico"),
        "pacto" to FxDef(FxTipo.GOTAS),
        // murim
        "qi" to f("aura"), "reino" to FxDef(FxTipo.CONQUISTA, rotulo = "Avanço de reino"), "pulso" to FxDef(FxTipo.PULSO),
        "pilula" to p("brilhos"), "desafio" to FxDef(FxTipo.SELO, "vermelho", padrao = "DESAFIO"),
        "pulso parado" to FxDef(FxTipo.PULSO, "plano"), "arrogante" to f("grito"), "aura" to f("esmaga"),
        // ficção científica
        "hud" to FxDef(FxTipo.JANELA, "verde", "HUD"), "holo" to f("holo"), "terminal" to FxDef(FxTipo.TERMINAL),
        "invasao" to FxDef(FxTipo.TERMINAL), "ia" to FxDef(FxTipo.DIGITAR), "implante" to FxDef(FxTipo.DECIFRAR),
        "salto" to p("estrelas"), "alarme" to f("alarme"), "download" to FxDef(FxTipo.BARRA, "azul", "DOWNLOAD"),
        "scan" to f("scan"), "neon" to l("neon"), "clone" to f("duplo"),
        // terror e sobrenatural
        "parede" to l("sangrar"), "sussurros" to FxDef(FxTipo.MULTIDAO), "derrete" to l("derreter"),
        "impronunciavel" to FxDef(FxTipo.DECIFRAR), "observa" to f("respirar"), "relogio" to FxDef(FxTipo.RELOGIO),
        // romance e cena quente
        "coracao" to f("coracao"), "cora" to f("corar"), "olhar" to f("lento"), "perto" to l("juntar"), "maos" to f("respirar"),
        "confessa" to FxDef(FxTipo.DIGITAR), "ciume" to f("furia"), "promessa" to l("dourar"), "carta" to FxDef(FxTipo.CARTA),
        "danca" to f("balancar"), "chat" to FxDef(FxTipo.CHAT), "ofegante" to FxDef(FxTipo.RESPIRO), "quente" to f("quente"),
        // sangue
        "escorre" to FxDef(FxTipo.GOTAS), "dor" to l("sangrar"), "mortos" to FxDef(FxTipo.CONTAR, "sobe", "mortos"),
        "furia" to f("furia"), "turvo" to f("desmaio"), "ferida" to FxDef(FxTipo.FICHA, "vermelho"), "lamina" to FxDef(FxTipo.CORTE),
        "sujo" to l("sangrar"), "morte" to FxDef(FxTipo.OBITO),
        // comédia
        "grilos" to FxDef(FxTipo.DIGITAR, padrao = "cri…  cri…  cri…"), "raiva" to f("raiva"), "alma" to l("fantasma"),
        "zoom" to f("zoom"), "mentira" to FxDef(FxTipo.SELO, "vermelho", padrao = "MENTIRA"),
        "plateia" to FxDef(FxTipo.MULTIDAO, padrao = "kkkk|hahaha|kkk|haha|kkkkk"),
        // clima, tempo, mistério, letra
        "aurora" to f("aurora"), "depois" to FxDef(FxTipo.LEGENDA), "lugar" to FxDef(FxTipo.LEGENDA),
        "regressao" to FxDef(FxTipo.RELOGIO), "dias" to FxDef(FxTipo.CONTAR, "dias", "dias"), "documento" to FxDef(FxTipo.CARTA),
        "revela" to l("zoom"), "trancado" to FxDef(FxTipo.SELO, "roxo", padrao = "???"), "assina" to FxDef(FxTipo.DIGITAR, "italico"),
        "senha" to FxDef(FxTipo.DECIFRAR), "pausa" to FxDef(FxTipo.PAUSA), "cores" to l("arco"),
        // telas
        "ligacao" to FxDef(FxTipo.LIGACAO), "chamada" to FxDef(FxTipo.LIGACAO), "noticia" to FxDef(FxTipo.NOTIF, rotulo = "Jornal"),
        "rede" to FxDef(FxTipo.NOTIF, rotulo = "Rede social"), "live" to FxDef(FxTipo.CONTAR, "sobe", "assistindo"),
        "apagada" to FxDef(FxTipo.CHAT, "apagada"), "visto" to FxDef(FxTipo.CHAT, "visto"), "banco" to FxDef(FxTipo.NOTIF, rotulo = "Banco"),
        "e-mail" to FxDef(FxTipo.NOTIF, rotulo = "E-mail"), "email" to FxDef(FxTipo.NOTIF, rotulo = "E-mail"),
        "digitando" to FxDef(FxTipo.CHAT, "digitando"), "bateria" to FxDef(FxTipo.BARRA, "vermelho", "BATERIA"),
        // som e celular
        "vibra" to FxDef(FxTipo.ONDAS, "vibra", padrao = "bzz"), "som" to FxDef(FxTipo.ONDAS, "som"), "trilha" to FxDef(FxTipo.ONDAS, "som"),
        "batida" to f("batida"), "flash" to f("flash"), "sino" to FxDef(FxTipo.ONDAS, "sino", padrao = "dong…"),
        // começos e personagens
        "abertura" to FxDef(FxTipo.ABERTURA), "creditos" to FxDef(FxTipo.CREDITOS), "epigrafe" to f("epigrafe"),
        "entra" to FxDef(FxTipo.ABERTURA), "ficha" to FxDef(FxTipo.FICHA), "afinidade" to FxDef(FxTipo.BARRA, "rosa", "AFINIDADE"),
    )

    fun def(nome: String): FxDef? = CATALOGO[chave(nome)]
    fun conhecido(nome: String) = def(nome) != null

    private val RX_LINHA_INLINE = Regex("^(—\\s*)?\\{([^{}:\\n]{1,30}):\\s*([^{}\\n]*)\\}\\s*([.!?…]*)$")
    /** {nome: texto} dentro da frase. */
    val RX_INLINE = Regex("\\{([^{}:\\n]{1,30}):\\s*([^{}\\n]*)\\}")

    private fun campos(s: String) = s.split('|').map { it.trim() }

    /** O efeito de uma linha inteira, ou null se a linha é texto comum. */
    fun lerLinha(linha0: String): FxUso? {
        val l = linha0.trim()
        if (l.length < 3) return null
        if (l.startsWith("[") && l.endsWith("]") && !l.startsWith("[[")) {
            val ps = campos(l.substring(1, l.length - 1))
            val d = def(ps[0]) ?: return null
            return FxUso(chave(ps[0]), d, ps.drop(1).filter { it.isNotEmpty() })
        }
        if (l.startsWith("::")) {
            val r = l.drop(2).trim()
            val nome = r.substringBefore(' ')
            val d = def(nome) ?: return null
            val resto = r.substringAfter(' ', "").trim()
            return FxUso(chave(nome), d, if (resto.isEmpty()) emptyList() else campos(resto))
        }
        val m = RX_LINHA_INLINE.find(l) ?: return null
        val d = def(m.groupValues[2]) ?: return null
        val cs = campos(m.groupValues[3]).toMutableList()
        if (m.groupValues[1].isNotEmpty() && cs.isNotEmpty()) cs[0] = "— " + cs[0]
        if (m.groupValues[4].isNotEmpty() && cs.isNotEmpty()) cs[cs.size - 1] = cs.last() + m.groupValues[4]
        return FxUso(chave(m.groupValues[2]), d, cs)
    }

    private val SEM_VOZ = setOf(FxTipo.PAUSA, FxTipo.ONDAS, FxTipo.CONTAR, FxTipo.BARRA, FxTipo.PULSO, FxTipo.RELOGIO)

    /** O que o "Ouvir", a busca e as exportações leem desse efeito. */
    fun plano(u: FxUso): String {
        if (u.def.tipo in SEM_VOZ && u.def.tipo != FxTipo.PULSO && u.def.tipo != FxTipo.RELOGIO) return ""
        val cs = u.campos.ifEmpty { u.def.padrao.split('|').filter { it.isNotBlank() } }
        return when (u.def.tipo) {
            FxTipo.TRADUZ, FxTipo.CORRIGE -> cs.lastOrNull().orEmpty()
            FxTipo.JANELA -> (listOf(u.def.rotulo) + cs).joinToString(". ")
            FxTipo.SELO -> u.def.rotulo + cs.joinToString(" ")
            FxTipo.CHAT -> cs.joinToString(". ") { it.substringAfter(':').trim() }
            FxTipo.PULSO, FxTipo.RELOGIO -> cs.joinToString(". ")
            else -> cs.joinToString(". ")
        }.trim()
    }

    /** Texto sem nenhum efeito (resumos, trechos de divulgação): efeitos viram o texto que eles mostram. */
    fun textoSimples(t: String): String = t.lines().joinToString("\n") { linha ->
        val u = lerLinha(linha)
        if (u != null) plano(u) else RX_INLINE.replace(linha) { m -> if (conhecido(m.groupValues[1])) campos(m.groupValues[2]).last() else m.value }
    }
}
