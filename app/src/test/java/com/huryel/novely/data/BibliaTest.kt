package com.huryel.novely.data

import org.junit.Assert.assertTrue
import org.junit.Test

class BibliaTest {
    private val fichas = listOf(
        Ficha(1, 1, nome = "Han Seo-jun", apelidos = "Seo-jun", idade = "17", aparencia = "olhos castanhos"),
        Ficha(2, 1, nome = "Kang Min-ho"),
        Ficha(3, 1, nome = "Baek Do-hyun"),
    )
    private val caps = listOf(
        Biblia.Cap(1, "Cap 1", "Seo-jun tinha dezessete anos. Os olhos castanhos de Seo-jun fitavam o chão.\n\n— Levanta — disse Kang Min-ho.\n\nBaek Do-hyun socou Seo-jun, o rival de sempre. Kang Min-ho ajudou Seo-jun, amigo.\n\n* * *\n\nBaek Do-hyun golpeou Seo-jun de novo e Kang Min-ho viu."),
        Biblia.Cap(2, "Cap 2", "Seo-jun estava com o braço quebrado. Seo-jun socou a parede.\n\nOs olhos verdes de Seo-jun brilhavam.\n\nBaek Do-hyun morreu na queda.\n\nO rapaz de 16 anos, Seo-jun, ouviu. Depois disso, Seo-jum riu."),
        Biblia.Cap(3, "Cap 3", "— Voltei — disse Baek Do-hyun.\n\nSeo-jun era nível C. Mas Seo-jun caiu para o nível D."),
    )

    @Test fun achaOsFurosPrincipais() {
        val r = Biblia.analisar(caps, fichas)
        val t = r.alertas.joinToString("\n") { it.titulo }
        println(t)
        assertTrue(t, "Han Seo-jun ficou mais novo" in t)
        assertTrue(t, "Baek Do-hyun fala depois de morrer" in t)
        assertTrue(t, "olhos verde ou castanho" in t)
        assertTrue(t, "Seo-jum" in t)
        assertTrue(t, "Nível de Han Seo-jun caiu" in t)
        assertTrue(t, "luta com" in t)
        assertTrue(r.relacoes.any { it.cenas >= 2 })
        assertTrue(r.retratos.first { it.ficha.id == 2L }.falas == 1)
    }

    @Test fun ignorarSomeComOAlerta() {
        val r = Biblia.analisar(caps, fichas)
        val r2 = Biblia.analisar(caps, fichas, r.alertas.map { it.chave }.toSet())
        assertTrue(r2.alertas.isEmpty())
    }

    @Test fun rapidoEmObraGrande() {
        val base = "Han Seo-jun tinha dezessete anos e olhava o mar. — Vamos — disse Kang Min-ho, enquanto Baek Do-hyun socou a parede. A cidade dormia.\n\n"
        val grande = (1..100).map { Biblia.Cap(it.toLong(), "C$it", base.repeat(80)) }
        Biblia.analisar(grande, fichas)
        val t = System.nanoTime(); Biblia.analisar(grande, fichas); val ms = (System.nanoTime() - t) / 1_000_000
        java.io.File("build/desempenho.txt").appendText("Bíblia viva: 100 capítulos (${base.split(' ').size * 80 * 100} palavras) em $ms ms\n")
        assertTrue("lento: $ms ms", ms < 5000)
    }
}

class ElencoTest {
    private val fichas = listOf(
        Ficha(1, 1, nome = "Han Seo-jun", apelidos = "Seo-jun", descricao = "garoto de 17 anos"),
        Ficha(2, 1, nome = "Lee Hana", descricao = "menina do clube"),
    )

    @Test fun descobreQuemFala() {
        val ps = listOf(
            "— Vem comigo — disse Lee Hana. — Agora.",
            "— Não posso — respondeu Seo-jun.",
            "— Por quê?",
            "— Porque não.",
            "Lee Hana suspirou.",
        )
        val falas = ps.map { it.startsWith("—") }
        val r = Elenco.dividir(ps, falas, Elenco.Nomes(fichas))
        // fala, narração, fala
        org.junit.Assert.assertEquals(listOf(2L, null, 2L), r[0].map { it.quem })
        org.junit.Assert.assertEquals("Vem comigo", r[0][0].texto)
        org.junit.Assert.assertEquals(1L, r[1][0].quem)
        // sem "disse": alterna entre os dois
        org.junit.Assert.assertEquals(2L, r[2][0].quem)
        org.junit.Assert.assertEquals(1L, r[3][0].quem)
        org.junit.Assert.assertEquals(null, r[4][0].quem)
        org.junit.Assert.assertEquals('F', Elenco.genero(fichas[1], ps.joinToString("\n"), Elenco.Nomes(fichas)))
        org.junit.Assert.assertEquals('M', Elenco.genero(fichas[0], ps.joinToString("\n"), Elenco.Nomes(fichas)))
    }
}

class WordTest {
    @Test fun textoDoWord() {
        val xml = """<w:body><w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Capítulo 1 — O Chão</w:t></w:r></w:p>
            <w:p><w:r><w:t xml:space="preserve">Ele disse </w:t></w:r><w:r><w:rPr><w:i/></w:rPr><w:t>não</w:t></w:r><w:r><w:rPr><w:i/></w:rPr><w:t xml:space="preserve"> mesmo</w:t></w:r><w:r><w:t xml:space="preserve"> e </w:t></w:r><w:r><w:rPr><w:b/></w:rPr><w:t>saiu</w:t></w:r><w:r><w:t>.</w:t></w:r></w:p>
            <w:p><w:r><w:t>— Fala &amp; ação.</w:t></w:r></w:p><w:p></w:p></w:body>"""
        val t = textoDoWord(xml)
        org.junit.Assert.assertEquals("Capítulo 1 — O Chão\n\nEle disse *não mesmo* e **saiu**.\n\n— Fala & ação.\n\n", t)
    }
}

class NomesTest {
    @Test fun parteDoNomeNaoPegaOutraPessoa() {
        val rx = Marcas.regexNomes(listOf("Gwon Mu-hyeok", "Mu-hyeok", "Gwon"))!!
        val achados = rx.findAll("Gwon Mu-hyeok riu. A voz de Gwon Jang-ho ecoou. Gwon olhou.").map { it.value }.toList()
        org.junit.Assert.assertEquals(listOf("Gwon Mu-hyeok", "Gwon"), achados)
    }

    @Test fun epitetoViraApelido() {
        org.junit.Assert.assertEquals("Gwon Mu-hyeok" to "Demônio do Céu do Norte", Marcas.separarEpiteto("Gwon Mu-hyeok, o Demônio do Céu do Norte"))
        org.junit.Assert.assertEquals("Lin Feng" to null, Marcas.separarEpiteto("Lin Feng"))
    }
}
