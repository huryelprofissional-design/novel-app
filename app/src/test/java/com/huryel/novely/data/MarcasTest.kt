package com.huryel.novely.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarcasTest {
    @Test fun definicaoComVirgula() {
        val r = Marcas.encontrar("@Lin Feng, o último herdeiro do clã Yun, entrou.")
        assertEquals(1, r.size)
        assertEquals("Lin Feng", r[0].first.nome)
        assertTrue(r[0].first.definicao)
        assertFalse(r[0].first.lugar)
        assertEquals("Lin Feng, o último herdeiro do clã Yun, entrou.", r[0].second)
    }

    @Test fun mencaoSemVirgula() {
        val r = Marcas.encontrar("— Vocês esperavam outra pessoa? — perguntou @Lin Feng.")
        assertEquals("Lin Feng", r[0].first.nome)
        assertFalse(r[0].first.definicao)
        assertNull(r[0].second)
    }

    @Test fun nomeParaAntesDePalavraMinuscula() {
        val r = Marcas.encontrar("@Lin Feng entrou no salão.")
        assertEquals("Lin Feng", r[0].first.nome)
    }

    @Test fun lugarComE() {
        val r = Marcas.encontrar("Vinha de &Hwanseo, o reino das montanhas de jade.")
        assertTrue(r[0].first.lugar)
        assertEquals("Hwanseo", r[0].first.nome)
        assertEquals("Vinha de Hwanseo, o reino das montanhas de jade.", r[0].second)
    }

    @Test fun apelidos() {
        val r = Marcas.encontrar("@Lin Feng (Lin, Herdeiro), de cabelos brancos.")
        assertEquals(listOf("Lin", "Herdeiro"), r[0].first.apelidos)
        assertEquals("Lin Feng, de cabelos brancos.", Marcas.limpar("@Lin Feng (Lin, Herdeiro), de cabelos brancos."))
    }

    @Test fun nomeComDe() {
        assertEquals("Ana de Souza", Marcas.encontrar("@Ana de Souza, a ferreira.")[0].first.nome)
    }

    @Test fun emailNaoConta() {
        assertTrue(Marcas.encontrar("mande para joao@site.com agora").isEmpty())
        assertEquals("joao@site.com", Marcas.limpar("joao@site.com"))
    }

    @Test fun limparVariosNoParagrafo() {
        assertEquals("Lin Feng e Li Mei chegaram a Hwanseo.", Marcas.limpar("@Lin Feng e @Li Mei chegaram a &Hwanseo."))
    }

    @Test fun digitandoSugestao() {
        assertEquals('@' to "Li", Marcas.digitando("perguntou @Li"))
        assertEquals('&' to "", Marcas.digitando("Vinha de &"))
        assertNull(Marcas.digitando("joao@si"))
    }

    // ── casos reportados: hífen, nome cortado, nomes misturados ──

    @Test fun hifenComEspacosEhSeparador() {
        val r = Marcas.encontrar("@Lin Feng - o último herdeiro do clã Yun.")
        assertEquals("Lin Feng", r[0].first.nome)
        assertTrue(r[0].first.definicao)
        assertEquals("Lin Feng - o último herdeiro do clã Yun.", Marcas.limpar("@Lin Feng - o último herdeiro do clã Yun."))
    }

    @Test fun nomeComHifenInteiro() {
        assertEquals("Ha-eun", Marcas.encontrar("@Ha-eun, a curandeira.")[0].first.nome)
        assertEquals("Kang Seo-jun", Marcas.encontrar("@Kang Seo-jun, o espadachim.")[0].first.nome)
    }

    @Test fun colchetesPegamQualquerNome() {
        val r = Marcas.encontrar("@[Seo-yeon do clã Baek], a filha mais nova.")
        assertEquals("Seo-yeon do clã Baek", r[0].first.nome)
        assertTrue(r[0].first.definicao)
        assertEquals("Seo-yeon do clã Baek, a filha mais nova.", Marcas.limpar("@[Seo-yeon do clã Baek], a filha mais nova."))
    }

    @Test fun nomeConhecidoTemPrioridade() {
        assertEquals("Lin Feng", Marcas.encontrar("@Lin Feng Entrou no salão.", listOf("Lin Feng"))[0].first.nome)
        assertEquals("Lin Feng", Marcas.encontrar("@lin feng, de novo.", listOf("Lin Feng"))[0].first.nome)
        assertEquals("Kang Seo-jun", Marcas.encontrar("@Kang Seo-jun, x", listOf("Kang", "Kang Seo-jun"))[0].first.nome)
        assertEquals("Kang", Marcas.encontrar("@Kang, x", listOf("Kang", "Kang Seo-jun"))[0].first.nome)
    }

    @Test fun pontuacaoGrudadaNoNome() {
        assertEquals("Lin Feng", Marcas.encontrar("— Pare, @Lin Feng!")[0].first.nome)
        assertEquals("Lin Feng", Marcas.encontrar("(@Lin Feng), disse.")[0].first.nome)
        assertEquals("Lin Feng", Marcas.encontrar("“@Lin Feng”")[0].first.nome)
    }

    @Test fun leitorNaoMisturaNomes() {
        val rx = Marcas.regexNomes(listOf("Ha", "Ha-eun"))!!
        assertEquals(listOf("Ha-eun", "Ha"), rx.findAll("Ha-eun e Ha chegaram.").map { it.value }.toList())
        assertTrue(Marcas.regexNomes(listOf("Ha"))!!.findAll("Ha-eun chegou.").none())
        assertEquals(listOf("Lin Feng"), Marcas.regexNomes(listOf("Lin Feng"))!!.findAll("Lin Feng, venha!").map { it.value }.toList())
        assertEquals(listOf("Lin Feng"), Marcas.regexNomes(listOf("Lin Feng"))!!.findAll("—Lin Feng.").map { it.value }.toList())
    }

    @Test fun marcarUsaColchetesQuandoPrecisa() {
        assertEquals("@Lin Feng", Marcas.marcar('@', "Lin Feng"))
        assertEquals("@[Seo-yeon do clã Baek]", Marcas.marcar('@', "Seo-yeon do clã Baek"))
    }

    @Test fun parteDoNomeUnica() {
        val p = Marcas.partesUnicas(mapOf("Kang Seo-jun" to 1L, "Kang Min-ho" to 2L, "Li Mei" to 3L))
        assertEquals(1L, p["Seo-jun"])
        assertEquals(2L, p["Min-ho"])
        assertNull(p["Kang"]) // dois Kang: não vale sozinho
        assertEquals(3L, p["Mei"])
    }

    @Test fun achaNomesSemFicha() {
        val t = "Ele olhou para Seo-jun. Depois, Seo-jun riu. Então disse Seo-jun que não. Viu Ha-eun uma vez."
        val r = Marcas.nomesSemFicha(t, emptyList())
        assertEquals("Seo-jun", r.first().first)
        assertTrue(r.none { it.first == "Ha-eun" }) // só 1 vez
        assertTrue(Marcas.nomesSemFicha(t, listOf("Kang Seo-jun")).isEmpty() || Marcas.nomesSemFicha(t, listOf("Seo-jun")).isEmpty())
    }

    @Test fun seuNaoViraNome() {
        val p = Marcas.partesUnicas(mapOf("Seu Cristóvão" to 1L, "Dona Benta" to 2L, "Kang Seo-jun" to 3L))
        assertNull(p["Seu"]); assertNull(p["Dona"])
        assertEquals(1L, p["Cristóvão"]); assertEquals(3L, p["Seo-jun"])
    }

    @Test fun confiancaDoNome() {
        val t = "Seu pai chegou. Ele viu Cristóvão sorrir. Cristóvão saiu."
        assertTrue(Marcas.confianca("Viu Seu Cristóvão.", 4, "Seu Cristóvão", cadastrado = true) >= Marcas.LIMIAR_FORTE)
        val meio = Marcas.confianca(t, t.indexOf("Cristóvão"), "Cristóvão", cadastrado = false)
        assertTrue(meio in Marcas.LIMIAR_LEVE until Marcas.LIMIAR_FORTE)
        assertTrue(Marcas.confianca(t, t.lastIndexOf("Cristóvão"), "Cristóvão", cadastrado = false) < Marcas.LIMIAR_LEVE)
        assertEquals(0, Marcas.confianca(t, 0, "Seu", cadastrado = false))
        assertEquals(0, Marcas.confianca(t, t.indexOf("Cristóvão"), "Cristóvão", cadastrado = true, ignorado = true))
        assertTrue(Marcas.inicioDeFrase("— Cristóvão, venha!", 2))
        assertTrue(!Marcas.inicioDeFrase("disse Cristóvão", 6))
    }

    @Test fun removerEfeitoDeixaOTexto() {
        assertEquals("Você subiu. HP 30/100", Marcas.semEfeito("[#sistema:vermelho Você subiu | HP 30/100]"))
        assertEquals("Você subiu de nível", Marcas.semEfeito("[#sistema+ Você subiu de nível]"))
        assertEquals("Habilidade adquirida", Marcas.semEfeito("[[Habilidade adquirida]]"))
        assertEquals("Missões. Sobreviver", Marcas.semEfeito("[[Missões | [x] Sobreviver]]"))
        assertEquals("Você subiu", Marcas.semEfeito("[!Você subiu!]"))
        assertEquals("Espada do Vazio", Marcas.semEfeito("[+Espada do Vazio | lendário]"))
        assertEquals("Do fundo vieram as chamas e o grito", Marcas.semEfeito("Do fundo vieram as {fogo+:azul: chamas} e o !!grito!!"))
        assertEquals("Seul, 03:14", Marcas.semEfeito(":: Seul | 03:14"))
        assertEquals("segredo aqui", Marcas.semEfeitoNoTrecho("||segredo|| aqui"))
    }

    @Test fun escreverEfeitoComIntensidade() {
        assertEquals("{fogo+:azul: chamas}", Marcas.escreverEfeito("fogo", "azul", 1, "chamas", false))
        assertEquals("{neon: luz}", Marcas.escreverEfeito("neon", null, 0, "luz", false))
        assertEquals("[#sistema- Você subiu]", Marcas.escreverEfeito("sistema", "roxo", -1, "Você subiu", true))
        assertEquals("[#perigo:vermelho Fuja]", Marcas.escreverEfeito("perigo", "vermelho", 0, "Fuja", true))
        assertEquals("chamas", Marcas.semEfeito(Marcas.escreverEfeito("fogo", "azul", 1, "chamas", false)))
    }
}
