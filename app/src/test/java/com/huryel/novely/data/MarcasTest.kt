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
}
