package com.huryel.novely.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CamposFichaTest {
    @Test fun extras() {
        assertEquals(listOf("Arma" to "Espada Rúnica", "Signo" to "Lobo"), CamposFicha.extras("Arma: Espada Rúnica\nsem dois pontos\nSigno: Lobo\nVazio:"))
    }
    @Test fun condicoes() {
        val c = "cap 12: ferido\n1: vivo\nCapítulo 30 — morto"
        assertEquals(listOf(1, 12, 30), CamposFicha.porCapitulo(c).map { it.first })
        assertNull(CamposFicha.condicaoEm(c, 0))
        assertEquals("vivo", CamposFicha.condicaoEm(c, 5))
        assertEquals("ferido", CamposFicha.condicaoEm(c, 29))
        assertEquals("morto", CamposFicha.condicaoEm(c, 31))
    }
    @Test fun segredos() {
        val (vis, faltam) = CamposFicha.segredosAte("15: é filho do rei\n3: tem medo do escuro", 10)
        assertEquals(listOf(3 to "tem medo do escuro"), vis)
        assertEquals(1, faltam)
    }
}

class AncoraTest {
    @Test fun achaDeNovoDepoisDeInserirParagrafos() {
        val antes = listOf("Primeiro parágrafo da cena.", "A porta da torre rangeu e Dorian entrou.", "Fim.")
        val depois = listOf("Primeiro parágrafo da cena.", "Um parágrafo novo que o autor escreveu.", "Outro novo aqui, bem no meio.", "A porta da torre rangeu e Dorian entrou.", "Fim.")
        assertEquals(1, Ancora.ancorar(antes, 1, antes[1]))
        assertEquals(3, Ancora.ancorar(depois, 1, antes[1]))
        assertEquals(1, Ancora.ancorar(depois, 1, "trecho que sumiu do capítulo inteiro"))
    }
}
