package com.huryel.novely.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.takahirom.roborazzi.captureRoboImage
import com.huryel.novely.data.Biblia
import com.huryel.novely.data.Ficha
import com.huryel.novely.data.TipoFicha
import com.huryel.novely.ui.theme.NovelyTheme
import com.huryel.novely.ui.theme.TemaLeitura
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
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
    private fun foto(nome: String, avancarMs: Long = 0, conteudo: @androidx.compose.runtime.Composable () -> Unit) {
        val act = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        act.setContent { NovelyTheme(TemaLeitura.OLED) { androidx.compose.material3.Surface { conteudo() } } }
        repeat(5) { ShadowLooper.idleMainLooper(); Thread.sleep(30) }
        repeat((avancarMs / 50).toInt()) { ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS) }
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

    @Test fun efeitos() {
        fun obra(id: Long, t: String, cor: Long, lidos: Int, status: String) = com.huryel.novely.data.ResumoObra(id, t, status, null, 0.5f, 0.5f, 1f, cor, null, 1L, 40, 90000, lidos, "", 0, 0)
        val livros = listOf(obra(1, "O Último Mago", 0xFF3B2A4A, 12, "LENDO"), obra(2, "Sangue de Dragão", 0xFF7A1F1F, 40, com.huryel.novely.data.StatusObra.CONCLUIDA),
            obra(3, "Murim Renascido", 0xFF1F4A7A, 3, "LENDO"), obra(4, "A Torre", 0xFF2A5A3A, 20, "LENDO"))
        foto("efeitos", 1500) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(12.dp)) {
                androidx.compose.foundation.layout.Row {
                    livros.take(3).forEachIndexed { i, o -> androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.weight(1f)) { LivroEstante(o, 0, if (i == 0) 3 else null, {}) {} } }
                }
                BannerRetro {}
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(top = 16.dp).fillMaxWidth().height(260.dp)) {
                    androidx.compose.material3.Text("A chuva batia forte na janela enquanto Seo-jun esperava.", modifier = androidx.compose.ui.Modifier.padding(16.dp))
                    ClimaCena("chuva")
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(16.dp)) { Fogueira(640, 1000f, null) {} }
                }
            }
        }
    }
}
