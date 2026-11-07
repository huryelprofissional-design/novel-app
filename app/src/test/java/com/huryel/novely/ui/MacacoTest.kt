package com.huryel.novely.ui

import com.huryel.novely.data.Ancora
import com.huryel.novely.data.Biblia
import com.huryel.novely.data.CamposFicha
import com.huryel.novely.data.Ficha
import com.huryel.novely.data.Marcas
import com.huryel.novely.data.Texto
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/**
 * MODO CAOS: joga milhares de textos absurdos em tudo que lê o que a pessoa escreve.
 * Qualquer exceção aqui é um crash no celular de alguém (o leitor e o editor rodam essas funções a cada tecla).
 */
class MacacoTest {
    private val pedacos = listOf(
        "[#", "[#]", "[# ]", "[#sistema", "[#sistema+", "[#sistema-:", "[#sistema+:vermelho", "[#sistema:: |", "[#a-b-c-]", "[#chefe | | | |]",
        "{", "}", "{fogo", "{fogo:", "{fogo: ", "{fogo+:azul:", "{fogo+-: x}", "{{", "}}", "{{x", "{ {fogo: }}", "{neon:roxo: }",
        "[[", "]]", "[[ | ]]", "[[x]] [[y", "[!", "!]", "[!!]", "[+", "[+ |]", "::", ":: |", ":: | | |", ">", ">>", ">>>", ">>:", ">>Nome:", ">>>Nome:",
        "##", "###", "== ", "@@", "@@ ", "N.A.:", "~", "~chuva", "~fim", "~pausa", "~~", "~~~~", "!!", "!!!!", "((", "))", "^^", "||", "|||", "%%", "<<", ">>",
        "[S]", "[SSS]", "* * *", "***", "—", "– ", "\"", "“", "”", "'", "\\", "/", "\u0000", "​", "‏", "‮", "﻿", "\r", "\r\n", "\t",
        "🔥", "👨‍👩‍👧‍👦", "\uD83D", "\uDE00", "ﷺ", "𝕏", "ء", "あ", "<script>alert(1)</script>", "'; DROP TABLE--", "\${jndi:ldap://x}", "%s%s%n",
        "Seu", "Dona", "Cristóvão", "Seu Cristóvão", " ", "\n", "\n\n", "1: vivo", "cap 12:", "99999999999: x", "-1: x", ": : :", "a", "AAAA",
    )

    private fun lixo(r: Random, n: Int) = buildString { repeat(n) { append(pedacos[r.nextInt(pedacos.size)]); if (r.nextInt(4) == 0) append(' ') } }

    @Test fun nadaQuebraComTextoAbsurdo() {
        val r = Random(20261107)
        val fichas = listOf(Ficha(1, 1, nome = "Seu Cristóvão", apelidos = "Cris, , ,"), Ficha(2, 1, nome = "{fogo: x}"), Ficha(3, 1, nome = "A"))
        repeat(3000) { i ->
            val t = lixo(r, 1 + r.nextInt(40)).let { if (i % 7 == 0) it.replace(" ", "\n") else it }
            try {
                montarBlocos(t, ArrayList())
                t.lines().forEach { estilizar(it); Marcas.semEfeito(it); lerBlocoFx(it.trim()); forcaBlocoFx(it.trim()); lerInlineFx(it, 0) }
                Marcas.semEfeitoNoTrecho(t); Marcas.limparEfeitos(t); Marcas.limpar(t); Marcas.encontrar(t, listOf("Seu Cristóvão", "Cris"))
                Marcas.confianca(t, r.nextInt(t.length + 1), "Cristóvão", r.nextBoolean(), r.nextBoolean())
                Marcas.inicioDeFrase(t, r.nextInt(t.length + 1))
                Marcas.partesUnicas(mapOf(t to 1L, "Seu Cristóvão" to 2L))
                Marcas.escreverEfeito("fogo", if (r.nextBoolean()) null else "azul", r.nextInt(3) - 1, t, r.nextBoolean())
                CamposFicha.extras(t); CamposFicha.porCapitulo(t); CamposFicha.condicaoEm(t, r.nextInt(50)); CamposFicha.segredosAte(t, r.nextInt(50))
                Ancora.ancorar(t.lines(), r.nextInt(5) - 1, t)
                Texto.contarPalavras(t); Texto.normalizar(t); Texto.dividirEmPedacos(t, 1 + r.nextInt(50)).joinToString("").let { assertEquals("dividir não perde texto", t, it) }
                contagens(t)
            } catch (e: Throwable) {
                throw AssertionError("QUEBROU com o texto #$i: ${t.take(300).map { if (it.code < 32 || it.code in 0xD800..0xDFFF || it.code in 0x200B..0x202E) "\\u%04X".format(it.code) else "$it" }.joinToString("")}", e)
            }
        }
        // a Bíblia lê tudo de uma vez
        val caps = (1..20).map { Biblia.Cap(it.toLong(), lixo(r, 3), lixo(r, 200)) }
        Biblia.analisar(caps, fichas)
    }

    @Test fun capituloGigante() {
        // 10 MB de "A" sem espaço nem quebra: o editor divide em pedaços sem travar e sem perder letra
        val t = "A".repeat(10_000_000)
        val p = Texto.dividirEmPedacos(t)
        assertEquals(t.length, p.sumOf { it.length })
        assertEquals(1, Texto.contarPalavras(t))
        montarBlocos("A".repeat(2_000_000))
        // 50 mil linhas vazias e emojis
        montarBlocos("\n".repeat(50_000) + "🔥".repeat(100_000))
    }
}
