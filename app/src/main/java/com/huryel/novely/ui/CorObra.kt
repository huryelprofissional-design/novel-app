package com.huryel.novely.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.huryel.novely.NovelyApp
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cor de destaque tirada da capa: a cor viva que mais aparece, clareada para ler bem sobre o preto.
 * Sem capa (ou capa cinza), usa a cor base da obra.
 */
object CorDaObra {
    private val cache = HashMap<String, Int>()

    fun deBytes(b: ByteArray?): Int? {
        if (b == null) return null
        val o = BitmapFactory.Options().apply { inSampleSize = 8 }
        return try { BitmapFactory.decodeByteArray(b, 0, b.size, o)?.let { calcular(it) } } catch (_: Exception) { null }
    }

    fun deArquivo(caminho: String?): Int? {
        if (caminho.isNullOrEmpty()) return null
        cache[caminho]?.let { return it }
        val o = BitmapFactory.Options().apply { inSampleSize = 16 }
        val c = try { BitmapFactory.decodeFile(caminho, o)?.let { calcular(it) } } catch (_: Exception) { null }
        if (c != null) cache[caminho] = c
        return c
    }

    private fun calcular(bmp: android.graphics.Bitmap): Int? {
        val pesos = DoubleArray(24); val somaR = DoubleArray(24); val somaG = DoubleArray(24); val somaB = DoubleArray(24)
        val hsv = FloatArray(3)
        val passo = maxOf(1, maxOf(bmp.width, bmp.height) / 60)
        var y = 0
        while (y < bmp.height) {
            var x = 0
            while (x < bmp.width) {
                val px = bmp.getPixel(x, y)
                android.graphics.Color.colorToHSV(px, hsv)
                val peso = (hsv[1] * hsv[2]).toDouble().let { it * it }
                if (hsv[1] > 0.22f && hsv[2] > 0.18f) {
                    val k = ((hsv[0] / 15f).toInt()).coerceIn(0, 23)
                    pesos[k] += peso; somaR[k] += android.graphics.Color.red(px) * peso; somaG[k] += android.graphics.Color.green(px) * peso; somaB[k] += android.graphics.Color.blue(px) * peso
                }
                x += passo
            }
            y += passo
        }
        val melhor = pesos.indices.maxByOrNull { pesos[it] } ?: return null
        if (pesos[melhor] < 0.5) return null
        val cor = android.graphics.Color.rgb((somaR[melhor] / pesos[melhor]).toInt(), (somaG[melhor] / pesos[melhor]).toInt(), (somaB[melhor] / pesos[melhor]).toInt())
        return realcar(cor)
    }

    /** Deixa a cor viva e clara o bastante para texto e botões sobre o preto. */
    fun realcar(cor: Int): Int {
        val hsv = FloatArray(3); android.graphics.Color.colorToHSV(cor, hsv)
        hsv[1] = hsv[1].coerceIn(0.45f, 0.78f); hsv[2] = hsv[2].coerceIn(0.88f, 1f)
        return android.graphics.Color.HSVToColor(hsv)
    }
}

/** Cor de destaque da obra (ou null quando o recurso está desligado). */
@Composable
fun rememberCorObra(capa: String?, corBase: Long): Color? {
    val ligado by NovelyApp.instancia.prefs.corDaObra.estado.collectAsState()
    val cor by produceState<Int?>(null, capa, corBase) {
        value = withContext(Dispatchers.IO) { CorDaObra.deArquivo(capa) ?: CorDaObra.realcar(corBase.toInt()) }
    }
    return if (ligado) cor?.let { Color(it) } else null
}

@Composable
fun rememberCorBytes(b: ByteArray?): Color? {
    val ligado by NovelyApp.instancia.prefs.corDaObra.estado.collectAsState()
    val cor by produceState<Int?>(null, b) { value = withContext(Dispatchers.IO) { CorDaObra.deBytes(b) } }
    return if (ligado) cor?.let { Color(it) } else null
}

/** Pinta os destaques da tela com a cor da obra. */
@Composable
fun ComCorDaObra(cor: Color?, conteudo: @Composable () -> Unit) {
    val p = LocalPaleta.current
    if (cor == null) { conteudo(); return }
    val nova = remember(p, cor) { p.copy(destaque = cor, sobreDestaque = if (cor.luminance() > 0.45f) Color(0xFF0B0B0B) else Color.White) }
    CompositionLocalProvider(LocalPaleta provides nova) { conteudo() }
}

@Suppress("unused") private fun Color.hex() = String.format("#%06X", 0xFFFFFF and toArgb())
