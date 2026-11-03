package com.huryel.novely.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Progresso
import com.huryel.novely.ui.theme.LocalPaleta
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

val LARANJA_FX = Color(0xFFFF6A2B)
val ROXO_FX = Color(0xFFC13DFF)
val AZUL_FX = Color(0xFF3A8BFF)
val OURO_FX = Color(0xFFFFC94A)
val GRAD_FX = Brush.linearGradient(listOf(LARANJA_FX, ROXO_FX))

/** Reflexo de luz passando por cima (botões, insígnias, lombadas douradas). Só redesenha. */
fun Modifier.brilho(periodo: Int = 2600, atraso: Int = 0): Modifier = composed {
    val t = rememberInfiniteTransition(label = "brilho")
    val x by t.animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(periodo, atraso, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "x")
    drawWithContent {
        drawContent()
        val w = size.width
        drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
            start = Offset(w * x - w * 0.3f, 0f), end = Offset(w * x + w * 0.3f, size.height)))
    }
}

/** Anel girando em degradê (autor com capítulo novo, avatar do perfil). */
@Composable
fun AnelGirando(tamanho: Dp, espessura: Dp = 2.5.dp, ativo: Boolean = true, conteudo: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "anel")
    val ang by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "ang")
    Box(Modifier.size(tamanho), contentAlignment = Alignment.Center) {
        if (ativo) Canvas(Modifier.fillMaxSize()) {
            rotate(ang) { drawCircle(Brush.sweepGradient(listOf(LARANJA_FX, ROXO_FX, AZUL_FX, LARANJA_FX)), style = Stroke(espessura.toPx())) }
        } else Canvas(Modifier.fillMaxSize()) { drawCircle(Color.White.copy(alpha = 0.2f), style = Stroke(1.5.dp.toPx())) }
        Box(Modifier.padding(espessura + 2.dp).fillMaxSize().clip(CircleShape), contentAlignment = Alignment.Center, content = conteudo)
    }
}

/** Anel de progresso que se enche animado (0 → fração) ao aparecer. */
@Composable
fun AnelProgresso(fracao: Float, tamanho: Dp, cor: Color, espessura: Dp = 5.dp, duracao: Int = 1200, conteudo: @Composable BoxScope.(Float) -> Unit = {}) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fracao) { anim.animateTo(fracao.coerceIn(0f, 1f), tween(duracao, 200, FastOutSlowInEasing)) }
    Box(Modifier.size(tamanho), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val e = espessura.toPx(); val r = Size(size.width - e, size.height - e); val o = Offset(e / 2, e / 2)
            drawArc(Color.White.copy(alpha = 0.12f), 0f, 360f, false, o, r, style = Stroke(e))
            drawArc(cor, -90f, 360f * anim.value, false, o, r, style = Stroke(e, cap = StrokeCap.Round))
        }
        conteudo(anim.value)
    }
}

private data class Papelzinho(val x: Float, val vx: Float, val vy: Float, val rot: Float, val vr: Float, val cor: Color, val w: Float, val h: Float, val atraso: Float)
private val CORES_CONFETE = listOf(LARANJA_FX, ROXO_FX, AZUL_FX, OURO_FX, Color(0xFFF07AAE), Color(0xFF6FCF97))

/** Chuva de confete (dura ~2,5 s). Mude [chave] para soltar de novo. */
@Composable
fun Confete(chave: Any, modifier: Modifier = Modifier.fillMaxSize(), quantidade: Int = 70) {
    val tempo = remember(chave) { Animatable(0f) }
    val pedacos = remember(chave) {
        val r = Random(chave.hashCode())
        List(quantidade) { Papelzinho(r.nextFloat(), (r.nextFloat() - 0.5f) * 0.25f, 0.55f + r.nextFloat() * 0.7f, r.nextFloat() * 360, (r.nextFloat() - 0.5f) * 900,
            CORES_CONFETE[it % CORES_CONFETE.size], 6f + r.nextFloat() * 6, 9f + r.nextFloat() * 8, r.nextFloat() * 0.35f) }
    }
    LaunchedEffect(chave) { tempo.animateTo(1f, tween(2600, easing = LinearEasing)) }
    if (tempo.value >= 1f) return
    Canvas(modifier) {
        pedacos.forEach { p ->
            val t = ((tempo.value - p.atraso) / (1 - p.atraso)).coerceIn(0f, 1f); if (t <= 0f) return@forEach
            val x = (p.x + p.vx * t) * size.width; val y = -30f + p.vy * t * size.height * 1.1f + 400f * t * t
            rotate(p.rot + p.vr * t, Offset(x, y)) {
                drawRect(p.cor.copy(alpha = 1f - t * t), Offset(x - p.w, y - p.h / 2), Size(p.w * 2 * density / 2.5f, p.h * density / 2.5f))
            }
        }
    }
}

/** Comemorações por cima de tudo: subiu de nível, conquista nova, baú aberto. */
@Composable
fun Celebracoes() {
    var atual by remember { mutableStateOf<Progresso.Evento?>(null) }
    val fila = remember { ArrayDeque<Progresso.Evento>() }
    LaunchedEffect(Unit) { Progresso.eventos.collect { e -> if (atual == null) atual = e else fila.addLast(e) } }
    val e = atual ?: return
    val fechar = { atual = fila.removeFirstOrNull() }
    val entrada = remember(e) { Animatable(0f) }
    LaunchedEffect(e) { entrada.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
    val t = rememberInfiniteTransition(label = "raios")
    val giro by t.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "giro")
    val (cor, rotulo) = when (e) {
        is Progresso.Evento.Nivel -> ROXO_FX to "SUBIU DE NÍVEL"
        is Progresso.Evento.Conquista -> Color(e.cor) to "CONQUISTA DESBLOQUEADA"
        is Progresso.Evento.Bau -> OURO_FX to "BAÚ DO DIA"
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f * entrada.value.coerceIn(0f, 1f)))
        .clickable(remember { MutableInteractionSource() }, null) { fechar() }, contentAlignment = Alignment.Center) {
        // raios girando atrás
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = entrada.value.coerceIn(0f, 1f) }) {
            val c = Offset(size.width / 2, size.height * 0.42f)
            rotate(giro, c) {
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0); val a2 = Math.toRadians(i * 30.0 + 9)
                    val r = size.maxDimension
                    val path = androidx.compose.ui.graphics.Path().apply { moveTo(c.x, c.y); lineTo(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat()); lineTo(c.x + r * cos(a2).toFloat(), c.y + r * sin(a2).toFloat()); close() }
                    drawPath(path, cor.copy(alpha = 0.10f))
                }
            }
            drawCircle(Brush.radialGradient(listOf(cor.copy(alpha = 0.45f), Color.Transparent), c, size.width * 0.45f), size.width * 0.45f, c)
        }
        Confete(e)
        Column(Modifier.padding(30.dp).graphicsLayer { scaleX = entrada.value; scaleY = entrada.value }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(rotulo, color = cor, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 4.sp)
            Spacer(Modifier.height(40.dp))
            Box(Modifier.size(150.dp).rotate(45f).clip(RoundedCornerShape(38.dp)).background(Brush.linearGradient(listOf(cor, cor.copy(alpha = 0.55f)))).brilho(2200),
                contentAlignment = Alignment.Center) {
                Text(when (e) { is Progresso.Evento.Nivel -> "${e.nivel}"; is Progresso.Evento.Conquista -> e.icone; is Progresso.Evento.Bau -> "🪙" },
                    fontSize = if (e is Progresso.Evento.Nivel) 62.sp else 58.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.rotate(-45f))
            }
            Spacer(Modifier.height(44.dp))
            Text(when (e) { is Progresso.Evento.Nivel -> "Nível ${e.nivel}"; is Progresso.Evento.Conquista -> e.titulo; is Progresso.Evento.Bau -> "Baú aberto!" },
                fontWeight = FontWeight.Black, fontSize = 30.sp, color = Color.White, textAlign = TextAlign.Center)
            Text(when (e) { is Progresso.Evento.Nivel -> "Novo título: ${e.titulo}"; is Progresso.Evento.Conquista -> "+100 XP"; is Progresso.Evento.Bau -> "+${e.moedas} moedas · +${e.xp} XP" },
                fontSize = 15.sp, color = Color(0xFFDDDDDD), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(36.dp))
            Text("Continuar", fontWeight = FontWeight.Black, color = Color.Black, modifier = Modifier.clip(RoundedCornerShape(28.dp)).background(Color.White)
                .clickable { fechar() }.padding(horizontal = 40.dp, vertical = 16.dp))
        }
    }
}

// ───────── perfil de jogador ─────────

@Composable
fun CartaoNivel() {
    val p = LocalPaleta.current
    val prefs = NovelyApp.instancia.prefs
    val xp by prefs.xp.estado.collectAsState()
    val seq by prefs.sequencia.estado.collectAsState()
    val i = remember(xp) { Progresso.info(xp) }
    val barra = remember { Animatable(0f) }
    LaunchedEffect(i.fracao) { barra.animateTo(i.fracao, tween(1400, 300, FastOutSlowInEasing)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clip(RoundedCornerShape(22.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF2A0F45), Color(0xFF14081F)))).border(1.dp, ROXO_FX.copy(alpha = 0.35f), RoundedCornerShape(22.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(GRAD_FX).brilho(3000).padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text("NÍVEL ${i.nivel}", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
            }
            Text("  ${Progresso.titulo(i.nivel)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFD9B8FF), fontSize = 14.sp, modifier = Modifier.weight(1f))
            if (seq > 0) Text("🔥 $seq", fontWeight = FontWeight.Black, color = LARANJA_FX, fontSize = 15.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("XP ${i.noNivel} / ${i.precisa}", fontSize = 11.5.sp, color = p.textoSuave, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("faltam ${i.precisa - i.noNivel} para o nível ${i.nivel + 1}", fontSize = 11.5.sp, color = p.textoSuave)
        }
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.1f))) {
            Box(Modifier.fillMaxWidth(barra.value.coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(GRAD_FX).brilho(2400))
        }
    }
}

@Composable
fun CartaoMissoes() {
    val p = LocalPaleta.current
    val prefs = NovelyApp.instancia.prefs
    val dia by prefs.missoesDia.estado.collectAsState()
    val bau by prefs.bauDia.estado.collectAsState()
    val ms = remember(dia, bau) { Progresso.missoes() }
    val feitas = ms.count { it.pronta }
    val pronto = feitas == ms.size && !Progresso.bauAberto
    val t = rememberInfiniteTransition(label = "bau")
    val tremor by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(120), RepeatMode.Reverse), label = "tremor")
    val pulo by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulo")
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clip(RoundedCornerShape(22.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF2A1A06), Color(0xFF120B04)))).border(1.dp, OURO_FX.copy(alpha = 0.3f), RoundedCornerShape(22.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Missões de hoje", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color.White)
                Text(if (Progresso.bauAberto) "Baú aberto hoje ✓ · amanhã tem mais" else "$feitas de ${ms.size} · complete todas e abra o baú", fontSize = 12.sp, color = Color(0xFFD9C2A0))
            }
            Text(if (Progresso.bauAberto) "🎁" else "🧰", fontSize = 40.sp, modifier = Modifier
                .graphicsLayer { if (pronto) { rotationZ = tremor; translationY = -10f * pulo } }
                .clickable(enabled = pronto) { Progresso.abrirBau() })
        }
        ms.forEach { m ->
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = if (m.pronta) 0.04f else 0.07f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(if (m.pronta) Color(0xFF6FCF97) else Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Text(if (m.pronta) "✓" else "", color = Color.Black, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(m.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (m.pronta) Color(0xFF888888) else Color.White,
                        textDecoration = if (m.pronta) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                    if (!m.pronta && m.alvo > 1) {
                        val f by animateFloatAsState(m.feito.toFloat() / m.alvo, tween(800), label = "m")
                        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.1f))) {
                            Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(OURO_FX))
                        }
                        Text("${m.feito} de ${m.alvo}", fontSize = 11.sp, color = p.textoSuave, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
        if (pronto) Text("Toque no baú para abrir! 🪙 10 + ⚡ 50 XP", fontWeight = FontWeight.ExtraBold, color = OURO_FX, fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp).clip(RoundedCornerShape(14.dp)).background(OURO_FX.copy(alpha = 0.15f)).clickable { Progresso.abrirBau() }.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
fun RankingSemana() {
    val p = LocalPaleta.current
    var l by remember { mutableStateOf<List<Progresso.NoRanking>?>(null) }
    LaunchedEffect(Unit) { l = try { Progresso.ranking() } catch (_: Exception) { emptyList() } }
    val lista = l ?: return
    if (lista.size < 2) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clip(RoundedCornerShape(20.dp)).background(p.superficie).padding(14.dp)) {
        Text("ESTA SEMANA ENTRE QUEM VOCÊ SEGUE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = p.textoSuave, letterSpacing = 1.sp)
        lista.take(6).forEachIndexed { i, r ->
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${i + 1}" }, modifier = Modifier.width(30.dp), fontWeight = FontWeight.Black)
                Text(r.nome, fontWeight = FontWeight.Bold, color = if (r.eu) LARANJA_FX else p.texto, modifier = Modifier.weight(1f))
                Text("${r.xp} XP", fontSize = 12.sp, color = if (r.eu) LARANJA_FX else p.textoSuave)
            }
        }
    }
}
