package com.huryel.novely.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

/**
 * Vidro líquido de verdade: o que está ATRÁS do painel (o texto, a capa…) aparece desfocado através dele.
 *
 * Como funciona: o conteúdo de fundo ([fonteVidro]) se desenha numa camada; depois desenha de novo essa
 * camada borrada, só dentro dos retângulos dos painéis de vidro ([vidroLiquido]), que ficam por cima.
 * O desfoque usa o RenderEffect do Android 12+; em aparelhos mais antigos o painel fica escuro translúcido.
 */
class EstadoVidro {
    internal val areas = mutableStateMapOf<Any, Pair<Rect, Float>>()
    internal var origem by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberVidro(): EstadoVidro = remember { EstadoVidro() }

val temDesfoque: Boolean get() = Build.VERSION.SDK_INT >= 31

/** Marca o conteúdo que aparece borrado atrás dos painéis de vidro. */
fun Modifier.fonteVidro(e: EstadoVidro, desfoque: Dp = 34.dp): Modifier = composed {
    val camada = rememberGraphicsLayer()
    val borrada = rememberGraphicsLayer()
    this
        .onGloballyPositioned { e.origem = it.positionInRoot() }
        .drawWithContent {
            if (e.areas.isEmpty() || !temDesfoque) { drawContent(); return@drawWithContent }
            camada.record { this@drawWithContent.drawContent() }
            drawLayer(camada)
            val r = desfoque.toPx()
            borrada.renderEffect = BlurEffect(r, r, TileMode.Clamp)
            borrada.record { drawLayer(camada) }
            e.areas.values.forEach { (ret, raio) ->
                val local = ret.translate(-e.origem)
                val caminho = Path().apply { addRoundRect(RoundRect(local, CornerRadius(raio))) }
                clipPath(caminho) {
                    drawLayer(borrada)
                    drawRect(Color(0x1A0E1218)) // leve fumê para o texto do painel ler bem
                }
            }
        }
}

/** Painel de vidro líquido: mostra o fundo borrado, um brilho de cima e uma borda de luz. */
fun Modifier.vidroLiquido(e: EstadoVidro?, raio: Dp): Modifier = composed {
    val id = remember { Any() }
    val raioPx = with(LocalDensity.current) { raio.toPx() }
    DisposableEffect(e) { onDispose { e?.areas?.remove(id) } }
    val real = e != null && temDesfoque
    val forma = RoundedCornerShape(raio)
    this
        .onGloballyPositioned { c -> e?.areas?.put(id, Rect(c.positionInRoot(), c.size.toSize()) to raioPx) }
        .clip(forma)
        .then(if (real) Modifier else Modifier.background(Color(0xEB181B21)))
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (real) 0.16f else 0.10f), Color.White.copy(alpha = if (real) 0.05f else 0.02f))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.28f))), forma)
}
