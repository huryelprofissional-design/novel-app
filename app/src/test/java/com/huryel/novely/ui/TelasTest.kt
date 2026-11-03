package com.huryel.novely.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.takahirom.roborazzi.captureRoboImage
import com.huryel.novely.data.Biblia
import com.huryel.novely.data.Ficha
import com.huryel.novely.data.TipoFicha
import com.huryel.novely.ui.theme.NovelyTheme
import com.huryel.novely.ui.theme.TemaLeitura
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
}
