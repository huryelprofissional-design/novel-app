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
}
