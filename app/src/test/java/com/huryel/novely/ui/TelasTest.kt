package com.huryel.novely.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.takahirom.roborazzi.captureRoboImage
import com.huryel.novely.data.Biblia
import com.huryel.novely.data.Ficha
import com.huryel.novely.data.TipoFicha
import com.huryel.novely.ui.theme.NovelyTheme
import com.huryel.novely.ui.theme.TemaLeitura
import androidx.compose.foundation.verticalScroll
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

// Fotografa telas com dados de exemplo (PNG em build/telas) para conferir o visual sem celular.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w400dp-h860dp-xxhdpi")
class TelasTest {
    private fun foto(nome: String, conteudo: @androidx.compose.runtime.Composable () -> Unit) {
        val act = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        act.setContent { NovelyTheme(TemaLeitura.OLED) { androidx.compose.material3.Surface { conteudo() } } }
        repeat(5) { ShadowLooper.idleMainLooper(); Thread.sleep(30) }
        java.io.File("build/telas").mkdirs()
        act.window.decorView.captureRoboImage("build/telas/$nome.png")
    }

    private val exemplo by lazy {
        val fichas = listOf(
            Ficha(1, 1, nome = "Han Seo-jun", apelidos = "Seo-jun", idade = "17", aparencia = "olhos castanhos"),
            Ficha(2, 1, nome = "Kang Min-ho"), Ficha(3, 1, nome = "Baek Do-hyun"), Ficha(4, 1, nome = "Lee Hana"),
            Ficha(5, 1, nome = "Choi Yuna"), Ficha(6, 1, nome = "Ginásio Lee", tipo = TipoFicha.LUGAR),
        )
        val c = listOf(
            "Seo-jun tinha dezessete anos. Os olhos castanhos de Seo-jun fitavam o chão.\n\n— Levanta — disse Kang Min-ho.\n\nSeo-jun treinou no Ginásio Lee com Kang Min-ho, o mestre amigo que ajudou.\n\n* * *\n\nBaek Do-hyun socou Seo-jun, o rival. Baek Do-hyun golpeou de novo.\n\n* * *\n\nLee Hana abraçou Seo-jun. Lee Hana corou.",
            "Seo-jun estava com o braço quebrado. Seo-jun socou a parede.\n\nOs olhos verdes de Seo-jun brilhavam.\n\n* * *\n\nLee Hana beijou Seo-jun.\n\n* * *\n\nChoi Yuna, irmã de Lee Hana, chegou. Choi Yuna e Lee Hana, família.\n\n* * *\n\nBaek Do-hyun morreu na queda.",
            "— Voltei — disse Baek Do-hyun.\n\nKang Min-ho treinou Seo-jun, amigo. Seo-jun agradeceu.\n\n* * *\n\nChoi Yuna ajudou Seo-jun. Choi Yuna e Seo-jun treinaram juntos.\n\n* * *\n\nChoi Yuna e Lee Hana, irmã e família, voltaram para casa.",
        ).mapIndexed { i, t -> Biblia.Cap(i + 1L, "Capítulo ${i + 1}", t) }
        Biblia.analisar(c, fichas)
    }

    @Test fun bibliaViva() {
        for (aba in listOf("ALERTAS", "PESSOAS", "MAPA", "RELACOES")) foto("biblia_$aba") { ConteudoBiblia(exemplo, aba, {}, {}, { _, _ -> }) }
    }

    @Test fun leitores() {
        val L = com.huryel.novely.data.Publicador::Leitura
        val leituras = ArrayList<com.huryel.novely.data.Publicador.Leitura>()
        val r = java.util.Random(7)
        for (u in 1..40) {
            var cap = 1
            while (cap <= 6) {
                val total = 60
                val larga = r.nextInt(100) < (if (cap == 3) 45 else 12)
                val bloco = if (larga) (if (cap == 3) 34 + r.nextInt(3) else r.nextInt(total)) else total - 1
                leituras.add(L("u$u", "c$cap", bloco, total, 300L + r.nextInt(600), 1L, System.currentTimeMillis() - r.nextInt(14) * 86_400_000L, if (u % 4 == 0) "pc" else "android"))
                if (larga) break
                cap++
            }
        }
        val pars = (0 until 60).map { "Parágrafo $it: Seo-jun olhou a chuva bater na janela do ginásio e pensou no que o pai tinha dito." }
        val caps = (1..6).map { Triple("c$it", listOf("O Chão", "Primeiro Soco", "A Conversa Longa", "Sangue", "Final do Torneio", "Depois")[it - 1], pars) }
        val n = calcularNumeros(caps, leituras, mapOf("c1" to (12L to 4L)))
        org.junit.Assert.assertEquals(40, n.leitores)
        org.junit.Assert.assertTrue(n.caps[2].paradaBloco in 34..36)
        foto("leitores") { ConteudoEstatisticas(n, abertoInicial = 2) }
    }

    @Test fun megaPack() {
        val ex = listOf(
            "evolucao" to "Nv 11 > 12 | Força 14 > 17 | Agilidade 9 > 11", "missao-ok" to "Sobreviver à primeira noite | +300 XP | +1 Baú",
            "reino" to "Condensação > Fundação > Núcleo Dourado > Alma Nascente | 3", "arquivo" to "Caso 0417 | Nome: ||Han Seo-jun|| | Status: ||VIVO||",
            "grupo" to "Família | Mãe: alguém viu o Ji-woo? | eu: tava comigo", "capitulo" to "12 | O Rei sem Nome",
        )
        foto("megapack") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                ex.forEach { (n, r) -> BlocoFx(n, if (n == "evolucao") "dourado" else null, r, androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(17f, androidx.compose.ui.unit.TextUnitType.Sp))) }
            }
        }
    }
}
