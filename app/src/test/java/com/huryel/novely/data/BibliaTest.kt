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
