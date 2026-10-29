package com.huryel.novely.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** Testes com textos SINTÉTICOS (sem conteúdo protegido). */
class TextoTest {

    private val vocab = listOf("a", "torre", "cinza", "vento", "noite", "Kael", "passo", "pedra", "coração", "escada",
        "porta", "sangue", "juramento", "luz", "silêncio", "antiga", "cidade", "voz", "mão", "olhos")

    private fun gerar(palavras: Int, semente: Int = 1): String {
        val r = Random(semente)
        val sb = StringBuilder(palavras * 7)
        var n = 0
        while (n < palavras) {
            val tam = 40 + r.nextInt(80)
            if (r.nextInt(5) == 0) sb.append("— ")
            for (i in 0 until tam) {
                if (n >= palavras) break
                sb.append(vocab[r.nextInt(vocab.size)])
                sb.append(if (i % 12 == 11) ". " else " ")
                n++
            }
            sb.append("\n\n")
        }
        return sb.toString()
    }

    @Test fun detectaTitulos() {
        assertTrue(Texto.ehTituloCapitulo("Capítulo 12"))
        assertTrue(Texto.ehTituloCapitulo("CAPITULO 3 - O Juramento"))
        assertTrue(Texto.ehTituloCapitulo("Chapter 1205: The End"))
        assertTrue(Texto.ehTituloCapitulo("Prólogo"))
        assertTrue(Texto.ehTituloCapitulo("# A Fenda"))
        assertTrue(Texto.ehTituloCapitulo("Cap. IV"))
        assertFalse(Texto.ehTituloCapitulo("O capítulo anterior terminou de forma estranha e todos ficaram calados."))
        assertFalse(Texto.ehTituloCapitulo(""))
        assertTrue(Texto.ehTituloCapitulo("Vol 2 parte", "Vol"))
    }

    @Test fun tituloEstritoDoEditor() {
        assertTrue(Texto.ehTituloEstrito("Capítulo 2"))
        assertTrue(Texto.ehTituloEstrito("Capítulo 12 — O Juramento"))
        assertTrue(Texto.ehTituloEstrito("capitulo 3: Fogo"))
        assertTrue(Texto.ehTituloEstrito("Prólogo"))
        assertTrue(Texto.ehTituloEstrito("Prólogo —"))
        assertTrue(Texto.ehTituloEstrito("Epílogo — Depois"))
        assertFalse(Texto.ehTituloEstrito("Capítulo 3 foi o pior de todos."))
        assertFalse(Texto.ehTituloEstrito("O prólogo dele era longo."))
        assertFalse(Texto.ehTituloEstrito("— Capítulo 2 — disse ela."))
        assertEquals("Prólogo", Texto.tituloLimpo("Prólogo —"))
        assertEquals("PRÓLOGO", Texto.tipoEspecial("Prólogo — A Noite"))
    }

    @Test fun contaPalavras() {
        assertEquals(5, Texto.contarPalavras("Olá, mundo — tudo bem? Sim."))
        assertEquals(0, Texto.contarPalavras("  ... — "))
    }

    @Test fun pedacosReconstroemTextoExato() {
        listOf(10, 5_000, 50_000).forEach { n ->
            val t = gerar(n, n)
            val partes = Texto.dividirEmPedacos(t, 4000)
            assertEquals(t, partes.joinToString(""))
            assertTrue(partes.all { it.length < 4000 * 3 })
        }
        // linha única gigante, sem quebras
        val linha = gerar(30_000).replace("\n", " ")
        val partes = Texto.dividirEmPedacos(linha, 4000)
        assertEquals(linha, partes.joinToString(""))
        assertTrue(partes.size > 10)
    }

    @Test fun htmlEpub() {
        val md = Texto.htmlParaMarkdown("<html><body><h1>Capítulo 1</h1><p>Ela <em>riu</em> &amp; <b>saiu</b>.</p><hr/><p>Fim&#8230;</p></body></html>")
        assertEquals("# Capítulo 1\n\nEla *riu* & **saiu**.\n\n* * *\n\nFim…", md)
        val x = Texto.markdownParaXhtml(md)
        assertTrue(x.contains("<em>riu</em>") && x.contains("<hr/>") && x.contains("&amp;"))
    }

    @Test fun normalizaBusca() {
        assertEquals("coracao acao", Texto.normalizar("Coração AÇÃO"))
    }

    /** Medições reais no servidor de build (não no celular). */
    @Test fun desempenho() {
        val linhas = StringBuilder()
        listOf(5_000, 20_000, 35_000, 50_000, 100_000).forEach { n ->
            val t = gerar(n, n)
            val kb = t.length / 1024
            val t0 = System.nanoTime(); val c = Texto.contarPalavras(t)
            val t1 = System.nanoTime(); val partes = Texto.dividirEmPedacos(t)
            val t2 = System.nanoTime(); val norm = Texto.normalizar(t); var achados = 0; var i = norm.indexOf("juramento")
            while (i >= 0) { achados++; i = norm.indexOf("juramento", i + 1) }
            val t3 = System.nanoTime()
            assertTrue(c in (n - 5)..(n + 5))
            fun ms(a: Long, b: Long) = "%.1f".format((b - a) / 1e6)
            linhas.append("$n palavras (${kb} KB): contar ${ms(t0, t1)} ms · dividir em ${partes.size} blocos ${ms(t1, t2)} ms · buscar ($achados achados) ${ms(t2, t3)} ms\n")
        }
        File("build").mkdirs()
        File("build/desempenho.txt").writeText(linhas.toString())
    }
}
