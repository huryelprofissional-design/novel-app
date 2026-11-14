package com.huryel.novely.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.ui.theme.InstrumentSerif
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ───────── Efeitos 2.0: blocos [$nome:cor campo | campo] e trechos {$nome: texto} ─────────
// Mesmos nomes e campos do PC (web/fx2.js). Os antigos ([#…], {fogo: …}) não mudam.

val BLOCOS_V2 = setOf(
    "sistema", "avisos", "recompensa", "despertar", "penalidade", "patrocinio", "portal", "morte", "recarga", "fusao", "carregando",
    "pincel", "jade", "ameixa", "dez", "constelacao", "vitral", "brasao", "livro", "pocao", "eclosao", "juramento",
    "repete", "foto-muda", "digital", "cofre", "relogio", "recortes", "hud", "autodestruicao", "dobra", "dna", "olho-ia", "radar",
    "video", "live", "pix", "visto", "forum", "traduz", "alarme", "batimento", "fio", "bilhete", "polaroid", "tempo-parado",
    "abertura", "arco", "dias", "coord",
)
val APELIDOS_V2 = mapOf("patrocínio" to "patrocinio", "fusão" to "fusao", "poção" to "pocao", "eclosão" to "eclosao", "ovo" to "eclosao",
    "relógio" to "relogio", "autodestruição" to "autodestruicao", "vídeo" to "video", "fórum" to "forum", "tradutor" to "traduz", "brasão" to "brasao",
    "constelação" to "constelacao", "recorte" to "recortes", "coordenadas" to "coord", "foto" to "foto-muda", "olho" to "olho-ia", "ia" to "olho-ia",
    "tempoparado" to "tempo-parado", "contador" to "dias")
/** Os que abrem com um toque (os outros repetem a animação ao tocar). */
private val TOQUE_V2 = setOf("eclosao", "foto-muda", "bilhete")
/** Vibração (ms entre toques) de quem vibra o celular. */
private val VIBRA_V2 = mapOf("despertar" to listOf(0, 100, 260), "penalidade" to listOf(0, 300), "morte" to listOf(0), "autodestruicao" to listOf(0, 140),
    "batimento" to listOf(0, 160, 700, 160), "recompensa" to listOf(0, 90), "juramento" to listOf(800), "cofre" to listOf(2200))

/** "[$sistema:azul …]" → nome sem o $ (ou null). */
fun nomeBlocoV2(cab: String): String? { val n0 = cab.substringBefore(':').lowercase().trimEnd('+', '-'); val n = APELIDOS_V2[n0] ?: n0; return n.takeIf { it in BLOCOS_V2 } }

private val V2_MONO = FontFamily.Monospace
private val V2_SERIF = InstrumentSerif

@Composable private fun parado() = LocalSemMovimento.current

/** 0 → 1 uma vez. */
@Composable
private fun ent(ms: Int = 700, atraso: Int = 0, easing: Easing = FastOutSlowInEasing): Float {
    if (parado()) return 1f
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(atraso.toLong()); a.animateTo(1f, tween(ms, easing = easing)) }
    return a.value
}
/** 0..1 sem parar. */
@Composable
private fun volta(ms: Int, vaiVolta: Boolean = false): Float {
    if (parado()) return 0.5f
    val t = rememberInfiniteTransition(label = "v2")
    return t.animateFloat(0f, 1f, infiniteRepeatable(tween(ms, easing = LinearEasing), if (vaiVolta) RepeatMode.Reverse else RepeatMode.Restart), label = "v").value
}
/** Número que anda de [de] até [ate]. */
@Composable
private fun conta(de: Float, ate: Float, ms: Int, linear: Boolean = false, atraso: Int = 0): Float {
    if (parado()) return ate
    val a = remember { Animatable(de) }
    LaunchedEffect(Unit) { delay(atraso.toLong()); a.animateTo(ate, tween(max(1, ms), easing = if (linear) LinearEasing else FastOutSlowInEasing)) }
    return a.value
}
private fun numV2(s: String, padrao: Float = 0f) = Regex("-?\\d+([.,]\\d+)?").find(s.replace(".", ""))?.value?.replace(',', '.')?.toFloatOrNull() ?: padrao
private fun segundos(s: String): Int = if (':' in s) s.split(':').map { it.trim().toIntOrNull() ?: 0 }.fold(0) { a, b -> a * 60 + b } else numV2(s, 0f).toInt()
private fun hms(v: Int): String { val s = max(0, v); return "%02d:%02d:%02d".format(s / 3600, (s / 60) % 60, s % 60) }
private fun ms(v: Int): String { val s = max(0, v); return if (s >= 3600) hms(s) else "%02d:%02d".format(s / 60, s % 60) }
private fun reais(v: Float) = "R$ " + java.text.NumberFormat.getNumberInstance(java.util.Locale("pt", "BR")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(v.toDouble())
private fun milharV2(v: Int) = java.text.NumberFormat.getIntegerInstance(java.util.Locale("pt", "BR")).format(v.toLong())
private fun raridade(r: String): Color { val x = r.lowercase(); return when {
    x.startsWith("lend") -> Color(0xFFF5C04A); x.startsWith("mít") || x.startsWith("mit") -> Color(0xFFFF4458); x.startsWith("ép") || x.startsWith("ep") -> Color(0xFFB57BFF)
    x.startsWith("incom") -> Color(0xFF6FCF97); x.startsWith("rar") -> Color(0xFF5CC8FF); else -> Color(0xFFB0B0B8) } }
private fun corRankV2(r: String) = when (r.uppercase()) { "S" -> Color(0xFFF5A623); "SS" -> Color(0xFFFF4458); "SSS" -> Color(0xFFFF2AD4); "A" -> Color(0xFFB57BFF)
    "B" -> Color(0xFF5CC8FF); "C" -> Color(0xFF6FCF97); else -> Color(0xFFB0B0B8) }

/** Rótulo em maiúsculas, espaçado. */
@Composable private fun Rot2(t: String, cor: Color, tam: Float = 10f, modifier: Modifier = Modifier) =
    Text(t.uppercase(), fontWeight = FontWeight.Bold, fontSize = tam.sp, letterSpacing = (tam * 0.25f).sp, color = cor, textAlign = TextAlign.Center, modifier = modifier)

/** Cantoneiras de Sistema. */
private fun Modifier.cantos(cor: Color) = drawBehind {
    val l = 12.dp.toPx(); val e = 2.dp.toPx()
    listOf(Offset(0f, 0f) to Pair(1, 1), Offset(size.width, 0f) to Pair(-1, 1), Offset(0f, size.height) to Pair(1, -1), Offset(size.width, size.height) to Pair(-1, -1)).forEach { (o, d) ->
        drawRect(cor, Offset(if (d.first > 0) o.x else o.x - l, if (d.second > 0) o.y else o.y - e), Size(l, e))
        drawRect(cor, Offset(if (d.first > 0) o.x else o.x - e, if (d.second > 0) o.y else o.y - l), Size(e, l))
    }
}
/** Mostra só a parte da esquerda (pincel, assinatura). */
private fun Modifier.revelar(f: Float) = drawWithContent { clipRect(right = size.width * f) { this@drawWithContent.drawContent() } }
/** Faixa de luz passando por cima. */
private fun Modifier.reflexo(p: Float, alfa: Float = 0.45f) = drawWithContent {
    drawContent(); val x = (-0.4f + 1.8f * p) * size.width
    drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = alfa), Color.Transparent), Offset(x - 60f, 0f), Offset(x + 60f, size.height)))
}

// ───── partículas ─────
@Composable
private fun Particulas(tipo: String, n: Int, modifier: Modifier = Modifier.fillMaxSize()) {
    if (parado()) return
    val t = rememberInfiniteTransition(label = "pt")
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "pv")
    val sementes = remember(tipo, n) { List(n) { i -> Random(i * 7919 + tipo.hashCode()).let { r -> FloatArray(5) { r.nextFloat() } } } }
    Canvas(modifier) {
        val w = size.width; val h = size.height
        sementes.forEach { s ->
            val per = when (tipo) { "petala2" -> 1.4f + s[4]; "risco" -> 0.6f + s[4] * 0.5f; "brasa" -> 2.5f + 2 * s[4]; "coracao" -> 2f + s[4]; else -> 1.6f + s[4] * 1.2f }
            val p = ((v * 20f / per) + s[0]) % 1f
            when (tipo) {
                "moeda" -> drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3B0), Color(0xFFF5C04A), Color(0xFFA8730C))), 4.5.dp.toPx(), Offset(s[1] * w + (s[2] - .5f) * 80f * p, -10f + p * (h + 20)))
                "petala2" -> { val c = Offset(-10f + p * (w + 20), s[1] * h + (s[2] - .5f) * 120f * p)
                    rotate(p * 720f, c) { drawOval(if (s[3] < .6f) Color(0xFFE0405E) else Color(0xFFFFF2F4), Offset(c.x - 4.5f.dp.toPx(), c.y - 3f.dp.toPx()), Size(9.dp.toPx(), 6.dp.toPx())) } }
                "coracao" -> { val x = w - 22.dp.toPx() + (s[2] - .8f) * 60f * p; val y = h - 14.dp.toPx() - p * 150.dp.toPx(); val r = (4f + 3f * p).dp.toPx()
                    val a = if (p < .1f) p * 10 else 1 - p; val cor = listOf(Color(0xFFFF3D6E), Color(0xFFFF8FAB), Color(0xFFFFD1DC))[(s[3] * 3).toInt().coerceIn(0, 2)].copy(alpha = a.coerceIn(0f, 1f))
                    drawCircle(cor, r * .55f, Offset(x - r * .45f, y)); drawCircle(cor, r * .55f, Offset(x + r * .45f, y))
                    drawPath(Path().apply { moveTo(x - r, y + r * .1f); lineTo(x, y + r * 1.1f); lineTo(x + r, y + r * .1f); close() }, cor) }
                "brasa" -> { val c = Offset(s[1] * w + (s[2] - .5f) * 80f * p, h - p * (h + 10)); val a = (if (p < .15f) p / .15f else 1 - p).coerceIn(0f, 1f)
                    drawCircle(Color(0xFFFF6A00).copy(alpha = a * .35f), 4.dp.toPx(), c); drawCircle(Color(0xFFFFB347).copy(alpha = a), 1.5.dp.toPx(), c) }
                "risco" -> { val ang = s[1] * 2 * PI.toFloat(); val d0 = 8.dp.toPx() + p * 260.dp.toPx(); val len = 80.dp.toPx() * p * 1.6f; val a = (if (p < .3f) p / .3f else 1 - p).coerceIn(0f, 1f)
                    val c = Offset(w / 2, h / 2); drawLine(Color(0xFFD6E6FF).copy(alpha = a), c + Offset(cos(ang) * d0, sin(ang) * d0), c + Offset(cos(ang) * (d0 + len), sin(ang) * (d0 + len)), 1.5f.dp.toPx()) }
                "poeira" -> drawCircle(Color(0xFFDFE6EA).copy(alpha = .5f + .45f * sin(p * 2 * PI.toFloat())), 1.dp.toPx(), Offset(s[1] * w, s[2] * h + 3f * sin(p * 2 * PI.toFloat())))
            }
        }
    }
}

@Composable
private fun FaixaPerigo(faixa: Float) = Canvas(Modifier.width(280.dp).height(12.dp)) {
    clipRect { val p = 34.dp.toPx(); var x = -p + faixa * p
        while (x < size.width) { drawPath(Path().apply { moveTo(x, size.height); lineTo(x + p / 2, 0f); lineTo(x + p, 0f); lineTo(x + p / 2, size.height); close() }, Color(0xFFF5C400)); x += p } }
}

/** Vibra o celular (respeita o "Reduzir animações" e a vibração ao toque do sistema). */
@Composable
private fun Vibrar(nome: String) {
    val padrao = VIBRA_V2[nome] ?: return
    if (parado()) return
    val vista = LocalView.current
    LaunchedEffect(Unit) { padrao.forEach { espera -> delay(espera.toLong()); vista.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } }
}

/** Palco do efeito: fundo preto, cantos redondos; tocar repete (ou abre, nos de toque). */
@Composable
fun BlocoV2(nome: String, corNome: String?, c: List<String>, estilo: TextStyle, modifier: Modifier = Modifier) {
    var vez by remember { mutableIntStateOf(0) }
    var aberto by remember { mutableStateOf(false) }
    val vista = LocalView.current
    Box(modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black).heightIn(min = 170.dp)
        .clickable(remember { MutableInteractionSource() }, null) {
            if (nome in TOQUE_V2) { aberto = !aberto; if (aberto && nome == "eclosao") vista.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } else vez++
        }.padding(16.dp), contentAlignment = Alignment.Center) {
        key(vez) {
            Vibrar(nome)
            ConteudoV2(nome, corNome, c, estilo, aberto)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.ConteudoV2(nome: String, corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    when (nome) {
        "sistema" -> fxV2_sistema(corNome, c, estilo, aberto)
        "avisos" -> fxV2_avisos(corNome, c, estilo, aberto)
        "recompensa" -> fxV2_recompensa(corNome, c, estilo, aberto)
        "despertar" -> fxV2_despertar(corNome, c, estilo, aberto)
        "penalidade" -> fxV2_penalidade(corNome, c, estilo, aberto)
        "patrocinio" -> fxV2_patrocinio(corNome, c, estilo, aberto)
        "portal" -> fxV2_portal(corNome, c, estilo, aberto)
        "morte" -> fxV2_morte(corNome, c, estilo, aberto)
        "recarga" -> fxV2_recarga(corNome, c, estilo, aberto)
        "fusao" -> fxV2_fusao(corNome, c, estilo, aberto)
        "carregando" -> fxV2_carregando(corNome, c, estilo, aberto)
        "pincel" -> fxV2_pincel(corNome, c, estilo, aberto)
        "jade" -> fxV2_jade(corNome, c, estilo, aberto)
        "ameixa" -> fxV2_ameixa(corNome, c, estilo, aberto)
        "dez" -> fxV2_dez(corNome, c, estilo, aberto)
        "constelacao" -> fxV2_constelacao(corNome, c, estilo, aberto)
        "vitral" -> fxV2_vitral(corNome, c, estilo, aberto)
        "brasao" -> fxV2_brasao(corNome, c, estilo, aberto)
        "livro" -> fxV2_livro(corNome, c, estilo, aberto)
        "pocao" -> fxV2_pocao(corNome, c, estilo, aberto)
        "eclosao" -> fxV2_eclosao(corNome, c, estilo, aberto)
        "juramento" -> fxV2_juramento(corNome, c, estilo, aberto)
        "repete" -> fxV2_repete(corNome, c, estilo, aberto)
        "foto-muda" -> fxV2_foto_muda(corNome, c, estilo, aberto)
        "digital" -> fxV2_digital(corNome, c, estilo, aberto)
        "cofre" -> fxV2_cofre(corNome, c, estilo, aberto)
        "relogio" -> fxV2_relogio(corNome, c, estilo, aberto)
        "recortes" -> fxV2_recortes(corNome, c, estilo, aberto)
        "hud" -> fxV2_hud(corNome, c, estilo, aberto)
        "autodestruicao" -> fxV2_autodestruicao(corNome, c, estilo, aberto)
        "dobra" -> fxV2_dobra(corNome, c, estilo, aberto)
        "dna" -> fxV2_dna(corNome, c, estilo, aberto)
        "olho-ia" -> fxV2_olho_ia(corNome, c, estilo, aberto)
        "radar" -> fxV2_radar(corNome, c, estilo, aberto)
        "video" -> fxV2_video(corNome, c, estilo, aberto)
        "live" -> fxV2_live(corNome, c, estilo, aberto)
        "pix" -> fxV2_pix(corNome, c, estilo, aberto)
        "visto" -> fxV2_visto(corNome, c, estilo, aberto)
        "forum" -> fxV2_forum(corNome, c, estilo, aberto)
        "traduz" -> fxV2_traduz(corNome, c, estilo, aberto)
        "alarme" -> fxV2_alarme(corNome, c, estilo, aberto)
        "batimento" -> fxV2_batimento(corNome, c, estilo, aberto)
        "fio" -> fxV2_fio(corNome, c, estilo, aberto)
        "bilhete" -> fxV2_bilhete(corNome, c, estilo, aberto)
        "polaroid" -> fxV2_polaroid(corNome, c, estilo, aberto)
        "tempo-parado" -> fxV2_tempo_parado(corNome, c, estilo, aberto)
        "abertura" -> fxV2_abertura(corNome, c, estilo, aberto)
        "arco" -> fxV2_arco(corNome, c, estilo, aberto)
        "dias" -> fxV2_dias(corNome, c, estilo, aberto)
        "coord" -> fxV2_coord(corNome, c, estilo, aberto)
        else -> Text(c.joinToString(" | "), style = estilo)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_sistema(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val cor = corFx(corNome, Color(0xFF8FD0FF)); val a = ent(700, easing = LinearEasing); val scan = volta(2600)
        val tit = if (c.size > 1) f(0) else "SISTEMA"; val ls = if (c.size > 1) c.drop(1) else c
        Column(Modifier.fillMaxWidth(0.94f).graphicsLayer { scaleX = if (a < .45f) .5f + a / .45f * .5f else 1f; scaleY = if (a < .45f) .03f else .03f + (a - .45f) / .55f * .97f; alpha = min(1f, a * 3) }
            .background(Brush.verticalGradient(listOf(lerp(Color.Black, cor, .22f), lerp(Color.Black, cor, .1f)))).border(1.dp, cor.copy(alpha = .45f)).cantos(cor)
            .drawWithContent { drawContent(); val y = (-0.4f + 1.5f * scan) * size.height; drawRect(Brush.verticalGradient(listOf(Color.Transparent, cor.copy(alpha = .16f), Color.Transparent), y, y + size.height * .4f), Offset(0f, y), Size(size.width, size.height * .4f)) }
            .padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("◆ " + tit.uppercase(), color = cor, fontSize = 11.sp, letterSpacing = 2.4.sp, fontWeight = FontWeight.SemiBold)
            ls.forEachIndexed { k, l -> val b = ent(400, 750 + k * 300)
                Text(buildAnnotatedString { l.split("???").forEachIndexed { i, p -> if (i > 0) withStyle(SpanStyle(color = Color(0xFFFFD76B), fontWeight = FontWeight.ExtraBold)) { append("???") }; append(p) } },
                    color = Color(0xFFE4F1FF), fontSize = 14.sp, modifier = Modifier.graphicsLayer { alpha = b; translationX = (1 - b) * -10f * density }) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_avisos(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val cor = corFx(corNome, Color(0xFF3A8BFF))
        Column(Modifier.fillMaxWidth(0.94f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            c.forEachIndexed { k, l -> val b = ent(500, k * 500)
                val ic = if (Regex("^[+\\d]").containsMatchIn(l) || Regex("xp|moeda|ouro", RegexOption.IGNORE_CASE).containsMatchIn(l)) "+" else if (Regex("t[ií]tulo|conquista|novo", RegexOption.IGNORE_CASE).containsMatchIn(l)) "★" else "!"
                Row(Modifier.fillMaxWidth().graphicsLayer { alpha = b; translationY = (1 - b) * -30f * density }.clip(RoundedCornerShape(10.dp)).background(lerp(Color(0xFF0B1220), cor, .16f))
                    .border(1.dp, cor.copy(alpha = .35f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(cor), contentAlignment = Alignment.Center) { Text(ic, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp)); Text(l, color = Color(0xFFE3EFFF), fontSize = 13.sp)
                }
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_recompensa(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val cor = raridade(f(0)); val a = ent(1400); val brilho = volta(2000, true); val luz = volta(2400)
        Column(Modifier.width(210.dp).graphicsLayer { rotationY = (1 - a) * 900f; val e = .4f + .6f * a; scaleX = e; scaleY = e; alpha = a; cameraDistance = 14f * density }
            .clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(lerp(Color.Black, cor, .22f), Color(0xFF0C0C0C))))
            .border(1.dp + (2.dp * brilho * a), cor.copy(alpha = .6f + .4f * brilho), RoundedCornerShape(12.dp)).reflexo(luz).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Rot2(f(0).ifEmpty { "comum" }, cor, 10f)
            Text(f(1), fontFamily = V2_SERIF, fontSize = 22.sp, color = Color(0xFFFFF3D8), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
            c.drop(2).forEach { Text(it, fontSize = 12.sp, color = Color(0xFFD9D0BD), textAlign = TextAlign.Center) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_despertar(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val cor = corRankV2(f(0)); val flash = ent(1000); val a = ent(1200, 300); val t = ent(500, 1300)
        Box(Modifier.matchParentSize().background(Color.White.copy(alpha = 1 - flash)))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(f(0).ifEmpty { "S" }.uppercase(), fontFamily = V2_SERIF, fontSize = 96.sp, fontWeight = FontWeight.Black,
                style = TextStyle(brush = Brush.verticalGradient(listOf(Color.White, cor, lerp(cor, Color.Black, .4f))), shadow = Shadow(cor.copy(alpha = .7f), blurRadius = 36f)),
                modifier = Modifier.graphicsLayer { val e = 3f - 2f * a; scaleX = e; scaleY = e; rotationZ = (1 - a) * -25f; alpha = a })
            c.drop(1).forEach { l -> val k = l.indexOf(':')
                Text(buildAnnotatedString { if (k > 0) { append(l.substring(0, k + 1) + " "); withStyle(SpanStyle(color = Color(0xFFFFD76B), fontWeight = FontWeight.Bold)) { append(l.substring(k + 1).trim()) } } else append(l) },
                    color = Color(0xFFDDDDDD), fontSize = 14.sp, modifier = Modifier.graphicsLayer { alpha = t }) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_penalidade(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val s = segundos(f(1).ifEmpty { "10" }); val resta = conta(s.toFloat(), 0f, s * 1000, linear = true); val pisca = volta(1000) < .5f
        Box(Modifier.matchParentSize().background(Brush.radialGradient(listOf(Color.Transparent, Color(0xFFC80000).copy(alpha = if (pisca) .55f else .28f)), radius = 700f)))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("PENALIDADE", fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = 6.sp, color = Color(0xFFFF3B3B), style = TextStyle(shadow = Shadow(Color.Red, blurRadius = 30f)))
            Text(f(0), color = Color(0xFFFFBDBD), fontSize = 13.sp, textAlign = TextAlign.Center)
            Text(hms(ceil(resta).toInt()), fontFamily = V2_MONO, fontSize = 22.sp, color = Color.White)
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_patrocinio(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        Particulas("moeda", 16, Modifier.matchParentSize())
        val n = conta(0f, numV2(f(1), 100f), 1600)
        Column(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xD1281C06)).border(1.dp, Color(0x80FFC85A), RoundedCornerShape(12.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Rot2("✦ Constelação", Color(0xFFF5C04A), 10f)
            Text(f(0), fontFamily = V2_SERIF, fontSize = 22.sp, color = Color(0xFFFFD76B), textAlign = TextAlign.Center)
            Text(buildAnnotatedString { append("patrocinou você com "); withStyle(SpanStyle(color = Color(0xFFFFD76B), fontWeight = FontWeight.ExtraBold)) { append(milharV2(n.toInt())) }; append(" " + f(2).ifEmpty { "moedas" } + ".") },
                color = Color(0xFFF7E6C0), fontSize = 13.sp)
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_portal(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val cor = corRankV2(f(0)); val g = volta(3000); val g2 = volta(2000)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize()) {
                    val cs = listOf(cor, Color(0xFF2AD1FF), cor, Color(0xFFFF2AD4), cor)
                    drawCircle(Brush.radialGradient(listOf(lerp(Color.Black, cor, .35f), Color.Black)), size.minDimension * .3f)
                    rotate(g * 360f) { drawCircle(Brush.sweepGradient(cs), size.minDimension * .43f, style = Stroke(size.minDimension * .1f)) }
                    rotate(-g2 * 360f) { drawCircle(Brush.sweepGradient(cs), size.minDimension * .3f, style = Stroke(size.minDimension * .06f), alpha = .7f) }
                }
                Text(f(0).ifEmpty { "A" }.uppercase(), fontFamily = V2_SERIF, fontWeight = FontWeight.Black, fontSize = 42.sp, color = Color(0xFFF1E8FF), style = TextStyle(shadow = Shadow(cor, blurRadius = 30f)))
            }
            Rot2("Portal rank ${f(0).ifEmpty { "A" }}" + if (f(1).isNotEmpty()) " · ${f(1)}" else "", Color(0xFFC9B9FF), 11f, Modifier.padding(top = 4.dp))
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_morte(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val txt = f(0).ifEmpty { "Você morreu" }.uppercase(); val s = numV2(f(1), 3f).toInt(); val resta = conta(s.toFloat(), 0f, s * 1000, linear = true, atraso = 1600); val t = ent(600, 1400)
        Box(Modifier.matchParentSize().background(Brush.radialGradient(listOf(Color(0x99460000), Color.Black))))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row { txt.forEachIndexed { i, ch -> val b = ent(600, i * 90); Text(if (ch == ' ') " " else ch.toString(), fontFamily = V2_SERIF, fontSize = 28.sp, letterSpacing = 4.sp, color = Color(0xFFDD1111), modifier = Modifier.graphicsLayer { alpha = b; scaleX = 1.3f - .3f * b; scaleY = 1.3f - .3f * b }) } }
            if (s > 0) Text("Renascendo em ${ceil(resta).toInt()}…", color = Color(0xFFAAAAAA), fontSize = 13.sp, modifier = Modifier.graphicsLayer { alpha = t })
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_recarga(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val s = max(1, numV2(f(1), 10f).toInt()); val p = conta(0f, 1f, s * 1000, linear = true)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize()) { val e = 8.dp.toPx(); val r = Size(size.width - e, size.height - e); val o = Offset(e / 2, e / 2)
                    drawArc(Color(0xFF1D2A3F), 0f, 360f, false, o, r, style = Stroke(e)); drawArc(Color(0xFF5FB0FF), -90f, 360f * (1 - p), false, o, r, style = Stroke(e, cap = StrokeCap.Round)) }
                Text("${ceil(s * (1 - p)).toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color.White)
            }
            Text(f(0), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF9CC8FF), modifier = Modifier.padding(top = 6.dp))
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_fusao(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val m = Regex("^(.+?)\\s*\\+\\s*(.+?)\\s*=\\s*(.+)$").find(f(0))?.groupValues; val a1 = m?.get(1) ?: f(0); val a2 = m?.get(2) ?: f(1); val r = m?.get(3) ?: f(2)
        val a = ent(1200, easing = FastOutLinearInEasing); val n = ent(500, 1150); val fl = ent(500, 1100)
        Box(Modifier.fillMaxWidth().height(110.dp)) {
            Text(a1, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(BiasAlignment(-0.85f + a * 0.7f, 0f)).graphicsLayer { alpha = 1 - a }.clip(RoundedCornerShape(20.dp)).background(Color(0xFFD94A1E)).padding(horizontal = 12.dp, vertical = 6.dp))
            Text(a2, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(BiasAlignment(0.85f - a * 0.7f, 0f)).graphicsLayer { alpha = 1 - a }.clip(RoundedCornerShape(20.dp)).background(Color(0xFF2F8F8A)).padding(horizontal = 12.dp, vertical = 6.dp))
            Canvas(Modifier.matchParentSize()) { if (fl in 0.01f..0.99f) drawCircle(Color.White.copy(alpha = 1 - fl), 10.dp.toPx() * (1 + fl * 8), center) }
            Text(r, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.align(Alignment.Center).graphicsLayer { val e = if (n < .7f) n / .7f * 1.15f else 1.15f - (n - .7f) / .3f * .15f; scaleX = e; scaleY = e }
                .clip(RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFD94A1E), Color(0xFFF0A030), Color(0xFF2F8F8A)))).padding(horizontal = 16.dp, vertical = 8.dp))
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_carregando(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val alvo = min(100f, numV2(f(1), 100f)); val p = conta(0f, 1f, 3000, linear = true)
        Column(Modifier.fillMaxWidth(0.9f)) {
            Text(f(0).ifEmpty { "Carregando" } + "…", fontFamily = V2_MONO, fontSize = 13.sp, color = Color(0xFF9FE3FF))
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(20) { k -> val on = k < p * 20 * alvo / 100f; Box(Modifier.weight(1f).height(10.dp).background(if (on) Color(0xFF3FD0FF) else Color(0xFF0C2A35))) }
            }
            Text("${(p * alvo).toInt()}%", fontFamily = V2_MONO, fontSize = 20.sp, color = Color.White)
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_pincel(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val a = ent(1600); val t = ent(600, 1500); val s = ent(300, 1800)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(f(0), fontFamily = FontFamily.Serif, fontSize = 52.sp, color = Color(0xFFF1ECE2), fontWeight = FontWeight.Bold, modifier = Modifier.revelar(a))
            if (f(1).isNotEmpty()) Text(f(1), fontFamily = leitura, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = Color(0xFFC9A46A), textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = t })
            Text(f(2).ifEmpty { f(0).lastOrNull()?.toString() ?: "印" }, fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp).graphicsLayer { val e = if (s < .6f) s / .6f * 1.35f else 1.35f - (s - .6f) / .4f * .35f; scaleX = e; scaleY = e; rotationZ = -8f }.clip(RoundedCornerShape(3.dp)).background(Color(0xFFB3261E)).padding(horizontal = 6.dp, vertical = 2.dp))
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_jade(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val luz = volta(2600); val (rot, nm, rest) = if (c.size > 2) Triple(f(0), f(1), c.drop(2)) else Triple("Placa de jade", f(0), c.drop(1))
        Column(Modifier.fillMaxWidth(0.84f).clip(RoundedCornerShape(14.dp)).background(Brush.radialGradient(listOf(Color(0xFF7FD4A8), Color(0xFF2E8A62), Color(0xFF14543A)), Offset(120f, 40f), 700f))
            .border(2.dp, Color(0xFFA8E6C4), RoundedCornerShape(14.dp)).reflexo(luz, .5f).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Rot2(rot, Color(0xFF0D3A27), 10f)
            Text(nm, fontFamily = V2_SERIF, fontSize = 25.sp, color = Color(0xFFF2FFF7), style = TextStyle(shadow = Shadow(Color(0xFF0B3A26), Offset(0f, 2f), 1f)), textAlign = TextAlign.Center)
            rest.forEach { Text(it, fontStyle = FontStyle.Italic, fontFamily = leitura, fontSize = 14.sp, color = Color(0xFFE2FFF0), textAlign = TextAlign.Center) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_ameixa(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        Particulas("petala2", 18, Modifier.matchParentSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(f(0), fontFamily = V2_SERIF, fontSize = 23.sp, color = Color(0xFFFFE3EA), textAlign = TextAlign.Center, style = TextStyle(shadow = Shadow(Color(0x99FF5A78), blurRadius = 26f)))
            c.drop(1).forEach { Text(it, fontStyle = FontStyle.Italic, fontFamily = leitura, fontSize = 14.sp, color = Color(0xFFE8A9BF), textAlign = TextAlign.Center) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_dez(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val a = ent(1100); val cn = "一二三四五六七八九十"
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Rot2(f(0), Color(0xFFC9A46A), 10f, Modifier.padding(bottom = 6.dp))
            Column(Modifier.widthIn(max = 300.dp).fillMaxWidth().drawWithContent { val w = size.width; clipRect(left = w / 2 * (1 - a), right = w / 2 * (1 + a)) { this@drawWithContent.drawContent() } }
                .background(Color(0xFFE8D7B0)).drawBehind { drawRect(Color(0xFF5A3A1E), size = Size(8.dp.toPx(), size.height)); drawRect(Color(0xFF5A3A1E), Offset(size.width - 8.dp.toPx(), 0f), Size(8.dp.toPx(), size.height)) }
                .padding(horizontal = 22.dp, vertical = 10.dp)) {
                c.drop(1).take(10).forEachIndexed { k, x -> Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(cn[k].toString(), fontFamily = FontFamily.Serif, fontSize = 18.sp, color = Color(0xFF9C2A1A), fontWeight = FontWeight.Bold); Spacer(Modifier.width(10.dp))
                    Text(x.replace(Regex("^\\d+[.)]?\\s*"), ""), fontFamily = leitura, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF3B2A14)) } }
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_constelacao(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val a = ent(2000, 600); val t = ent(800, 2200)
        val pts = remember(f(0)) { val r = Random(f(0).hashCode()); List(6) { k -> Offset((18 + k * 37 + r.nextFloat() * 10) / 220f, (18 + r.nextFloat() * 74) / 110f) } }
        val est = pts.indices.map { ent(400, it * 100) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(220.dp, 110.dp)) {
                val P = pts.map { Offset(it.x * size.width, it.y * size.height) }; val q = a * 4f
                for (k in 0 until 4) { val parte = (q - k).coerceIn(0f, 1f); if (parte > 0f) drawLine(Color(0xFF9FC3FF), P[k], P[k] + (P[k + 1] - P[k]) * parte, 1.2.dp.toPx()) }
                P.forEachIndexed { k, o -> drawCircle(Color.White.copy(alpha = est[k] * .3f), 6.dp.toPx(), o); drawCircle(Color.White.copy(alpha = est[k]), (if (k == 2) 3.8f else 3f).dp.toPx(), o) }
            }
            Text(f(0), fontFamily = V2_SERIF, fontSize = 20.sp, letterSpacing = 2.sp, color = Color(0xFFDBE7FF), modifier = Modifier.graphicsLayer { alpha = t })
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_vitral(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val luz = volta(3000); val forma = RoundedCornerShape(70.dp, 70.dp, 6.dp, 6.dp)
        Box(Modifier.clip(forma).background(Brush.sweepGradient(listOf(Color(0xFF8B1E3F), Color(0xFF1E4F8B), Color(0xFFC79A1E), Color(0xFF2C7A3B), Color(0xFF6B2C8B), Color(0xFF8B1E3F))))
            .drawBehind { val passo = 24.dp.toPx(); var x = -size.height; while (x < size.width + size.height) { drawLine(Color(0xD90A0A0A), Offset(x, 0f), Offset(x + size.height * .58f, size.height), 2.dp.toPx()); drawLine(Color(0xD90A0A0A), Offset(x + size.height * .58f, 0f), Offset(x, size.height), 2.dp.toPx()); x += passo } }
            .border(3.dp, Color(0xFF111111), forma).reflexo(luz, .5f).padding(horizontal = 28.dp, vertical = 22.dp)) {
            Text(c.joinToString(" · "), fontFamily = V2_SERIF, fontSize = 23.sp, color = Color(0xFFFFF7E0), textAlign = TextAlign.Center, style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 12f)))
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_brasao(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val a = ent(700); val nomeCasa = f(0); val ini = nomeCasa.replace(Regex("^(casa|clã|cla|família|familia|reino)\\s+(de\\s+)?", RegexOption.IGNORE_CASE), "").firstOrNull()?.uppercase() ?: "?"
        val escudo = GenericShape { s, _ -> moveTo(0f, 0f); lineTo(s.width, 0f); lineTo(s.width, s.height * .6f); lineTo(s.width / 2, s.height); lineTo(0f, s.height * .6f); close() }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp, 86.dp).graphicsLayer { translationY = (1 - a) * -40f * density; rotationZ = (1 - a) * -10f; alpha = a }.clip(escudo)
                .background(Brush.linearGradient(0.5f to Color(0xFF7A1A2C), 0.5f to Color(0xFF3A0A14))), contentAlignment = Alignment.Center) {
                Text(ini, fontFamily = V2_SERIF, fontSize = 38.sp, color = Color(0xFFE8C46A), modifier = Modifier.padding(bottom = 14.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column { Text(nomeCasa, fontFamily = V2_SERIF, fontSize = 21.sp, color = Color(0xFFF0E2C0)); if (f(1).isNotEmpty()) Text("“${f(1)}”", fontStyle = FontStyle.Italic, fontFamily = leitura, fontSize = 15.sp, color = Color(0xFFC9A46A)) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_livro(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val a = ent(1100, 300); val t = ent(800, 1200)
        Row(Modifier.widthIn(max = 320.dp).fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 124.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp, 0.dp, 0.dp, 6.dp)).background(Color(0xFFEFE3C8)).padding(12.dp), contentAlignment = Alignment.Center) {
                Rot2(if (c.size > 1) f(0) else "", Color(0xFF7A2A1A), 10f) }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.matchParentSize().clip(RoundedCornerShape(0.dp, 6.dp, 6.dp, 0.dp)).background(Color(0xFFEFE3C8)))
                Text(if (c.size > 1) c.drop(1).joinToString(" ") else f(0), fontFamily = leitura, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = Color(0xFF3A2A16), modifier = Modifier.padding(12.dp).graphicsLayer { alpha = t })
                Box(Modifier.matchParentSize().graphicsLayer { transformOrigin = TransformOrigin(0f, .5f); rotationY = -180f * a; alpha = if (a < .5f) 1f else 0f; cameraDistance = 12f * density }
                    .clip(RoundedCornerShape(0.dp, 6.dp, 6.dp, 0.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFD8C7A2), Color(0xFFE9DCBC)))))
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_pocao(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val r = c.firstOrNull { Regex("^(comum|incomum|raro|[ée]pico|lend|m[ií]t)", RegexOption.IGNORE_CASE).containsMatchIn(it) }
        val cor = if (r != null) raridade(r) else Color(0xFF8F6BFF); val nivel = volta(2000, true); val bol = volta(2000)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp, 86.dp).clip(RoundedCornerShape(14.dp, 14.dp, 30.dp, 30.dp)).background(Color(0x0AFFFFFF)).border(2.dp, Color(0x99C8DCFF), RoundedCornerShape(14.dp, 14.dp, 30.dp, 30.dp))) {
                Canvas(Modifier.matchParentSize()) {
                    val topo = size.height * (1 - (.62f - .06f * nivel))
                    drawRect(Brush.verticalGradient(listOf(cor, lerp(cor, Color.Black, .45f)), topo, size.height), Offset(0f, topo), Size(size.width, size.height - topo))
                    listOf(.3f, .55f, .7f, .42f).forEachIndexed { k, x -> val p = (bol + k * .25f) % 1f; drawCircle(Color.White.copy(alpha = .75f * (1 - p)), 3.dp.toPx(), Offset(size.width * x, size.height - p * (size.height - topo))) }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column { Text(f(0), fontFamily = V2_SERIF, fontSize = 20.sp, color = lerp(cor, Color.White, .6f)); Text(c.drop(1).joinToString(" · "), fontSize = 12.5.sp, color = lerp(cor, Color(0xFFCCCCCC), .5f)) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_eclosao(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val treme = volta(1400); val ab by animateFloatAsState(if (aberto) 1f else 0f, tween(700), label = "ovo")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp, 92.dp).graphicsLayer {
                rotationZ = if (aberto) -6f * ab else when { treme < .7f -> 0f; treme < .75f -> -8f; treme < .8f -> 8f; treme < .85f -> -5f; treme < .9f -> 4f; else -> 0f }
                val e = if (ab < .3f) 1f + ab / .3f * .12f else 1.12f - (ab - .3f) / .7f * .82f; scaleX = e; scaleY = e; alpha = 1 - ab }
                .clip(RoundedCornerShape(50)).background(Brush.radialGradient(listOf(Color(0xFFD8F0C8), Color(0xFF5C8F4A), Color(0xFF2C4A22)), Offset(70f, 60f), 260f))) {
                if (aberto) Canvas(Modifier.matchParentSize()) { val p = Path(); val y = size.height * .45f; p.moveTo(0f, y); for (k in 1..8) p.lineTo(size.width * k / 8f, y + if (k % 2 == 0) -8f else 8f); drawPath(p, Color(0xFF0F1A0A), style = Stroke(3.dp.toPx())) }
            }
            Text(f(0), fontFamily = V2_SERIF, fontSize = 19.sp, color = Color(0xFFCDEEBD), modifier = Modifier.graphicsLayer { alpha = ab }, style = TextStyle(shadow = Shadow(Color(0xFF8FE07A).copy(alpha = ab), blurRadius = 30f)))
            if (!aberto) Text("toque no ovo", fontSize = 11.sp, color = Color(0xFF8A8A8A), letterSpacing = 1.sp)
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_juramento(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val g = ent(800, easing = FastOutLinearInEasing); val t = ent(1000, 800)
        Box(Modifier.fillMaxWidth().height(140.dp)) {
            Canvas(Modifier.matchParentSize()) { val y = -14f + g * (size.height - 40.dp.toPx()); if (g < 1f) drawOval(Color(0xFFB10F1F), Offset(size.width / 2 - 6.dp.toPx(), y), Size(12.dp.toPx(), 16.dp.toPx()))
                else drawOval(Color(0xFFB10F1F).copy(alpha = (1 - t).coerceIn(0f, 1f)), Offset(size.width / 2 - 24.dp.toPx(), size.height - 30.dp.toPx()), Size(48.dp.toPx(), 5.dp.toPx())) }
            Text(c.joinToString(" "), fontFamily = leitura, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color(0xFFFF4D5E), textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(Color(0x99C80014), blurRadius = 24f)), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).graphicsLayer { alpha = t; scaleX = 1.08f - .08f * t; scaleY = 1.08f - .08f * t })
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_repete(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val n = numV2(f(1), 7f).toInt().coerceIn(2, 12)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            repeat(n) { k -> val a = ent(300, k * 350); val km = min(k, 6); val ult = k == n - 1
                Text(f(0), fontFamily = leitura, fontStyle = if (ult) FontStyle.Normal else FontStyle.Italic, fontWeight = if (ult) FontWeight.Bold else FontWeight.Normal, fontSize = if (ult) 21.sp else 16.sp,
                    letterSpacing = (km * .5f).sp, color = if (ult) Color(0xFFDD1111) else lerp(Color(0xFFDCDCDC), Color(0xFF8A1010), k / (n - 1f)), maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = a; translationX = km * km * .6f * density; rotationZ = -.7f * km }) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_foto_muda(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val ab by animateFloatAsState(if (aberto) .9f else 0f, tween(1400), label = "foto")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.graphicsLayer { rotationZ = -3f }.background(Color(0xFFE9E2D2)).padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 26.dp)) {
                Canvas(Modifier.size(190.dp, 110.dp)) {
                    drawRect(Color(0xFF8A8172), size = Size(size.width, size.height * .55f)); drawRect(Color(0xFF524C40), Offset(0f, size.height * .55f), Size(size.width, size.height * .45f))
                    fun pessoa(x: Float, alt: Float, cor: Color) { val w = 22.dp.toPx(); drawRoundRect(cor, Offset(x, size.height - alt), Size(w, alt), CornerRadius(w / 2)); drawCircle(cor, 8.dp.toPx(), Offset(x + w / 2, size.height - alt - 9.dp.toPx())) }
                    pessoa(42.dp.toPx(), 58.dp.toPx(), Color(0xFF2A251E)); pessoa(122.dp.toPx(), 58.dp.toPx(), Color(0xFF2A251E))
                    pessoa(80.dp.toPx(), 74.dp.toPx(), Color.Black.copy(alpha = ab))
                    if (ab > .5f) { drawCircle(Color.White.copy(alpha = ab), 1.2.dp.toPx(), Offset(88.dp.toPx(), size.height - 82.dp.toPx())); drawCircle(Color.White.copy(alpha = ab), 1.2.dp.toPx(), Offset(94.dp.toPx(), size.height - 82.dp.toPx())) }
                }
                Text(f(0), fontFamily = Caveat, fontSize = 17.sp, color = Color(0xFF3A3226), modifier = Modifier.align(Alignment.BottomStart).offset(x = 2.dp, y = 22.dp))
            }
            if (f(1).isNotEmpty()) Text(f(1), fontFamily = leitura, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = Color(0xFFDD3333), modifier = Modifier.padding(top = 10.dp).graphicsLayer { alpha = ab })
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_digital(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val scan = volta(1600, true); val pct = conta(0f, min(100f, numV2(f(1), 98f)), 2000)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(70.dp, 90.dp)) {
                for (k in 1..12) { val r = k * 3.6.dp.toPx(); drawOval(Color(0xFF5FF0C8).copy(alpha = .8f), Offset(size.width / 2 - r * .8f, size.height * .6f - r), Size(r * 1.6f, r * 2f), style = Stroke(1.3.dp.toPx())) }
                val y = scan * size.height; drawRect(Color(0xFF5FF0C8).copy(alpha = .35f), Offset(0f, y - 5.dp.toPx()), Size(size.width, 10.dp.toPx())); drawLine(Color.White, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
            }
            Spacer(Modifier.width(16.dp))
            Column { Rot2("Biometria", Color(0xFF5FF0C8), 10f); Text("Corresponde: ${f(0)}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFFEAFFF8)); Text("${pct.toInt()}%", fontFamily = V2_MONO, fontSize = 24.sp, color = Color(0xFF5FF0C8)) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_cofre(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val dig = Regex("\\d").findAll(f(0)).map { it.value.toInt() }.take(6).toList().ifEmpty { listOf(0) }
        val fim = ent(400, dig.size * 400 + 1200)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dig.forEachIndexed { k, n -> val v = conta(0f, (10 + n).toFloat(), 1400, atraso = k * 400)
                    Box(Modifier.size(38.dp, 36.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp))) {
                        Column(Modifier.offset(y = (-v * 36).dp)) { repeat(21) { d -> Box(Modifier.size(38.dp, 36.dp), contentAlignment = Alignment.Center) { Text("${d % 10}", fontFamily = V2_MONO, fontSize = 24.sp, color = Color(0xFFFFD27A)) } } }
                    }
                }
            }
            Text("ABERTO", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 4.sp, color = Color(0xFF7FD1A0), modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = fim })
            if (c.size > 1) Text(c.drop(1).joinToString(" "), fontFamily = leitura, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = Color(0xFFCCCCCC), textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = fim })
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_relogio(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val m = Regex("(\\d{1,2})[:h](\\d{2})").find(f(0))?.groupValues; val hh = (m?.get(1)?.toIntOrNull() ?: 0) % 12; val mm = m?.get(2)?.toIntOrNull() ?: 0
        val angH = hh * 30f + mm * .5f; val angM = mm * 6f
        val p = conta(0f, 1f, 1800); val trava = ent(250, 1700)
        val h = -260f + (angH + 3f + 260f) * p - 3f * trava; val mi = -1300f + (angM + 4f + 1300f) * p - 4f * trava
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(98.dp)) {
                val r = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(Color(0xFFEFE6D0), Color(0xFFCDBF9C)), center, r), r); drawCircle(Color(0xFF5A4630), r - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
                for (k in 0 until 12) rotate(k * 30f) { drawLine(Color(0xFF3A2C1C), Offset(center.x, center.y - r + 8.dp.toPx()), Offset(center.x, center.y - r + 14.dp.toPx()), 2.dp.toPx()) }
                rotate(h) { drawLine(Color(0xFF1F160D), center, Offset(center.x, center.y - 26.dp.toPx()), 4.dp.toPx(), StrokeCap.Round) }
                rotate(mi) { drawLine(Color(0xFF1F160D), center, Offset(center.x, center.y - 38.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round) }
                drawCircle(Color(0xFF8A1A1A), 4.dp.toPx())
            }
            Spacer(Modifier.width(16.dp))
            Column { Text(f(0), fontFamily = leitura, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = Color(0xFFEEEEEE)); if (f(1).isNotEmpty()) Rot2(f(1), Color(0xFFE0533F), 11f) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_recortes(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val fontes = listOf(V2_SERIF, Maquina, FontFamily.Serif, FontFamily.SansSerif, Caveat, FontFamily.Monospace)
        val fundos = listOf(Color(0xFFF2EEE2) to Color(0xFF111111), Color(0xFF111111) to Color(0xFFF2EEE2), Color(0xFFE8D24A) to Color(0xFF111111), Color(0xFFC93030) to Color.White, Color.White to Color(0xFFC93030), Color(0xFF9CC3E6) to Color(0xFF111111))
        FlowRow(Modifier.widthIn(max = 320.dp), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            c.joinToString(" ").forEachIndexed { i, ch ->
                if (ch == ' ') Spacer(Modifier.width(10.dp)) else { val a = ent(250, i * 90); val (bg, fg) = fundos[(i * 5 + 2) % 6]; val r = ((i * 37) % 13) - 6f
                    Text(ch.toString(), fontFamily = fontes[(i * 7) % 6], fontWeight = if (i % 2 == 1) FontWeight.ExtraBold else FontWeight.Normal, fontSize = 22.sp, color = fg,
                        modifier = Modifier.graphicsLayer { rotationZ = r; alpha = a; val e = 1.8f - .8f * a; scaleX = e; scaleY = e }.background(bg).padding(horizontal = 5.dp, vertical = 2.dp)) }
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_hud(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val cor = corFx(corNome, Color(0xFF7FF0FF)); val liga = ent(600, easing = LinearEasing); val pisca = volta(1000) < .5f
        val dados = c.drop(1); val esq = dados.filterIndexed { k, _ -> k % 2 == 0 }; val dir = dados.filterIndexed { k, _ -> k % 2 == 1 }
        Box(Modifier.fillMaxWidth().height(170.dp).graphicsLayer { alpha = if (liga < .5f) liga * 2 else if (liga < .75f) .3f else 1f }.cantos(cor)) {
            Canvas(Modifier.size(62.dp).align(BiasAlignment(0f, -0.1f))) { drawCircle(cor.copy(alpha = .6f), 23.dp.toPx(), style = Stroke(1.dp.toPx())); drawLine(cor, Offset(center.x, 0f), Offset(center.x, size.height), 1.dp.toPx()); drawLine(cor, Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx()) }
            Column(Modifier.align(Alignment.TopStart).padding(10.dp)) { esq.forEach { Text(it.uppercase(), fontFamily = V2_MONO, fontSize = 11.sp, color = cor) } }
            Column(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalAlignment = Alignment.End) { dir.forEach { Text(it.uppercase(), fontFamily = V2_MONO, fontSize = 11.sp, color = cor) } }
            Text(f(0).uppercase(), fontFamily = V2_MONO, fontSize = 11.sp, color = Color(0xFFFFB547), textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp).graphicsLayer { alpha = if (pisca) 1f else .3f })
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_autodestruicao(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val s = segundos(f(0).ifEmpty { "60" }); val resta = conta(s.toFloat(), 0f, s * 1000, linear = true); val sir = volta(1200, true); val faixa = volta(800)
        Box(Modifier.matchParentSize().background(lerp(Color(0xFF140303), Color(0xFF3A0505), sir)))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FaixaPerigo(faixa)
            Rot2(f(1).ifEmpty { "Autodestruição" }, Color(0xFFFF5A5A), 12f)
            Text(ms(ceil(resta).toInt()), fontFamily = V2_MONO, fontSize = 48.sp, color = Color.White, style = TextStyle(shadow = Shadow(Color.Red, blurRadius = 30f)))
            FaixaPerigo(faixa)
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_dobra(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        Box(Modifier.matchParentSize().background(Brush.radialGradient(listOf(Color(0xFF0B1A3A), Color.Black))))
        Particulas("risco", 42, Modifier.matchParentSize())
        Text(c.joinToString(" · ").uppercase(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 3.sp, color = Color(0xFFDFEAFF), textAlign = TextAlign.Center, style = TextStyle(shadow = Shadow(Color(0xFF6AA0FF), blurRadius = 24f)))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_dna(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val g = volta(2000)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(232.dp, 84.dp)) {
                for (k in 0 until 16) { val x = 4.dp.toPx() + k * (size.width - 8.dp.toPx()) / 15f; val fase = ((g + k / 16f) % 1f) * 2 * PI.toFloat(); val amp = 32.dp.toPx(); val y1 = center.y - cos(fase) * amp; val y2 = center.y + cos(fase) * amp
                    drawLine(Color.White.copy(alpha = .12f), Offset(x, y1), Offset(x, y2), 1.dp.toPx())
                    drawCircle(Color(0xFF4FD1FF), (2.2f + 1.8f * ((sin(fase) + 1) / 2)).dp.toPx(), Offset(x, y1)); drawCircle(Color(0xFFFF4FAE), (2.2f + 1.8f * ((1 - sin(fase)) / 2)).dp.toPx(), Offset(x, y2)) }
            }
            Text(buildAnnotatedString { val t = f(0); val m = Regex("[\\d.,]+%").find(t); if (m != null) { append(t.substring(0, m.range.first)); withStyle(SpanStyle(color = Color(0xFF4FD1FF), fontWeight = FontWeight.Bold)) { append(m.value) }; append(t.substring(m.range.last + 1)) } else append(t); if (f(1).isNotEmpty()) append(" · " + f(1)) },
                fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Color(0xFFCCEEEE), textAlign = TextAlign.Center)
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_olho_ia(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val liga = ent(1200); val pul = volta(3000, true); val t = ent(600, 1000)
        Row(Modifier.fillMaxWidth(0.94f), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(66.dp)) { val r = size.minDimension / 2; val luz = if (liga < .6f) liga / .6f * 1.4f else 1.4f - (liga - .6f) / .4f * .4f
                drawCircle(Color.Red.copy(alpha = (.25f + .35f * pul) * min(1f, liga)), r * 1.3f); drawCircle(Color(0xFF333333), r)
                drawCircle(Brush.radialGradient(listOf(Color.White, lerp(Color.Black, Color(0xFFFF2A2A), min(1f, luz)), Color(0xFF7A0000), Color(0xFF1A0000)), center, r * .9f), r * .9f) }
            Spacer(Modifier.width(16.dp))
            Column { Rot2(if (c.size > 1) f(0) else "IA", Color(0xFFFF6B6B), 10.5f); Text(if (c.size > 1) c.drop(1).joinToString(" ") else f(0), fontFamily = V2_MONO, fontSize = 14.5.sp, color = Color(0xFFF2DEDE), modifier = Modifier.graphicsLayer { alpha = t }) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_radar(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val g = volta(3000); val n = numV2(f(0), 3f).toInt().coerceIn(1, 8)
        val blips = remember(c) { val r = Random(c.joinToString().hashCode()); List(n) { val a = r.nextFloat() * 2 * PI.toFloat(); val d = .2f + r.nextFloat() * .25f; a to d } }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(122.dp)) { val r = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(Color(0xFF032010), Color.Black)), r); for (k in 1..3) drawCircle(Color(0x4050FF8C), r * k / 3f, style = Stroke(1f))
                drawCircle(Color(0x8050FF8C), r, style = Stroke(1.dp.toPx()))
                val ang = g * 360f - 90f
                rotate(ang + 90f) { drawCircle(Brush.sweepGradient(listOf(Color(0x8C50FF8C), Color.Transparent, Color.Transparent, Color.Transparent), center), r) }
                blips.forEach { (a, d) -> val grau = ((a * 180f / PI.toFloat()) + 360f) % 360f; val atras = ((ang - grau) % 360f + 360f) % 360f; val al = (1f - atras / 216f).coerceIn(0f, 1f)
                    drawCircle(Color(0xFF6FFF9F).copy(alpha = al), 3.5.dp.toPx(), center + Offset(cos(a) * d * size.width, sin(a) * d * size.width)) }
            }
            Text(c.joinToString(" · ").uppercase(), fontFamily = V2_MONO, fontSize = 12.sp, color = Color(0xFF6FFF9F), modifier = Modifier.padding(top = 6.dp))
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_video(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val s = segundos(f(1)); val agora = conta(s.toFloat(), (s + 3600).toFloat(), 3_600_000, linear = true); val onda = volta(1600)
        Box(Modifier.size(140.dp, 176.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF2B3A55), Color(0xFF11161F)))).border(2.dp, Color(0xFF333333), RoundedCornerShape(18.dp))) {
            Column(Modifier.align(Alignment.TopCenter).padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).drawBehind { drawCircle(Color(0xFF8A5CF6).copy(alpha = .6f * (1 - onda)), size.minDimension / 2 + 16.dp.toPx() * onda) }.clip(CircleShape).background(Color(0xFF8A5CF6)), contentAlignment = Alignment.Center) {
                    Text(f(0).firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White) }
                Text(f(0), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp)); Text(ms(agora.toInt()), fontSize = 11.sp, color = Color(0xFFBBCCDD))
            }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp, 42.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF3D4A5C)))
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf(Color(0xFF2C3646), Color(0xFFE0393E), Color(0xFF2C3646)).forEach { Box(Modifier.size(22.dp).clip(CircleShape).background(it)) } }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_live(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val pisca = volta(1200) < .5f
        Box(Modifier.size(170.dp, 190.dp).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(Color(0xFF3A2540), Color(0xFF120A14)))).border(2.dp, Color(0xFF333333), RoundedCornerShape(16.dp))) {
            Particulas("coracao", 10, Modifier.matchParentSize())
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                Text("AO VIVO", fontWeight = FontWeight.ExtraBold, fontSize = 9.5.sp, color = Color.White, modifier = Modifier.graphicsLayer { alpha = if (pisca) 1f else .4f }.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF2D55)).padding(horizontal = 6.dp, vertical = 2.dp))
                Spacer(Modifier.weight(1f)); Text(f(1), fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Text(f(0), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(start = 10.dp, top = 32.dp))
            Column(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 8.dp, end = 26.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                c.drop(2).forEachIndexed { k, x -> val a = ent(400, 200 + k * 1000); val d = x.indexOf(':')
                    Text(buildAnnotatedString { if (d > 0) { withStyle(SpanStyle(color = Color(0xFFFFB3C7), fontWeight = FontWeight.Bold)) { append(x.substring(0, d) + " ") }; append(x.substring(d + 1).trim()) } else append(x) },
                        fontSize = 10.5.sp, lineHeight = 13.sp, color = Color.White, modifier = Modifier.graphicsLayer { alpha = a; translationY = (1 - a) * 10f * density }) }
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_pix(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val a = ent(500); val v = conta(0f, numV2(f(0), 100f), 1400)
        Row(Modifier.fillMaxWidth(0.94f).graphicsLayer { alpha = a; translationY = (1 - a) * -30f * density }.clip(RoundedCornerShape(16.dp)).background(Color(0xF228282C)).border(1.dp, Color(0xFF3A3A40), RoundedCornerShape(16.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF16B39A)), contentAlignment = Alignment.Center) { Text("$", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column { Text("BANCO · agora", fontSize = 10.sp, color = Color(0xFF999999), fontWeight = FontWeight.SemiBold); Text("Pix recebido", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Text(buildAnnotatedString { withStyle(SpanStyle(color = Color(0xFF4FE0BF))) { append(reais(v)) }; if (f(1).isNotEmpty()) { append(" de "); withStyle(SpanStyle(color = Color.White)) { append(f(1)) } } }, fontSize = 13.sp, color = Color(0xFFDDDDDD)) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_visto(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val a = ent(300); val vi = ent(300, 1000); val t = ent(4000, 1800, LinearEasing); val pul = volta(1000)
        val dots = when { t <= 0f -> 0f; t < .1f -> t / .1f; t < .6f -> 1f; t < .75f -> 1 - (t - .6f) / .15f; else -> 0f }
        Column(Modifier.fillMaxWidth(0.9f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(f(0), color = Color.White, fontSize = 13.5.sp, modifier = Modifier.graphicsLayer { alpha = a; translationY = (1 - a) * 10f * density }.clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)).background(Color(0xFF3A8BFF)).padding(horizontal = 12.dp, vertical = 8.dp))
            Text("Visto" + if (f(1).isNotEmpty()) " às ${f(1)}" else "", fontSize = 10.5.sp, color = Color(0xFF8A8A8A), modifier = Modifier.graphicsLayer { alpha = vi })
            Row(Modifier.align(Alignment.Start).graphicsLayer { alpha = dots }.clip(RoundedCornerShape(16.dp)).background(Color(0xFF262626)).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { k -> val y = sin(((pul + k * .15f) % 1f) * 2 * PI.toFloat()); Box(Modifier.size(6.dp).graphicsLayer { translationY = -3f * density * max(0f, y) }.clip(CircleShape).background(Color(0xFFAAAAAA))) } }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_forum(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val votos = numV2(f(2), 100f); val v = conta(votos / 20, votos, 2000)
        Row(Modifier.fillMaxWidth(0.94f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF141414)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp)).padding(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("▲", color = Color(0xFFFF6A2B), fontSize = 12.sp); Text(milharV2(v.toInt()), color = Color(0xFFFF6A2B), fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("▼", color = Color(0xFF666666), fontSize = 12.sp) }
            Spacer(Modifier.width(12.dp))
            Column { Text("${f(0)} · há 2 h", fontSize = 10.5.sp, color = Color(0xFF8A8A8A)); Text(f(1), fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 3.dp))
                Text("${numV2(f(3), 0f).toInt()} comentários" + if (f(4).isNotEmpty()) " · ${f(4)}" else "", fontSize = 11.sp, color = Color(0xFF999999)) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_traduz(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val t = volta(3600)
        val a = when { t < .35f -> 1f; t < .5f -> 1 - (t - .35f) / .15f; t < .9f -> 0f; else -> (t - .9f) / .1f }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Rot2("Traduzido" + if (f(2).isNotEmpty()) " do ${f(2)}" else "", Color(0xFF8FB0E0), 10f)
            Box(Modifier.padding(top = 6.dp), contentAlignment = Alignment.Center) {
                Text(f(0), fontFamily = leitura, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, color = Color(0xFFF2F2F2), modifier = Modifier.graphicsLayer { alpha = a })
                Text(f(1), fontFamily = leitura, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, color = Color(0xFFF2F2F2), textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = 1 - a })
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_alarme(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val v = volta(160, true)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(f(0).ifEmpty { "06:00" }, fontWeight = FontWeight.Light, fontSize = 56.sp, color = Color.White, modifier = Modifier.graphicsLayer { translationX = (v - .5f) * 3f * density; rotationZ = (v - .5f) * 1.2f })
            Text(f(1).ifEmpty { "Alarme" }, fontSize = 13.5.sp, color = Color(0xFFBBBBBB), modifier = Modifier.padding(bottom = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Adiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEEEEEE), modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF262626)).padding(horizontal = 16.dp, vertical = 6.dp))
                Text("Parar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFF9F0A)).padding(horizontal = 16.dp, vertical = 6.dp))
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_batimento(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val m = Regex("(\\d+)\\s*>\\s*(\\d+)").find(f(0))?.groupValues; val de = m?.get(1)?.toFloatOrNull() ?: 72f; val ate = m?.get(2)?.toFloatOrNull() ?: numV2(f(0), 120f)
        val bpm = conta(de, ate, 2600); val lento = volta(800); val rapido = volta(340); val acelerou = ent(10, 2600) >= 1f
        val p = if (acelerou) rapido else lento
        val e = when { p < .15f -> 1 + .18f * p / .15f; p < .3f -> 1.18f - .18f * (p - .15f) / .15f; p < .45f -> 1 + .1f * (p - .3f) / .15f; p < .6f -> 1.1f - .1f * (p - .45f) / .15f; else -> 1f }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("♥", fontSize = 62.sp, color = Color(0xFFFF3D6E), style = TextStyle(shadow = Shadow(Color(0x99FF3D6E), blurRadius = 40f)), modifier = Modifier.graphicsLayer { scaleX = e; scaleY = e })
            Spacer(Modifier.width(18.dp))
            Column { Row(verticalAlignment = Alignment.Bottom) { Text("${bpm.toInt()}", fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color.White); Text(" bpm", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFFFF8FAB), modifier = Modifier.padding(bottom = 8.dp)) }
                if (c.size > 1) Text(c.drop(1).joinToString(" "), fontFamily = leitura, fontStyle = FontStyle.Italic, fontSize = 16.sp, color = Color(0xFFF3CFD8)) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_fio(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val a = ent(1800, 300)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(f(0), fontFamily = leitura, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = Color(0xFFF5D6DC))
            Canvas(Modifier.weight(1f, fill = false).widthIn(max = 180.dp).fillMaxWidth().height(60.dp).padding(horizontal = 6.dp)) {
                val w = size.width; val h = size.height
                val caminho = Path().apply { moveTo(0f, h / 2); cubicTo(w * .25f, -h * .2f, w * .37f, h * 1.2f, w * .5f, h / 2); cubicTo(w * .63f, -h * .1f, w * .8f, h * .05f, w, h / 2) }
                val pm = PathMeasure(); pm.setPath(caminho, false); val parte = Path(); pm.getSegment(0f, pm.length * a, parte, true)
                drawPath(parte, Color(0x66E0263A), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)); drawPath(parte, Color(0xFFE0263A), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
            Text(f(1), fontFamily = leitura, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = Color(0xFFF5D6DC))
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_bilhete(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val ab by animateFloatAsState(if (aberto) 1f else 0f, tween(900, easing = FastOutSlowInEasing), label = "bil")
        val (capa, txt) = if (c.size > 1) f(0) to c.drop(1).joinToString(" ") else "toque para abrir" to f(0)
        Box(Modifier.widthIn(max = 260.dp).fillMaxWidth().heightIn(min = 112.dp)) {
            Text(txt, fontFamily = Caveat, fontSize = 21.sp, lineHeight = 24.sp, color = Color(0xFF2B3A8A),
                modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFBF6E8))
                    .drawBehind { var y = 36.dp.toPx(); while (y < size.height) { drawLine(Color(0xFFCFD8EF), Offset(0f, y), Offset(size.width, y), 1f); y += 24.sp.toPx() } }.padding(horizontal = 14.dp, vertical = 12.dp))
            Box(Modifier.matchParentSize().graphicsLayer { transformOrigin = TransformOrigin(.5f, 0f); rotationX = -180f * ab; alpha = if (ab < .5f) 1f else 0f; cameraDistance = 12f * density }
                .clip(RoundedCornerShape(4.dp)).background(Color(0xFFF1E9D2)), contentAlignment = Alignment.Center) { Text(capa, fontFamily = Caveat, fontSize = 23.sp, color = Color(0xFF7A2A2A)) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_polaroid(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val a = ent(3200)
        Box(Modifier.graphicsLayer { rotationZ = 2.5f }.background(Color(0xFFF4F1EA)).padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 30.dp)) {
            Box(Modifier.size(170.dp, 112.dp)) {
                Canvas(Modifier.matchParentSize()) {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFFF7B267), Color(0xFFF79D65)), 0f, size.height * .45f), size = Size(size.width, size.height * .45f))
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF2A6F97), Color(0xFF1B4965)), size.height * .45f, size.height), Offset(0f, size.height * .45f), Size(size.width, size.height * .55f))
                    drawCircle(Color(0xFFFFE8A3), 13.dp.toPx(), Offset(size.width * .62f, size.height * .26f))
                    listOf(58f to 46f, 78f to 40f).forEach { (x, al) -> val w = 16.dp.toPx(); drawRoundRect(Color(0xFF1B1B26), Offset(x.dp.toPx(), size.height - al.dp.toPx()), Size(w, al.dp.toPx()), CornerRadius(w / 2)); drawCircle(Color(0xFF1B1B26), 6.dp.toPx(), Offset(x.dp.toPx() + w / 2, size.height - al.dp.toPx() - 7.dp.toPx())) }
                }
                Box(Modifier.matchParentSize().background(Color(0xFF2A2010).copy(alpha = (1 - a) * .97f)))
            }
            Text(f(0), fontFamily = Caveat, fontSize = 19.sp, color = Color(0xFF333333), textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).offset(y = 26.dp))
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_tempo_parado(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val onda = ent(1400)
        Box(Modifier.matchParentSize().background(Color(0xFF101418)))
        Particulas("poeira", 26, Modifier.matchParentSize())
        Canvas(Modifier.matchParentSize()) { if (onda < 1f) drawCircle(Color.White.copy(alpha = .6f * (1 - onda)), 15.dp.toPx() * (1 + onda * 9), style = Stroke(1.dp.toPx())) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(f(0), fontFamily = leitura, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, color = Color(0xFFE9EEF2), textAlign = TextAlign.Center)
            c.drop(1).forEach { Text(it, fontFamily = leitura, fontSize = 15.sp, color = Color(0xFF99AAAA), textAlign = TextAlign.Center) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_abertura(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val a = ent(1200); val t = ent(1000, 600)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(f(0), fontFamily = leitura, fontWeight = FontWeight.Bold, fontSize = 92.sp, color = Color(0xFFFF6A2B), style = TextStyle(drawStyle = Stroke(4f)),
                modifier = Modifier.graphicsLayer { alpha = a; translationY = (1 - a) * 20f * density })
            Spacer(Modifier.width(16.dp)); Box(Modifier.width(1.dp).height(60.dp).background(Color(0xFF333333))); Spacer(Modifier.width(16.dp))
            Column { Rot2("Capítulo", Color(0xFFFF6A2B), 10f); Text(f(1), fontFamily = leitura, fontStyle = FontStyle.Italic, fontSize = 25.sp, color = Color(0xFFF4F4F4), modifier = Modifier.revelar(t)) }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_arco(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        Particulas("brasa", 18, Modifier.matchParentSize())
        val a = ent(1600); val l = ent(1200, 600)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Rot2("Arco ${f(0)}", Color(0xFFFF8A4A), 11f)
            Text(f(1), fontFamily = V2_SERIF, fontSize = 32.sp, letterSpacing = (2f + 14f * (1 - a)).sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = a })
            Box(Modifier.padding(top = 10.dp).width(190.dp * l).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFFF8A4A), Color.Transparent))))
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_dias(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val m = Regex("(\\d+)\\s*>\\s*(\\d+)").find(f(0))?.groupValues; val de = m?.get(1)?.toFloatOrNull() ?: 1f; val ate = m?.get(2)?.toFloatOrNull() ?: numV2(f(0), 100f)
        val v = conta(de, ate, 2500); val larg = max(3, ate.toInt().toString().length)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Rot2(f(1).ifEmpty { "dias" }, Color(0xFFAAAAAA), 10f)
            Text(v.toInt().toString().padStart(larg, '0'), fontFamily = V2_MONO, fontSize = 60.sp, letterSpacing = 4.sp, color = Color(0xFFFF6A2B), style = TextStyle(shadow = Shadow(Color(0x59FF6A2B), blurRadius = 40f)))
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.fxV2_coord(corNome: String?, c: List<String>, estilo: TextStyle, aberto: Boolean) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    val leitura = estilo.fontFamily ?: FontFamily.Serif
        val final = f(0); var mostrado by remember { mutableStateOf(final) }; val t = ent(500, 1300); val sem = parado()
        LaunchedEffect(final) { if (sem) return@LaunchedEffect; val ini = System.currentTimeMillis(); val r = Random(final.hashCode())
            while (true) { val p = (System.currentTimeMillis() - ini) / 1200f; if (p >= 1f) { mostrado = final; break }
                mostrado = final.mapIndexed { i, ch -> if (ch.isDigit() && i / final.length.toFloat() > p * .9f) ('0' + r.nextInt(10)) else ch }.joinToString(""); delay(50) } }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(mostrado, fontFamily = V2_MONO, fontSize = 18.sp, letterSpacing = 1.sp, color = Color(0xFF7FD1C2), textAlign = TextAlign.Center)
            if (c.size > 1) Text(c.drop(1).joinToString(" · "), fontFamily = leitura, fontStyle = FontStyle.Italic, fontSize = 16.sp, color = Color(0xFFEEEEEE), modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = t })
        }
}

// ───────── trechos {$nome:cor: texto} ─────────
/** Nomes guardados com o $ na frente, para não confundir com os antigos. */
val INLINE_V2 = setOf("\$corte", "\$corar", "\$aura", "\$cristal", "\$assinatura")

fun estiloInlineV2(nome: String, cc: Color?): SpanStyle? = when (nome) {
    "\$corte" -> SpanStyle(fontWeight = FontWeight.Bold)
    "\$corar" -> SpanStyle()
    "\$aura" -> { val c = cc ?: Color(0xFFA47CFF); SpanStyle(color = lerp(c, Color.White, .75f), shadow = Shadow(c, blurRadius = 18f)) }
    "\$cristal" -> SpanStyle(brush = Brush.linearGradient(listOf(Color(0xFFBFE9FF), Color.White, Color(0xFF7FC8FF), Color(0xFFE9F7FF), Color(0xFF9AD7FF)), tileMode = TileMode.Mirror), fontWeight = FontWeight.SemiBold)
    "\$assinatura" -> SpanStyle(fontFamily = Caveat, color = cc ?: Color(0xFF9EC1FF), fontSize = TextUnit(1.45f, TextUnitType.Em))
    else -> null
}

/** Quadro a quadro (110 ms por quadro). */
fun animarInlineV2(t: AnnotatedString, quadro: Int): AnnotatedString {
    val fx = t.getStringAnnotations("fx", 0, t.length).filter { it.item in INLINE_V2 }
    if (fx.isEmpty()) return t
    return buildAnnotatedString {
        append(t)
        fx.forEach { a ->
            val n = a.end - a.start
            when (a.item) {
                "\$corte" -> { val fase = quadro % 24 / 24f
                    if (fase in .18f..0.8f) { val meio = a.start + n / 2; addStyle(SpanStyle(baselineShift = BaselineShift(.12f)), a.start, meio); addStyle(SpanStyle(baselineShift = BaselineShift(-.05f)), meio, a.end)
                        addStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), a.start, a.end) } }
                "\$corar" -> { val v = (sin(quadro / 27f * 2 * PI) * .5 + .5).toFloat()
                    addStyle(SpanStyle(color = lerp(Color(0xFFE8E8E8), Color(0xFFFFB3C4), v), shadow = Shadow(Color(0xFFFF7896).copy(alpha = .8f * v), blurRadius = 16f * v)), a.start, a.end) }
                "\$aura" -> { val v = (sin(quadro / 18f * 2 * PI) * .5 + .5).toFloat(); val base = t.spanStyles.firstOrNull { it.start <= a.start && it.end >= a.end && it.item.shadow != null }?.item?.shadow?.color ?: Color(0xFFA47CFF)
                    addStyle(SpanStyle(shadow = Shadow(base, blurRadius = 14f + 26f * v)), a.start, a.end) }
                "\$cristal" -> { val x = (quadro % 27) / 27f * 600f
                    addStyle(SpanStyle(brush = Brush.linearGradient(listOf(Color(0xFFBFE9FF), Color.White, Color(0xFF7FC8FF), Color(0xFFE9F7FF), Color(0xFF9AD7FF)), Offset(x, 0f), Offset(x + 200f, 60f), TileMode.Mirror)), a.start, a.end) }
                "\$assinatura" -> { val ciclo = quadro % 50; val vistos = (ciclo * n / 24f).toInt()
                    if (vistos < n) addStyle(SpanStyle(color = Color.Transparent), a.start + vistos, a.end) }
            }
        }
    }
}

/** Catálogo dos 2.0 (o painel e a cola leem daqui). */
val MEGA_PACK_V2 = listOf(
    Triple("Janela do Sistema 2.0", "[\$sistema MISSÃO DE EMERGÊNCIA | Sobreviva até o amanhecer | Recompensa: ???]", "rpg"),
    Triple("Pilha de avisos", "[\$avisos Você derrotou o Lobo das Cinzas | +300 XP | Novo título disponível]", "rpg"),
    Triple("Recompensa girando", "[\$recompensa lendário | Espada do Vazio | +40 ATK]", "rpg"),
    Triple("Despertar", "[\$despertar S | Classe: Necromante]", "rpg"),
    Triple("Penalidade", "[\$penalidade Missão diária não cumprida | 00:00:10]", "rpg"),
    Triple("Patrocínio de constelação", "[\$patrocinio O Rei sem Nome | 1000]", "rpg"),
    Triple("Portal aberto", "[\$portal A | Gangnam]", "rpg"),
    Triple("Você morreu", "[\$morte Você morreu | 3]", "rpg"),
    Triple("Recarga de habilidade", "[\$recarga Corte Fantasma | 12s]", "rpg"),
    Triple("Fusão de habilidades", "[\$fusao Chama + Vento = Tornado de Fogo]", "rpg"),
    Triple("Carregando do Sistema", "[\$carregando Sincronizando com o usuário | 100]", "rpg"),
    Triple("Pincel de caligrafia", "[\$pincel 天魔神功 | Arte Divina do Demônio Celestial]", "murim"),
    Triple("Placa de jade", "[\$jade Ordem do Líder | Monte Hua | Quem portar esta placa fala pela seita.]", "murim"),
    Triple("Flores de ameixa", "[\$ameixa Vinte e Quatro Ameixas Florescentes | 7ª forma]", "murim"),
    Triple("Os Dez Grandes", "[\$dez Os Dez Grandes Mestres | Espada Celestial | Demônio Sangrento | Punho de Ferro do Norte]", "murim"),
    Triple("Constelação", "[\$constelacao Órion Caído]", "fantasia"),
    Triple("Vitral", "[\$vitral A Luz Não Perdoa]", "fantasia"),
    Triple("Brasão de casa", "[\$brasao Casa Valerian | Ferro e Cinza]", "fantasia"),
    Triple("Livro que se abre", "[\$livro Capítulo Perdido | No princípio, a torre não tinha portas.]", "fantasia"),
    Triple("Poção", "[\$pocao Elixir da Lua | Cura 50% | raro]", "fantasia"),
    Triple("Ovo chocando (toque)", "[\$eclosao Filhote de Wyvern]", "fantasia"),
    Triple("Juramento de sangue", "[\$juramento Pelo meu sangue, eu juro.]", "fantasia"),
    Triple("Frase que se repete", "[\$repete eu não estou sozinho | 7]", "terror"),
    Triple("Foto que muda (toque)", "[\$foto-muda Acampamento, 1987]", "terror"),
    Triple("Digital", "[\$digital Han Seo-jun | 98%]", "misterio"),
    Triple("Cofre", "[\$cofre 4-1-7 | Dentro: uma carta e uma chave.]", "misterio"),
    Triple("Relógio parado", "[\$relogio 03:14 | Hora da morte]", "misterio"),
    Triple("Bilhete de recortes", "[\$recortes VOCÊ É O PRÓXIMO]", "misterio"),
    Triple("HUD do capacete", "[\$hud Alerta: oxigênio baixo | O₂ 34% | ALT 880 m | PRESSÃO OK | TEMP -41°]", "scifi"),
    Triple("Autodestruição", "[\$autodestruicao 00:59]", "scifi"),
    Triple("Salto no espaço", "[\$dobra Saltando para Kepler-442]", "scifi"),
    Triple("Exame de DNA", "[\$dna Compatível 99,7% | Humano?]", "scifi"),
    Triple("Olho da IA", "[\$olho-ia MÃE | Bom dia, tripulação. Faltam 3 de vocês.]", "scifi"),
    Triple("Radar", "[\$radar 3 contatos | 400 m]", "scifi"),
    Triple("Chamada de vídeo", "[\$video Hana | 03:12]", "celular"),
    Triple("Live", "[\$live yuna.k | 12,4 mil | jae_03: ela tá chorando?? | min.s: alguém liga pra polícia]", "celular"),
    Triple("Pix recebido", "[\$pix 5000 | Desconhecido]", "celular"),
    Triple("Visto e sem resposta", "[\$visto você tá bem? me responde | 03:14]", "celular"),
    Triple("Post de fórum", "[\$forum torre_norte | Alguém mais viu a luz no 13º andar? | 342 | 87]", "celular"),
    Triple("Tradutor", "[\$traduz 살려줘 | me ajude | coreano]", "celular"),
    Triple("Despertador", "[\$alarme 06:00 | Primeiro dia na Academia]", "celular"),
    Triple("Coração acelerado", "[\$batimento 72 > 140 | Ele sorriu.]", "romance"),
    Triple("Fio vermelho", "[\$fio Hana | Seo-jun]", "romance"),
    Triple("Bilhete dobrado (toque)", "[\$bilhete para: você | Me espera depois da aula.]", "romance"),
    Triple("Polaroid", "[\$polaroid Nosso último verão]", "romance"),
    Triple("Tempo parado", "[\$tempo-parado Tudo parou. | Nem a chuva caía.]", "acao"),
    Triple("Abertura 2.0", "[\$abertura 12 | O Rei sem Nome]", "estrutura"),
    Triple("Começo de arco", "[\$arco II | A Torre Caída]", "estrutura"),
    Triple("Contador de dias", "[\$dias 1 > 100 | Dias desde o apocalipse]", "estrutura"),
    Triple("Coordenadas", "[\$coord 37.5665° N · 126.9780° E | Seul]", "estrutura"),
    Triple("Palavra cortada", "{\$corte: acabou}", "inline"),
    Triple("Corando", "{\$corar: texto}", "inline"),
    Triple("Aura", "{\$aura:roxo: texto}", "inline"),
    Triple("Cristal", "{\$cristal: texto}", "inline"),
    Triple("Assinatura", "{\$assinatura: Nome}", "inline"),
)
