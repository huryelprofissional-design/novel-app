package com.huryel.novely.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FxTest {
    @Test fun blocoComCampos() {
        val u = Fx.lerLinha("[Sistema | Missão aceita.]")!!
        assertEquals(FxTipo.JANELA, u.def.tipo)
        assertEquals(listOf("Missão aceita."), u.campos)
    }

    @Test fun nomeComAcentoEEspaco() {
        assertEquals(FxTipo.PULSO, Fx.lerLinha("[pulso parado | ele parou]")!!.def.tipo)
        assertEquals(FxTipo.JANELA, Fx.lerLinha("[missão | Sobreviva | 500 XP]")!!.def.tipo)
    }

    @Test fun linhaInteiraComFala() {
        val u = Fx.lerLinha("— {sussurro: fica quieto}.")!!
        assertEquals("— fica quieto.", u.campos.single())
    }

    @Test fun cena() {
        val u = Fx.lerLinha("::som chuva")!!
        assertEquals("som", u.nome); assertEquals(listOf("chuva"), u.campos)
    }

    @Test fun textoComumNaoViraEfeito() {
        assertNull(Fx.lerLinha("[nota do autor]"))
        assertNull(Fx.lerLinha("Ele disse {algo} estranho."))
        assertNull(Fx.lerLinha("— Vem — disse Kang."))
    }

    @Test fun limpezaNaoApagaEfeitosNovos() {
        val t = "{sussurro: fica}\n[vida | 100 | 20]\n::vibra\nEle {cora: corou} e saiu."
        assertEquals(t, Limpeza.limparTexto(t))
        assertNotNull(Fx.lerLinha(Limpeza.limparTexto("[chat | Ana: oi | eu: oi]")))
    }

    @Test fun textoSimples() {
        assertEquals("Ele corou e saiu.", Fx.textoSimples("Ele {cora: corou} e saiu."))
        assertEquals("SISTEMA. Missão aceita.", Fx.textoSimples("[sistema | Missão aceita.]"))
    }
}
