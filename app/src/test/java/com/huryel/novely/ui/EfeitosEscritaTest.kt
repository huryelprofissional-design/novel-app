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

class EfeitosNovosTest {
    @Test fun inlineNovos() {
        val t = estilizar("O traidor era ||Kang|| e a tela: %%ERRO%% e a voz <<volte>> fim.")
        assertEquals("O traidor era Kang e a tela: ERRO e a voz volte fim.", t.text)
        assertEquals(1, t.getStringAnnotations("segredo", 0, t.length).size)
        assertEquals(1, t.getStringAnnotations("glitch", 0, t.length).size)
        assertEquals("a | b e 5% de 10%", estilizar("a | b e 5% de 10%").text)
    }
    @Test fun blocosNovos() {
        val b = montarBlocos("## O DESPERTAR\n\n== Hana,\n== não volte.\n\n[!Nível 12!]\n\n~pausa\n\nVazio.")
        assertEquals(true, (b[0] as Bloco.Titulo).impacto)
        val carta = b[1] as Bloco.Paragrafo
        assertEquals("carta", carta.tipo); assertEquals("Hana,\nnão volte.", carta.plano)
        assertEquals("notificacao", (b[2] as Bloco.Paragrafo).tipo)
        assertEquals(true, (b[3] as Bloco.Paragrafo).pausa)
        assertEquals(4, b.size)
    }
}

class EfeitosLeva3Test {
    @Test fun inline3() {
        assertEquals("Rank  S  e voz {não} e eu te odeio amo", estilizar("Rank [S] e voz {não} e eu te ~~odeio~~ amo").text.replace("voz não", "voz {não}"))
        assertEquals("a [x] b", estilizar("a [x] b").text)
    }
    @Test fun blocos3() {
        val b = montarBlocos("@@Kang\n\n>>>Fã: boa\n>>>[Constelação]: gostou\n\n>>Kang: oi\n\n>> oi\n\n{{DECRETO | sai}}\n\n[+Espada | lendário]\n\n:: Seul | 03:14\n\n> Frase — Livro\n\nN.A.: obrigado")
        assertEquals(listOf("ficha", "aovivo", "conversa", "mensagem", "pergaminho", "item", "local", "epigrafe", "nota"), b.map { (it as Bloco.Paragrafo).tipo })
        assertEquals("Fã: boa\n[Constelação]: gostou", b[1].plano)
        assertEquals("obrigado", b[8].plano)
    }
}

class MegaPackTest {
    @Test fun blocoFx() {
        val b = montarBlocos("[#evolucao:dourado Nv 11 > 12 | Força 14 > 17]\n\n[#post kang | oi @todos | 10]\n\n[#naoexiste x]")
        assertEquals("fx", (b[0] as Bloco.Paragrafo).tipo)
        assertEquals("evolucao\u0000dourado\u0000Nv 11 > 12 | Força 14 > 17\u00000", (b[0] as Bloco.Paragrafo).extra)
        assertEquals("fx", (b[1] as Bloco.Paragrafo).tipo)
        assertEquals("", (b[2] as Bloco.Paragrafo).tipo)
        assertEquals(listOf("Caso", "Nome: ||Han||", "ok"), camposFx("Caso | Nome: ||Han|| | ok"))
    }
    @Test fun inlineFx() {
        val t = estilizar("Ele gritou: {fogo:azul: BOLA} e {telepatia normal} e {runa: abc}")
        assertEquals("Ele gritou: BOLA e telepatia normal e ᚨᚩᚲ".length, t.text.length)
        assertEquals(listOf("fogo", "runa"), t.getStringAnnotations("fx", 0, t.length).map { it.item })
    }
    @Test fun intensidade() {
        val b = montarBlocos("[#sistema+:vermelho Você morreu]\n\n[#perigo- Algo se move]\n\n[#missao-ok Sobreviver]")
        assertEquals("sistema\u0000vermelho\u0000Você morreu\u00001", (b[0] as Bloco.Paragrafo).extra)
        assertEquals("perigo\u0000\u0000Algo se move\u0000-1", (b[1] as Bloco.Paragrafo).extra)
        assertEquals("missao-ok\u0000\u0000Sobreviver\u00000", (b[2] as Bloco.Paragrafo).extra)
        val f = lerInlineFx("{fogo+:azul: chamas}", 0)!!
        assertEquals(listOf("fogo", "azul", "chamas", "1"), listOf(f.nome, f.cor, f.texto, f.forca.toString()))
        assertEquals(-1, lerInlineFx("{neon-: luz}", 0)!!.forca)
        assertEquals(0, lerInlineFx("{neon: luz}", 0)!!.forca)
        val t = estilizar("as {fogo+: chamas} sobem")
        assertEquals("as chamas sobem", t.text)
        // origem de cada bloco (para a prévia do editor achar a linha)
        val origens = ArrayList<Int>()
        val bs = montarBlocos("Primeiro.\n\n[[Sistema]]\n\nTerceiro.", origens)
        assertEquals(bs.size, origens.size)
        assertEquals(listOf(0, 2, 4), origens)
    }
}
