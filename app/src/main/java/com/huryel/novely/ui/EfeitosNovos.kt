package com.huryel.novely.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.R
import com.huryel.novely.ui.theme.InstrumentSerif
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ───────── Efeitos novos (pacote 2): blocos [#nome campo | campo] ─────────

val BLOCOS_NOVOS = setOf(
    "levelup", "loja", "status", "drop", "ameaca", "pergaminho", "vozes", "ecg", "lacre", "grimorio", "circulo", "bencao", "maldicao", "taro",
    "mira", "notificacao", "perdidas", "story", "bateria", "musica", "radio", "datilografado", "fimarco", "epigrafe", "enquanto", "hora", "lembranca",
)
val APELIDOS_NOVOS = mapOf("level-up" to "levelup", "ameaça" to "ameaca", "bênção" to "bencao", "benção" to "bencao", "maldição" to "maldicao", "tarô" to "taro", "tarot" to "taro",
    "notificação" to "notificacao", "rádio" to "radio", "fim-de-arco" to "fimarco", "epígrafe" to "epigrafe", "lembrança" to "lembranca", "relogio" to "hora", "relógio" to "hora")

val Caveat = FontFamily(Font(R.font.caveat))
val Maquina = FontFamily(Font(R.font.special_elite))
private val MONO2 = FontFamily.Monospace

@Composable
private fun semMov() = LocalSemMovimento.current

/** Valor 0..1 que vai e volta (ou fica no meio com "Reduzir animações"). */
@Composable
private fun ciclo(ms: Int, reverso: Boolean = false): Float {
    if (semMov()) return 0.5f
    val t = rememberInfiniteTransition(label = "c")
    return t.animateFloat(0f, 1f, infiniteRepeatable(tween(ms, easing = LinearEasing), if (reverso) RepeatMode.Reverse else RepeatMode.Restart), label = "v").value
}

/** Aparece uma vez (0 → 1). */
@Composable
private fun entrada(ms: Int = 700, atraso: Int = 0): Float {
    if (semMov()) return 1f
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(atraso.toLong()); a.animateTo(1f, tween(ms, easing = FastOutSlowInEasing)) }
    return a.value
}

@Composable private fun RotM(t: String, cor: Color, tam: Float = 10f) =
    Text(t.uppercase(), fontFamily = MONO2, fontWeight = FontWeight.Bold, fontSize = tam.sp, letterSpacing = 2.sp, color = cor)

private fun numero(s: String, padrao: Int = 0) = Regex("-?\\d+").find(s)?.value?.toIntOrNull() ?: padrao

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlocoNovo(nome: String, corNome: String?, c: List<String>, estilo: TextStyle) {
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    when (nome) {
        // ───── RPG ─────
        "levelup" -> {
            val a = entrada(900); val brilho = ciclo(1600, true)
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                RotM("Level up", Color(0xFFFFC94A), 11f)
                Text("NV. ${numero(f(0), 1)}", fontWeight = FontWeight.Black, fontSize = 46.sp, color = Color(0xFFFFF3C4),
                    style = TextStyle(shadow = Shadow(Color(0xFFFFC94A).copy(alpha = 0.4f + brilho * 0.5f), blurRadius = 30f)),
                    modifier = Modifier.graphicsLayer { val e = if (a < 0.6f) a / 0.6f * 1.15f else 1.15f - (a - 0.6f) / 0.4f * 0.15f; scaleX = e; scaleY = e; alpha = a.coerceIn(0f, 1f) })
                if (c.size > 1) Text(c.drop(1).joinToString("  ·  ").uppercase(), fontFamily = MONO2, fontSize = 11.sp, letterSpacing = 1.5.sp, color = Color(0xFFFFC94A),
                    textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = ((a - 0.5f) * 2).coerceIn(0f, 1f) })
            }
        }
        "loja" -> {
            val azul = corFx(corNome, Color(0xFF2F6FD6))
            val saldo = if (Regex("^\\d[\\d.]*\\s*(pts|pontos)?$", RegexOption.IGNORE_CASE).matches(f(0))) f(0) else null
            val itens = if (saldo != null) c.drop(1) else c.drop(if (c.size > 1 && numero(f(0), -1) < 0) 1 else 0)
            val titulo = if (saldo == null && c.size > 1 && numero(f(0), -1) < 0) f(0) else "Loja do Sistema"
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, azul).background(azul.copy(alpha = 0.12f))) {
                Row(Modifier.fillMaxWidth().background(azul).padding(horizontal = 12.dp, vertical = 7.dp)) {
                    Text(titulo.uppercase(), fontFamily = MONO2, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.5.sp, color = Color.White, modifier = Modifier.weight(1f))
                    if (saldo != null) Text(saldo.uppercase().let { if (it.any { ch -> ch.isLetter() }) it else "$it PTS" }, fontFamily = MONO2, fontSize = 11.sp, color = Color.White)
                }
                itens.forEachIndexed { k, item ->
                    val preco = Regex("(\\d[\\d.]*)\\s*(pts|g|moedas)?$", RegexOption.IGNORE_CASE).find(item)
                    val a = entrada(400, 120 * k)
                    Row(Modifier.fillMaxWidth().graphicsLayer { alpha = a; translationX = (1 - a) * 30f }.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Text(if (preco != null) item.substring(0, preco.range.first).trim() else item, color = Color(0xFFE6ECF8), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (preco != null) Text(preco.groupValues[1], color = Color(0xFFFFC94A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    if (k < itens.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(azul.copy(alpha = 0.35f)))
                }
            }
        }
        "status" -> {
            val pisca = ciclo(1200, true)
            FlowRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.forEach { s ->
                    val ruim = s.startsWith("-") || s.startsWith("−") || s.startsWith("▼")
                    val cor = if (ruim) Color(0xFFE05A8A) else Color(0xFF7DDF6A)
                    val txt = s.trimStart('+', '-', '−', '▲', '▼', ' ')
                    Text((if (ruim) "▼ " else "▲ ") + txt, color = cor, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.graphicsLayer { alpha = if (ruim) 0.55f + pisca * 0.45f else 1f }.border(1.dp, cor.copy(alpha = 0.5f)).background(cor.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }
        "drop" -> {
            val r = f(0).lowercase()
            val cor = when {
                r.startsWith("lend") -> Color(0xFFFFB03A); r.startsWith("mít") || r.startsWith("mit") -> Color(0xFFFF4A4A)
                r.startsWith("ép") || r.startsWith("ep") -> Color(0xFFB57BFF); r.startsWith("rar") -> Color(0xFF5AB0FF); else -> Color(0xFFBBBBBB)
            }
            val v = ciclo(2000)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, cor).drawBehind {
                val x = size.width * (v * 2f - 0.5f)
                drawRect(Brush.horizontalGradient(listOf(Color.Transparent, cor.copy(alpha = 0.28f), Color.Transparent), startX = x - 120f, endX = x + 120f))
                drawRect(cor.copy(alpha = 0.06f))
            }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = cor, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
                Column {
                    Text("ITEM ${f(0).uppercase()} OBTIDO", fontFamily = MONO2, fontSize = 10.sp, letterSpacing = 1.5.sp, color = cor, fontWeight = FontWeight.Bold)
                    Text(f(1).ifBlank { "???" }, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    c.drop(2).forEach { Text(it, color = Color(0xFFBBBBBB), fontSize = 12.sp) }
                }
            }
        }
        "ameaca" -> {
            val m = Regex("(\\d+)\\s*/\\s*(\\d+)").find(c.joinToString(" "))
            val n = m?.groupValues?.get(1)?.toIntOrNull() ?: 5; val max = (m?.groupValues?.get(2)?.toIntOrNull() ?: 10).coerceIn(3, 20)
            val pisca = ciclo(600, true)
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row { Text(f(0).uppercase(), fontFamily = MONO2, fontSize = 11.sp, letterSpacing = 1.5.sp, color = Color(0xFFCCCCCC), modifier = Modifier.weight(1f))
                    Text("AMEAÇA $n/$max", fontFamily = MONO2, fontSize = 11.sp, letterSpacing = 1.5.sp, color = Color(0xFFE5553B)) }
                Row(Modifier.padding(top = 6.dp).fillMaxWidth().height(16.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(max) { k ->
                        val cor = when { k < n - 1 -> Color(0xFFE5553B); k == n - 1 -> Color(0xFFFF2A2A).copy(alpha = if (n >= max - 1) 0.4f + pisca * 0.6f else 1f); else -> Color(0xFF2A2A2A) }
                        Box(Modifier.weight(1f).fillMaxHeight().background(cor))
                    }
                }
                c.drop(1).filter { !Regex("^\\d+\\s*/\\s*\\d+$").matches(it) }.forEach { Text(it, color = Color(0xFFAAAAAA), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
        // ───── Murim ─────
        "pergaminho" -> {
            val a = entrada(1400)
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 340.dp).fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF8A5A2B)))
                    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp).heightIn(max = (8 + 300 * a).dp).background(Brush.verticalGradient(listOf(Color(0xFFE9DCC0), Color(0xFFD8C69F))))
                        .padding(horizontal = 16.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(f(0), fontFamily = InstrumentSerif, fontSize = 24.sp, color = Color(0xFF7A1F14), textAlign = TextAlign.Center)
                        c.drop(1).forEach { Text(it, fontFamily = InstrumentSerif, fontSize = 16.sp, color = Color(0xFF3A2A14), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)) }
                    }
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF8A5A2B)))
                }
            }
        }
        // ───── Terror ─────
        "vozes" -> {
            val v = ciclo(4000)
            Box(Modifier.fillMaxWidth().height((90 + 22 * c.size.coerceAtMost(6)).dp).padding(vertical = 6.dp)) {
                c.take(8).forEachIndexed { k, t ->
                    val fase = ((v + k * 0.27f) % 1f)
                    val alfa = sin(fase * PI).toFloat()
                    val x = listOf(0.04f, 0.55f, 0.18f, 0.62f, 0.08f, 0.45f, 0.3f, 0.7f)[k]; val y = (k.toFloat() / c.size.coerceAtLeast(1))
                    Text(t, fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic, fontSize = 19.sp, color = Color(0xFFEDE6DA).copy(alpha = 0.15f + alfa * 0.55f),
                        modifier = Modifier.align(androidx.compose.ui.BiasAlignment(x * 2 - 1, y * 2 - 0.9f)).graphicsLayer { translationY = (1 - alfa) * 10f })
                }
            }
        }
        "ecg" -> {
            val bpm = numero(f(0), 80)
            val v = ciclo(if (bpm <= 0) 3000 else (60000 / bpm.coerceIn(30, 220)) * 2)
            val cor = if (bpm <= 0) Color(0xFFFF4A4A) else Color(0xFF4FE08A)
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.fillMaxWidth().height(60.dp)) {
                    val w = size.width; val h = size.height; val y0 = h / 2
                    val p = Path(); p.moveTo(0f, y0)
                    if (bpm <= 0) p.lineTo(w, y0) else {
                        var x = 0f; val passo = w / 2f
                        while (x < w) {
                            p.lineTo(x + passo * 0.35f, y0); p.lineTo(x + passo * 0.42f, y0 - h * 0.12f); p.lineTo(x + passo * 0.48f, y0)
                            p.lineTo(x + passo * 0.54f, y0 + h * 0.1f); p.lineTo(x + passo * 0.6f, y0 - h * 0.45f); p.lineTo(x + passo * 0.66f, y0 + h * 0.3f)
                            p.lineTo(x + passo * 0.72f, y0); x += passo
                        }
                    }
                    clipRect(right = w * v) { drawPath(p, cor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)) }
                    drawCircle(cor, 3.dp.toPx(), Offset(w * v, y0))
                }
                Text(if (bpm <= 0) "— — —  SEM PULSO" else "♥ $bpm BPM", fontFamily = MONO2, fontSize = 12.sp, color = cor)
                c.drop(1).forEach { Text(it, color = Color(0xFFAAAAAA), fontSize = 12.sp) }
            }
        }
        // ───── Mistério ─────
        "lacre" -> {
            var aberta by remember { mutableStateOf(false) }
            val a by animateFloatAsState(if (aberta) 1f else 0f, tween(if (semMov()) 0 else 700), label = "lacre")
            val texto = if (c.size > 1) c.drop(1).joinToString("\n") else f(0)
            val letra = if (c.size > 1) f(0).take(2) else "✦"
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.widthIn(max = 320.dp).fillMaxWidth().heightIn(min = 130.dp).background(Color(0xFFE8DCC4)).clickable { aberta = !aberta }.padding(18.dp), contentAlignment = Alignment.Center) {
                    Text(texto, fontFamily = Caveat, fontSize = 22.sp, color = Color(0xFF3A2A14), textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = a })
                    if (a < 1f) Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = 1 - a; translationX = a * 120f; translationY = a * 80f; rotationZ = a * 50f }) {
                        Box(Modifier.size(54.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFD0453A), Color(0xFF7A1410)))), contentAlignment = Alignment.Center) {
                            Text(letra, fontFamily = InstrumentSerif, fontSize = 22.sp, color = Color(0xFFF5C2B0))
                        }
                        Text("toque para abrir", fontSize = 11.sp, color = Color(0xFF7A6A54), modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        // ───── Fantasia ─────
        "grimorio" -> {
            val t = c.joinToString("\n")
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).background(Color(0xFFE9DFC7)).border(1.dp, Color(0xFFB59A6A)).padding(16.dp)) {
                Text(t.take(1), fontFamily = InstrumentSerif, fontSize = 58.sp, lineHeight = 52.sp, color = Color(0xFF8A1D12), modifier = Modifier.padding(end = 8.dp))
                Text(t.drop(1), fontFamily = InstrumentSerif, fontSize = 18.sp, lineHeight = 24.sp, color = Color(0xFF2A1D10), modifier = Modifier.padding(top = 6.dp))
            }
        }
        "circulo" -> {
            val cor = corFx(corNome, Color(0xFF9A7CFF))
            val g = ciclo(10000)
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2 - 6.dp.toPx(); val ctr = center
                        drawCircle(cor.copy(alpha = 0.18f), r + 6.dp.toPx(), ctr)
                        rotate(g * 360f, ctr) {
                            drawCircle(cor, r, ctr, style = Stroke(2.dp.toPx()))
                            val tri = Path(); for (k in 0..2) { val ang = -PI / 2 + k * 2 * PI / 3; val x = ctr.x + cos(ang).toFloat() * r; val y = ctr.y + sin(ang).toFloat() * r; if (k == 0) tri.moveTo(x, y) else tri.lineTo(x, y) }; tri.close()
                            drawPath(tri, cor.copy(alpha = 0.7f), style = Stroke(1.2.dp.toPx()))
                            for (k in 0 until 12) { val ang = k * PI / 6; drawCircle(cor, 2.dp.toPx(), Offset(ctr.x + cos(ang).toFloat() * (r - 10.dp.toPx()), ctr.y + sin(ang).toFloat() * (r - 10.dp.toPx()))) }
                        }
                        rotate(-g * 720f, ctr) {
                            drawCircle(lerp(cor, Color.White, 0.4f), r * 0.62f, ctr, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                            val tri = Path(); for (k in 0..2) { val ang = PI / 2 + k * 2 * PI / 3; val x = ctr.x + cos(ang).toFloat() * r * 0.62f; val y = ctr.y + sin(ang).toFloat() * r * 0.62f; if (k == 0) tri.moveTo(x, y) else tri.lineTo(x, y) }; tri.close()
                            drawPath(tri, cor.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
                        }
                    }
                    Text(runar(f(0).ifBlank { "invocacao" }).take(6), color = lerp(cor, Color.White, 0.5f), fontSize = 12.sp, letterSpacing = 3.sp)
                }
                if (f(0).isNotBlank()) Text(f(0).uppercase(), fontFamily = MONO2, fontSize = 11.sp, letterSpacing = 2.sp, color = cor, modifier = Modifier.padding(top = 8.dp))
                c.drop(1).forEach { Text(it, color = Color(0xFFCCCCCC), fontSize = 13.sp, textAlign = TextAlign.Center) }
            }
        }
        "bencao", "maldicao" -> {
            val bom = nome == "bencao"
            val cor = corFx(corNome, if (bom) Color(0xFFFFE08A) else Color(0xFFB36BFF))
            val v = ciclo(3000, true); val a = entrada(900)
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp).graphicsLayer { alpha = a }, horizontalAlignment = Alignment.CenterHorizontally) {
                RotM(if (bom) "Bênção recebida" else "Maldição recebida", Color(0xFF999999))
                Text((if (bom) "✦ " else "☾ ") + f(0), fontFamily = InstrumentSerif, fontSize = 28.sp, color = cor, textAlign = TextAlign.Center,
                    style = TextStyle(shadow = Shadow(cor.copy(alpha = 0.4f + 0.4f * v), blurRadius = 26f)), modifier = Modifier.padding(top = 4.dp)
                        .graphicsLayer { if (!bom) alpha = 0.6f + 0.4f * v })
                c.drop(1).forEach { Text(it, color = Color(0xFFBBBBBB), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
        "taro" -> {
            val parado = semMov()
            var virada by remember { mutableStateOf(parado) }
            LaunchedEffect(Unit) { delay(900); virada = true }
            val r by animateFloatAsState(if (virada) 180f else 0f, tween(900, easing = FastOutSlowInEasing), label = "taro")
            val romano = Regex("^[IVXLC0]+$").matches(f(0).trim())
            val num = if (romano) f(0) else ""; val resto = if (romano) c.drop(1) else c
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(120.dp, 196.dp).graphicsLayer { rotationY = r; cameraDistance = 12f * density }.clickable { virada = !virada }) {
                    if (r < 90f) Box(Modifier.fillMaxSize().border(2.dp, Color(0xFFC9A46A)).background(Brush.linearGradient(listOf(Color(0xFF2B1A3A), Color(0xFF3A2450), Color(0xFF2B1A3A)))), contentAlignment = Alignment.Center) {
                        Text("✦", color = Color(0xFFC9A46A), fontSize = 30.sp)
                    } else Column(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.border(2.dp, Color(0xFFC9A46A)).background(Color(0xFFF1E6CC)).padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                        Text(num, fontFamily = InstrumentSerif, fontSize = 16.sp, color = Color(0xFF2A1D10))
                        Text(resto.getOrNull(0).orEmpty(), fontFamily = InstrumentSerif, fontSize = 22.sp, color = Color(0xFF2A1D10), textAlign = TextAlign.Center)
                        Text(resto.drop(1).joinToString(" · "), fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = Color(0xFF5A4A30), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        // ───── Sci-fi ─────
        "mira" -> {
            val a = entrada(1100)
            val pisca = ciclo(800, true)
            val ciano = Color(0xFF5AE0FF); val verm = Color(0xFFFF4A4A)
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(220.dp, 140.dp).border(1.dp, ciano.copy(alpha = 0.4f))) {
                    Canvas(Modifier.fillMaxSize()) {
                        val ctr = center
                        drawLine(ciano, Offset(ctr.x, size.height * 0.2f), Offset(ctr.x, size.height * 0.8f), 1f)
                        drawLine(ciano, Offset(size.width * 0.25f, ctr.y), Offset(size.width * 0.75f, ctr.y), 1f)
                        val r = 20.dp.toPx() * (1f + (1f - a) * 1.6f)
                        drawCircle(verm.copy(alpha = a), r, ctr, style = Stroke(2.dp.toPx()))
                    }
                    Text(listOf(f(0), f(1)).filter { it.isNotBlank() }.joinToString(" · ").uppercase(), fontFamily = MONO2, fontSize = 10.sp, color = ciano, modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
                    if (a > 0.95f) Text(f(2).ifBlank { "TRAVADO" }.uppercase(), fontFamily = MONO2, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = verm.copy(alpha = 0.5f + pisca * 0.5f),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp))
                }
            }
        }
        // ───── Celular ─────
        "notificacao" -> {
            val a = entrada(600)
            val app = f(0).ifBlank { "Mensagens" }
            val corApp = when { app.contains("whats", true) -> Color(0xFF25D366); app.contains("insta", true) -> Color(0xFFD43C8A); app.contains("tele", true) -> Color(0xFF2AABEE); else -> Color(0xFF5A8CFF) }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).graphicsLayer { translationY = (1 - a) * -80f; alpha = a }.clip(RoundedCornerShape(18.dp)).background(Color(0xEE28282C))
                .padding(12.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(corApp), contentAlignment = Alignment.Center) { Text(app.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Row { Text(app, color = Color(0xFF9A9A9A), fontSize = 11.sp, modifier = Modifier.weight(1f)); Text("agora", color = Color(0xFF9A9A9A), fontSize = 11.sp) }
                    if (c.size > 1) Text(f(1), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    c.drop(2).forEach { Text(it, color = Color(0xFFE6E6E6), fontSize = 14.sp) }
                }
            }
        }
        "perdidas" -> {
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                RotM("Chamadas perdidas", Color(0xFF9A9A9A))
                c.forEachIndexed { k, l ->
                    val hora = Regex("\\d{1,2}:\\d{2}").find(l)?.value.orEmpty()
                    val vezes = Regex("(?:x\\s*(\\d+)|\\((\\d+)\\))").find(l)
                    val n = vezes?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
                    val nome = l.replace(hora, "").replace(vezes?.value ?: "\u0000", "").trim()
                    val a = entrada(350, 150 * k)
                    Row(Modifier.fillMaxWidth().graphicsLayer { alpha = a }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✆", color = Color(0xFFFF6B6B), fontSize = 16.sp, modifier = Modifier.padding(end = 10.dp))
                        Text(nome + (n?.let { " ($it)" } ?: ""), color = Color(0xFFFF6B6B), fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(hora, color = Color(0xFF888888), fontSize = 12.sp)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF262626)))
                }
            }
        }
        "story" -> {
            val v = ciclo(5000)
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(170.dp, 280.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF3A1D4A), Color(0xFFA14A3A))))) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) { k -> Box(Modifier.weight(1f).height(2.dp).background(Color.White.copy(alpha = 0.35f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(when (k) { 0 -> 1f; 1 -> v; else -> 0f }).background(Color.White)) } }
                    }
                    Text("${f(0)}${if (c.size > 2) " · " + f(2) else ""}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp, top = 18.dp))
                    Text(f(1), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(14.dp))
                }
            }
        }
        "bateria" -> {
            val pct = numero(f(0), 2).coerceIn(0, 100)
            val pisca = ciclo(900, true)
            val cor = if (pct <= 15) Color(0xFFFF5A4A) else if (pct <= 40) Color(0xFFFFC94A) else Color(0xFF6FCF97)
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp, 24.dp).border(2.dp, cor, RoundedCornerShape(4.dp)).padding(3.dp)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth((pct / 100f).coerceAtLeast(0.06f)).background(cor.copy(alpha = if (pct <= 15) 0.35f + pisca * 0.65f else 1f)))
                    }
                    Box(Modifier.size(3.dp, 8.dp).background(cor))
                }
                Text("$pct%" + if (c.size > 1) " · " + c.drop(1).joinToString(" ") else "", color = cor, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 12.dp))
            }
        }
        "musica" -> {
            val v = ciclo(30000)
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF181818)).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(listOf(Color(0xFFE5553B), Color(0xFF5A1D4A)))))
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(f(0), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(f(1), color = Color(0xFF9A9A9A), fontSize = 13.sp)
                    }
                    // barrinhas de equalizador
                    val e = ciclo(700, true)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(18.dp)) {
                        listOf(0.4f, 1f, 0.6f, 0.85f).forEachIndexed { k, h -> Box(Modifier.width(3.dp).fillMaxHeight((h * (if (k % 2 == 0) e else 1 - e)).coerceIn(0.2f, 1f)).background(Color(0xFF1ED760))) }
                    }
                }
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(3.dp).background(Color(0xFF333333))) { Box(Modifier.fillMaxHeight().fillMaxWidth(0.1f + v * 0.8f).background(Color(0xFF1ED760))) }
            }
        }
        // ───── Época ─────
        "radio" -> {
            val v = ciclo(800, true)
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.height(30.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(22) { k -> val base = 0.3f + ((k * 7) % 10) / 14f; Box(Modifier.width(3.dp).fillMaxHeight((base * (0.35f + 0.65f * if (k % 2 == 0) v else 1 - v)).coerceIn(0.15f, 1f)).background(Color(0xFFD9C49A))) }
                }
                Text(c.joinToString(" ").uppercase(), fontFamily = Maquina, fontSize = 15.sp, color = Color(0xFFD9C49A), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }
        }
        "datilografado" -> {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).background(Color(0xFFF3ECD9)).padding(horizontal = 18.dp, vertical = 16.dp)) {
                if (c.size > 1) Text(f(0).uppercase(), fontFamily = Maquina, fontSize = 15.sp, color = Color(0xFF1D1A16), modifier = Modifier.padding(bottom = 8.dp))
                (if (c.size > 1) c.drop(1) else c).forEach { Text(it, fontFamily = Maquina, fontSize = 15.sp, lineHeight = 23.sp, color = Color(0xFF2A2620)) }
            }
        }
        // ───── Estrutura ─────
        "fimarco" -> {
            val a = entrada(1200)
            Column(Modifier.fillMaxWidth().padding(vertical = 26.dp).graphicsLayer { alpha = a }, horizontalAlignment = Alignment.CenterHorizontally) {
                RotM("Fim do arco ${f(0)}", Color(0xFFE5553B), 11f)
                Text(f(1), fontFamily = InstrumentSerif, fontSize = 40.sp, lineHeight = 42.sp, color = Color(0xFFEDE6DA), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp))
                c.drop(2).forEach { Text(it, fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic, fontSize = 17.sp, color = Color(0xFFAAAAAA), textAlign = TextAlign.Center) }
            }
        }
        "epigrafe" -> {
            Column(Modifier.fillMaxWidth().padding(start = 50.dp, top = 12.dp, bottom = 16.dp), horizontalAlignment = Alignment.End) {
                Text("“${f(0)}”", fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic, fontSize = 20.sp, lineHeight = 26.sp, color = Color(0xFFCFC6B8), textAlign = TextAlign.End)
                if (c.size > 1) Text("— " + c.drop(1).joinToString(", "), fontFamily = MONO2, fontSize = 11.sp, letterSpacing = 1.5.sp, color = Color(0xFF888888), modifier = Modifier.padding(top = 8.dp))
            }
        }
        "enquanto" -> {
            Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF666666), Color.Transparent))))
                Text(c.joinToString(" ").uppercase(), fontFamily = MONO2, fontSize = 12.sp, letterSpacing = 4.sp, color = Color(0xFFAAAAAA), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF666666), Color.Transparent))))
            }
        }
        "hora" -> {
            val pisca = ciclo(1000)
            val h = f(0).ifBlank { "00:00" }
            Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(h.replace(":", if (pisca < 0.5f) ":" else " "), fontFamily = MONO2, fontSize = 44.sp, fontWeight = FontWeight.Medium, color = Color.White)
                c.drop(1).forEach { RotM(it, Color(0xFFE5553B), 11f) }
            }
        }
        "lembranca" -> {
            val v = ciclo(5000)
            val foco = (sin(v * 2 * PI).toFloat() * 0.5f + 0.5f)
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).drawBehind {
                val r = java.util.Random(7)
                repeat(14) { drawCircle(Color.White.copy(alpha = 0.25f + 0.5f * foco * r.nextFloat()), 1.2f + r.nextFloat() * 1.4f, Offset(r.nextFloat() * size.width, r.nextFloat() * size.height)) }
            }.padding(18.dp), contentAlignment = Alignment.Center) {
                Text(c.joinToString("\n"), fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic, fontSize = 20.sp, lineHeight = 27.sp, color = Color(0xFFDFE8FF), textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = 0.35f + 0.65f * foco }.blur((5f * (1 - foco)).dp))
            }
        }
        else -> Text(c.joinToString(" | "), style = estilo)
    }
}

// ───────── efeitos em linha novos ─────────
val INLINE_NOVOS = setOf("estrela", "fantasma", "sagrado", "cinzas", "mao", "contagem", "mente", "corrompido", "dano")
/** Os que mudam a cada quadro (o leitor anima). */
val INLINE_ANIMADOS = setOf("medo", "quebra", "vento", "raio", "neon", "idioma", "coracao", "estrela", "fantasma", "sagrado", "cinzas", "contagem", "mente", "corrompido", "dano") + INLINE_V2

private val MARCAS_Z = listOf('̀', '́', '̴', '̶', '̷', '͒', '͓', '͡', 'ͪ', '̖', '̗', '̥', '̮')
/** Texto amaldiçoado: acentos empilhados (fixos, para não mudar o tamanho do texto). */
fun corromper(s: String): String { val r = kotlin.random.Random(s.hashCode()); return buildString { s.forEach { ch -> append(ch); if (ch != ' ') repeat(1 + r.nextInt(2)) { append(MARCAS_Z[r.nextInt(MARCAS_Z.size)]) } } } }

fun estiloInlineNovo(nome: String, cc: Color?): androidx.compose.ui.text.SpanStyle? = when (nome) {
    "estrela" -> androidx.compose.ui.text.SpanStyle(color = cc ?: Color(0xFFFFE9A8), shadow = Shadow(Color(0xCCFFDC82), blurRadius = 14f))
    "fantasma" -> androidx.compose.ui.text.SpanStyle(color = cc ?: Color(0xFFCFE8FF), shadow = Shadow(Color(0xB3B4DCFF), blurRadius = 16f))
    "sagrado" -> androidx.compose.ui.text.SpanStyle(color = Color(0xFFFFF3C4), fontWeight = FontWeight.Bold, shadow = Shadow(cc ?: Color(0xFFFFD678), blurRadius = 28f))
    "cinzas" -> androidx.compose.ui.text.SpanStyle(color = cc ?: Color(0xFFE8DCCB))
    "mao" -> androidx.compose.ui.text.SpanStyle(fontFamily = Caveat, color = cc ?: Color(0xFF9FC6FF), fontSize = androidx.compose.ui.unit.TextUnit(1.3f, androidx.compose.ui.unit.TextUnitType.Em))
    "contagem" -> androidx.compose.ui.text.SpanStyle(fontFamily = FontFamily.Monospace, color = cc ?: Color(0xFFE5553B), fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.TextUnit(1.3f, androidx.compose.ui.unit.TextUnitType.Em))
    "mente" -> androidx.compose.ui.text.SpanStyle(color = cc ?: Color(0xFFC9A6FF), fontStyle = FontStyle.Italic, shadow = Shadow(Color(0x80C9A6FF), Offset(6f, 4f), 4f))
    "corrompido" -> androidx.compose.ui.text.SpanStyle(color = cc ?: Color(0xFFFF7A7A))
    "dano" -> androidx.compose.ui.text.SpanStyle(color = cc ?: Color(0xFFFF4A3A), fontWeight = FontWeight.Black, fontSize = androidx.compose.ui.unit.TextUnit(1.25f, androidx.compose.ui.unit.TextUnitType.Em),
        shadow = Shadow(Color(0xB3FF4A3A), blurRadius = 14f))
    else -> null
}

/** Quadro a quadro dos novos (110 ms por quadro). Devolve o texto com estilos extras (e a contagem já trocada). */
fun animarInlineNovos(t: androidx.compose.ui.text.AnnotatedString, quadro: Int): androidx.compose.ui.text.AnnotatedString {
    val fx = t.getStringAnnotations("fx", 0, t.length).filter { it.item in INLINE_NOVOS }
    if (fx.isEmpty()) return t
    var r = t
    // contagem: troca os dígitos (mesma largura) pelo tempo que falta
    fx.filter { it.item == "contagem" }.forEach { a ->
        val orig = t.text.substring(a.start, a.end)
        val m = Regex("\\d+").find(orig) ?: return@forEach
        val faltam = (m.value.toInt() - quadro * 110 / 1000).coerceAtLeast(0)
        val novo = orig.replaceRange(m.range, faltam.toString().padStart(m.value.length, '0'))
        r = androidx.compose.ui.text.buildAnnotatedString { append(r.subSequence(0, a.start)); append(androidx.compose.ui.text.AnnotatedString(novo, r.subSequence(a.start, a.end).spanStyles)); append(r.subSequence(a.end, r.length)) }
            .let { s -> androidx.compose.ui.text.buildAnnotatedString { append(s); addStringAnnotation("fx", "contagem", a.start, a.end) } }
    }
    val base = r
    return androidx.compose.ui.text.buildAnnotatedString {
        append(base)
        fx.forEach { a ->
            val fase = (quadro % 36) / 36f
            when (a.item) {
                "estrela" -> addStyle(androidx.compose.ui.text.SpanStyle(shadow = Shadow(Color(0xFFFFE9A8).copy(alpha = 0.5f + 0.5f * sin(fase * 2 * PI).toFloat().let { it * it }), blurRadius = 10f + 14f * sin(fase * PI).toFloat())), a.start, a.end)
                "fantasma" -> { val al = (0.15f + 0.85f * sin(fase * PI).toFloat()); addStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFCFE8FF).copy(alpha = al)), a.start, a.end) }
                "sagrado" -> addStyle(androidx.compose.ui.text.SpanStyle(shadow = Shadow(Color(0xFFFFD678), blurRadius = 18f + 16f * sin(fase * 2 * PI).toFloat().let { it * it })), a.start, a.end)
                "cinzas" -> { val n = a.end - a.start; val ciclo = quadro % (n + 24)
                    for (k in a.start until a.end) { val d = ciclo - (k - a.start); when { d in 0..4 -> addStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFC86A3A)), k, k + 1)
                        d in 5..14 -> addStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF555555).copy(alpha = (1f - (d - 5) / 10f).coerceIn(0f, 1f)), baselineShift = androidx.compose.ui.text.style.BaselineShift((d - 5) * 0.06f)), k, k + 1)
                        d > 14 && d < n + 10 -> addStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent), k, k + 1) } } }
                "mente" -> { val d = 4f + 10f * fase; addStyle(androidx.compose.ui.text.SpanStyle(shadow = Shadow(Color(0xFFC9A6FF).copy(alpha = 0.6f * (1 - fase)), Offset(d, d * 0.6f), 4f + d)), a.start, a.end) }
                "corrompido" -> { val rr = kotlin.random.Random(quadro); for (k in a.start until a.end) if (rr.nextFloat() < 0.25f) addStyle(androidx.compose.ui.text.SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift(rr.nextFloat() * 0.16f - 0.08f)), k, k + 1) }
                "dano" -> { val p = (quadro % 18) / 18f; addStyle(androidx.compose.ui.text.SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift(0.1f + p * 0.5f),
                    color = Color(0xFFFF4A3A).copy(alpha = if (p < 0.15f) p / 0.15f else (1.15f - p).coerceIn(0f, 1f))), a.start, a.end) }
            }
        }
    }
}
