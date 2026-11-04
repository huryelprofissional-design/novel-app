package com.huryel.novely.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EfeitosEscritaTest {
    @Test fun cenaVaiAteFim() {
        val b = montarBlocos("Antes.\n\n~chuva\n\nMolhado.\n\n[[Nível 3 alcançado]]\n\n>> cadê vc\n\n~fim\n\nSeco.")
        assertEquals(listOf("Antes.", "Molhado.", "Nível 3 alcançado", "cadê vc", "Seco."), b.map { it.plano })
        val ps = b.map { it as Bloco.Paragrafo }
        assertNull(ps[0].clima); assertEquals("chuva", ps[1].clima); assertEquals("sistema", ps[2].tipo); assertEquals("mensagem", ps[3].tipo); assertNull(ps[4].clima)
    }
    @Test fun simbolosSomem() {
        assertEquals("Ele disse VEM e baixinho psiu, a espada brilhou.", estilizar("Ele disse !!VEM!! e baixinho ((psiu)), a ^^espada^^ brilhou.").text)
        assertEquals("Socorro!! Alguém!!", estilizar("Socorro!! Alguém!!").text)
        assertEquals("Ah (sim) ok", estilizar("Ah (sim) ok").text)
    }
    @Test fun trocaDireta() {
        val b = montarBlocos("~neve\nA.\n~tensão\nB.\n~pare\nC.").map { (it as Bloco.Paragrafo).clima }
        assertEquals(listOf("neve", "tensao", null), b)
    }
}
