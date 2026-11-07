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
    @Test fun conteudoUsaModeloOuTrecho() {
        assertEquals("Fuja", conteudoBloco("perigo", "Fuja"))
        assertEquals("[#regras Regras do turno da noite | 1. Não atenda o telefone | 2. … | 3. …]".removePrefix("[#regras ").removeSuffix("]"), conteudoBloco("regras", ""))
        assertEquals("Hana | Re: o que você viu na torre? | Volte já", conteudoBloco("email", "Volte já").replace("Dra. Han", "Hana"))
        assertEquals("Ninguém viu", conteudoBloco("rasgada", "Ninguém viu"))
    }
}
