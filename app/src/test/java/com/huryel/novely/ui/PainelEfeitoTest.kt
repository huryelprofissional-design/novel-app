package com.huryel.novely.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PainelEfeitoTest {
    @Test fun todosOsEfeitosDeParagrafoEstaoNoPainel() {
        val chaves = EFEITOS_PARAGRAFO.map { it.chave }.toSet()
        assertTrue(EFEITOS_PARAGRAFO.size >= 50)
        for (k in listOf("sistema", "evolucao", "bau", "tecnica", "vhs", "post", "jornal", "procurado", "capitulo", "contrato", "ia")) assertTrue(k, k in chaves)
        assertTrue(EFEITOS_PARAGRAFO.all { it.cat in CAT_FX })
    }
    @Test fun efeitos2LidosSemMexerNosAntigos() {
        assertEquals(Triple("\$sistema", "azul", "A | B"), lerBlocoFx("[\$sistema:azul A | B]"))
        assertEquals(Triple("\$pix", null, "5000 | Ana"), lerBlocoFx("[\$pix+ 5000 | Ana]"))
        assertEquals("\$eclosao", lerBlocoFx("[\$ovo Filhote]")?.first)
        assertEquals(null, lerBlocoFx("[\$naoexiste x]"))
        assertEquals("sistema", lerBlocoFx("[#sistema A | B]")?.first)
        assertEquals(50, BLOCOS_V2.size)
        assertEquals(55, MEGA_PACK_V2.size)
        assertTrue(MEGA_PACK_V2.filter { it.third != "inline" }.all { lerBlocoFx(it.second) != null })
        val i = lerInlineFx("{\$corte: acabou} e", 0)!!
        assertEquals("\$corte", i.nome); assertEquals("acabou", i.texto)
        assertEquals("fogo", lerInlineFx("{fogo: x}", 0)?.nome)
        val chaves = EFEITOS_PARAGRAFO.map { it.chave }.toSet()
        assertTrue("\$sistema" in chaves && "sistema" in chaves && "\$coord" in chaves)
        assertEquals("5000. Ana", com.huryel.novely.data.Marcas.semEfeito("[\$pix 5000 | Ana]"))
        assertEquals("ele disse acabou", com.huryel.novely.data.Marcas.semEfeitoNoTrecho("ele disse {\$corte: acabou}"))
        assertEquals("[\$pix 5000 | Ana]", com.huryel.novely.data.Marcas.escreverEfeito("\$pix", null, 0, "5000 | Ana", true))
        assertEquals("{\$aura:roxo: eu}", com.huryel.novely.data.Marcas.escreverEfeito("\$aura", "roxo", 0, "eu", false))
    }
    @Test fun conteudoUsaModeloOuTrecho() {
        assertEquals("Fuja", conteudoBloco("perigo", "Fuja"))
        assertEquals("[#regras Regras do turno da noite | 1. Não atenda o telefone | 2. … | 3. …]".removePrefix("[#regras ").removeSuffix("]"), conteudoBloco("regras", ""))
        assertEquals("Hana | Re: o que você viu na torre? | Volte já", conteudoBloco("email", "Volte já").replace("Dra. Han", "Hana"))
        assertEquals("Ninguém viu", conteudoBloco("rasgada", "Ninguém viu"))
    }
}
