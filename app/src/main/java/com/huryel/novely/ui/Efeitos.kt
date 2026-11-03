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

// ───────── Clima da cena (leitor) ─────────

private val PALAVRAS_CLIMA = listOf(
    "chuva" to Regex("\\b(chuva|chovia|chover|choveu|garoa|tempestade|trov[aã]o|trovoada|rel[aâ]mpago|aguaceiro|temporal)", RegexOption.IGNORE_CASE),
    "neve" to Regex("\\b(neve|nevava|nevasca|flocos de neve|nevando|geada)", RegexOption.IGNORE_CASE),
    "fogo" to Regex("\\b(fogo|chamas?|fogueira|lareira|inc[eê]ndio|brasas?|labaredas?|queimava|ardia)", RegexOption.IGNORE_CASE),
    "vento" to Regex("\\b(vento|ventania|vendaval|rajadas?|brisa|uivava)", RegexOption.IGNORE_CASE),
    "noite" to Regex("\\b(noite|luar|madrugada|estrelas|escurid[aã]o|meia-noite)", RegexOption.IGNORE_CASE),
)

/** Qual clima o trecho sugere (chuva, neve, fogo, vento, noite) ou null. Noite pede duas menções, porque aparece demais. */
fun detectarClima(texto: String): String? {
    var melhor: String? = null; var nota = 0
    for ((tipo, rx) in PALAVRAS_CLIMA) {
        val n = rx.findAll(texto).take(6).count()
        val minimo = if (tipo == "noite") 2 else 1
        if (n >= minimo && n > nota) { melhor = tipo; nota = n }
    }
    return melhor
}

val ROTULO_CLIMA = mapOf("chuva" to "🌧 chuva na cena", "neve" to "❄ neve na cena", "fogo" to "🔥 fogo na cena", "vento" to "🌬 vento na cena", "noite" to "🌙 noite na cena")
val SOM_CLIMA = mapOf("chuva" to "chuva", "neve" to "vento", "fogo" to "lareira", "vento" to "vento", "noite" to "noite")

private class Particula(var x: Float, var y: Float, val v: Float, val tam: Float, val fase: Float)

/** Partículas por cima do texto (não pegam toque). Somem e aparecem com fade quando o clima muda. */
@Composable
fun ClimaCena(tipo: String?, modifier: Modifier = Modifier) {
    var mostrado by remember { mutableStateOf(tipo) }
    val alfa = remember { Animatable(0f) }
    LaunchedEffect(tipo) {
        if (tipo != mostrado) { alfa.animateTo(0f, tween(500)); mostrado = tipo }
        if (tipo != null) alfa.animateTo(1f, tween(900))
    }
    val m = mostrado ?: return
    val r = remember(m) { Random(m.hashCode()) }
    val parts = remember(m) {
        val n = when (m) { "chuva" -> 90; "neve" -> 70; "fogo" -> 45; "noite" -> 26; else -> 22 }
        List(n) { Particula(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.8f, 0.5f + r.nextFloat(), r.nextFloat() * 6.28f) }
    }
    var tempo by remember { mutableLongStateOf(0L) }
    LaunchedEffect(m) { var antes = 0L; while (true) withFrameNanos { if (antes != 0L) tempo += it - antes; antes = it } }
    Canvas(modifier.fillMaxSize()) {
        val t = tempo / 1e9f
        val a = alfa.value
        val w = size.width; val h = size.height
        when (m) {
            "chuva" -> parts.forEach { p ->
                val y = ((p.y + t * 1.3f * p.v) % 1f) * (h + 80) - 40
                val x = ((p.x + t * 0.12f * p.v) % 1f) * w
                drawLine(Color(0xFFBFD8FF).copy(alpha = 0.32f * a * p.tam), Offset(x, y), Offset(x - 6f * p.v, y + 26f * p.v), 1.6f * density / 2, StrokeCap.Round)
            }
            "neve" -> parts.forEach { p ->
                val y = ((p.y + t * 0.07f * p.v) % 1f) * (h + 20) - 10
                val x = p.x * w + sin(t * 0.9f + p.fase) * 22f * density
                drawCircle(Color.White.copy(alpha = 0.7f * a * (0.4f + p.tam / 2)), 1.6f * density * p.tam, Offset(x, y))
            }
            "fogo" -> parts.forEach { p ->
                val ciclo = (p.y + t * 0.18f * p.v) % 1f
                val y = h - ciclo * h * 0.75f
                val x = p.x * w + sin(t * 2f + p.fase) * 14f * density
                val brilho = (1 - ciclo) * (0.6f + 0.4f * sin(t * 9f + p.fase))
                drawCircle(Color(0xFFFF8A2A).copy(alpha = 0.55f * a * brilho), 2.2f * density * p.tam, Offset(x, y))
                drawCircle(Color(0xFFFFC46B).copy(alpha = 0.25f * a * brilho), 5f * density * p.tam, Offset(x, y))
            }
            "noite" -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0A1030).copy(alpha = 0.22f * a), Color.Transparent, Color(0xFF0A1030).copy(alpha = 0.22f * a))))
                parts.forEach { p ->
                    val x = (p.x * w + sin(t * 0.3f * p.v + p.fase) * 40f * density + w) % w
                    val y = (p.y * h + cos(t * 0.25f * p.v + p.fase) * 30f * density + h) % h
                    val pulso = ((sin(t * 1.6f * p.v + p.fase) + 1) / 2).let { it * it }
                    drawCircle(Color(0xFFE8FF8A).copy(alpha = 0.22f * a * pulso), 7f * density, Offset(x, y))
                    drawCircle(Color(0xFFF4FFC4).copy(alpha = 0.8f * a * pulso), 1.6f * density, Offset(x, y))
                }
            }
            else -> parts.forEach { p ->
                val x = ((p.x + t * 0.35f * p.v) % 1f) * (w + 200) - 100
                val y = p.y * h + sin(t * 1.5f + p.fase) * 16f * density
                drawLine(Color.White.copy(alpha = 0.16f * a * p.tam), Offset(x, y), Offset(x - 60f * p.v * density / 2, y - 3f), 1.2f * density / 2, StrokeCap.Round)
            }
        }
    }
}

// ───────── Fogueira do escritor (editor) ─────────

/** Anel da meta do dia com uma chama que cresce com as palavras e "+XP" subindo a cada 10 palavras. */
@Composable
fun Fogueira(hoje: Long, meta: Float, somAtual: String?, aoSom: (String?) -> Unit) {
    val p = LocalPaleta.current
    val frac = (hoje / meta.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val bolhas = remember { mutableStateListOf<Long>() }
    var ultimo by remember { mutableLongStateOf(hoje) }
    LaunchedEffect(hoje) {
        if (hoje / 10 > ultimo / 10 && hoje > ultimo) bolhas.add(System.nanoTime())
        ultimo = hoje
    }
    val chama = rememberInfiniteTransition(label = "chama")
    val tremor by chama.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tremor")
    var menu by remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center) {
        bolhas.forEach { id ->
            key(id) {
                val sobe = remember { Animatable(0f) }
                LaunchedEffect(Unit) { sobe.animateTo(1f, tween(1300, easing = LinearOutSlowInEasing)); bolhas.remove(id) }
                Text("+1 XP", fontSize = 11.sp, fontWeight = FontWeight.Black, color = OURO_FX,
                    modifier = Modifier.graphicsLayer { translationY = -sobe.value * 70f * density; alpha = 1f - sobe.value; scaleX = 0.8f + sobe.value * 0.4f; scaleY = scaleX })
            }
        }
        Box(Modifier.clip(CircleShape).background(p.superficie.copy(alpha = 0.92f)).clickable { menu = !menu }) {
            AnelProgresso(frac, 52.dp, if (frac >= 1f) Color(0xFF6FC2B4) else LARANJA_FX, 4.dp, 600) {
                Text("🔥", fontSize = (13 + 15 * frac).sp, modifier = Modifier.graphicsLayer { scaleX = tremor; scaleY = 2f - tremor; alpha = 0.55f + 0.45f * frac.coerceAtLeast(0.15f) })
            }
        }
        androidx.compose.material3.DropdownMenu(menu, { menu = false }, containerColor = p.superficie) {
            Text("${hoje}/${meta.toInt()} palavras hoje", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.destaque, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            Text("SOM AMBIENTE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = p.textoSuave, modifier = Modifier.padding(horizontal = 14.dp))
            (com.huryel.novely.data.Ambiente.TIPOS + listOf<Pair<String?, String>>(null to "🔇 Silêncio")).forEach { (k, nome) ->
                androidx.compose.material3.DropdownMenuItem(text = {
                    Text(nome, fontWeight = if (k == somAtual) FontWeight.Black else FontWeight.Normal, color = if (k == somAtual) p.destaque else p.texto)
                }, onClick = { aoSom(k); menu = false })
            }
        }
    }
}
