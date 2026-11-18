package com.huryel.novely.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.data.FxTipo
import com.huryel.novely.data.FxUso
import com.huryel.novely.ui.theme.InstrumentSerif
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ───────── Efeitos de escrita no leitor (catálogo em data/Fx.kt) ─────────

private val FxRoxo = Color(0xFF9D7BFF)
private val FxCreme = Color(0xFFEFE8FF)
private val FxSuave = Color(0xFF8D82A8)
private val FxSangue = Color(0xFFE0445A)
private val FxOuro = Color(0xFFF3D27A)
private val FxRosa = Color(0xFFFF86B6)
private val FxGelo = Color(0xFFD6F2FF)
private val FxCiano = Color(0xFF7FE3FF)
private val FxVerde = Color(0xFF4DFF9A)
private val FxMono = FontFamily.Monospace

private fun fase(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun suave(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)

/** Progresso 0→1 que recomeça quando [vez] muda; parado em 1 com "reduzir animações". */
@Composable
private fun progresso(vez: Int, dur: Int): Float {
    val sem = reduzirAnimacoes()
    val a = remember(vez) { Animatable(if (sem) 1f else 0f) }
    LaunchedEffect(vez, sem) { if (!sem) a.animateTo(1f, tween(dur, easing = LinearEasing)) }
    return a.value
}

private fun vibrar(ctx: android.content.Context, padrao: LongArray) {
    try {
        val v = if (android.os.Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(android.os.VibratorManager::class.java))?.defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (v != null && v.hasVibrator()) v.vibrate(android.os.VibrationEffect.createWaveform(padrao, -1))
    } catch (_: Exception) {}
}

private fun cs(u: FxUso): List<String> = u.campos.ifEmpty { u.def.padrao.split('|').filter { it.isNotBlank() } }
private fun num(s: String?, pad: Float) = s?.replace(".", "")?.replace(",", ".")?.filter { it.isDigit() || it == '.' || it == '-' }?.toFloatOrNull() ?: pad

/** Estilo de um {nome: texto} no meio da frase (sem animação). */
fun estiloFxInline(nome: String): androidx.compose.ui.text.SpanStyle {
    val d = com.huryel.novely.data.Fx.def(nome) ?: return androidx.compose.ui.text.SpanStyle()
    val brilho = { c: Color -> Shadow(c.copy(alpha = 0.7f), blurRadius = 14f) }
    return when (d.modo) {
        "dourar" -> androidx.compose.ui.text.SpanStyle(color = FxOuro, shadow = brilho(FxOuro))
        "sangrar" -> androidx.compose.ui.text.SpanStyle(color = FxSangue)
        "neon" -> androidx.compose.ui.text.SpanStyle(color = Color(0xFFFF6AD5), shadow = brilho(Color(0xFFFF3CCF)))
        "congelar" -> androidx.compose.ui.text.SpanStyle(color = FxGelo, shadow = brilho(FxCiano))
        "sussurro", "quente" -> androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic, color = if (d.modo == "quente") FxRosa else FxSuave, fontSize = 0.88.em2())
        "grito", "furia", "intrusa" -> androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = if (d.modo == "grito") Color.Unspecified else FxSangue)
        "corar" -> androidx.compose.ui.text.SpanStyle(color = FxRosa, shadow = brilho(FxRosa))
        "aura", "esmaga" -> androidx.compose.ui.text.SpanStyle(color = Color(0xFFE6DCFF), shadow = brilho(FxRoxo))
        "sepia" -> androidx.compose.ui.text.SpanStyle(color = Color(0xFFE3CC9E), fontStyle = FontStyle.Italic)
        "holo", "scan" -> androidx.compose.ui.text.SpanStyle(color = FxCiano, fontFamily = FxMono)
        "fantasma" -> androidx.compose.ui.text.SpanStyle(color = Color(0xFFCFD8FF).copy(alpha = 0.6f), fontStyle = FontStyle.Italic)
        else -> when (d.tipo) {
            FxTipo.DECIFRAR -> androidx.compose.ui.text.SpanStyle(fontFamily = FxMono, color = FxCiano)
            FxTipo.DIGITAR -> androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic)
            else -> androidx.compose.ui.text.SpanStyle(color = Color(0xFFD9CCFF), fontStyle = FontStyle.Italic)
        }
    }
}
private fun Double.em2() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)

/** Um efeito de linha inteira. Toque nele para ver de novo. */
@Composable
fun FxBloco(u: FxUso, estilo: TextStyle, modifier: Modifier = Modifier) {
    var vez by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    LaunchedEffect(vez) {
        when (u.def.modo) {
            "vibra" -> vibrar(ctx, longArrayOf(0, 120, 80, 160))
            "batida" -> vibrar(ctx, longArrayOf(0, 60, 120, 60, 420, 60, 120, 60))
            "esmaga", "alarme" -> vibrar(ctx, longArrayOf(0, 40))
        }
        if (u.def.modo == "som") {
            val tipo = com.huryel.novely.data.Fx.chave(u.campos.firstOrNull().orEmpty())
            when {
                tipo in com.huryel.novely.data.Ambiente.TIPOS.map { it.first } -> com.huryel.novely.data.Ambiente.tocar(tipo)
                tipo == "fim" || tipo == "parar" || tipo == "silencio" -> com.huryel.novely.data.Ambiente.parar()
            }
        }
    }
    val base = estilo.copy(textAlign = TextAlign.Center, fontSize = estilo.fontSize * 1.08f)
    Box(modifier.fillMaxWidth().padding(vertical = 6.dp)
        .clickable(remember { MutableInteractionSource() }, null) { vez++ }, contentAlignment = Alignment.Center) {
        when (u.def.tipo) {
            FxTipo.LETRAS -> FxLetras(cs(u).joinToString(" "), u.def.modo, base, vez)
            FxTipo.FRASE -> FxFrase(cs(u).joinToString(" ").ifEmpty { if (u.def.modo == "batida" || u.def.modo == "coracao") "♥" else "" }, u.def.modo, base, vez)
            FxTipo.DIGITAR -> FxDigitar(cs(u).joinToString(" "), base.copy(fontStyle = if (u.def.modo == "italico") FontStyle.Italic else base.fontStyle), vez)
            FxTipo.DECIFRAR -> FxDecifrar(cs(u).joinToString(" "), vez)
            FxTipo.TRADUZ -> FxTraduz(cs(u), base, vez)
            FxTipo.CORRIGE -> FxCorrige(cs(u), base, vez)
            FxTipo.ECO -> FxEco(cs(u).joinToString(" "), vez)
            FxTipo.MULTIDAO -> FxMultidao(cs(u), vez)
            FxTipo.JANELA -> FxJanela(u.def.modo, u.def.rotulo, cs(u), vez)
            FxTipo.FICHA -> FxFicha(cs(u), u.def.modo == "vermelho", vez)
            FxTipo.CONTAR -> FxContar(cs(u), u.def.modo, u.def.rotulo, vez)
            FxTipo.BARRA -> FxBarra(cs(u), u.def.modo, u.def.rotulo, vez)
            FxTipo.CHAT -> FxChat(cs(u), u.def.modo, vez)
            FxTipo.NOTIF -> FxNotif(u.def.rotulo, cs(u).joinToString(" "), vez)
            FxTipo.CARTA -> FxCarta(cs(u).joinToString("\n"), vez)
            FxTipo.SELO -> FxSelo(u.def.rotulo + cs(u).joinToString(" "), u.def.modo, vez)
            FxTipo.CONQUISTA -> FxConquista(u.def.rotulo, cs(u).joinToString(" "), vez)
            FxTipo.QUADROS -> FxQuadros(cs(u), vez)
            FxTipo.DIVIDE -> FxDivide(cs(u), vez)
            FxTipo.ESCOLHA -> FxEscolha(cs(u), vez)
            FxTipo.PULSO -> FxPulso(u.def.modo == "plano", cs(u).joinToString(" "), vez)
            FxTipo.LIGACAO -> FxLigacao(cs(u).firstOrNull() ?: "Desconhecido", vez)
            FxTipo.TERMINAL -> FxTerminal(cs(u), vez)
            FxTipo.OBITO -> FxObito(cs(u), vez)
            FxTipo.LEGENDA -> FxLegenda(cs(u).joinToString(" · "), vez)
            FxTipo.ABERTURA -> FxAbertura(cs(u), vez)
            FxTipo.CREDITOS -> FxCreditos(cs(u), vez)
            FxTipo.CORTE -> FxCorte(cs(u).joinToString(" "), base, vez)
            FxTipo.RACHA -> FxRacha(cs(u).joinToString(" "), base, vez)
            FxTipo.GOTAS -> FxGotas(cs(u).joinToString(" "), vez)
            FxTipo.RELOGIO -> FxRelogio(cs(u).joinToString(" "), vez)
            FxTipo.PARTICULAS -> FxParticulas(u.def.modo, cs(u).joinToString(" "), vez)
            FxTipo.TINTA -> FxTinta(cs(u).joinToString(" "), u.def.modo, vez)
            FxTipo.VINHETA -> FxVinheta(cs(u).joinToString(" "), vez)
            FxTipo.RESPIRO -> FxRespiro(cs(u).joinToString(" "), base, vez)
            FxTipo.ONDAS -> FxOndas(cs(u).joinToString(" "), vez)
            FxTipo.PAUSA -> FxDigitar("·     ·     ·", base.copy(color = FxSuave), vez)
        }
    }
}

// ───────── letra por letra ─────────

private class Sorte(val x: Float, val y: Float, val r: Float, val k: Float)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FxLetras(texto: String, modo: String, base: TextStyle, vez: Int) {
    val dur = when (modo) { "tremer" -> 2200; "onda", "pulo" -> 2400; "neon", "fantasma", "derreter" -> 2600; else -> 1600 }
    val t = progresso(vez, dur)
    val sortes = remember(texto) { val r = Random(texto.hashCode()); texto.map { Sorte(r.nextFloat() * 180 - 90, r.nextFloat() * 120 - 60, r.nextFloat() * 280 - 140, r.nextFloat()) } }
    val n = texto.length.coerceAtLeast(1)
    val st = when (modo) {
        "dourar", "zoom" -> base.copy(fontFamily = InstrumentSerif, fontSize = base.fontSize * 1.45f)
        "neon" -> base.copy(fontFamily = FxMono, letterSpacing = 3.sp)
        "pulo" -> base.copy(fontWeight = FontWeight.SemiBold)
        else -> base
    }
    var idx = 0
    FlowRow(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        texto.split(' ').forEachIndexed { wi, palavra ->
            Row {
                (if (wi > 0) " $palavra" else palavra).forEach { ch ->
                    val i = idx++
                    val s = sortes.getOrElse(i) { Sorte(0f, 0f, 0f, 0f) }
                    val k = fase(t, i.toFloat() / n * 0.5f, i.toFloat() / n * 0.5f + 0.5f)
                    val e = suave(k)
                    var cor = st.color
                    var sombra: Shadow? = null
                    Text(ch.toString(), style = st.copy(color = when (modo) {
                        "sangrar" -> lerp(FxCreme, FxSangue, e)
                        "dourar" -> lerp(FxCreme, FxOuro, e).also { sombra = Shadow(FxOuro.copy(alpha = 0.6f * e), blurRadius = 14f) }
                        "congelar" -> lerp(FxCreme, FxGelo, e).also { sombra = Shadow(FxCiano.copy(alpha = 0.7f * e), blurRadius = 12f) }
                        "neon" -> Color(0xFFFF6AD5).also { sombra = Shadow(Color(0xFFFF3CCF), blurRadius = 18f) }
                        "arco" -> Color.hsv(((i * 28f + t * 360f) % 360f), 0.55f, 1f)
                        "fantasma" -> Color(0xFFCFD8FF)
                        else -> cor
                    }, shadow = sombra ?: st.shadow), modifier = Modifier.graphicsLayer {
                        when (modo) {
                            "subir" -> { alpha = e; translationY = (1 - e) * 22.dp.toPx() }
                            "zoom" -> { alpha = e; scaleX = 1 + 3 * (1 - e); scaleY = scaleX }
                            "juntar", "espalhar" -> { val q = if (modo == "espalhar") suave(fase(t, s.k * 0.3f, s.k * 0.3f + 0.6f)) else e
                                alpha = q; translationX = s.x * (1 - q) * density; translationY = s.y * (1 - q) * density; rotationZ = s.r * (1 - q) }
                            "piscar", "neon" -> { val q = fase(t, s.k * 0.5f, s.k * 0.5f + 0.4f); alpha = if (q >= 1f) 1f else if (sin(q * 40 + s.k * 9) > 0) 1f else 0.12f }
                            "tremer" -> if (t < 1f) { translationX = sin(t * 260 + s.k * 50) * 1.6f * density; translationY = cos(t * 300 + s.r) * 1.6f * density }
                            "onda" -> { val q = (t * 3 - i * 0.06f).coerceIn(0f, 3f) % 1f; translationY = -sin(q * PI.toFloat()) * 10.dp.toPx() }
                            "pulo" -> { val q = (t * 2 - i * 0.05f).coerceIn(0f, 2f) % 1f; translationY = -abs(sin(q * PI.toFloat())) * 15.dp.toPx() }
                            "fantasma" -> { val q = fase(t, s.k * 0.4f, s.k * 0.4f + 0.6f); alpha = 1f - 0.9f * sin(q * PI.toFloat()); translationY = -sin(q * PI.toFloat()) * 6.dp.toPx() }
                            "derreter" -> { val q = fase(t, s.k * 0.3f, s.k * 0.3f + 0.7f); val m = sin(q * PI.toFloat()); scaleY = 1 + 0.7f * m; translationY = m * 10.dp.toPx(); alpha = 1 - 0.5f * m }
                            "sangrar" -> translationY = e * 2.dp.toPx()
                            else -> alpha = e
                        }
                    })
                }
            }
        }
    }
}

// ───────── a frase inteira ─────────

@Composable
private fun FxFrase(texto: String, modo: String, base: TextStyle, vez: Int) {
    val dur = when (modo) { "panico", "lento", "aurora", "coracao", "batida", "respirar", "balancar" -> 3000; "desmaio", "alarme" -> 2800; "sussurro", "quente", "duplo", "sepia", "aura", "epigrafe" -> 1800; else -> 1100 }
    val t = progresso(vez, dur)
    val e = suave(t)
    val pi = PI.toFloat()
    var st = base
    var fundo: Modifier = Modifier
    when (modo) {
        "sussurro" -> st = base.copy(fontSize = base.fontSize * 0.82f, fontStyle = FontStyle.Italic, color = FxSuave, letterSpacing = (1 + 8 * (1 - e)).sp)
        "quente" -> st = base.copy(fontSize = base.fontSize * 0.85f, fontStyle = FontStyle.Italic, color = FxRosa, shadow = Shadow(FxRosa.copy(alpha = 0.6f), blurRadius = 14f))
        "grito" -> st = base.copy(fontWeight = FontWeight.Bold, fontSize = base.fontSize * 1.4f)
        "duplo" -> st = base.copy(shadow = Shadow(Color(0xBBFF508C), Offset((3 + 14 * sin(e * pi)) * 1f, 0f), 0f))
        "intrusa" -> st = base.copy(color = Color(0xFFFF4D6A), fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, shadow = Shadow(Color(0x99FF2850), blurRadius = 14f))
        "aura" -> st = base.copy(fontStyle = FontStyle.Italic, color = Color(0xFFE6DCFF), shadow = Shadow(FxRoxo, blurRadius = 10f + 30f * sin(e * pi)))
        "sepia" -> { st = base.copy(fontStyle = FontStyle.Italic, color = lerp(FxCreme, Color(0xFFE3CC9E), e)); fundo = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2014).copy(alpha = e)).padding(horizontal = 14.dp, vertical = 8.dp) }
        "esmaga" -> st = base.copy(fontWeight = FontWeight.Bold, fontSize = base.fontSize * 1.25f, color = Color(0xFFC9B6FF))
        "holo", "scan" -> st = base.copy(fontFamily = FxMono, color = FxCiano, shadow = Shadow(Color(0xFF3CC8FF), blurRadius = 10f))
        "alarme" -> { st = base.copy(fontFamily = FxMono, color = Color(0xFFFF8F9F), letterSpacing = 2.sp); fundo = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF3B0710).copy(alpha = if (t < 1) abs(sin(t * 4 * pi)) else 0.3f)).padding(horizontal = 16.dp, vertical = 10.dp) }
        "coracao", "batida" -> st = base.copy(fontSize = base.fontSize * 2.2f, color = Color(0xFFFF5C8A), shadow = Shadow(Color(0x99FF508C), blurRadius = 18f))
        "corar" -> { st = base.copy(color = lerp(FxCreme, Color(0xFFFFA3C4), e), shadow = Shadow(Color(0xFFFF6EA0).copy(alpha = 0.7f * e), blurRadius = 14f))
            fundo = Modifier.background(Brush.radialGradient(listOf(Color(0x38FF5A96).copy(alpha = 0.22f * e), Color.Transparent))).padding(horizontal = 16.dp, vertical = 6.dp) }
        "furia" -> st = base.copy(color = Color(0xFFFF4D5E), fontWeight = FontWeight.Bold, shadow = Shadow(Color(0xFFFF0000), blurRadius = 14f))
        "raiva" -> st = base.copy(color = Color(0xFFFFB0B0))
        "zoom" -> { st = base.copy(fontWeight = FontWeight.SemiBold); fundo = Modifier.drawBehind { linhasVelocidade(e) } }
        "lento", "epigrafe" -> st = base.copy(fontFamily = InstrumentSerif, fontSize = base.fontSize * 1.3f, letterSpacing = (2 - 8 * (1 - e)).coerceAtLeast(-2f).sp, fontStyle = if (modo == "epigrafe") FontStyle.Italic else null)
        "flash" -> st = base.copy(fontFamily = InstrumentSerif, fontSize = base.fontSize * 1.6f)
        "aurora" -> st = base.copy(fontFamily = InstrumentSerif, fontSize = base.fontSize * 1.5f, brush = Brush.linearGradient(listOf(Color(0xFF6BFFB0), Color(0xFF6BB8FF), Color(0xFFD29BFF), Color(0xFF6BFFB0)),
            start = Offset(-600f * (1 - t), 0f), end = Offset(600f + 600f * t, 0f)))
        "balancar" -> st = base.copy(fontStyle = FontStyle.Italic)
        "respirar" -> st = base.copy(fontStyle = FontStyle.Italic)
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().then(if (modo == "flash" && t < 0.5f) Modifier.background(Color.White.copy(alpha = (1 - t * 2) * 0.85f)) else Modifier)) {
        Text(texto + if (modo == "raiva") "  💢" else "", style = st, modifier = fundo.graphicsLayer {
            when (modo) {
                "sussurro" -> alpha = 0.3f + 0.4f * e
                "grito", "flash" -> { val k = fase(t, 0f, 0.5f); scaleX = if (k < 0.6f) 0.3f + 1.75f * (k / 0.6f) else 1.35f - 0.35f * ((k - 0.6f) / 0.4f); scaleY = scaleX; alpha = fase(t, 0f, 0.15f)
                    if (modo == "grito" && t in 0.15f..0.45f) translationX = sin(t * 120) * 4.dp.toPx() }
                "balancar" -> { rotationZ = sin(t * 4 * pi) * 6 * (1 - t * 0.3f); translationX = sin(t * 4 * pi) * 5.dp.toPx() }
                "intrusa" -> alpha = if (t < 0.6f && sin(t * 60) < -0.3f) 0.1f else 1f
                "desmaio" -> { val m = sin(t * pi); alpha = 1 - 0.85f * m; scaleX = 1 - 0.07f * m; scaleY = scaleX }
                "panico" -> { val f = 2 + t * 14; val m = abs(sin(t * f * pi)); scaleX = 1 + 0.15f * m * (1 - fase(t, 0.92f, 1f)); scaleY = scaleX }
                "esmaga" -> { val k = fase(t, 0f, 0.35f); scaleY = 1 - 0.65f * sin(k * pi * 0.5f).let { if (k >= 1) 0.3f else it }; scaleX = 1 + 0.05f + 0.25f * (1 - scaleY) * 0.4f
                    if (t in 0.2f..0.5f) translationX = sin(t * 140) * 4.dp.toPx() }
                "holo" -> alpha = if (t < 1f && sin(t * 90) > 0.7f) 0.35f else 1f
                "scan" -> {}
                "coracao", "batida" -> { val c = (t * 4) % 1f; scaleX = 1 + 0.35f * maxOf(0f, sin(c * pi * 4).let { if (c < 0.45f) it else 0f }); scaleY = scaleX }
                "furia" -> if (t < 0.9f) translationX = sin(t * 160) * 4.dp.toPx()
                "raiva" -> { val m = abs(sin(t * 10 * pi)); scaleX = 1 + 0.04f * m; scaleY = scaleX }
                "zoom" -> { val k = suave(fase(t, 0.1f, 0.45f)); scaleX = 1 + 0.6f * k; scaleY = scaleX }
                "lento", "epigrafe" -> alpha = e
                "aurora" -> alpha = fase(t, 0f, 0.3f)
                "respirar" -> { val m = sin(t * 2 * pi).let { it * it }; scaleX = 1 + 0.06f * m; scaleY = scaleX; alpha = 1 - 0.25f * m }
                "duplo", "aura", "corar", "quente" -> alpha = fase(t, 0f, 0.3f)
                else -> {}
            }
        }.then(if (modo == "scan") Modifier.drawWithContent { drawContent(); if (t < 1f) { val y = size.height * ((t * 2) % 1f); drawLine(FxCiano, Offset(-20f, y), Offset(size.width + 20f, y), 2.dp.toPx()) } } else Modifier))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.linhasVelocidade(e: Float) {
    val c = center; val r = size.maxDimension
    for (i in 0 until 36) {
        val a = i * (2 * PI / 36).toFloat()
        drawLine(FxRoxo.copy(alpha = 0.25f * e), Offset(c.x + cos(a) * r * 0.35f, c.y + sin(a) * r * 0.35f), Offset(c.x + cos(a) * r, c.y + sin(a) * r), 1.5f)
    }
}

// ───────── texto que se escreve ─────────

@Composable
private fun FxDigitar(texto: String, base: TextStyle, vez: Int) {
    val t = progresso(vez, (texto.length * 55).coerceIn(600, 4000))
    val n = (texto.length * t).toInt()
    Text(texto.take(n) + if (t < 1f) "▍" else "", style = base)
}

private const val SIMBOLOS = "▓▒░#@%&\$ΣΨΩ01ʘ¥"

@Composable
private fun FxDecifrar(texto: String, vez: Int) {
    val t = progresso(vez, 1600)
    Text(embaralhado("", texto, t), style = TextStyle(fontFamily = FxMono, fontSize = 18.sp, letterSpacing = 2.sp, color = FxCiano, textAlign = TextAlign.Center))
}

private fun embaralhado(de: String, para: String, t: Float): String {
    if (t >= 1f) return para
    val r = Random((t * 30).toInt())
    val n = maxOf(de.length, para.length)
    return buildString {
        for (i in 0 until n) {
            val p = i.toFloat() / n
            when {
                t > p + 0.15f -> para.getOrNull(i)?.let { append(it) }
                t > p -> append(if (para.getOrNull(i) == ' ') ' ' else SIMBOLOS[r.nextInt(SIMBOLOS.length)])
                else -> de.getOrNull(i)?.let { append(it) }
            }
        }
    }
}

@Composable
private fun FxTraduz(c: List<String>, base: TextStyle, vez: Int) {
    val o = c.firstOrNull().orEmpty(); val tr = c.getOrNull(1) ?: o
    val t = progresso(vez, 2400)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(o.uppercase(), fontFamily = FxMono, fontSize = 11.sp, letterSpacing = 2.sp, color = FxSuave)
        Text(if (t < 0.35f) o else embaralhado(o, tr, fase(t, 0.35f, 1f)), style = base.copy(fontStyle = FontStyle.Italic))
    }
}

@Composable
private fun FxCorrige(c: List<String>, base: TextStyle, vez: Int) {
    val velho = c.firstOrNull().orEmpty(); val novo = c.getOrNull(1).orEmpty()
    val t = progresso(vez, 2200)
    val risco = fase(t, 0.1f, 0.35f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(velho, style = base.copy(color = base.color.copy(alpha = 0.55f), textAlign = TextAlign.Start), modifier = Modifier.drawWithContent {
            drawContent(); drawLine(FxSangue, Offset(-4f, size.height * 0.55f), Offset(-4f + (size.width + 8f) * risco, size.height * 0.55f), 2.dp.toPx())
        })
        Text("  " + novo.take((novo.length * fase(t, 0.45f, 1f)).toInt()), style = base.copy(textAlign = TextAlign.Start, fontStyle = FontStyle.Italic))
    }
}

@Composable
private fun FxEco(texto: String, vez: Int) {
    val t = progresso(vez, 2000)
    val st = TextStyle(fontFamily = InstrumentSerif, fontSize = 30.sp, color = FxCreme, textAlign = TextAlign.Center)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(texto, style = st)
        Text(texto, style = st, modifier = Modifier.graphicsLayer { alpha = 0.42f * fase(t, 0.3f, 0.6f); scaleX = 0.88f; scaleY = 0.88f })
        Text(texto, style = st, modifier = Modifier.graphicsLayer { alpha = 0.18f * fase(t, 0.6f, 0.9f); scaleX = 0.76f; scaleY = 0.76f })
    }
}

@Composable
private fun FxMultidao(palavras: List<String>, vez: Int) {
    val t = progresso(vez, 2600)
    val ps = palavras.ifEmpty { listOf("…") }
    val pos = remember(palavras) { val r = Random(palavras.hashCode()); List(12) { Triple(r.nextFloat() * 0.75f, r.nextFloat() * 0.8f, r.nextFloat() * 24 - 12) } }
    BoxWithConstraints(Modifier.fillMaxWidth().height(150.dp).clipToBounds()) {
        pos.forEachIndexed { i, (x, y, rot) ->
            val k = fase(t, i * 0.06f, i * 0.06f + 0.15f)
            Text(ps[i % ps.size], fontSize = 13.sp, fontStyle = FontStyle.Italic, color = Color(0xFFC9BDF0),
                modifier = Modifier.offset(maxWidth * x, maxHeight * y).graphicsLayer { alpha = 0.65f * k; scaleX = 0.4f + 0.6f * k; scaleY = scaleX; rotationZ = rot })
        }
    }
}

// ───────── janelas e fichas ─────────

private fun corJanela(modo: String) = when (modo) { "vermelho" -> FxSangue; "ouro" -> Color(0xFFE6C15A); "roxo" -> FxRoxo; "verde" -> FxVerde; else -> Color(0xFF5FB7FF) }

@Composable
private fun FxJanela(modo: String, titulo: String, c: List<String>, vez: Int) {
    val cor = corJanela(modo)
    val corpo = c.joinToString("\n")
    val t = progresso(vez, 700 + corpo.length * 28)
    val abre = suave(fase(t, 0f, 0.25f).coerceAtLeast(if (corpo.isEmpty()) 1f else 0f))
    Column(Modifier.fillMaxWidth(0.92f).graphicsLayer { scaleY = 0.02f + 0.98f * abre; scaleX = 0.2f + 0.8f * fase(abre, 0f, 0.4f) }
        .clip(RoundedCornerShape(3.dp)).background(cor.copy(alpha = 0.14f)).border(1.dp, cor, RoundedCornerShape(3.dp))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(titulo, fontFamily = FxMono, fontSize = 11.sp, letterSpacing = 3.sp, color = cor, modifier = Modifier.weight(1f))
            Text("◆", fontSize = 10.sp, color = cor)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(cor.copy(alpha = 0.7f)))
        val k = fase(t, 0.25f, 1f)
        Text(corpo.take((corpo.length * k).toInt()) + if (k in 0.01f..0.99f) "▍" else "", fontFamily = FxMono, fontSize = 13.sp, lineHeight = 19.sp,
            color = lerp(cor, Color.White, 0.65f), modifier = Modifier.padding(10.dp).heightIn(min = 20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FxFicha(c: List<String>, ferida: Boolean, vez: Int) {
    val t = progresso(vez, 600 + c.size * 300)
    val cor = if (ferida) FxSangue else FxRoxo
    Column(Modifier.fillMaxWidth(0.92f).drawBehind { drawLine(cor, Offset(0f, 0f), Offset(0f, size.height), 2.dp.toPx()) }.padding(start = 14.dp, top = 4.dp, bottom = 4.dp)) {
        Text(c.firstOrNull().orEmpty(), fontFamily = InstrumentSerif, fontSize = 28.sp, color = FxCreme, style = TextStyle(shadow = Shadow(cor.copy(alpha = 0.5f), blurRadius = 14f)),
            modifier = Modifier.graphicsLayer { alpha = fase(t, 0f, 0.2f) })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
            c.drop(1).forEachIndexed { i, a ->
                val k = suave(fase(t, 0.2f + i * 0.8f / c.size, 0.35f + i * 0.8f / c.size))
                Text(a, fontFamily = FxMono, fontSize = 11.sp, color = if (ferida) Color(0xFFFFB0BA) else Color(0xFFCFC3F0),
                    modifier = Modifier.graphicsLayer { alpha = k; translationY = (1 - k) * 14.dp.toPx() }
                        .border(1.dp, cor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(horizontal = 9.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
private fun FxContar(c: List<String>, modo: String, rotulo: String, vez: Int) {
    val (de, ate) = when (modo) {
        "par" -> num(c.getOrNull(0), 1f) to num(c.getOrNull(1), num(c.getOrNull(0), 1f) + 1)
        "desce" -> num(c.getOrNull(0), 10f) to num(c.getOrNull(1), 0f)
        "dias" -> 1f to num(c.getOrNull(0), 30f)
        else -> 0f to num(c.getOrNull(0), 100f)
    }
    val t = progresso(vez, if (abs(ate - de) < 12) 1300 else 1700)
    val v = de + (ate - de) * suave(t)
    val pop = if (t >= 1f) 1f else 0f
    Row(verticalAlignment = Alignment.Bottom) {
        Text(String.format(java.util.Locale("pt", "BR"), "%,d", v.toLong()), fontFamily = InstrumentSerif, fontSize = 44.sp, color = FxCreme,
            style = TextStyle(shadow = Shadow(FxRoxo, blurRadius = 18f + 10 * pop)))
        Text("  " + (c.getOrNull(if (modo == "par") 2 else 1)?.takeIf { modo != "desce" } ?: rotulo).uppercase(), fontFamily = FxMono, fontSize = 12.sp, letterSpacing = 2.sp, color = FxSuave,
            modifier = Modifier.padding(bottom = 10.dp))
    }
}

@Composable
private fun FxBarra(c: List<String>, modo: String, rotulo: String, vez: Int) {
    val nome = if (c.size >= 3) c[0] else null
    val ns = if (c.size >= 3) c.drop(1) else c
    val de = num(ns.getOrNull(0), 100f).coerceIn(0f, 100f); val ate = num(ns.getOrNull(1), de).coerceIn(0f, 100f)
    val t = progresso(vez, 1800)
    val frente = de + (ate - de) * suave(fase(t, 0.25f, 0.45f))
    val fantasma = de + (ate - de) * suave(fase(t, 0.5f, 0.9f))
    val cores = when (modo) { "vermelho" -> listOf(Color(0xFFC21F3A), Color(0xFFFF5C6F)); "azul" -> listOf(Color(0xFF2D7CFF), FxCiano); "rosa" -> listOf(Color(0xFFFF5C9A), Color(0xFFFFB3D0)); else -> listOf(FxRoxo, Color(0xFFC7B3FF)) }
    Column(Modifier.fillMaxWidth(0.88f)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            Text((nome?.let { "$rotulo · $it" } ?: rotulo), fontFamily = FxMono, fontSize = 12.sp, color = FxCreme, modifier = Modifier.weight(1f))
            Text("${frente.toInt()}%", fontFamily = FxMono, fontSize = 12.sp, color = FxCreme)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1330)).border(1.dp, Color(0xFF2C2348), RoundedCornerShape(6.dp))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((maxOf(fantasma, frente) / 100f).coerceIn(0f, 1f)).background(Color.White.copy(alpha = 0.5f)))
            Box(Modifier.fillMaxHeight().fillMaxWidth((frente / 100f).coerceIn(0f, 1f)).clip(RoundedCornerShape(6.dp)).background(Brush.horizontalGradient(cores)))
        }
    }
}

// ───────── telas de celular ─────────

@Composable
private fun FxChat(c: List<String>, modo: String, vez: Int) {
    data class Msg(val minha: Boolean, val quem: String, val txt: String)
    val msgs = when (modo) {
        "visto" -> c.map { Msg(true, "", it) }
        "digitando" -> listOf(Msg(false, c.firstOrNull().orEmpty(), c.getOrNull(1).orEmpty()))
        else -> c.map { linha ->
            val quem = linha.substringBefore(':', "").trim()
            val txt = if (':' in linha) linha.substringAfter(':').trim() else linha
            Msg(Fx2.ehEu(quem), quem, txt)
        }
    }
    val passo = if (modo == "digitando") 2600 else 1200
    val total = msgs.size * passo + if (modo == "apagada") 1600 else 400
    val ms = progresso(vez, total) * total
    Column(Modifier.fillMaxWidth(0.94f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        msgs.forEachIndexed { i, m ->
            val ini = i * passo.toFloat()
            val digitando = !m.minha && ms in ini..(ini + if (modo == "digitando") 2000f else 650f) && ms < total
            val aparece = ms >= ini + (if (m.minha) 0f else if (modo == "digitando") 2000f else 650f) || ms >= total
            val apagou = modo == "apagada" && ms >= ini + passo + 900f
            if (digitando || aparece) Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.minha) Alignment.End else Alignment.Start) {
                if (!m.minha && m.quem.isNotEmpty() && i == msgs.indexOfFirst { it.quem == m.quem }) Text(m.quem, fontSize = 10.sp, color = FxSuave, modifier = Modifier.padding(start = 6.dp, bottom = 1.dp))
                val txt = when { digitando -> "• • •"; apagou -> "mensagem apagada"; else -> m.txt }
                if (txt.isNotEmpty()) Text(txt, fontSize = 14.sp, fontStyle = if (apagou) FontStyle.Italic else null,
                    color = if (m.minha) Color.Black else if (apagou) FxSuave else FxCreme,
                    modifier = Modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(15.dp))
                        .background(if (apagou) Color.Transparent else if (m.minha) FxRoxo else Color(0xFF2A2142))
                        .then(if (apagou) Modifier.border(1.dp, Color(0xFF4A3D70), RoundedCornerShape(15.dp)) else Modifier)
                        .padding(horizontal = 11.dp, vertical = 6.dp))
            }
        }
        if (modo == "visto" && ms >= total - 300) Text("visto", fontSize = 10.sp, color = FxSuave, modifier = Modifier.align(Alignment.End))
    }
}

internal object Fx2 { fun ehEu(q: String) = com.huryel.novely.data.Fx.chave(q) in setOf("eu", "me", "mim", "voce") }

@Composable
private fun FxNotif(app: String, texto: String, vez: Int) {
    val t = progresso(vez, 900)
    val k = fase(t, 0f, 0.7f)
    val y = if (k < 0.7f) -1f + k / 0.7f * 1.05f else 0.05f - 0.05f * ((k - 0.7f) / 0.3f)
    Column(Modifier.fillMaxWidth(0.94f).graphicsLayer { translationY = y * 120.dp.toPx(); alpha = fase(t, 0f, 0.2f) }
        .clip(RoundedCornerShape(14.dp)).background(Color(0xEB282040)).padding(horizontal = 12.dp, vertical = 9.dp)) {
        Row { Text(app.uppercase(), fontSize = 11.sp, letterSpacing = 1.sp, color = FxSuave, modifier = Modifier.weight(1f)); Text("agora", fontSize = 11.sp, color = FxSuave) }
        Text(texto, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun FxLigacao(nome: String, vez: Int) {
    val t = progresso(vez, 3000)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { if (t < 0.9f) translationX = sin(t * 300) * 2.dp.toPx() }) {
        Box(Modifier.size(56.dp).drawBehind {
            for (j in 0..1) { val c = ((t * 3 + j * 0.5f) % 1f); if (t < 1f) drawCircle(FxRoxo.copy(alpha = 1 - c), radius = size.minDimension / 2 * (1 + c * 0.8f), style = Stroke(2.dp.toPx())) }
        }.clip(CircleShape).background(Color(0xFF2A2142)), contentAlignment = Alignment.Center) {
            Text(nome.take(1).uppercase(), fontFamily = InstrumentSerif, fontSize = 26.sp, color = FxCreme)
        }
        Text(nome, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FxCreme, modifier = Modifier.padding(top = 6.dp))
        Text("chamando…", fontSize = 11.sp, color = FxSuave)
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.padding(top = 6.dp)) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(FxSangue)); Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF2FBF71)))
        }
    }
}

@Composable
private fun FxTerminal(linhas: List<String>, vez: Int) {
    val tudo = linhas.joinToString("\n")
    val t = progresso(vez, 400 + tudo.length * 30)
    Text(tudo.take((tudo.length * t).toInt()) + if (t < 1f) "▍" else "", fontFamily = FxMono, fontSize = 12.sp, lineHeight = 18.sp, color = Color(0xFF59FF8A),
        style = TextStyle(shadow = Shadow(Color(0x9959FF8A), blurRadius = 8f)),
        modifier = Modifier.fillMaxWidth(0.94f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF03100A)).border(1.dp, Color(0xFF1D4A2E), RoundedCornerShape(6.dp)).padding(10.dp).heightIn(min = 60.dp))
}

@Composable
private fun FxCarta(texto: String, vez: Int) {
    val t = progresso(vez, 1000)
    val e = suave(t)
    Text(texto, fontStyle = FontStyle.Italic, fontSize = 16.sp, lineHeight = 23.sp, color = Color(0xFF3A2A14), fontFamily = FontFamily.Serif,
        modifier = Modifier.fillMaxWidth(0.88f).graphicsLayer { rotationX = -90f * (1 - e); rotationZ = -1.5f * e; cameraDistance = 12 * density; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f); alpha = fase(t, 0f, 0.2f) }
            .background(Brush.verticalGradient(listOf(Color(0xFFEFE4CB), Color(0xFFE2D3B2)))).padding(horizontal = 18.dp, vertical = 14.dp))
}

// ───────── carimbos e cartões ─────────

@Composable
private fun FxSelo(texto: String, modo: String, vez: Int) {
    val t = progresso(vez, 700)
    val k = fase(t, 0f, 0.55f)
    val cor = when (modo) { "ouro" -> FxOuro; "roxo" -> Color(0xFFC7B3FF); else -> Color(0xFFFF6B7F) }
    Text(texto.uppercase(), fontFamily = FxMono, fontSize = 20.sp, letterSpacing = 4.sp, color = cor,
        modifier = Modifier.padding(vertical = 14.dp).graphicsLayer { rotationZ = -12f; val s = if (k < 0.8f) 3.2f - 2.3f * (k / 0.8f) else 0.9f + 0.1f * ((k - 0.8f) / 0.2f); scaleX = s; scaleY = s; alpha = fase(t, 0f, 0.3f)
            if (t in 0.4f..0.7f) translationX = sin(t * 150) * 3.dp.toPx() }
            .border(3.dp, cor, RoundedCornerShape(2.dp)).padding(3.dp).border(1.dp, cor, RoundedCornerShape(2.dp)).padding(horizontal = 14.dp, vertical = 6.dp))
}

@Composable
private fun FxConquista(rotulo: String, nome: String, vez: Int) {
    val t = progresso(vez, 1500)
    val k = fase(t, 0f, 0.4f)
    val brilho = fase(t, 0.4f, 0.9f)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { val s = if (k < 0.7f) 0.3f + (k / 0.7f) * 0.8f else 1.1f - 0.1f * ((k - 0.7f) / 0.3f); scaleX = s; scaleY = s; alpha = fase(t, 0f, 0.15f) }
        .clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF2A1F45), Color(0xFF160F28)))).border(1.dp, Color(0xFFE6C15A), RoundedCornerShape(12.dp))
        .drawWithContent { drawContent(); if (brilho in 0.01f..0.99f) { val x = size.width * (brilho * 1.8f - 0.4f); drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent), Offset(x - 60f, 0f), Offset(x + 60f, size.height))) } }
        .padding(horizontal = 16.dp, vertical = 10.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFF3C2), Color(0xFFE6C15A)))), contentAlignment = Alignment.Center) { Text("✦", fontSize = 20.sp, color = Color(0xFF3A2A00)) }
        Column(Modifier.padding(start = 12.dp)) {
            Text(rotulo.uppercase(), fontFamily = FxMono, fontSize = 9.sp, letterSpacing = 2.sp, color = FxOuro)
            Text(nome, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FxCreme)
        }
    }
}

@Composable
private fun FxQuadros(itens: List<String>, vez: Int) {
    val t = progresso(vez, 400 + itens.size * 400)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        itens.forEachIndexed { i, s ->
            val k = fase(t, i * 0.9f / itens.size, i * 0.9f / itens.size + 0.25f)
            Text(s, fontSize = 14.sp, color = FxCreme, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false).widthIn(max = 120.dp).graphicsLayer { alpha = k; val sc = 0.6f + 0.4f * suave(k); scaleX = sc; scaleY = sc; translationY = (1 - k) * 30.dp.toPx() }
                    .border(2.dp, FxCreme).background(Color(0xFF0E0A1A)).padding(horizontal = 8.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun FxDivide(c: List<String>, vez: Int) {
    val t = progresso(vez, 1100)
    Row(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(10.dp))) {
        Box(Modifier.weight(1f).fillMaxHeight().graphicsLayer { translationX = -(1 - suave(fase(t, 0f, 0.7f))) * size.width }.background(Color(0xFF0D1530)), contentAlignment = Alignment.Center) {
            Text(c.getOrNull(0).orEmpty(), fontFamily = InstrumentSerif, fontSize = 24.sp, color = Color(0xFFBCD2FF), textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
        }
        Box(Modifier.width(2.dp).fillMaxHeight().background(FxRoxo))
        Box(Modifier.weight(1f).fillMaxHeight().graphicsLayer { translationX = (1 - suave(fase(t, 0.2f, 0.9f))) * size.width }.background(Color(0xFF2A0A12)), contentAlignment = Alignment.Center) {
            Text(c.getOrNull(1).orEmpty(), fontFamily = InstrumentSerif, fontSize = 24.sp, color = Color(0xFFFFBCC8), textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun FxEscolha(c: List<String>, vez: Int) {
    val t = progresso(vez, 1800)
    val risco = fase(t, 0.55f, 0.75f)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(c.getOrNull(0).orEmpty(), fontSize = 16.sp, color = FxCreme, modifier = Modifier.graphicsLayer { val s = 1 + 0.12f * sin(fase(t, 0.35f, 0.55f) * PI.toFloat()); scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(10.dp)).background(FxRoxo.copy(alpha = 0.22f)).border(1.dp, FxRoxo, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 8.dp))
        Text(c.getOrNull(1).orEmpty(), fontSize = 16.sp, color = FxCreme, modifier = Modifier.graphicsLayer { alpha = 1 - 0.65f * fase(t, 0.7f, 0.9f) }
            .border(1.dp, FxRoxo, RoundedCornerShape(10.dp)).drawWithContent { drawContent(); drawLine(FxSangue, Offset(-6f, size.height / 2), Offset(-6f + (size.width + 12f) * risco, size.height / 2), 2.dp.toPx()) }
            .padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable
private fun FxPulso(plano: Boolean, texto: String, vez: Int) {
    val t = progresso(vez, 2600)
    val cor = if (plano) Color(0xFFFF5C6F) else FxVerde
    val pts = remember(plano) {
        val base = if (plano) listOf(0f to 30f, 40f to 30f, 48f to 30f, 54f to 8f, 60f to 52f, 66f to 30f, 110f to 30f, 116f to 22f, 120f to 38f, 124f to 30f, 300f to 30f)
        else listOf(0f to 30f, 40f to 30f, 48f to 30f, 54f to 8f, 60f to 52f, 66f to 30f, 110f to 30f, 118f to 30f, 124f to 8f, 130f to 52f, 136f to 30f, 180f to 30f, 188f to 30f, 194f to 8f, 200f to 52f, 206f to 30f, 300f to 30f)
        base
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth(0.94f).height(60.dp)) {
            val sx = size.width / 300f; val sy = size.height / 60f; val lim = 300f * t
            val path = Path(); var primeiro = true
            for (i in pts.indices) {
                val (x, y) = pts[i]
                if (x > lim) { val (x0, y0) = pts[i - 1]; val f = (lim - x0) / (x - x0); path.lineTo(lim * sx, (y0 + (y - y0) * f) * sy); break }
                if (primeiro) { path.moveTo(x * sx, y * sy); primeiro = false } else path.lineTo(x * sx, y * sy)
            }
            drawPath(path, cor.copy(alpha = 0.35f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, cor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        if (texto.isNotEmpty()) Text(texto, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = FxCreme, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun FxObito(c: List<String>, vez: Int) {
    val t = progresso(vez, 2400)
    val chama by rememberInfiniteTransition(label = "vela").animateFloat(0.85f, 1.1f, androidx.compose.animation.core.infiniteRepeatable(tween(300), androidx.compose.animation.core.RepeatMode.Reverse), label = "chama")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
        Box(Modifier.size(8.dp, 14.dp).graphicsLayer { scaleY = chama }.clip(RoundedCornerShape(50)).background(Brush.radialGradient(listOf(Color(0xFFFFF3C2), Color(0xFFFFB347), Color.Transparent))))
        Text(c.getOrNull(0).orEmpty(), fontFamily = InstrumentSerif, fontSize = 32.sp, color = FxCreme, modifier = Modifier.graphicsLayer { alpha = fase(t, 0.1f, 0.6f) })
        Text(c.getOrNull(1).orEmpty(), fontFamily = FxMono, fontSize = 11.sp, letterSpacing = 3.sp, color = FxSuave, modifier = Modifier.graphicsLayer { alpha = fase(t, 0.5f, 1f) })
    }
}

@Composable
private fun FxLegenda(texto: String, vez: Int) {
    val t = progresso(vez, 1400)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
        Box(Modifier.width(160.dp).height(1.dp).graphicsLayer { scaleX = suave(fase(t, 0f, 0.5f)); transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }.background(FxRoxo))
        Text(texto.uppercase(), fontFamily = FxMono, fontSize = 13.sp, letterSpacing = 5.sp, color = FxCreme, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp).graphicsLayer { val k = fase(t, 0.45f, 1f); alpha = k; translationY = (1 - k) * 14.dp.toPx() })
    }
}

@Composable
private fun FxAbertura(c: List<String>, vez: Int) {
    val t = progresso(vez, 2400)
    val faixa = suave(fase(t, 0f, 0.35f))
    // [abertura | rótulo | título]  ·  [entra | Nome | título do personagem]
    val (pequeno, grande) = c.getOrNull(0).orEmpty() to c.getOrNull(1).orEmpty()
    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF05030B)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text((if (grande.isEmpty()) "" else if (c.size >= 2 && pequeno.length > grande.length) grande else pequeno).uppercase(), fontFamily = FxMono, fontSize = 10.sp, letterSpacing = 4.sp, color = FxSuave,
                modifier = Modifier.graphicsLayer { alpha = fase(t, 0.3f, 0.6f) })
            Text(if (grande.isEmpty()) pequeno else if (pequeno.length > grande.length) pequeno else grande, fontFamily = InstrumentSerif, fontSize = 34.sp, color = FxCreme, textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(FxRoxo.copy(alpha = 0.7f), blurRadius = 20f)), modifier = Modifier.graphicsLayer { alpha = fase(t, 0.45f, 1f) })
        }
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(28.dp * faixa).background(Color.Black))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(28.dp * faixa).background(Color.Black))
    }
}

@Composable
private fun FxCreditos(linhas: List<String>, vez: Int) {
    val t = progresso(vez, 3200)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().graphicsLayer { translationY = (1 - t) * 140.dp.toPx(); alpha = fase(t, 0f, 0.2f) }) {
        linhas.forEachIndexed { i, l ->
            Text(l, fontFamily = InstrumentSerif, fontSize = if (i == 0) 26.sp else 19.sp, color = if (i == 0) FxCreme else Color(0xFFCFC3F0), textAlign = TextAlign.Center)
        }
    }
}

// ───────── cortes, rachaduras, sangue ─────────

@Composable
private fun FxCorte(texto: String, base: TextStyle, vez: Int) {
    val t = progresso(vez, 1200)
    val lamina = fase(t, 0f, 0.35f)
    val separa = suave(fase(t, 0.38f, 0.75f))
    val st = base.copy(fontFamily = InstrumentSerif, fontSize = base.fontSize * 1.45f)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp).drawWithContent {
        drawContent()
        if (lamina > 0f && t < 0.75f) rotate(-11f) { drawLine(Color.White.copy(alpha = 1 - fase(t, 0.4f, 0.75f)), Offset(-30f, size.height / 2), Offset(-30f + (size.width + 60f) * lamina, size.height / 2), 2.dp.toPx()) }
    }) {
        Text(texto, style = st, modifier = Modifier.graphicsLayer { translationX = -5.dp.toPx() * separa; translationY = -5.dp.toPx() * separa; rotationZ = -2f * separa }
            .drawWithContent { clipRect(bottom = size.height * 0.5f) { this@drawWithContent.drawContent() } })
        Text(texto, style = st, modifier = Modifier.graphicsLayer { translationX = 5.dp.toPx() * separa; translationY = 5.dp.toPx() * separa; rotationZ = 1.5f * separa }
            .drawWithContent { clipRect(top = size.height * 0.5f) { this@drawWithContent.drawContent() } })
    }
}

@Composable
private fun FxRacha(texto: String, base: TextStyle, vez: Int) {
    val t = progresso(vez, 900)
    val pts = listOf(-10f to 34f, 30f to 28f, 48f to 40f, 76f to 18f, 104f to 36f, 126f to 22f, 156f to 40f, 210f to 26f)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp).graphicsLayer { if (t in 0.1f..0.4f) translationX = sin(t * 140) * 4.dp.toPx() }
        .drawWithContent {
            drawContent()
            val sx = size.width / 200f; val sy = size.height / 60f; val lim = -10f + 220f * suave(t)
            val path = Path(); path.moveTo(pts[0].first * sx, pts[0].second * sy)
            for (i in 1 until pts.size) { val (x, y) = pts[i]; if (x > lim) { val (x0, y0) = pts[i - 1]; val f = (lim - x0) / (x - x0); path.lineTo(lim * sx, (y0 + (y - y0) * f) * sy); break }; path.lineTo(x * sx, y * sy) }
            drawPath(path, FxRoxo.copy(alpha = 0.5f), style = Stroke(4.dp.toPx()))
            drawPath(path, Color(0xFFE9E4FF), style = Stroke(1.4.dp.toPx()))
        }.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text(texto, style = base.copy(fontSize = base.fontSize * 1.2f))
    }
}

@Composable
private fun FxGotas(texto: String, vez: Int) {
    val t = progresso(vez, 4200)
    val gs = remember(texto) { val r = Random(texto.hashCode()); List(6) { Triple(0.06f + r.nextFloat() * 0.88f, 0.4f + r.nextFloat() * 0.6f, r.nextFloat() * 0.3f) } }
    Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.TopCenter) {
        Text(texto, fontFamily = InstrumentSerif, fontSize = 30.sp, color = lerp(FxCreme, FxSangue, fase(t, 0f, 0.25f)), modifier = Modifier.padding(top = 8.dp).drawBehind {
            gs.forEach { (x, v, d) ->
                val k = fase(t, 0.15f + d, 1f); val l = k * k * v * 80.dp.toPx(); val w = (2 + v * 2.5f).dp.toPx(); val cx = size.width * x
                drawRect(Color(0xFFB3122E), Offset(cx - w / 2, size.height - 6.dp.toPx()), Size(w, l))
                drawCircle(Color(0xFFB3122E), w * 0.9f, Offset(cx, size.height - 6.dp.toPx() + l))
            }
        })
    }
}

@Composable
private fun FxRelogio(texto: String, vez: Int) {
    val t = progresso(vez, 2400)
    val e = suave(t)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Canvas(Modifier.size(64.dp)) {
            drawCircle(FxRoxo, size.minDimension / 2 - 2.dp.toPx(), style = Stroke(2.dp.toPx()))
            rotate(1440f * (1 - e)) { drawLine(FxCreme, center, Offset(center.x + size.width * 0.22f, center.y), 2.5.dp.toPx(), StrokeCap.Round) }
            rotate(360f * (1 - e)) { drawLine(FxCreme, center, Offset(center.x, center.y - size.height * 0.2f), 3.5.dp.toPx(), StrokeCap.Round) }
        }
        Text(texto, fontStyle = FontStyle.Italic, fontSize = 18.sp, color = Color(0xFFE3CC9E), modifier = Modifier.graphicsLayer { alpha = fase(t, 0.6f, 1f) })
    }
}

// ───────── cena: cor, sombra, respiração, som ─────────

@Composable
private fun FxTinta(texto: String, modo: String, vez: Int) {
    val t = progresso(vez, 1800)
    val cor = when (modo) { "roxo" -> Color(0xFF5B2FC0); else -> Color(0xFF4A0018) }
    Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).drawBehind {
        drawCircle(Brush.radialGradient(listOf(cor, cor.copy(alpha = 0.6f), Color.Transparent), center, size.maxDimension * 0.7f * suave(t) + 1f), size.maxDimension * 0.7f * suave(t) + 1f)
    }, contentAlignment = Alignment.Center) {
        Text(texto, fontFamily = InstrumentSerif, fontSize = 28.sp, color = FxCreme, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp).graphicsLayer { alpha = fase(t, 0.3f, 0.8f) })
    }
}

@Composable
private fun FxVinheta(texto: String, vez: Int) {
    val t = progresso(vez, 2400)
    val e = suave(t)
    Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0B0717)).drawWithContent {
        drawContent()
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.97f)), center, size.maxDimension * (1.2f - 0.85f * e)))
    }, contentAlignment = Alignment.Center) {
        Text(texto.ifEmpty { "…" }, fontFamily = InstrumentSerif, fontSize = 26.sp, color = FxCreme, modifier = Modifier.graphicsLayer { alpha = fase(t, 0.5f, 1f) })
    }
}

@Composable
private fun FxRespiro(texto: String, base: TextStyle, vez: Int) {
    val t = progresso(vez, 4800)
    Box(Modifier.fillMaxWidth().height(90.dp).drawBehind {
        val c = (t * 3) % 1f
        if (t < 1f) drawOval(Brush.radialGradient(listOf(Color(0x47DCDCFF).copy(alpha = 0.28f * (1 - c)), Color.Transparent)), Offset(center.x - 90.dp.toPx() * (0.3f + c), center.y - 30.dp.toPx() * (0.3f + c)), Size(180.dp.toPx() * (0.3f + c), 60.dp.toPx() * (0.3f + c)))
    }, contentAlignment = Alignment.Center) {
        Text(texto, style = base.copy(fontStyle = FontStyle.Italic, color = Color(0xFFFFC6DC)), modifier = Modifier.graphicsLayer { val m = sin(t * 3 * 2 * PI.toFloat()).let { it * it }; alpha = 1 - 0.4f * m; scaleX = 1 + 0.04f * m; scaleY = scaleX })
    }
}

@Composable
private fun FxOndas(texto: String, vez: Int) {
    val t = progresso(vez, 3000)
    val alturas = remember { val r = Random(7); List(16) { 0.2f + r.nextFloat() * 0.8f to (2 + r.nextFloat() * 4) } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.height(44.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
            alturas.forEach { (h, f) ->
                val m = if (t < 1f) (0.15f + 0.85f * abs(sin(t * f * 2 * PI.toFloat()))) else 0.35f
                Box(Modifier.width(5.dp).fillMaxHeight(h * m).clip(RoundedCornerShape(2.dp)).background(Brush.verticalGradient(listOf(FxRoxo, Color(0xFF5A3FC0)))))
            }
        }
        Text(texto.uppercase(), fontFamily = FxMono, fontSize = 12.sp, letterSpacing = 2.sp, color = FxSuave, modifier = Modifier.padding(top = 8.dp))
    }
}

// ───────── partículas (calculadas pelo tempo, sem estado) ─────────

@Composable
private fun FxParticulas(tipo: String, texto: String, vez: Int) {
    val t = progresso(vez, 3600)
    val seeds = remember(tipo) { val r = Random(tipo.hashCode()); List(60) { FloatArray(5) { r.nextFloat() } } }
    val fim = if (t >= 1f) 0f else 1f - fase(t, 0.85f, 1f)
    Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).drawBehind {
        if (fim <= 0f) return@drawBehind
        val s = t * 3.6f; val W = size.width; val H = size.height
        when (tipo) {
            "brasas" -> seeds.take(40).forEach { q -> val y = H - ((q[1] * H * 1.4f + s * (40 + q[2] * 60).dp.toPx()) % (H * 1.4f)); val x = q[0] * W + sin(s * 3 + q[3] * 6) * 8f
                drawCircle(Color(0xFFFF8A3D).copy(alpha = fim * (y / H).coerceIn(0.2f, 1f)), (1 + q[4] * 2).dp.toPx(), Offset(x, y)) }
            "vagalumes" -> seeds.take(26).forEach { q -> val x = (q[0] * W + sin(s * 0.8f + q[3] * 6) * 30f); val y = (q[1] * H + cos(s * 0.6f + q[4] * 6) * 20f)
                val a = 0.35f + 0.65f * abs(sin(s * 2.5f + q[2] * 6)); drawCircle(Color(0xFFE8FF8C).copy(alpha = a * 0.35f * fim), 7.dp.toPx(), Offset(x, y)); drawCircle(Color(0xFFE8FF8C).copy(alpha = a * fim), 2.dp.toPx(), Offset(x, y)) }
            "brilhos" -> seeds.take(18).forEach { q -> val v = (q[2] + s * 0.6f) % 1f; val k = sin(v * PI.toFloat()); val x = (0.12f + q[0] * 0.76f) * W; val y = (0.15f + q[1] * 0.7f) * H; val r = (4 + q[3] * 6).dp.toPx() * k
                drawLine(Color(0xFFFFF0C8).copy(alpha = k * fim), Offset(x - r, y), Offset(x + r, y), 1.5f); drawLine(Color(0xFFFFF0C8).copy(alpha = k * fim), Offset(x, y - r), Offset(x, y + r), 1.5f) }
            "notas" -> seeds.take(12).forEach { q -> val y = H - ((q[1] * H + s * (25 + q[2] * 20).dp.toPx()) % (H * 1.2f)); val x = q[0] * W + sin(s * 2 + q[3] * 6) * 10f
                drawCircle(Color(0xFFC7B3FF).copy(alpha = fim * (y / H).coerceIn(0f, 1f)), 4.dp.toPx(), Offset(x, y)); drawLine(Color(0xFFC7B3FF).copy(alpha = fim * (y / H).coerceIn(0f, 1f)), Offset(x + 4.dp.toPx(), y), Offset(x + 4.dp.toPx(), y - 14.dp.toPx()), 1.5.dp.toPx()) }
            "estrelas" -> seeds.forEach { q -> val a = q[0] * 2 * PI.toFloat(); val d0 = ((q[1] * 0.3f + s * (0.15f + q[2] * 0.25f)) % 1f); val d = d0 * d0 * W * 0.7f; val l = d0 * 30.dp.toPx()
                drawLine(Color(0xFFE6E6FF).copy(alpha = fim * d0), Offset(center.x + cos(a) * d, center.y + sin(a) * d), Offset(center.x + cos(a) * (d + l), center.y + sin(a) * (d + l)), 1.5f) }
            "faiscas", "moedas" -> seeds.forEach { q -> val moeda = tipo == "moedas"; val st = s.coerceAtMost(3f)
                val vx = (q[0] - 0.5f) * (if (moeda) 160 else 360).dp.toPx(); val vy = (if (moeda) -(200 + q[1] * 160) else (q[1] - 0.7f) * 360).dp.toPx(); val g = (if (moeda) 420 else 260).dp.toPx()
                val x = center.x + vx * st * 0.5f; val y = (if (moeda) H else center.y) + vy * st * 0.5f + g * st * st * 0.25f
                if (moeda) drawOval(FxOuro.copy(alpha = fim), Offset(x - 5.dp.toPx() * abs(cos(s * 8 + q[2] * 6)), y - 5.dp.toPx()), Size(10.dp.toPx() * abs(cos(s * 8 + q[2] * 6)) + 1f, 10.dp.toPx()))
                else drawLine(Color(0xFFFFD27A).copy(alpha = (1 - st / 3f) * fim), Offset(x, y), Offset(x - vx * 0.02f, y - vy * 0.02f), 2f) }
        }
    }, contentAlignment = Alignment.Center) {
        Text(texto, fontStyle = FontStyle.Italic, fontSize = 20.sp, color = FxCreme, textAlign = TextAlign.Center, style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 12f)), modifier = Modifier.padding(12.dp))
    }
}
