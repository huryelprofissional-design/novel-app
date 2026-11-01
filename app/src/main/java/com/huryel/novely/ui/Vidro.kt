package com.huryel.novely.ui

import android.graphics.ColorMatrixColorFilter
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

/**
 * Vidro líquido (igual ao exemplo em HTML): o que está ATRÁS do painel aparece através dele com
 *  - desfoque leve, saturação 1,8 e brilho 1,08 (as cores de trás ficam mais vivas);
 *  - refração na borda: perto da beirada o fundo "entorta" para dentro, como uma lente (Android 13+);
 *  - aro de luz na borda, brilho especular no canto de cima e um reflexo azulado embaixo.
 *
 * Como funciona: o conteúdo de fundo ([fonteVidro]) é desenhado numa camada; para cada painel
 * ([vidroLiquido]) o recorte daquela região passa pelos efeitos e é desenhado de novo, só dentro do painel.
 * Android 12: desfoque + cor, sem refração. Android 11 ou menos: painel escuro translúcido.
 */
class EstadoVidro {
    internal val areas = mutableStateMapOf<Any, AreaVidro>()
    internal var origem by mutableStateOf(Offset.Zero)
}

internal data class AreaVidro(val ret: Rect, val raio: Float, val borda: Float, val forca: Float)

@Composable
fun rememberVidro(): EstadoVidro = remember { EstadoVidro() }

val temDesfoque: Boolean get() = Build.VERSION.SDK_INT >= 31

private const val REFRACAO = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float bezel;
uniform float scale;
half4 main(float2 p) {
    float2 c = size * 0.5;
    float2 q0 = p - c;
    float2 q = abs(q0) - (c - radius);
    float2 o = max(q, 0.0);
    float fora = length(o);
    float dentro = min(max(q.x, q.y), 0.0);
    float dist = fora + dentro - radius;
    float2 n = float2(0.0, 0.0);
    if (fora > 0.0) { n = (o / fora) * sign(q0); }
    else if (q.x > q.y) { n = float2(sign(q0.x), 0.0); }
    else { n = float2(0.0, sign(q0.y)); }
    float t = dist > 0.0 ? 0.0 : clamp(1.0 + dist / bezel, 0.0, 1.0);
    float mag = pow(t, 1.4);
    return content.eval(p - n * mag * scale * 0.5);
}
"""

private val corVidro: ColorMatrixColorFilter by lazy {
    val m = android.graphics.ColorMatrix().apply { setSaturation(1.8f) }
    m.postConcat(android.graphics.ColorMatrix().apply { setScale(1.08f, 1.08f, 1.08f, 1f) })
    ColorMatrixColorFilter(m)
}

@RequiresApi(31)
private fun efeito(desfoque: Float, tam: IntSize, a: AreaVidro, shader: Any?): androidx.compose.ui.graphics.RenderEffect {
    val cor = android.graphics.RenderEffect.createColorFilterEffect(corVidro)
    val borrar = android.graphics.RenderEffect.createBlurEffect(desfoque, desfoque, Shader.TileMode.CLAMP)
    var ef = android.graphics.RenderEffect.createChainEffect(cor, borrar) // primeiro borra, depois a cor
    if (Build.VERSION.SDK_INT >= 33 && shader is RuntimeShader) {
        shader.setFloatUniform("size", tam.width.toFloat(), tam.height.toFloat())
        shader.setFloatUniform("radius", a.raio.coerceAtMost(minOf(tam.width, tam.height) / 2f))
        shader.setFloatUniform("bezel", a.borda)
        shader.setFloatUniform("scale", a.forca)
        val refr = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
        ef = android.graphics.RenderEffect.createChainEffect(ef, refr) // a refração vem antes de tudo
    }
    return ef.asComposeRenderEffect()
}

/** Marca o conteúdo que aparece através dos painéis de vidro. */
fun Modifier.fonteVidro(e: EstadoVidro, desfoque: Dp = 10.dp): Modifier = composed {
    val camada = rememberGraphicsLayer()
    val ctx = LocalGraphicsContext.current
    val camadas = remember { HashMap<Any, GraphicsLayer>() }
    val shaders = remember { HashMap<Any, Any>() }
    DisposableEffect(Unit) { onDispose { camadas.values.forEach { ctx.releaseGraphicsLayer(it) }; camadas.clear(); shaders.clear() } }
    this
        .onGloballyPositioned { e.origem = it.positionInRoot() }
        .drawWithContent {
            if (e.areas.isEmpty() || !temDesfoque) { drawContent(); return@drawWithContent }
            camada.record { this@drawWithContent.drawContent() }
            drawLayer(camada)
            camadas.keys.filter { it !in e.areas }.forEach { k -> camadas.remove(k)?.let { ctx.releaseGraphicsLayer(it) }; shaders.remove(k) }
            e.areas.forEach { (id, a) ->
                val local = a.ret.translate(-e.origem)
                val tam = IntSize(local.width.roundToInt(), local.height.roundToInt())
                if (tam.width <= 0 || tam.height <= 0) return@forEach
                val l = camadas.getOrPut(id) { ctx.createGraphicsLayer() }
                val sh = if (Build.VERSION.SDK_INT >= 33) shaders.getOrPut(id) { RuntimeShader(REFRACAO) } else null
                l.renderEffect = efeito(desfoque.toPx(), tam, a, sh)
                l.record(size = tam) { translate(-local.left, -local.top) { drawLayer(camada) } }
                translate(local.left, local.top) {
                    val caminho = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, tam.toSize()), CornerRadius(a.raio))) }
                    clipPath(caminho) { drawLayer(l) }
                }
            }
        }
}

/** Painel de vidro líquido. [borda] = largura da faixa que refrata; [forca] = quanto entorta. */
fun Modifier.vidroLiquido(e: EstadoVidro?, raio: Dp, borda: Dp = 28.dp, forca: Dp = 60.dp): Modifier = composed {
    val id = remember { Any() }
    val dens = LocalDensity.current
    val raioPx = with(dens) { raio.toPx() }
    val bordaPx = with(dens) { borda.toPx() }
    val forcaPx = with(dens) { forca.toPx() }
    DisposableEffect(e) { onDispose { e?.areas?.remove(id) } }
    val real = e != null && temDesfoque
    val forma = RoundedCornerShape(raio)
    this
        .onGloballyPositioned { c -> e?.areas?.put(id, AreaVidro(Rect(c.positionInRoot(), c.size.toSize()), raioPx, bordaPx, forcaPx)) }
        .clip(forma)
        .then(if (real) Modifier else Modifier.background(Color(0xEB181B21)))
        .drawWithContent {
            val w = size.width; val h = size.height
            val cr = CornerRadius(raioPx.coerceAtMost(minOf(w, h) / 2f))
            // tinta do vidro (135°) + brilho especular no alto à esquerda + reflexo azulado embaixo à direita
            drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f)), Offset.Zero, Offset(w, h)))
            drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.26f), Color.Transparent), Offset(w * 0.18f, 0f), maxOf(w, h) * 0.48f))
            drawRect(Brush.radialGradient(listOf(Color(0x3882CDFF), Color.Transparent), Offset(w, h), maxOf(w, h) * 0.6f))
            // brilho interno suave
            drawRoundRect(Color.White.copy(alpha = 0.06f), cornerRadius = cr, style = Stroke(12.dp.toPx()))
            drawContent()
            // luz fina embaixo por dentro
            drawRoundRect(Brush.verticalGradient(0.85f to Color.Transparent, 1f to Color.White.copy(alpha = 0.30f)), cornerRadius = cr, style = Stroke(2.dp.toPx()))
            // aro de luz (mais forte no canto de cima à esquerda e embaixo à direita)
            val s = 1.5.dp.toPx()
            drawRoundRect(
                Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.95f), 0.3f to Color.White.copy(alpha = 0.14f),
                    0.55f to Color.White.copy(alpha = 0.04f), 1f to Color.White.copy(alpha = 0.55f),
                    start = Offset.Zero, end = Offset(w, h),
                ),
                topLeft = Offset(s / 2, s / 2), size = Size(w - s, h - s), cornerRadius = cr, style = Stroke(s),
            )
        }
}
