package com.huryel.novely.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ───────── Mega Pack: blocos [#efeito:cor campo | campo] ─────────

/** Cores que o autor escreve depois do nome do efeito. */
val CORES_FX = linkedMapOf(
    "roxo" to Color(0xFFA47CFF), "vermelho" to Color(0xFFFF3B4E), "azul" to Color(0xFF4FB8FF), "verde" to Color(0xFF3DDC84),
    "dourado" to Color(0xFFFFC94A), "rosa" to Color(0xFFFF5FB0), "branco" to Color(0xFFE9E7F2), "sombrio" to Color(0xFF7A6F8F),
    "laranja" to Color(0xFFFF6A2B),
)
fun corFx(nome: String?, padrao: Color = CORES_FX["roxo"]!!): Color = nome?.let { CORES_FX[it.lowercase().replace("ê", "e")] } ?: padrao

/** Todos os blocos que o leitor sabe desenhar. */
val BLOCOS_FX = setOf(
    "evolucao", "atributos", "habilidade", "missao-ok", "missao-falhou", "perigo", "conquista", "classe", "titulo", "chefe", "andar",
    "sistema", "bau", "ranking", "tempo", "inimigo",
    "reino", "tecnica", "seita", "pilula", "duelo", "dantian", "tribulacao",
    "rasgada", "vhs", "regras", "foto",
    "arquivo", "depoimento", "telegrama", "linha", "prova",
    "profecia", "contrato",
    "terminal", "holo", "corte", "scan", "codigo", "alerta-vermelho", "ia",
    "post", "jornal", "email", "ligacao", "busca", "voz", "bloqueio", "apagada", "digitando", "grupo", "localizacao",
    "diario", "procurado",
    "aviso", "salto", "capitulo", "proximo",
)
private val APELIDOS_FX = mapOf("evolução" to "evolucao", "missão-ok" to "missao-ok", "missao-concluida" to "missao-ok", "missão-falhou" to "missao-falhou",
    "baú" to "bau", "técnica" to "tecnica", "pílula" to "pilula", "tribulação" to "tribulacao", "diário" to "diario", "ligação" to "ligacao",
    "localização" to "localizacao", "capítulo" to "capitulo", "próximo" to "proximo", "título" to "titulo", "alerta" to "alerta-vermelho", "transmissao" to "corte")

/** "[#evolucao:dourado Nv 11 > 12 | …]" → (nome, cor, resto). */
fun lerBlocoFx(linha: String): Triple<String, String?, String>? {
    if (!linha.startsWith("[#") || !linha.endsWith("]")) return null
    val corpo = linha.substring(2, linha.length - 1)
    val cab = corpo.substringBefore(' ')
    val nome0 = cab.substringBefore(':').lowercase()
    val nome = APELIDOS_FX[nome0] ?: nome0
    if (nome !in BLOCOS_FX) return null
    val cor = cab.substringAfter(':', "").ifEmpty { null }
    return Triple(nome, cor, corpo.substringAfter(' ', "").trim())
}

/** Campos separados por "|" (sem quebrar os ||segredos||). */
fun camposFx(s: String): List<String> = s.split(Regex("(?<!\\|)\\|(?!\\|)")).map { it.trim() }.filter { it.isNotEmpty() }

private val ESC = Color(0xFF07070B)
private val MONO = FontFamily.Monospace
private fun mix(c: Color, f: Float) = lerp(ESC, c, f)

/** Entra subindo e aparecendo (com atraso). */
private fun Modifier.entrar(atrasoMs: Int = 0, dy: Float = 18f): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(atrasoMs.toLong()); a.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
    graphicsLayer { alpha = a.value; translationY = (1 - a.value) * dy * density }
}

/** Caixa de Sistema: fundo escuro tingido e cantoneiras na cor. */
@Composable
private fun Painel(cor: Color, modifier: Modifier = Modifier, fundo: Float = 0.09f, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp).background(mix(cor, fundo)).border(1.dp, cor.copy(alpha = 0.22f))
        .drawBehind {
            val l = 16.dp.toPx(); val e = 2.dp.toPx()
            listOf(Offset(0f, 0f) to Pair(1, 1), Offset(size.width, 0f) to Pair(-1, 1), Offset(0f, size.height) to Pair(1, -1), Offset(size.width, size.height) to Pair(-1, -1)).forEach { (o, d) ->
                drawRect(cor, Offset(if (d.first > 0) o.x else o.x - l, if (d.second > 0) o.y else o.y - e), Size(l, e))
                drawRect(cor, Offset(if (d.first > 0) o.x else o.x - e, if (d.second > 0) o.y else o.y - l), Size(e, l))
            }
        }.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = conteudo)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Linha(conteudo: @Composable () -> Unit) = FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { conteudo() }
@Composable private fun Rot(t: String, cor: Color, modifier: Modifier = Modifier) =
    Text(t.uppercase(), fontFamily = MONO, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 2.4.sp, color = cor, modifier = modifier)
@Composable private fun Chip(t: String, cor: Color, atraso: Int = 0) =
    Text(t, fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = lerp(cor, Color.White, 0.6f),
        modifier = Modifier.entrar(atraso).border(1.dp, cor.copy(alpha = 0.5f)).background(cor.copy(alpha = 0.1f)).padding(horizontal = 9.dp, vertical = 3.dp))

private fun claro(c: Color) = lerp(c, Color.White, 0.78f)

/** Faz o "de novo" quando o bloco volta a aparecer na tela. */
@Composable
private fun piscar(ms: Int): Boolean {
    val t = rememberInfiniteTransition(label = "pisca")
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(ms, easing = LinearEasing)), label = "v")
    return v < 0.5f
}

@Composable
fun BlocoFx(nome: String, corNome: String?, resto: String, estilo: TextStyle, modifier: Modifier = Modifier) {
    val c = camposFx(resto)
    val f = { i: Int -> c.getOrNull(i).orEmpty() }
    Box(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        when (nome) {
            "sistema" -> fx0(nome, corNome, resto, c, f, estilo)
            "evolucao" -> fx1(nome, corNome, resto, c, f, estilo)
            "atributos" -> fx2(nome, corNome, resto, c, f, estilo)
            "habilidade" -> fx3(nome, corNome, resto, c, f, estilo)
            "missao-ok" -> fx4(nome, corNome, resto, c, f, estilo)
            "missao-falhou" -> fx5(nome, corNome, resto, c, f, estilo)
            "perigo" -> fx6(nome, corNome, resto, c, f, estilo)
            "conquista" -> fx7(nome, corNome, resto, c, f, estilo)
            "classe", "titulo" -> fx8(nome, corNome, resto, c, f, estilo)
            "chefe" -> fx9(nome, corNome, resto, c, f, estilo)
            "andar" -> fx10(nome, corNome, resto, c, f, estilo)
            "bau" -> fx11(nome, corNome, resto, c, f, estilo)
            "ranking" -> fx12(nome, corNome, resto, c, f, estilo)
            "tempo" -> fx13(nome, corNome, resto, c, f, estilo)
            "inimigo" -> fx14(nome, corNome, resto, c, f, estilo)
            "reino" -> fx15(nome, corNome, resto, c, f, estilo)
            "tecnica" -> fx16(nome, corNome, resto, c, f, estilo)
            "seita" -> fx17(nome, corNome, resto, c, f, estilo)
            "pilula" -> fx18(nome, corNome, resto, c, f, estilo)
            "duelo" -> fx19(nome, corNome, resto, c, f, estilo)
            "dantian" -> fx20(nome, corNome, resto, c, f, estilo)
            "tribulacao" -> fx21(nome, corNome, resto, c, f, estilo)
            "rasgada" -> fx22(nome, corNome, resto, c, f, estilo)
            "vhs" -> fx23(nome, corNome, resto, c, f, estilo)
            "regras" -> fx24(nome, corNome, resto, c, f, estilo)
            "foto" -> fx25(nome, corNome, resto, c, f, estilo)
            "arquivo" -> fx26(nome, corNome, resto, c, f, estilo)
            "depoimento" -> fx27(nome, corNome, resto, c, f, estilo)
            "telegrama" -> fx28(nome, corNome, resto, c, f, estilo)
            "linha" -> fx29(nome, corNome, resto, c, f, estilo)
            "prova" -> fx30(nome, corNome, resto, c, f, estilo)
            "profecia" -> fx31(nome, corNome, resto, c, f, estilo)
            "contrato" -> fx32(nome, corNome, resto, c, f, estilo)
            "terminal" -> fx33(nome, corNome, resto, c, f, estilo)
            "holo" -> fx34(nome, corNome, resto, c, f, estilo)
            "corte" -> fx35(nome, corNome, resto, c, f, estilo)
            "scan" -> fx36(nome, corNome, resto, c, f, estilo)
            "codigo" -> fx37(nome, corNome, resto, c, f, estilo)
            "alerta-vermelho" -> fx38(nome, corNome, resto, c, f, estilo)
            "ia" -> fx39(nome, corNome, resto, c, f, estilo)
            "post" -> fx40(nome, corNome, resto, c, f, estilo)
            "jornal" -> fx41(nome, corNome, resto, c, f, estilo)
            "email" -> fx42(nome, corNome, resto, c, f, estilo)
            "ligacao" -> fx43(nome, corNome, resto, c, f, estilo)
            "busca" -> fx44(nome, corNome, resto, c, f, estilo)
            "voz" -> fx45(nome, corNome, resto, c, f, estilo)
            "bloqueio" -> fx46(nome, corNome, resto, c, f, estilo)
            "apagada" -> fx47(nome, corNome, resto, c, f, estilo)
            "digitando" -> fx48(nome, corNome, resto, c, f, estilo)
            "grupo" -> fx49(nome, corNome, resto, c, f, estilo)
            "localizacao" -> fx50(nome, corNome, resto, c, f, estilo)
            "diario" -> fx51(nome, corNome, resto, c, f, estilo)
            "procurado" -> fx52(nome, corNome, resto, c, f, estilo)
            "aviso" -> fx53(nome, corNome, resto, c, f, estilo)
            "salto" -> fx54(nome, corNome, resto, c, f, estilo)
            "capitulo" -> fx55(nome, corNome, resto, c, f, estilo)
            "proximo" -> fx56(nome, corNome, resto, c, f, estilo)
            else -> Text(resto, style = estilo)
        }
    }
}

@Composable
private fun BoxScope.fx0(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome); Painel(cor) { Rot("// " + (if (c.size > 1) f(0) else "Sistema"), cor); Text(estilizar(if (c.size > 1) c.drop(1).joinToString("\n") else f(0)), fontFamily = MONO, fontSize = 14.sp, lineHeight = 20.sp, color = claro(cor)) } }
    }
}

@Composable
private fun BoxScope.fx1(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome)
                Painel(cor) {
                    Rot("// Evolução", cor)
                    val nv = Regex("(\\d+)\\s*>\\s*(\\d+)").find(f(0))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("NÍVEL ", fontSize = 12.sp, letterSpacing = 3.sp, color = claro(cor).copy(alpha = 0.6f))
                        Text(nv?.groupValues?.get(1) ?: "?", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("  →  ", fontSize = 20.sp, color = cor)
                        Text(nv?.groupValues?.get(2) ?: "?", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFC94A), modifier = Modifier.entrar(500))
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(cor.copy(alpha = 0.3f)))
                    c.drop(1).forEachIndexed { k, l ->
                        val m = Regex("^(.+?)\\s+(\\d+)\\s*>\\s*(\\d+)$").find(l)
                        Row(Modifier.fillMaxWidth().entrar(800 + k * 250), verticalAlignment = Alignment.CenterVertically) {
                            Text(m?.groupValues?.get(1) ?: l, fontSize = 13.sp, color = claro(cor), modifier = Modifier.weight(1f))
                            if (m != null) {
                                Text(m.groupValues[2], fontSize = 13.sp, color = claro(cor).copy(alpha = 0.45f), textDecoration = TextDecoration.LineThrough)
                                Text("  →  ", fontSize = 12.sp, color = cor)
                                Text(m.groupValues[3], fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                val d = m.groupValues[3].toInt() - m.groupValues[2].toInt()
                                Text(if (d >= 0) "+$d" else "$d", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A1424),
                                    modifier = Modifier.padding(start = 10.dp).background(if (d >= 0) Color(0xFF6FCF97) else Color(0xFFFF4458)).padding(horizontal = 6.dp, vertical = 1.dp))
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx2(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome)
                val vals = c.mapNotNull { Regex("^([^:]+?)\\s+(\\d+)$").find(it)?.let { m -> m.groupValues[1] to m.groupValues[2].toInt() } }
                Painel(cor) {
                    Rot("// Atributos", cor)
                    val a = remember { Animatable(0f) }; LaunchedEffect(Unit) { a.animateTo(1f, tween(1200, 300, FastOutSlowInEasing)) }
                    val maxV = maxOf(20, vals.maxOfOrNull { it.second } ?: 20)
                    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
                        val n = vals.size.coerceAtLeast(3); val cx = size.width / 2; val cy = size.height / 2; val r = minOf(size.width, size.height) / 2 - 26.dp.toPx()
                        fun pt(i: Int, rr: Float) = Offset(cx + cos(-PI / 2 + i * 2 * PI / n).toFloat() * rr, cy + sin(-PI / 2 + i * 2 * PI / n).toFloat() * rr)
                        for (k in 1..4) { val pth = Path(); (0 until n).forEach { i -> val p0 = pt(i, r * k / 4); if (i == 0) pth.moveTo(p0.x, p0.y) else pth.lineTo(p0.x, p0.y) }; pth.close(); drawPath(pth, cor.copy(alpha = if (k == 4) .5f else .2f), style = Stroke(1f)) }
                        (0 until n).forEach { i -> drawLine(cor.copy(alpha = .22f), Offset(cx, cy), pt(i, r)) }
                        val pv = Path(); vals.forEachIndexed { i, (_, v) -> val p0 = pt(i, r * v / maxV * a.value); if (i == 0) pv.moveTo(p0.x, p0.y) else pv.lineTo(p0.x, p0.y) }; pv.close()
                        drawPath(pv, cor.copy(alpha = .3f)); drawPath(pv, cor, style = Stroke(2.dp.toPx()))
                    }
                    Text(vals.joinToString("   ") { "${it.first} ${it.second}" }, fontFamily = MONO, fontSize = 12.sp, color = claro(cor), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    c.filter { Regex("^([^:]+?)\\s+(\\d+)$").find(it) == null }.forEach { Text(it, fontFamily = MONO, fontSize = 12.sp, color = Color(0xFFFFC94A), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                }
            }
    }
}

@Composable
private fun BoxScope.fx3(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome)
                val desc = c.drop(1).lastOrNull { it.length > 18 }
                val tags = c.drop(1).filter { it != desc }
                Painel(cor) {
                    Rot("Habilidade " + tags.take(2).joinToString(" · "), cor)
                    Text(f(0), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    desc?.let { Text(estilizar(it), fontSize = 13.5.sp, lineHeight = 19.sp, color = claro(cor).copy(alpha = 0.8f)) }
                    Linha { tags.drop(2).forEachIndexed { k, t -> Chip(t, cor, 300 + k * 150) } }
                }
            }
    }
}

@Composable
private fun BoxScope.fx4(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome, Color(0xFFFFC94A))
                Painel(cor, Modifier.entrar()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).rotate(45f).background(cor), contentAlignment = Alignment.Center) { Text("✓", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 20.sp, modifier = Modifier.rotate(-45f)) }
                        Column(Modifier.padding(start = 16.dp)) {
                            Rot("Missão concluída", cor); Text(f(0), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
                            Linha { c.drop(1).forEachIndexed { k, t -> Chip(t, cor, 600 + k * 250) } }
                        }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx5(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome, Color(0xFFFF3B4E))
                val treme = remember { Animatable(0f) }
                LaunchedEffect(Unit) { treme.animateTo(1f, tween(600)) }
                Painel(cor, Modifier.graphicsLayer { translationX = sin(treme.value * 20f) * (1 - treme.value) * 14f * density }) {
                    Rot("// Sistema", cor); Text("MISSÃO FALHOU", fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = cor)
                    Text(f(0), fontFamily = MONO, fontSize = 13.sp, color = claro(cor)); if (c.size > 1) Text("Penalidade: " + c.drop(1).joinToString(" · "), fontFamily = MONO, fontSize = 13.sp, color = claro(cor).copy(alpha = 0.75f))
                }
            }
    }
}

@Composable
private fun BoxScope.fx6(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome, Color(0xFFFF3B4E))
                val t = rememberInfiniteTransition(label = "perigo"); val desl by t.animateFloat(0f, 28f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "d")
                val alarme by t.animateFloat(0.05f, 0.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
                Column(Modifier.fillMaxWidth().background(cor.copy(alpha = alarme)).border(1.dp, cor.copy(alpha = 0.6f))) {
                    val faixa = Modifier.fillMaxWidth().height(12.dp).drawBehind { var x = -28f + desl; while (x < size.width) { val p = Path().apply { moveTo(x, size.height); lineTo(x + 14f, 0f); lineTo(x + 28f, 0f); lineTo(x + 14f, size.height); close() }; drawPath(p, Color(0xFFFFD000)); x += 28f } }.background(Color(0xFF111111))
                    Box(faixa)
                    Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PERIGO", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp, color = cor)
                        c.forEach { Text(estilizar(it), fontFamily = MONO, fontSize = 13.sp, color = claro(cor), textAlign = TextAlign.Center) }
                    }
                    Box(faixa)
                }
            }
    }
}

@Composable
private fun BoxScope.fx7(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome, Color(0xFFFFC94A))
                Row(Modifier.entrar(dy = 0f).graphicsLayer { }.background(Color(0xFF14141A)).border(1.dp, cor.copy(alpha = 0.4f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(cor), contentAlignment = Alignment.Center) { Text("🏆", fontSize = 20.sp) }
                    Column(Modifier.padding(start = 12.dp, end = 10.dp)) {
                        Rot("Conquista desbloqueada", cor); Text(f(0), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        if (c.size > 1) Text(c.drop(1).joinToString(" · "), fontSize = 12.sp, color = cor)
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx8(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome, if (nome == "titulo") Color(0xFFFFC94A) else CORES_FX["roxo"]!!)
                val gira = rememberInfiniteTransition(label = "raios").animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "g")
                Box(Modifier.fillMaxWidth().background(mix(cor, 0.08f)).border(1.dp, cor.copy(alpha = 0.4f)).padding(vertical = 18.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.matchParentSize()) { val cx = size.width / 2; val cy = size.height * .45f; for (k in 0 until 18) { val a = Math.toRadians((gira.value + k * 20).toDouble()); drawLine(cor.copy(alpha = .14f), Offset(cx, cy), Offset(cx + cos(a).toFloat() * size.width, cy + sin(a).toFloat() * size.width), 8f) } }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Rot(if (nome == "titulo") "Título obtido" else "Classe obtida", cor)
                        if (nome == "classe") Box(Modifier.entrar(300).size(64.dp).clip(Hexagono).background(Brush.radialGradient(listOf(claro(cor), cor, mix(cor, .4f)))), contentAlignment = Alignment.Center) { Text("⚔", fontSize = 28.sp, color = Color.White) }
                        Text(f(0), fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (nome == "titulo") Color(0xFF1A0A00) else Color.White, textAlign = TextAlign.Center,
                            modifier = Modifier.entrar(600).then(if (nome == "titulo") Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFFFC94A), Color(0xFFFF6A2B)))).padding(horizontal = 18.dp, vertical = 8.dp) else Modifier))
                        Linha { c.drop(1).forEachIndexed { k, t -> Chip(t, cor, 900 + k * 200) } }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx9(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome, Color(0xFFFF3B4E))
                val fases = Regex("(\\d+)\\s*fase").find(resto)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val t = rememberInfiniteTransition(label = "chefe"); val bat by t.animateFloat(0.2f, 0.6f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "b")
                val hp = remember { Animatable(0f) }; LaunchedEffect(Unit) { hp.animateTo(1f, tween(1600, 400, FastOutSlowInEasing)) }
                Column(Modifier.fillMaxWidth().background(Brush.radialGradient(listOf(mix(cor, bat), ESC), radius = 900f)).border(1.dp, cor.copy(alpha = 0.6f)).padding(16.dp)) {
                    Text("CHEFE À VISTA", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, color = cor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("BOSS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.background(cor).padding(horizontal = 6.dp, vertical = 2.dp))
                        Text("  " + f(0).uppercase(), fontSize = 19.sp, fontWeight = FontWeight.Black, color = claro(cor), modifier = Modifier.weight(1f))
                        Text(c.getOrNull(1).orEmpty(), fontFamily = MONO, fontSize = 12.sp, color = cor)
                    }
                    Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(14.dp).background(mix(cor, .15f)).border(1.dp, cor.copy(alpha = .5f))) {
                        Box(Modifier.fillMaxWidth(hp.value).fillMaxHeight().background(Brush.horizontalGradient(listOf(mix(cor, .6f), cor))))
                        Canvas(Modifier.matchParentSize()) { for (k in 1 until fases) drawRect(ESC, Offset(size.width * k / fases, 0f), Size(2.dp.toPx(), size.height)) }
                    }
                    Text("Fase 1 de $fases", fontFamily = MONO, fontSize = 12.sp, color = claro(cor).copy(alpha = .7f), modifier = Modifier.padding(top = 6.dp))
                }
            }
    }
}

@Composable
private fun BoxScope.fx10(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val alvo = f(0).filter { it.isDigit() }.toIntOrNull() ?: 1
                var n by remember { mutableIntStateOf(maxOf(1, alvo - 9)) }
                LaunchedEffect(alvo) { while (n < alvo) { delay(120); n++ } }
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF1B1A1F), Color(0xFF0C0B0E)))).border(1.dp, Color(0xFF3A3740)).padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ANDAR", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, color = Color(0xFF9A96A3))
                    Text("$n", fontSize = 72.sp, lineHeight = 76.sp, fontWeight = FontWeight.Black, color = Color(0xFFF4EEE2))
                    if (c.size > 1) Text(c[1], fontStyle = FontStyle.Italic, fontFamily = FontFamily.Serif, fontSize = 15.sp, color = Color(0xFFC9C3B4), modifier = Modifier.entrar(1200))
                }
            }
    }
}

@Composable
private fun BoxScope.fx11(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome, Color(0xFFFFC94A))
                val abre = remember { Animatable(0f) }; LaunchedEffect(Unit) { delay(400); abre.animateTo(1f, spring(0.45f, 200f)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(92.dp, 84.dp)) {
                        Box(Modifier.align(Alignment.BottomCenter).padding(horizontal = 4.dp).fillMaxWidth().height(46.dp).background(Brush.verticalGradient(listOf(Color(0xFF6B4218), Color(0xFF3A220C)))).border(2.dp, cor))
                        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp).size(12.dp, 14.dp).background(cor))
                        Box(Modifier.align(Alignment.TopCenter).padding(top = 6.dp).fillMaxWidth(0.75f).height(30.dp).graphicsLayer { alpha = abre.value * 0.8f }
                            .background(Brush.verticalGradient(listOf(Color.Transparent, cor.copy(alpha = 0.8f)))))
                        Box(Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 4.dp, end = 4.dp).fillMaxWidth().height(22.dp)
                            .graphicsLayer { transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f); rotationZ = -28f * abre.value; translationY = -6f * abre.value * density }
                            .background(Brush.verticalGradient(listOf(Color(0xFF8A5A20), Color(0xFF5A3410)))).border(2.dp, cor))
                    }
                    Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Rot("// " + f(0) + " aberto", cor)
                        c.drop(1).forEachIndexed { k, t -> Text("+ $t", fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, modifier = Modifier.entrar(1100 + k * 250)) }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx12(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Rot("// Ranking " + f(0), cor, Modifier.padding(bottom = 4.dp))
                    c.drop(1).forEachIndexed { k, l ->
                        val m = Regex("^(\\d+)\\s+(.+?)\\s+([\\d.,]+)\\s*(.*)$").find(l)
                        val eu = m?.groupValues?.get(2)?.lowercase() in setOf("você", "voce", "eu")
                        Row(Modifier.fillMaxWidth().entrar(200 + k * 200, 0f).background(cor.copy(alpha = if (eu) 0.22f else 0.07f)).then(if (eu) Modifier.border(1.dp, cor) else Modifier)
                            .padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(m?.groupValues?.get(1) ?: "", fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = cor, modifier = Modifier.width(28.dp))
                            Text(m?.groupValues?.get(2) ?: l, fontFamily = MONO, fontSize = 13.sp, color = Color.White, modifier = Modifier.weight(1f))
                            Text(m?.groupValues?.get(3) ?: "", fontFamily = MONO, fontSize = 13.sp, color = Color.White)
                            val mv = m?.groupValues?.get(4).orEmpty()
                            Text(when { mv.startsWith("↑") || mv.startsWith("+") -> "▲ " + mv.drop(1); mv.startsWith("↓") || mv.startsWith("-") -> "▼ " + mv.drop(1); mv.isNotBlank() -> mv.uppercase(); else -> "" },
                                fontFamily = MONO, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(56.dp),
                                color = when { mv.startsWith("↑") || mv.startsWith("+") -> Color(0xFF3DDC84); mv.startsWith("↓") || mv.startsWith("-") -> Color(0xFFFF4458); else -> Color(0xFFFFC94A) })
                        }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx13(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome)
                val ini = f(0).split(':').mapNotNull { it.trim().toIntOrNull() }.fold(0) { a, b -> a * 60 + b }
                var s by remember { mutableIntStateOf(ini) }
                LaunchedEffect(ini) { while (s > 0) { delay(1000); s-- } }
                val pouco = s <= 5; val pisca = piscar(500)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Rot("// Tempo restante", cor)
                    Text("%02d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60), fontFamily = MONO, fontWeight = FontWeight.Black, fontSize = 46.sp,
                        color = if (pouco) Color(0xFFFF3B4E).copy(alpha = if (pisca) 1f else 0.5f) else Color.White)
                    c.drop(1).forEach { Text(it, fontSize = 12.sp, color = Color(0xFF8E8A93)) }
                }
            }
    }
}

@Composable
private fun BoxScope.fx14(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val cor = corFx(corNome, Color(0xFFFF3B4E))
                Painel(cor) {
                    Row {
                        Box(Modifier.size(80.dp, 92.dp).background(Color(0xFF140A0C)).border(1.dp, cor.copy(alpha = .5f))) {
                            Canvas(Modifier.fillMaxSize().padding(10.dp)) { val w = size.width; val h = size.height
                                val p = Path().apply { moveTo(w * .1f, h * .8f); lineTo(w * .28f, h * .25f); lineTo(w * .42f, h * .42f); lineTo(w * .58f, h * .42f); lineTo(w * .72f, h * .25f); lineTo(w * .9f, h * .8f); lineTo(w * .65f, h * .9f); lineTo(w * .35f, h * .9f); close() }
                                drawPath(p, Color(0xFF2A1418)); drawPath(p, cor, style = Stroke(1.5f)); drawRect(Color(0xFFFF3B4E), Offset(w * .36f, h * .56f), Size(w * .08f, h * .04f)); drawRect(Color(0xFFFF3B4E), Offset(w * .56f, h * .56f), Size(w * .08f, h * .04f)) }
                            c.firstOrNull { it.startsWith("Nv", true) }?.let { Text(it, fontFamily = MONO, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(cor)) }
                        }
                        Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Rot("// Inimigo identificado", cor); Text(f(0), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Linha { c.filter { it.startsWith("fraco", true) || it.startsWith("forte", true) }.forEach { t ->
                                val cc = if (t.startsWith("fraco", true)) Color(0xFF7FD3FF) else Color(0xFFFF6A2B); Text(t, fontFamily = MONO, fontSize = 11.sp, color = cc, modifier = Modifier.border(1.dp, cc).padding(horizontal = 7.dp, vertical = 2.dp)) } }
                            c.drop(1).filter { !it.startsWith("Nv", true) && !it.startsWith("fraco", true) && !it.startsWith("forte", true) }.forEach { Text(estilizar(it), fontSize = 12.5.sp, color = Color(0xFF9E99A8)) }
                        }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx15(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val reinos = f(0).split('>').map { it.trim() }.filter { it.isNotEmpty() }
                val atual = (c.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: reinos.size).coerceIn(1, reinos.size.coerceAtLeast(1))
                val linha = remember { Animatable(0f) }; LaunchedEffect(Unit) { linha.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
                val t = rememberInfiniteTransition(label = "reino"); val onda by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200)), label = "o")
                Column {
                    Box(Modifier.fillMaxWidth()) {
                        Canvas(Modifier.fillMaxWidth().height(34.dp)) { val n = reinos.size; val passo = size.width / n; val y = 16.dp.toPx()
                            drawLine(Color(0xFF2B2B36), Offset(passo / 2, y), Offset(size.width - passo / 2, y), 2.dp.toPx())
                            drawLine(Color(0xFFFFC94A), Offset(passo / 2, y), Offset(passo / 2 + (passo * (atual - 1)) * linha.value, y), 2.dp.toPx())
                            reinos.indices.forEach { i -> val x = passo / 2 + passo * i; val ok = i + 1 < atual; val at = i + 1 == atual; val s = 26.dp.toPx()
                                if (at) drawRect(Color(0xFFFF6A2B).copy(alpha = (1 - onda) * .6f), Offset(x - s / 2 - onda * 14f, y - s / 2 - onda * 14f), Size(s + onda * 28f, s + onda * 28f), style = Stroke(2f))
                                drawRect(if (ok) Color(0xFFFFC94A) else if (at) Color(0xFFFF6A2B) else Color(0xFF1A1A22), Offset(x - s / 2, y - s / 2), Size(s, s))
                                drawRect(if (ok || at) Color(0xFFFFE6A8) else Color(0xFF3A3A46), Offset(x - s / 2, y - s / 2), Size(s, s), style = Stroke(2f)) }
                        }
                    }
                    Row(Modifier.fillMaxWidth()) { reinos.forEachIndexed { i, r -> Text(r, fontSize = 11.sp, lineHeight = 13.sp, textAlign = TextAlign.Center, fontWeight = if (i + 1 == atual) FontWeight.Black else FontWeight.Normal,
                        color = if (i + 1 == atual) Color.White else Color(0xFF8E8A93), modifier = Modifier.weight(1f).padding(top = 6.dp, start = 2.dp, end = 2.dp)) } }
                    c.drop(2).forEach { Text(estilizar(it), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = Color(0xFFFFE6A8), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp).entrar(1100)) }
                }
            }
    }
}

@Composable
private fun BoxScope.fx16(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF171310), Color(0xFF0B0907)))).border(1.dp, Color(0xFF6B5420)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Rot("Técnica marcial", Color(0xFFC9A24A))
                Text(f(0), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFFFFE6A8))
                val formas = c.drop(1).filter { it.contains("forma", true) || it == "???" || it.contains("?") }
                c.drop(1).filter { it !in formas }.let { gs -> if (gs.isNotEmpty()) Linha { gs.forEach { Chip(it, Color(0xFFFFC94A)) } } }
                formas.forEachIndexed { k, fo -> Text("${k + 1}ª Forma · " + (if (fo.contains("?")) "Nome ainda desconhecido" else fo.replace(Regex("^forma\\s*\\d*\\s*[:·-]?\\s*", RegexOption.IGNORE_CASE), "")),
                    fontSize = 13.5.sp, color = Color(0xFFD4CDB8), modifier = Modifier.entrar(300 + k * 200).then(if (fo.contains("?")) Modifier.blur(4.dp) else Modifier)) }
            }
    }
}

@Composable
private fun BoxScope.fx17(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Row(verticalAlignment = Alignment.CenterVertically) {
                val bal = rememberInfiniteTransition(label = "bal").animateFloat(-1.5f, 1.5f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "b")
                val emb = f(0).firstOrNull { it.code in 0x4E00..0x9FFF } ?: f(0).firstOrNull() ?: '?'
                Box(Modifier.size(90.dp, 120.dp).graphicsLayer { rotationZ = bal.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f) }.clip(Estandarte)
                    .background(Brush.verticalGradient(listOf(Color(0xFF9A1C1C), Color(0xFF5A0D0D)))), contentAlignment = Alignment.Center) {
                    Text("$emb", fontSize = 48.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = Color(0xFFFFD36B))
                }
                Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Seita", "Líder", "Lema").zip(c).forEach { (r, v) -> Column { Rot(r, Color(0xFFFF6A2B)); Text(v.replace(Regex("[\\u4E00-\\u9FFF]"), "").trim(), fontSize = if (r == "Seita") 20.sp else 15.sp,
                        fontFamily = if (r == "Seita") FontFamily.Serif else null, fontWeight = FontWeight.Bold, fontStyle = if (r == "Lema") FontStyle.Italic else null, color = if (r == "Seita") Color(0xFFFFE6A8) else Color.White) } }
                }
            }
    }
}

@Composable
private fun BoxScope.fx18(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Row(verticalAlignment = Alignment.CenterVertically) {
                val t = rememberInfiniteTransition(label = "pil"); val g by t.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "g"); val pul by t.animateFloat(1f, 1.08f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "p")
                Box(Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(72.dp).rotate(g).border(1.5.dp, Color(0x99A0FFD7)))
                    Box(Modifier.size(54.dp).rotate(-g * 1.4f).border(1.dp, Color(0x55A0FFD7)))
                    Box(Modifier.size(44.dp).graphicsLayer { scaleX = pul; scaleY = pul }.clip(Octogono).background(Brush.radialGradient(listOf(Color(0xFFE6FFF4), Color(0xFF4FD6A0), Color(0xFF0D6B4A)))))
                }
                Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Rot("Pílula" + (c.getOrNull(1)?.let { " · $it" } ?: ""), Color(0xFF6FE8B4)); Text(f(0), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    c.drop(2).forEachIndexed { k, e -> Text("+ $e", fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB8FFE0), modifier = Modifier.entrar(400 + k * 200)) }
                }
            }
    }
}

@Composable
private fun BoxScope.fx19(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val m = Regex("^(.+?)\\s*(\\d+)?%?\\s+vs\\s+(.+?)\\s*(\\d+)?%?$", RegexOption.IGNORE_CASE).find(f(0))
                val a = m?.groupValues?.get(1)?.trim() ?: f(0); val b = m?.groupValues?.get(3)?.trim() ?: f(1)
                val pa = (m?.groupValues?.get(2)?.toIntOrNull() ?: 50) / 100f; val pb = (m?.groupValues?.get(4)?.toIntOrNull() ?: 50) / 100f
                val t = rememberInfiniteTransition(label = "vs"); val vs by t.animateFloat(0.92f, 1.14f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "v")
                Box(Modifier.fillMaxWidth().height(150.dp)) {
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.weight(1f).fillMaxHeight().background(Brush.linearGradient(listOf(Color(0xFF3A1204), Color(0xFFA23A0D)))).padding(14.dp), verticalArrangement = Arrangement.Bottom) {
                            Text(a.uppercase(), fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 18.sp, color = Color.White)
                            Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(7.dp).background(Color(0x73000000))) { Box(Modifier.fillMaxWidth(pa).fillMaxHeight().background(Color(0xFFFFC94A))) }
                        }
                        Column(Modifier.weight(1f).fillMaxHeight().background(Brush.linearGradient(listOf(Color(0xFF0D5A99), Color(0xFF06203A)))).padding(14.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                            Text(b.uppercase(), fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 18.sp, color = Color.White, textAlign = TextAlign.End)
                            Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(7.dp).background(Color(0x73000000)), contentAlignment = Alignment.CenterEnd) { Box(Modifier.fillMaxWidth(pb).fillMaxHeight().background(Color(0xFF8FE4FF))) }
                        }
                    }
                    Text("DUELO", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
                    Text("VS", fontSize = 34.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, color = Color.White, modifier = Modifier.align(Alignment.Center).graphicsLayer { scaleX = vs; scaleY = vs })
                }
            }
    }
}

@Composable
private fun BoxScope.fx20(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val pc = (f(0).filter { it.isDigit() }.toIntOrNull() ?: 50).coerceIn(0, 100) / 100f
                val a = remember { Animatable(0f) }; LaunchedEffect(Unit) { a.animateTo(pc, tween(2000, 300, FastOutSlowInEasing)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp, 120.dp).clip(Frasco).background(Color(0xFF0D0A07)).border(2.dp, Color(0xFFC9A24A), Frasco)) {
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(a.value).background(Brush.verticalGradient(listOf(Color(0xFFFFE08A), Color(0xFFC98A1A)))))
                    }
                    Column(Modifier.padding(start = 18.dp)) { Rot("Dantian", Color(0xFFC9A24A)); Text("${(a.value * 100).toInt()}%", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFE08A)); c.drop(1).forEach { Text(it, fontSize = 13.sp, color = Color(0xFFC9BFA6)) } }
                }
            }
    }
}

@Composable
private fun BoxScope.fx21(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val m = Regex("(\\d+)\\s*/\\s*(\\d+)").find(f(0)); val atual = m?.groupValues?.get(1)?.toIntOrNull() ?: 1; val total = (m?.groupValues?.get(2)?.toIntOrNull() ?: 9).coerceIn(1, 12)
                val pisca = piscar(1600)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Rot("Tribulação celestial", Color(0xFFC59BFF))
                    Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..total).forEach { k -> Canvas(Modifier.size(18.dp, 26.dp)) { val w = size.width; val h = size.height
                            val p = Path().apply { moveTo(w * .6f, 0f); lineTo(w * .1f, h * .56f); lineTo(w * .45f, h * .56f); lineTo(w * .35f, h); lineTo(w * .9f, h * .4f); lineTo(w * .55f, h * .4f); close() }
                            drawPath(p, when { k < atual -> Color(0xFFE7D9FF); k == atual -> if (pisca) Color.White else Color(0xFF2A2238); else -> Color(0xFF2A2238) }) } }
                    }
                    Text("$atual / $total", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFFE7D9FF))
                    c.drop(1).forEach { Text(estilizar(it), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = Color(0xFFBFB3D8), textAlign = TextAlign.Center) }
                }
            }
    }
}

@Composable
private fun BoxScope.fx22(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Box(Modifier.fillMaxWidth().rotate(-1.4f).clip(Rasgado).background(Brush.verticalGradient(listOf(Color(0xFFE6D9BC), Color(0xFFCDBB92)))).padding(horizontal = 22.dp, vertical = 26.dp)) {
                Text(estilizar(c.joinToString("\n")), fontFamily = FontFamily.Cursive, fontSize = 22.sp, lineHeight = 28.sp, color = Color(0xFF3A2418))
            }
    }
}

@Composable
private fun BoxScope.fx23(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val ts = f(0); var seg by remember { mutableIntStateOf(0) }; LaunchedEffect(Unit) { while (true) { delay(1000); seg++ } }
                val t = rememberInfiniteTransition(label = "vhs"); val faixa by t.animateFloat(-0.2f, 1.1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "f"); val rec = piscar(1100)
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF1A2430), Color(0xFF0C1118)))).drawBehind {
                    var y = 0f; while (y < size.height) { drawRect(Color(0x47000000), Offset(0f, y), Size(size.width, 1f)); y += 3f }
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x2EFFFFFF), Color.Transparent), startY = size.height * faixa, endY = size.height * faixa + 34.dp.toPx()), Offset(0f, size.height * faixa), Size(size.width, 34.dp.toPx()))
                }.padding(16.dp)) {
                    val sombra = TextStyle(fontFamily = MONO, color = Color(0xFFE8F4FF), shadow = androidx.compose.ui.graphics.Shadow(Color(0xB3FF003C), Offset(2f, 0f)))
                    Row(Modifier.fillMaxWidth()) { Text("PLAY ▶", style = sombra, fontSize = 16.sp, modifier = Modifier.weight(1f)); Text("● REC", style = sombra, fontSize = 16.sp, color = Color(0xFFFF5060).copy(alpha = if (rec) 1f else 0f)) }
                    Text(estilizar(c.drop(1).joinToString("\n")), style = sombra, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(vertical = 20.dp))
                    val hm = Regex("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?").find(ts)
                    val base = hm?.let { it.groupValues[1].toInt() * 3600 + it.groupValues[2].toInt() * 60 + (it.groupValues[3].toIntOrNull() ?: 0) } ?: 0
                    val agora = base + seg
                    Text(if (hm != null) ts.replace(hm.value, "%02d:%02d:%02d".format(agora / 3600 % 24, agora / 60 % 60, agora % 60)) else ts, style = sombra, fontSize = 16.sp, letterSpacing = 1.5.sp)
                }
            }
    }
}

@Composable
private fun BoxScope.fx24(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val regras = c.drop(1)
                var vez by remember { mutableIntStateOf(0) }; LaunchedEffect(Unit) { while (true) { delay(2200); vez++ } }
                Column(Modifier.fillMaxWidth().background(Color(0xFFE9E4D8)).padding(18.dp)) {
                    Text(f(0).uppercase(), fontFamily = MONO, fontSize = 13.sp, letterSpacing = 2.sp, color = Color(0xFF1E1A14), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(2.dp).background(Color(0xFF1E1A14)))
                    regras.forEachIndexed { k, r ->
                        val alt = r.split(" / ").map { it.trim() }
                        val txt = if (alt.size > 1) alt[vez % alt.size] else r
                        Text(estilizar(if (Regex("^\\d+[.)]").containsMatchIn(txt)) txt else "${k + 1}. $txt"), fontFamily = MONO, fontSize = 14.sp, lineHeight = 21.sp,
                            color = if (alt.size > 1) Color(0xFF8A0F12) else Color(0xFF1E1A14), modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx25(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val t = rememberInfiniteTransition(label = "foto"); val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "v")
                val vulto = when { v < .4f -> 0f; v < .55f -> (v - .4f) / .15f; v < .8f -> 1f; v < .85f -> 1 - (v - .8f) / .05f; else -> 0f } * 0.9f
                Column(Modifier.align(Alignment.Center).width(230.dp).rotate(-2f).background(Color(0xFFF1EBDD)).padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp)) {
                    Canvas(Modifier.fillMaxWidth().height(150.dp)) { val w = size.width; val h = size.height
                        drawRect(Brush.verticalGradient(listOf(Color(0xFF6B6150), Color(0xFF3A342A), Color(0xFF2A251D))))
                        drawRect(Color(0xFFD9CFAE), Offset(w * .16f, h * .22f), Size(34.dp.toPx(), 46.dp.toPx())); drawRect(Color(0xFFD9CFAE), Offset(w * .6f, h * .22f), Size(34.dp.toPx(), 46.dp.toPx()))
                        val x = w * .6f + 9.dp.toPx(); val y = h * .22f + 10.dp.toPx()
                        drawRect(Color(0xFF0D0B08).copy(alpha = vulto), Offset(x + 4.dp.toPx(), y), Size(8.dp.toPx(), 9.dp.toPx())); drawRect(Color(0xFF0D0B08).copy(alpha = vulto), Offset(x, y + 9.dp.toPx()), Size(16.dp.toPx(), 26.dp.toPx()))
                        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0x8C000000)), radius = w * .7f))
                    }
                    Text(f(0), fontFamily = FontFamily.Cursive, fontSize = 20.sp, color = Color(0xFF3A2A18), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
    }
}

@Composable
private fun BoxScope.fx26(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Box(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Box(Modifier.offset(y = (-12).dp).fillMaxWidth(0.44f).height(14.dp).background(Color(0xFFD8C28E)))
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFD8C28E), Color(0xFFC9B27A)))).padding(18.dp)) {
                    Text(f(0).uppercase(), fontFamily = MONO, fontSize = 12.sp, letterSpacing = 2.sp, color = Color(0xFF2A2218), modifier = Modifier.padding(bottom = 8.dp))
                    c.drop(1).forEach { l -> TextoTarjado(l) }
                    Text("(toque nas tarjas)", fontFamily = MONO, fontSize = 10.sp, color = Color(0xB32A2218), modifier = Modifier.padding(top = 8.dp))
                }
                Text("CONFIDENCIAL", fontFamily = MONO, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFB01E1E), letterSpacing = 1.5.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 12.dp).entrar(400, 0f).rotate(-12f).border(3.dp, Color(0xFFB01E1E)).padding(horizontal = 8.dp, vertical = 3.dp))
            }
    }
}

@Composable
private fun BoxScope.fx27(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFE8E6E0), Color(0xFFD2CFC6)))).padding(16.dp)) {
                val grava = piscar(1200)
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("POLÍCIA · SALA 2", fontFamily = MONO, fontSize = 11.sp, letterSpacing = 1.5.sp, color = Color(0xFF25252B), modifier = Modifier.weight(1f)); Text("● GRAVANDO", fontFamily = MONO, fontSize = 11.sp, color = Color(0xFFB01E1E).copy(alpha = if (grava) 1f else 0f)) }
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF25252B)))
                c.forEachIndexed { k, l -> val d = l.indexOf(':')
                    Text(buildAnnotatedString { if (d > 0) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (k % 2 == 0) Color(0xFF13307A) else Color(0xFF7A1010))) { append(l.substring(0, d + 1)) }; append(if (d > 0) l.substring(d + 1) else l) },
                        fontFamily = MONO, fontSize = 14.sp, lineHeight = 22.sp, color = Color(0xFF25252B), modifier = Modifier.padding(top = 8.dp).entrar(300 + k * 900)) }
            }
    }
}

@Composable
private fun BoxScope.fx28(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val txt = c.joinToString(" ").uppercase(); var n by remember { mutableIntStateOf(0) }; LaunchedEffect(txt) { while (n < txt.length) { delay(45); n++ } }
                Column(Modifier.fillMaxWidth().rotate(0.8f).background(Brush.verticalGradient(listOf(Color(0xFFEAD9A4), Color(0xFFD9C688))))) {
                    Text("TELÉGRAFO NACIONAL · TELEGRAMA", fontFamily = MONO, fontSize = 10.sp, letterSpacing = 2.sp, color = Color(0xFFF1DFB4), modifier = Modifier.fillMaxWidth().background(Color(0xFF8A1C1C)).padding(horizontal = 10.dp, vertical = 6.dp))
                    Text(buildAnnotatedString { val vis = txt.take(n); var k = 0; Regex("STOP|PT").findAll(vis).forEach { m -> append(vis.substring(k, m.range.first)); withStyle(SpanStyle(color = Color(0xFF8A1C1C), fontWeight = FontWeight.Bold)) { append(m.value) }; k = m.range.last + 1 }; append(vis.substring(k)); if (n < txt.length) append("▌") },
                        fontFamily = MONO, fontSize = 15.sp, lineHeight = 24.sp, color = Color(0xFF2A2418), modifier = Modifier.padding(18.dp))
                }
            }
    }
}

@Composable
private fun BoxScope.fx29(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.fillMaxWidth().padding(start = 4.dp)) {
                val pisca = piscar(800)
                c.forEachIndexed { k, l -> val m = Regex("^(\\d{1,2}[:h]\\d{2})\\s+(.*)$").find(l); val gap = l.contains("???")
                    Row(Modifier.entrar(200 + k * 350).height(IntrinsicSize.Min)) {
                        Box(Modifier.width(14.dp).fillMaxHeight()) { Box(Modifier.align(Alignment.TopCenter).width(2.dp).fillMaxHeight().background(Color(0xFFB8975A))); Box(Modifier.align(Alignment.TopCenter).padding(top = 5.dp).size(10.dp).background(if (gap) Color(0xFFFF4458).copy(alpha = if (pisca) 1f else .3f) else Color(0xFFB8975A))) }
                        Text(buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (gap) Color(0xFFFF4458) else Color(0xFFE8D7AE))) { append(m?.groupValues?.get(1) ?: ""); append("  ") }; withStyle(SpanStyle(color = if (gap) Color(0xFFFF8A96) else Color(0xFFC9BFA6), fontStyle = if (gap) FontStyle.Italic else null)) { append(m?.groupValues?.get(2) ?: l) } },
                            fontFamily = MONO, fontSize = 13.5.sp, modifier = Modifier.padding(start = 10.dp, bottom = 14.dp))
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx30(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Box(Modifier.align(Alignment.Center).width(270.dp).background(Color(0x14E6F0FF)).border(1.dp, Color(0x4DE6F0FF)).padding(start = 14.dp, end = 14.dp, top = 26.dp, bottom = 14.dp)) {
                Column(Modifier.fillMaxWidth().background(Color(0xFFF2EBD9)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("PROVA " + f(0).uppercase(), fontFamily = MONO, fontSize = 10.sp, letterSpacing = 1.5.sp, color = Color(0xFF8A1C1C))
                    Text(f(1), fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1A14))
                    c.drop(2).forEach { Text(it, fontFamily = MONO, fontSize = 12.sp, color = Color(0xFF1E1A14)) }
                }
                Text("EVIDÊNCIA", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = Color(0xFFB01E1E), modifier = Modifier.align(Alignment.TopEnd).offset(y = 6.dp).entrar(500, 0f).rotate(8f).border(2.dp, Color(0xFFB01E1E)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
    }
}

@Composable
private fun BoxScope.fx31(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.fillMaxWidth().background(Brush.radialGradient(listOf(Color(0xFF1B2A55), Color(0xFF080A1C)), radius = 700f)).border(1.dp, Color(0x66A0C8FF)).padding(horizontal = 18.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("A PROFECIA", fontFamily = FontFamily.Serif, fontSize = 11.sp, letterSpacing = 5.sp, color = Color(0xFF8FB4FF), modifier = Modifier.padding(bottom = 12.dp))
                c.forEachIndexed { k, v -> Text(estilizar(v), fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 28.sp, color = Color(0xFFCFE4FF), textAlign = TextAlign.Center, modifier = Modifier.entrar(400 + k * 1400)) }
                Text("— ◇ —", color = Color(0xFF8FB4FF), letterSpacing = 6.sp, modifier = Modifier.padding(top = 12.dp).entrar(400 + c.size * 1400))
            }
    }
}

@Composable
private fun BoxScope.fx32(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val traco = remember { Animatable(0f) }; LaunchedEffect(Unit) { delay(500); traco.animateTo(1f, tween(2200, easing = FastOutSlowInEasing)) }
                Column(Modifier.fillMaxWidth().background(Brush.radialGradient(listOf(Color(0xFFF1DFB4), Color(0xFFCDAE70)), radius = 900f)).padding(18.dp)) {
                    Text("PACTO", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 3.sp, color = Color(0xFF2F1E0C), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(estilizar(f(0)), fontFamily = FontFamily.Serif, fontSize = 15.sp, lineHeight = 22.sp, color = Color(0xFF2F1E0C), modifier = Modifier.padding(vertical = 10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(f(1), fontFamily = FontFamily.Cursive, fontSize = 30.sp, color = Color(0xFF8A0F12), modifier = Modifier.weight(1f).drawWithClip(traco.value))
                        Box(Modifier.size(46.dp).graphicsLayer { val s = ((traco.value - .9f) * 10).coerceIn(0f, 1f); alpha = s; scaleX = 2 - s; scaleY = 2 - s }.clip(Lacre).background(Color(0xFF8A0F12)), contentAlignment = Alignment.Center) {
                            Text(f(1).take(1).uppercase(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, color = Color(0xFFFFD9C9)) }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx33(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { var mostradas by remember { mutableIntStateOf(0) }; LaunchedEffect(Unit) { while (mostradas < c.size) { delay(600); mostradas++ } }
                val cur = piscar(1000)
                Column(Modifier.fillMaxWidth().background(Color(0xFF050A07)).border(1.dp, Color(0xFF1D3A26))) {
                    Text("■ ■ ■   terminal", fontFamily = MONO, fontSize = 11.sp, color = Color(0xFF5AA070), modifier = Modifier.fillMaxWidth().background(Color(0xFF0D1A12)).padding(horizontal = 10.dp, vertical = 6.dp))
                    Column(Modifier.padding(14.dp)) {
                        c.take(mostradas).forEach { l -> Text(l, fontFamily = MONO, fontSize = 13.sp, lineHeight = 20.sp,
                            color = when { l.startsWith("ERRO", true) || l.contains("ERROR") -> Color(0xFFFF6A7A); l.startsWith("[OK]") -> Color(0xFFFFE08A); l.startsWith("$") -> Color(0xFF8CFFA8); else -> Color(0xFF5AA070) }) }
                        Text("$ " + if (cur) "█" else " ", fontFamily = MONO, fontSize = 13.sp, color = Color(0xFF8CFFA8))
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx34(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val t = rememberInfiniteTransition(label = "holo"); val g by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "g")
                Row(Modifier.fillMaxWidth().background(Color(0xFF050C12)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(100.dp)) { val cx = size.width / 2; val cy = size.height / 2 - 6.dp.toPx(); val r = 30.dp.toPx(); val a = Math.toRadians(g.toDouble())
                        val pts = listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f).flatMap { (x, z) -> listOf(Triple(x, -1f, z), Triple(x, 1f, z)) }
                        fun proj(p: Triple<Float, Float, Float>): Offset { val x = p.first * cos(a).toFloat() - p.third * sin(a).toFloat(); val z = p.first * sin(a).toFloat() + p.third * cos(a).toFloat(); val y = p.second * .8f + z * .35f; return Offset(cx + x * r, cy + y * r) }
                        val ar = listOf(0 to 2, 2 to 4, 4 to 6, 6 to 0, 1 to 3, 3 to 5, 5 to 7, 7 to 1, 0 to 1, 2 to 3, 4 to 5, 6 to 7)
                        ar.forEach { (i, j) -> drawLine(Color(0xFF6DFAFF), proj(pts[i]), proj(pts[j]), 1.5f) }
                        drawOval(Brush.radialGradient(listOf(Color(0xB350F0FF), Color.Transparent)), Offset(10.dp.toPx(), size.height - 18.dp.toPx()), Size(size.width - 20.dp.toPx(), 14.dp.toPx()))
                    }
                    Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(f(0).uppercase(), fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 2.sp, color = Color.White)
                        c.drop(1).forEach { Text(it, fontFamily = MONO, fontSize = 11.sp, color = Color(0xFF6FD8E0)) }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx35(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val ruido = "▒░▓#%&/\\?*"; var q by remember { mutableIntStateOf(0) }; LaunchedEffect(Unit) { while (true) { delay(140); q++ } }
                val r = Random(q); val cai = (q / 8) % 3
                Column(Modifier.fillMaxWidth().background(Color(0xFF07100D)).border(1.dp, Color(0xFF1D3A30)).padding(14.dp)) {
                    Text("📶  CANAL 7 · ONDA CURTA", fontFamily = MONO, fontSize = 11.sp, letterSpacing = 1.5.sp, color = Color(0xFF6AA890), modifier = Modifier.padding(bottom = 10.dp))
                    Text(buildAnnotatedString { c.forEachIndexed { k, s -> if (k % 2 == 1) { withStyle(SpanStyle(color = Color(0xFFCFE4DA).copy(alpha = if (k / 2 % 3 == cai) .12f else 1f))) { append(s) }; append(" "); withStyle(SpanStyle(color = Color(0xFF4A7A68))) { append((0 until 6).map { ruido[r.nextInt(ruido.length)] }.joinToString("")) } } else append(s); append(" ") } },
                        fontFamily = MONO, fontSize = 14.sp, lineHeight = 22.sp, color = Color(0xFFCFE4DA))
                    Text("▮▮▮ SINAL PERDIDO", fontFamily = MONO, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 13.sp, color = Color(0xFFFF6A7A).copy(alpha = if (q / 4 % 2 == 0) 1f else 0.2f), modifier = Modifier.padding(top = 8.dp))
                }
            }
    }
}

@Composable
private fun BoxScope.fx36(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val t = rememberInfiniteTransition(label = "scan"); val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "y")
                val cor = corFx(corNome, Color(0xFF5CC8FF))
                Row(Modifier.fillMaxWidth().background(Color(0xFF050B12)).border(1.dp, cor.copy(alpha = .5f)).padding(14.dp)) {
                    Canvas(Modifier.size(100.dp, 160.dp)) { val w = size.width; val h = size.height
                        drawCircle(cor.copy(alpha = .12f), 16.dp.toPx(), Offset(w / 2, h * .19f)); drawCircle(cor, 16.dp.toPx(), Offset(w / 2, h * .19f), style = Stroke(1.2f))
                        val b = Path().apply { moveTo(w * .18f, h * .97f); lineTo(w * .2f, h * .58f); quadraticTo(w * .2f, h * .41f, w / 2, h * .4f); quadraticTo(w * .8f, h * .41f, w * .8f, h * .58f); lineTo(w * .82f, h * .97f); close() }
                        drawPath(b, cor.copy(alpha = .12f)); drawPath(b, cor, style = Stroke(1.2f))
                        drawRect(cor, Offset(0f, h * y), Size(w, 3.dp.toPx())); drawRect(cor.copy(alpha = .25f), Offset(0f, h * y - 6.dp.toPx()), Size(w, 14.dp.toPx()))
                    }
                    Column(Modifier.padding(start = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Rot("Análise biométrica", cor)
                        listOf("Nome", "Idade", "Rank").zip(c).forEachIndexed { k, (r2, v) -> Row(Modifier.fillMaxWidth().entrar(400 + k * 450)) { Text(r2.uppercase(), fontFamily = MONO, fontSize = 12.sp, color = Color(0xFF6FA8C4), modifier = Modifier.weight(1f)); Text(v, fontFamily = MONO, fontSize = 12.sp, color = Color(0xFFCFF3FF)) } }
                        c.drop(3).forEachIndexed { k, v -> Text("! $v", fontFamily = MONO, fontSize = 12.sp, color = Color(0xFFFFC94A), modifier = Modifier.entrar(1800 + k * 400)) }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx37(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { var q by remember { mutableIntStateOf(0) }; LaunchedEffect(Unit) { while (true) { delay(70); q++ } }
                val cols = remember { List(40) { Random.nextInt(-20, 0) } }
                Box(Modifier.fillMaxWidth().height(190.dp).background(Color.Black)) {
                    Canvas(Modifier.fillMaxSize()) { val passo = 14.dp.toPx()
                        val n = (size.width / passo).toInt().coerceAtMost(cols.size); val linhas = (size.height / passo).toInt() + 12
                        for (i in 0 until n) { val topo = (cols[i] + q) % (linhas + 10); for (k in 0 until 10) { val y = (topo - k) * passo; if (y in 0f..size.height) drawRect(Color(0xFF2BFF7A).copy(alpha = (1 - k / 10f) * .7f), Offset(i * passo + 3, y), Size(passo * .5f, passo * .6f)) } }
                    }
                    Text(f(0).uppercase(), fontFamily = MONO, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 4.sp, color = Color(0xFFB8FFCB), textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).background(Color(0xD1000A04)).border(1.dp, Color(0xFF3AFF80)).padding(horizontal = 18.dp, vertical = 12.dp))
                }
            }
    }
}

@Composable
private fun BoxScope.fx38(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val t = rememberInfiniteTransition(label = "av"); val s by t.animateFloat(0.2f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "s")
                Box(Modifier.fillMaxWidth().background(Color(0xFF12020A)).drawBehind { drawRect(Brush.radialGradient(listOf(Color(0xFFFF0028).copy(alpha = .5f * s), Color.Transparent), radius = size.width * .6f)) }.padding(vertical = 26.dp, horizontal = 40.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.align(Alignment.CenterStart).offset(x = (-28).dp).size(14.dp, 64.dp).background(Color(0xFFFF2040).copy(alpha = s)))
                    Box(Modifier.align(Alignment.CenterEnd).offset(x = 28.dp).size(14.dp, 64.dp).background(Color(0xFFFF2040).copy(alpha = 1.2f - s)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CÓDIGO VERMELHO", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp, color = Color(0xFFFF8A96))
                        Text("ALERTA VERMELHO", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White.copy(alpha = .55f + s * .45f), textAlign = TextAlign.Center)
                        c.forEach { Text(it, fontFamily = MONO, fontSize = 13.sp, color = Color(0xFFFFC9CF), textAlign = TextAlign.Center) }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx39(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val t = rememberInfiniteTransition(label = "ia"); val o by t.animateFloat(0f, 6.28f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "o")
                Row(Modifier.fillMaxWidth().background(Color(0xFF04080A)).border(1.dp, Color(0x593AFF80)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(62.dp).clip(Hexagono).background(Color(0x0F3AFF80)).border(1.dp, Color(0xFF3AFF80), Hexagono), contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) { (0 until 7).forEach { k -> Box(Modifier.width(3.dp).height((8 + 14 * ((sin(o + k) + 1) / 2)).dp).background(Color(0xFF3AFF80))) } }
                    }
                    Column(Modifier.padding(start = 14.dp)) { Rot(f(0), Color(0xFF3AFF80)); Text(estilizar(c.drop(1).joinToString(" ")), fontFamily = MONO, fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFFC9FFD9)) }
                }
            }
    }
}

@Composable
private fun BoxScope.fx40(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { var curtiu by remember { mutableStateOf(false) }; val n0 = f(2).filter { it.isDigit() }.toIntOrNull() ?: 0
                Column(Modifier.fillMaxWidth().background(Color(0xFF17171D)).border(1.dp, Color(0xFF24242C)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val user = f(0).removePrefix("@")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(5.dp)).background(Brush.linearGradient(listOf(Color(0xFFFF6A2B), Color(0xFF7A3B8F)))), contentAlignment = Alignment.Center) { Text(user.take(1).uppercase(), fontWeight = FontWeight.Black, color = Color.White) }
                        Column(Modifier.padding(start = 10.dp)) { Text(user, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White); Text("@$user · agora", fontSize = 12.sp, color = Color(0xFF8E8A93)) }
                    }
                    Text(estilizar(f(1)), fontSize = 15.sp, lineHeight = 21.sp, color = Color(0xFFECE8E1))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text((if (curtiu) "♥ " else "♡ ") + "%,d".format(n0 + if (curtiu) 1 else 0).replace(',', '.'), fontSize = 14.sp, color = if (curtiu) Color(0xFFFF4458) else Color(0xFF8E8A93), modifier = Modifier.clickable { curtiu = !curtiu })
                        if (c.size > 3) Text("💬 " + f(3), fontSize = 14.sp, color = Color(0xFF8E8A93))
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx41(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.fillMaxWidth().rotate(-0.6f).background(Brush.verticalGradient(listOf(Color(0xFFEBE3CF), Color(0xFFDDD3BA)))).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("O DIÁRIO DA CIDADE", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF1C1A15), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Box(Modifier.padding(vertical = 6.dp).fillMaxWidth().height(3.dp).background(Color(0xFF1C1A15)))
                Text(f(0).uppercase(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 30.sp, color = Color(0xFF1C1A15), modifier = Modifier.entrar(0, -20f))
                if (c.size > 1) Text(f(1), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1C1A15), modifier = Modifier.padding(vertical = 6.dp))
                c.drop(2).forEach { Text(estilizar(it), fontFamily = FontFamily.Serif, fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF2A2720), textAlign = TextAlign.Justify) }
            }
    }
}

@Composable
private fun BoxScope.fx42(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.fillMaxWidth().background(Color(0xFF1A1A20)).border(1.dp, Color(0xFF24242C)).entrar()) {
                Text(f(1), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp))
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF6FCF97)), contentAlignment = Alignment.Center) { Text(f(0).take(1).uppercase(), fontWeight = FontWeight.Black, color = Color.Black) }
                    Column(Modifier.padding(start = 10.dp)) { Text(f(0), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White); Text("para mim · agora", fontSize = 12.sp, color = Color(0xFF8E8A93)) }
                }
                c.drop(2).forEach { Text(estilizar(it), fontSize = 14.5.sp, lineHeight = 22.sp, color = Color(0xFFD7D3CC), modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) }
            }
    }
}

@Composable
private fun BoxScope.fx43(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val t = rememberInfiniteTransition(label = "lig"); val o by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000)), label = "o"); val bal by t.animateFloat(-14f, 14f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "b")
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF1A1D33), Color(0xFF0B0C16)))).padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(84.dp).drawBehind { val e = 1 + o * 1.1f; drawRect(Color(0xFF6FCF97).copy(alpha = (1 - o) * .8f), Offset(size.width / 2 * (1 - e), size.height / 2 * (1 - e)), Size(size.width * e, size.height * e), style = Stroke(2f)) }
                        .background(Brush.linearGradient(listOf(Color(0xFF3A3F66), Color(0xFF1F2238)))), contentAlignment = Alignment.Center) { Text(if (f(0).contains("desconhecido", true)) "?" else f(0).take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White) }
                    Rot("Chamada recebida", Color(0xFF6FCF97), Modifier.padding(top = 14.dp))
                    Text(f(0), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (c.size > 1) Text(f(1), fontFamily = MONO, fontSize = 13.sp, color = Color(0xFF8E8A93))
                    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(56.dp)) {
                        Box(Modifier.size(56.dp).background(Color(0xFFE0283C)), contentAlignment = Alignment.Center) { Text("✕", fontSize = 22.sp, color = Color.White) }
                        Box(Modifier.size(56.dp).graphicsLayer { rotationZ = bal }.background(Color(0xFF2FBF71)), contentAlignment = Alignment.Center) { Text("✆", fontSize = 24.sp, color = Color.White) }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx44(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { var n by remember { mutableIntStateOf(0) }; val q = f(0); LaunchedEffect(q) { while (n < q.length) { delay(60); n++ } }
                Column {
                    Row(Modifier.fillMaxWidth().background(Color(0xFF1E1E25)).border(1.dp, Color(0xFF34343F)).padding(horizontal = 16.dp, vertical = 12.dp)) { Text("⌕  ", color = Color(0xFF8E8A93), fontSize = 17.sp); Text(q.take(n) + if (n < q.length) "▏" else "", fontSize = 15.5.sp, color = Color.White) }
                    if (n >= q.length) c.drop(1).forEachIndexed { k, s -> Text("⌕  $q $s", fontSize = 14.sp, color = Color(0xFFCFCBD3), modifier = Modifier.fillMaxWidth().entrar(k * 250).background(Color(0xFF1E1E25)).padding(horizontal = 18.dp, vertical = 9.dp)) }
                }
            }
    }
}

@Composable
private fun BoxScope.fx45(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { var tocando by remember { mutableStateOf(true) }; var n by remember { mutableIntStateOf(0) }
                LaunchedEffect(tocando) { while (tocando && n < 32) { delay(140); n++ }; if (n >= 32) tocando = false }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.background(Color(0xFFFF6A2B)).padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).background(Color.Black).clickable { if (n >= 32) n = 0; tocando = !tocando }, contentAlignment = Alignment.Center) { Text(if (tocando) "II" else "▶", color = Color(0xFFFF6A2B), fontWeight = FontWeight.Black) }
                        Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            (0 until 32).forEach { k -> Box(Modifier.width(3.dp).height((6 + kotlin.math.abs(sin(k * 1.7) * 10 + sin(k * .6) * 8)).dp).background(if (k < n) Color.Black else Color(0x4D000000))) }
                        }
                        Text(f(0), fontFamily = MONO, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                    }
                    if (c.size > 1) Text(buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Normal)) { append("Transcrição: ") }; append("“" + c.drop(1).joinToString(" ") + "”") },
                        fontStyle = FontStyle.Italic, fontSize = 13.sp, color = Color(0xFF8E8A93), textAlign = TextAlign.End, modifier = Modifier.padding(top = 8.dp))
                }
            }
    }
}

@Composable
private fun BoxScope.fx46(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.align(Alignment.Center).width(250.dp).height(400.dp).clip(RoundedCornerShape(10.dp)).border(6.dp, Color(0xFF22222A), RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3A1B6B), Color(0xFFB24A6A), Color(0xFFFF8A4B)))).padding(horizontal = 16.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(f(0), fontSize = 62.sp, fontWeight = FontWeight.Light, color = Color.White, modifier = Modifier.padding(top = 20.dp))
                c.drop(1).forEachIndexed { k, l -> val d = l.indexOf(':')
                    Column(Modifier.padding(top = 8.dp).fillMaxWidth().entrar(500 + k * 700).background(Color(0x8C14101E)).padding(horizontal = 11.dp, vertical = 8.dp)) {
                        Text(if (d > 0) l.substring(0, d) else "Notificação", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White); Text(if (d > 0) l.substring(d + 1).trim() else l, fontSize = 12.sp, color = Color.White) }
                }
                Spacer(Modifier.weight(1f)); Text("deslize para desbloquear", fontSize = 12.sp, color = Color(0xB3FFFFFF))
            }
    }
}

@Composable
private fun BoxScope.fx47(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val t = remember { Animatable(0f) }; LaunchedEffect(Unit) { delay(1800); t.animateTo(1f, tween(1400)) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(Modifier.background(lerp(Color(0xFFFF6A2B), Color(0xFF23232B), t.value)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(f(0), fontSize = 15.sp, color = Color.Black, modifier = Modifier.graphicsLayer { alpha = 1 - t.value; translationY = -6 * t.value * density }.blur((7 * t.value).dp))
                        Text("🚫 Esta mensagem foi apagada", fontSize = 14.sp, fontStyle = FontStyle.Italic, color = Color(0xFF9A96A3), modifier = Modifier.graphicsLayer { alpha = ((t.value - .5f) * 2).coerceIn(0f, 1f) })
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx48(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { var fase by remember { mutableIntStateOf(0) }; LaunchedEffect(Unit) { delay(1800); fase = 1; delay(900); fase = 2; delay(1800); fase = 3 }
                val t = rememberInfiniteTransition(label = "dig"); val s by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "s")
                Column { Text(f(0), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6FCF97), modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
                    Box(Modifier.background(Color(0xFF23232B)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (fase == 3) Text(estilizar(c.drop(1).joinToString(" ")), fontSize = 15.sp, color = Color(0xFFECE8E1), modifier = Modifier.entrar(0, 6f))
                        else Row(Modifier.height(18.dp).graphicsLayer { alpha = if (fase == 1) 0f else 1f }, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            (0 until 3).forEach { k -> Box(Modifier.size(8.dp).graphicsLayer { translationY = -6f * density * sin(((s - k * .15f) * 6.28f).coerceAtLeast(0f)).coerceAtLeast(0f) }.background(Color(0xFF8E8A93))) } }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx49(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.fillMaxWidth().background(Color(0xFF101015)).border(1.dp, Color(0xFF24242C))) {
                Text(f(0), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White, modifier = Modifier.fillMaxWidth().padding(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF24242C)))
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    c.drop(1).forEachIndexed { k, l -> val d = l.indexOf(':'); val quem = if (d > 0) l.substring(0, d).trim() else ""; val eu = quem.equals("eu", true) || quem.equals("você", true)
                        Row(Modifier.fillMaxWidth().entrar(200 + k * 600), horizontalArrangement = if (eu) Arrangement.End else Arrangement.Start) {
                            Column(Modifier.widthIn(max = 280.dp).background(if (eu) Color(0xFFFF6A2B) else Color(0xFF23232B)).padding(horizontal = 11.dp, vertical = 7.dp)) {
                                if (!eu && quem.isNotEmpty()) Text(quem, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = listOf(Color(0xFF6FCF97), Color(0xFF7FD3FF), Color(0xFFC59BFF), Color(0xFFFF8A65), Color(0xFFF07AAE))[(quem.hashCode() and 0x7fffffff) % 5])
                                Text(estilizar(if (d > 0) l.substring(d + 1).trim() else l), fontSize = 14.sp, color = if (eu) Color.Black else Color(0xFFECE8E1))
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx50(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val t = rememberInfiniteTransition(label = "pino"); val o by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600)), label = "o")
                Column(Modifier.fillMaxWidth().background(Color(0xFF101015)).border(1.dp, Color(0xFF24242C))) {
                    Canvas(Modifier.fillMaxWidth().height(140.dp)) { drawRect(Color(0xFF1A1D26))
                        drawLine(Color(0xFF2A2F3D), Offset(0f, size.height * .8f), Offset(size.width, size.height * .2f), 10.dp.toPx()); drawLine(Color(0xFF2A2F3D), Offset(size.width * .2f, 0f), Offset(size.width * .7f, size.height), 7.dp.toPx())
                        drawRect(Color(0xFF263A2E), Offset(size.width * .05f, size.height * .1f), Size(size.width * .2f, size.height * .3f))
                        val p = Offset(size.width * .6f, size.height * .42f); val s = 16.dp.toPx(); val e = 22.dp.toPx() * o
                        drawRect(Color(0xFF6FCF97).copy(alpha = (1 - o) * .6f), Offset(p.x - s / 2 - e, p.y - s / 2 - e), Size(s + e * 2, s + e * 2), style = Stroke(2f))
                        drawRect(Color(0xFF6FCF97), Offset(p.x - s / 2, p.y - s / 2), Size(s, s)) }
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(f(0), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White); if (c.size > 1) Text(f(1), fontSize = 12.sp, color = Color(0xFF8E8A93)) }
                        Text((c.getOrNull(2) ?: "ao vivo").uppercase(), fontFamily = MONO, fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.Black, modifier = Modifier.background(Color(0xFF6FCF97)).padding(horizontal = 7.dp, vertical = 2.dp))
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx51(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFEBDDBB), Color(0xFFD8C79B)))).drawBehind { drawRect(Color(0xFF5A3412), size = Size(10.dp.toPx(), size.height)) }
                .padding(start = 26.dp, end = 20.dp, top = 20.dp, bottom = 22.dp)) {
                Text(f(0).uppercase(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 1.5.sp, color = Color(0xFF4A2E14), modifier = Modifier.padding(bottom = 10.dp))
                c.drop(1).forEachIndexed { k, l -> Text(estilizar(l), fontFamily = FontFamily.Cursive, fontSize = 22.sp, lineHeight = 30.sp, color = Color(0xFF2B2118), modifier = Modifier.drawWithClipAtraso(300 + k * 1200)) }
            }
    }
}

@Composable
private fun BoxScope.fx52(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.align(Alignment.Center).width(260.dp).rotate(1.2f).background(Brush.radialGradient(listOf(Color(0xFFF1DFB4), Color(0xFFD2B57A)), radius = 600f)).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PROCURADO", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 38.sp, letterSpacing = 2.sp, color = Color(0xFF2F1E0C), modifier = Modifier.entrar(0, -30f))
                Box(Modifier.padding(vertical = 10.dp).size(120.dp).border(3.dp, Color(0xFF2F1E0C)).background(Brush.radialGradient(listOf(Color(0xFFC9A86A), Color(0xFF8A6A3A)))), contentAlignment = Alignment.BottomCenter) {
                    Canvas(Modifier.fillMaxSize()) { val w = size.width; val h = size.height; drawOval(Color(0xFF3B2A18), Offset(w * .16f, h * .28f), Size(w * .68f, h * .14f)); drawRect(Color(0xFF3B2A18), Offset(w * .3f, h * .12f), Size(w * .4f, h * .22f)); drawCircle(Color(0xFF3B2A18), w * .15f, Offset(w / 2, h * .52f)); drawRect(Color(0xFF3B2A18), Offset(w * .14f, h * .72f), Size(w * .72f, h * .28f)) }
                }
                Text("VIVO OU MORTO", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 3.sp, color = Color(0xFF2F1E0C))
                Text(f(0), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 21.sp, color = Color(0xFF2F1E0C), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp))
                c.drop(2).forEach { Text(it, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = Color(0xFF2F1E0C), textAlign = TextAlign.Center) }
                if (c.size > 1) Column(Modifier.padding(top = 10.dp).fillMaxWidth().border(2.dp, Color(0xFF2F1E0C)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RECOMPENSA", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 3.sp, color = Color(0xFF2F1E0C)); Text(f(1), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 30.sp, color = Color(0xFF7A1010)) }
            }
    }
}

@Composable
private fun BoxScope.fx53(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { var fechado by remember { mutableStateOf<String?>(null) }
                Column(Modifier.fillMaxWidth().background(Color(0xFF17140A)).border(1.5.dp, Color(0xB3FFC94A))) {
                    Text("AVISO DE CONTEÚDO", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp, color = Color(0xFF1A0A00), modifier = Modifier.fillMaxWidth().background(Color(0xFFFFC94A)).padding(horizontal = 14.dp, vertical = 9.dp))
                    if (fechado != null) Text("$fechado · ver aviso de novo", fontSize = 13.sp, color = Color(0xFF8E8A93), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { fechado = null }.padding(14.dp))
                    else Column(Modifier.padding(14.dp)) {
                        Text("Este capítulo contém:", fontSize = 14.sp, color = Color(0xFFD8D2C4))
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { resto.split(',', '|').map { it.trim() }.filter { it.isNotEmpty() }.take(4).forEach { Chip(it, Color(0xFFFFC94A)) } }
                        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Continuar", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1A0A00), textAlign = TextAlign.Center, modifier = Modifier.weight(1f).background(Color(0xFFFFC94A)).clickable { fechado = "Aviso dispensado" }.padding(10.dp))
                            Text("Pular a cena", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFDDDDDD), textAlign = TextAlign.Center, modifier = Modifier.weight(1f).background(Color(0xFF23232B)).clickable { fechado = "Pule até o próximo ◆" }.padding(10.dp))
                        }
                    }
                }
            }
    }
}

@Composable
private fun BoxScope.fx54(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { val g = remember { Animatable(0f) }; LaunchedEffect(Unit) { g.animateTo(1f, tween(2600, easing = FastOutSlowInEasing)) }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(92.dp)) { val r = size.width / 2; drawCircle(Color(0xFF14141A), r); drawCircle(Color(0xFFFFC94A), r - 2, style = Stroke(2.5.dp.toPx()))
                        val am = g.value * 12 * 2 * PI; val ah = g.value * 2 * PI
                        drawLine(Color(0xFFECE8E1), center, center + Offset(sin(ah).toFloat(), -cos(ah).toFloat()) * (r * .5f), 4.dp.toPx(), StrokeCap.Round)
                        drawLine(Color(0xFFFFC94A), center, center + Offset(sin(am).toFloat(), -cos(am).toFloat()) * (r * .78f), 2.5.dp.toPx(), StrokeCap.Round) }
                    Text(f(0).uppercase(), fontWeight = FontWeight.Black, fontSize = 28.sp, letterSpacing = (4 + (1 - g.value) * 20).sp, color = Color.White.copy(alpha = g.value), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                }
            }
    }
}

@Composable
private fun BoxScope.fx55(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CAPÍTULO", fontFamily = MONO, fontSize = 11.sp, letterSpacing = 6.sp, color = Color(0xFF8E8A93))
                Text(f(0), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 92.sp, lineHeight = 96.sp, color = Color(0xFFFFC94A),
                    style = TextStyle(drawStyle = Stroke(3f)), modifier = Modifier.entrar())
                if (c.size > 1) Text(f(1), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.entrar(400))
                Box(Modifier.padding(top = 12.dp).size(60.dp, 2.dp).background(Color(0xFFFFC94A)))
            }
    }
}

@Composable
private fun BoxScope.fx56(nome: String, corNome: String?, resto: String, c: List<String>, f: (Int) -> String, estilo: TextStyle) {
    when (0) { else -> { var ab by remember { mutableStateOf(false) }
                Column(Modifier.fillMaxWidth().border(1.dp, Color(0x66FFC94A)).clickable { ab = !ab }.padding(16.dp)) {
                    Rot("No próximo capítulo", Color(0xFFFFC94A))
                    Text(estilizar(resto), fontFamily = FontFamily.Serif, fontSize = 16.sp, lineHeight = 25.sp, color = Color(0xFFDDDDDD), modifier = Modifier.padding(top = 8.dp).then(if (ab) Modifier else Modifier.blur(5.dp)))
                    Text(if (ab) "(toque para esconder)" else "(toque para espiar)", fontSize = 12.sp, color = Color(0xFF8E8A93), modifier = Modifier.padding(top = 8.dp))
                }
            }
    }
}

/** Linha com ||tarjas|| que o leitor toca para ver. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextoTarjado(l: String) {
    val partes = remember(l) { l.split("||") }
    val abertas = remember(l) { mutableStateListOf<Int>() }
    FlowRow {
        partes.forEachIndexed { k, s ->
            if (k % 2 == 1) Text(s, fontFamily = MONO, fontSize = 14.sp, lineHeight = 24.sp, color = if (k in abertas) Color(0xFF7A1010) else Color(0xFF16130E),
                modifier = Modifier.background(if (k in abertas) Color.Transparent else Color(0xFF16130E)).clickable { if (k in abertas) abertas.remove(k) else abertas.add(k) }.padding(horizontal = 3.dp))
            else Text(s, fontFamily = MONO, fontSize = 14.sp, lineHeight = 24.sp, color = Color(0xFF2A2218))
        }
    }
}

/** Revela o conteúdo da esquerda para a direita (assinatura, escrita à mão). */
private fun Modifier.drawWithClip(f: Float): Modifier = drawWithContentClip(f)
private fun Modifier.drawWithContentClip(f: Float): Modifier = this.drawWithContent { clipRect(right = size.width * f) { this@drawWithContent.drawContent() } }
private fun Modifier.drawWithClipAtraso(atraso: Int): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(atraso.toLong()); a.animateTo(1f, tween(1300, easing = LinearEasing)) }
    drawWithContentClip(a.value)
}

// ───── formas ─────
private class Forma(val f: (Size) -> Path) : Shape {
    override fun createOutline(size: Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density) = androidx.compose.ui.graphics.Outline.Generic(f(size))
}
private fun poli(vararg p: Float) = Forma { s -> Path().apply { for (i in p.indices step 2) { val x = p[i] * s.width; val y = p[i + 1] * s.height; if (i == 0) moveTo(x, y) else lineTo(x, y) }; close() } }
private val Hexagono = poli(.5f, 0f, .93f, .25f, .93f, .75f, .5f, 1f, .07f, .75f, .07f, .25f)
private val Octogono = poli(.3f, 0f, .7f, 0f, 1f, .3f, 1f, .7f, .7f, 1f, .3f, 1f, 0f, .7f, 0f, .3f)
private val Estandarte = poli(0f, 0f, 1f, 0f, 1f, .86f, .5f, 1f, 0f, .86f)
private val Frasco = poli(.25f, 0f, .75f, 0f, .75f, .12f, 1f, .25f, 1f, 1f, 0f, 1f, 0f, .25f, .25f, .12f)
private val Lacre = poli(.5f, 0f, .63f, .12f, .8f, .08f, .84f, .25f, 1f, .34f, .92f, .5f, 1f, .66f, .84f, .75f, .8f, .92f, .63f, .88f, .5f, 1f, .37f, .88f, .2f, .92f, .16f, .75f, 0f, .66f, .08f, .5f, 0f, .34f, .16f, .25f, .2f, .08f, .37f, .12f)
private val Rasgado = poli(0f, .04f, .06f, 0f, .12f, .03f, .19f, 0f, .27f, .04f, .35f, .01f, .44f, .04f, .52f, 0f, .61f, .03f, .7f, 0f, .79f, .04f, .88f, .01f, .95f, .04f, 1f, .01f,
    1f, .96f, .94f, 1f, .86f, .97f, .77f, 1f, .68f, .96f, .58f, 1f, .49f, .97f, .4f, 1f, .31f, .96f, .22f, 1f, .13f, .97f, .05f, 1f, 0f, .96f)

// ───── Mega Pack: estilos em linha {efeito: texto} ─────

val INLINE_FX = setOf("fogo", "gelo", "neon", "medo", "eco", "runa", "idioma", "divino", "feitico", "cor", "sangue", "veneno", "raio", "sombra", "quebra", "vento", "ouro", "coracao")
private val APELIDOS_INLINE = mapOf("feitiço" to "feitico", "coração" to "coracao", "chamas" to "fogo", "runas" to "runa")
private val RX_INLINE = Regex("\\{([a-zçãéíóúâêô]+)(?::([a-zçãéíóúâêô]+))?:\\s")
/** "{fogo:azul: texto}" começando em i → (efeito, cor, texto, fim) */
fun lerInlineFx(linha: String, i: Int): Pair<Triple<String, String?, String>, Int>? {
    val m = RX_INLINE.find(linha, i)?.takeIf { it.range.first == i } ?: return null
    val nome = APELIDOS_INLINE[m.groupValues[1]] ?: m.groupValues[1]
    if (nome !in INLINE_FX) return null
    val fim = linha.indexOf('}', m.range.last + 1).takeIf { it > 0 } ?: return null
    return Triple(nome, m.groupValues[2].ifEmpty { null }, linha.substring(m.range.last + 1, fim)) to fim + 1
}

private val RUNAS = "ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛇᛈᛉᛊᛏᛒᛖᛗᛚᛜᛞᛟᚪᚫ"
fun runar(s: String) = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase().map { c -> val k = c - 'a'; if (k in 0..25) RUNAS[k] else if (c == ' ') ' ' else '·' }.joinToString("")

/** Estilo fixo de cada efeito em linha (os que mexem são animados em textoComEfeitos). */
fun estiloInlineFx(nome: String, cor: String?): SpanStyle {
    val cc = cor?.let { corFx(it) }
    return when (nome) {
        "fogo" -> SpanStyle(brush = Brush.verticalGradient(if (cc != null) listOf(claro(cc), cc, mix(cc, .6f)) else listOf(Color(0xFFFFE27A), Color(0xFFFF8A1F), Color(0xFFFF3B00))), fontWeight = FontWeight.Black,
            shadow = androidx.compose.ui.graphics.Shadow((cc ?: Color(0xFFFF5A00)).copy(alpha = .8f), blurRadius = 16f))
        "gelo" -> SpanStyle(brush = Brush.linearGradient(listOf(Color(0xFFBFEFFF), Color.White, Color(0xFF7FD3FF), Color(0xFFE9FAFF))), fontWeight = FontWeight.Black, shadow = androidx.compose.ui.graphics.Shadow(Color(0xCC78C8FF), blurRadius = 12f))
        "neon" -> { val n = cc ?: Color(0xFFFF2BD6); SpanStyle(color = claro(n), fontWeight = FontWeight.Bold, letterSpacing = androidx.compose.ui.unit.TextUnit(0.06f, androidx.compose.ui.unit.TextUnitType.Em), shadow = androidx.compose.ui.graphics.Shadow(n, blurRadius = 22f)) }
        "medo" -> SpanStyle(color = Color(0xFFD6D2E8), shadow = androidx.compose.ui.graphics.Shadow(Color(0x73A096DC), blurRadius = 8f))
        "eco" -> SpanStyle(color = cc ?: Color(0xFFB8CCFF), shadow = androidx.compose.ui.graphics.Shadow((cc ?: Color(0xFF8FB8FF)).copy(alpha = .5f), Offset(10f, 6f), 6f))
        "runa" -> SpanStyle(color = cc ?: Color(0xFFFFCF6A), letterSpacing = androidx.compose.ui.unit.TextUnit(0.12f, androidx.compose.ui.unit.TextUnitType.Em), shadow = androidx.compose.ui.graphics.Shadow(Color(0xCCFFAA28), blurRadius = 14f), textDecoration = TextDecoration.Underline)
        "idioma" -> SpanStyle(color = cc ?: Color(0xFF9BF0C8), shadow = androidx.compose.ui.graphics.Shadow(Color(0x9950F0AA), blurRadius = 10f), textDecoration = TextDecoration.Underline)
        "divino" -> SpanStyle(color = Color(0xFFFFF6D6), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, letterSpacing = androidx.compose.ui.unit.TextUnit(0.12f, androidx.compose.ui.unit.TextUnitType.Em), shadow = androidx.compose.ui.graphics.Shadow(cc ?: Color(0xFFFFBE3C), blurRadius = 26f))
        "feitico" -> SpanStyle(color = cc?.let { claro(it) } ?: Color(0xFFD9C4FF), fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, letterSpacing = androidx.compose.ui.unit.TextUnit(0.08f, androidx.compose.ui.unit.TextUnitType.Em), shadow = androidx.compose.ui.graphics.Shadow(cc ?: Color(0xFF8A4DFF), blurRadius = 16f))
        "cor" -> SpanStyle(color = cc ?: corFx(null), fontWeight = FontWeight.Bold)
        "sangue" -> SpanStyle(color = cc ?: Color(0xFFC3001E), fontWeight = FontWeight.Black, shadow = androidx.compose.ui.graphics.Shadow(Color(0xFF3A0008), Offset(0f, 4f), 2f))
        "veneno" -> SpanStyle(color = cc ?: Color(0xFF7CFF6B), fontWeight = FontWeight.ExtraBold, shadow = androidx.compose.ui.graphics.Shadow(Color(0x8C7CFF6B), blurRadius = 12f))
        "raio" -> SpanStyle(color = Color.White, fontWeight = FontWeight.Black, shadow = androidx.compose.ui.graphics.Shadow(cc ?: Color(0xFF4FB8FF), Offset(2f, -1f), 6f))
        "sombra" -> SpanStyle(color = Color(0xFFD9CDEE), fontWeight = FontWeight.ExtraBold, shadow = androidx.compose.ui.graphics.Shadow(cc ?: Color(0xF25A328C), Offset(0f, -8f), 18f))
        "quebra", "vento" -> SpanStyle(color = cc ?: Color.Unspecified, fontWeight = FontWeight.Bold)
        "ouro" -> SpanStyle(brush = Brush.linearGradient(listOf(Color(0xFF8A5A00), Color(0xFFFFD86B), Color(0xFFFFF6CF), Color(0xFFFFC94A), Color(0xFF8A5A00))), fontFamily = FontFamily.Serif, fontWeight = FontWeight.ExtraBold)
        "coracao" -> SpanStyle(color = cc ?: Color(0xFFFF6FA8), fontWeight = FontWeight.ExtraBold, shadow = androidx.compose.ui.graphics.Shadow(Color(0x80FF6FA8), blurRadius = 12f))
        else -> SpanStyle()
    }
}

/** Efeitos em linha que mexem a cada quadro: treme, quebra, vento, raio, neon piscando, idioma trocando glifos. */
fun animarInlineFx(t: AnnotatedString, quadro: Int): AnnotatedString {
    val fx = t.getStringAnnotations("fx", 0, t.length).filter { it.item in setOf("medo", "quebra", "vento", "raio", "neon", "idioma", "coracao") }
    if (fx.isEmpty()) return t
    val r = Random(quadro)
    return buildAnnotatedString {
        append(t)
        fx.forEach { a ->
            when (a.item) {
                "medo" -> for (k in a.start until a.end) addStyle(SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift(r.nextFloat() * 0.12f - 0.06f)), k, k + 1)
                "quebra", "vento" -> { val ciclo = quadro % 36; for (k in a.start until a.end) { val fase = ciclo - (k - a.start); if (fase in 6..16) addStyle(SpanStyle(color = Color.Transparent,
                    baselineShift = if (a.item == "quebra") androidx.compose.ui.text.style.BaselineShift(-0.5f) else null), k, k + 1) } }
                "raio" -> if (quadro % 3 == 0) addStyle(SpanStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.White, Offset(-2f, 1f), 10f)), a.start, a.end)
                "neon" -> if (quadro % 23 == 0 || quadro % 23 == 2) addStyle(SpanStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Transparent), color = Color(0x66FFFFFF)), a.start, a.end)
                "coracao" -> if (quadro % 8 < 2) addStyle(SpanStyle(fontSize = androidx.compose.ui.unit.TextUnit(1.1f, androidx.compose.ui.unit.TextUnitType.Em)), a.start, a.end)
                "idioma" -> {}
            }
        }
    }
}
val MEGA_PACK = listOf(
    Triple("Texto em chamas", "{fogo: texto}", "inline"),
    Triple("Texto congelado", "{gelo: texto}", "inline"),
    Triple("Neon", "{neon: texto}", "inline"),
    Triple("Tremendo de medo", "{medo: texto}", "inline"),
    Triple("Eco", "{eco: texto}", "inline"),
    Triple("Runas antigas", "{runa: texto}", "inline"),
    Triple("Outro idioma", "{idioma: tradução}", "inline"),
    Triple("Voz divina", "{divino: texto}", "inline"),
    Triple("Feitiço", "{feitico: texto}", "inline"),
    Triple("Painel de evolução", "[#evolucao Nv 11 > 12 | Força 14 > 17 | Agilidade 9 > 11]", "rpg"),
    Triple("Atributos", "[#atributos FOR 14 | AGI 9 | VIT 12 | INT 7 | SAB 10 | SOR 5]", "rpg"),
    Triple("Habilidade", "[#habilidade Corte Fantasma | ativa | rank A | 30 MP | 12s | descrição]", "rpg"),
    Triple("Missão concluída", "[#missao-ok Sobreviver à primeira noite | +300 XP | +1 Baú de bronze]", "rpg"),
    Triple("Missão falhou", "[#missao-falhou Proteger a vila | −200 XP | Reputação −5]", "rpg"),
    Triple("Alerta de perigo", "[#perigo Presença hostil detectada | Recomendação: FUGIR]", "rpg"),
    Triple("Conquista desbloqueada", "[#conquista Primeiro Sangue | +50 G]", "rpg"),
    Triple("Classe obtida", "[#classe Espadachim Sombrio | passiva 1 | passiva 2]", "rpg"),
    Triple("Título obtido", "[#titulo Aquele que Sobreviveu ao Abismo | +10% resistência]", "rpg"),
    Triple("Chefe à vista", "[#chefe Rei Goblin Gorok | Nv 45 | 3 fases]", "rpg"),
    Triple("Andar da masmorra", "[#andar 47 | Câmara dos Espelhos Quebrados]", "rpg"),
    Triple("Avanço de reino", "[#reino Condensação > Fundação > Núcleo Dourado > Alma Nascente | 3]", "murim"),
    Triple("Técnica marcial", "[#tecnica Palma do Dragão Celestial | grau | forma 1 | forma 2 | ???]", "murim"),
    Triple("Seita ou clã", "[#seita Monte Hua | Mestre Cheong | lema]", "murim"),
    Triple("Pílula", "[#pilula Pílula da Alma Pura | 9ª classe | efeito 1 | efeito 2]", "murim"),
    Triple("Duelo", "[#duelo Kang Min-ho 70% vs Ryu Seong 45%]", "murim"),
    Triple("Página rasgada", "[#rasgada texto]", "terror"),
    Triple("Fita VHS", "[#vhs 03 NOV 1998 02:14 | texto]", "terror"),
    Triple("Arquivo confidencial", "[#arquivo Caso 0417 | campo | campo]", "misterio"),
    Triple("Depoimento", "[#depoimento DET. PARK: pergunta | SUSPEITO: resposta]", "misterio"),
    Triple("Telegrama", "[#telegrama CHEGO AMANHÃ STOP ...]", "misterio"),
    Triple("Profecia", "[#profecia verso 1 | verso 2 | verso 3]", "fantasia"),
    Triple("Terminal", "[#terminal \$ comando | saída | [OK] resultado]", "scifi"),
    Triple("Holograma", "[#holo Projeção ativa | dado | dado]", "scifi"),
    Triple("Transmissão cortada", "[#corte texto | trecho perdido | texto]", "scifi"),
    Triple("Scanner de pessoa", "[#scan Kang Min-ho | 17 | rank F | anomalia]", "scifi"),
    Triple("Chuva de código", "[#codigo ACESSO CONCEDIDO]", "scifi"),
    Triple("Alerta vermelho", "[#alerta-vermelho Violação de casco no setor 7]", "scifi"),
    Triple("Post de rede social", "[#post kang_minho | texto | curtidas | comentários]", "celular"),
    Triple("Manchete de jornal", "[#jornal MANCHETE | subtítulo | texto]", "celular"),
    Triple("E-mail", "[#email Dra. Han | Re: o que você viu na torre? | corpo]", "celular"),
    Triple("Ligação recebida", "[#ligacao Número desconhecido | +82 02 ••• 0417]", "celular"),
    Triple("Pesquisa", "[#busca como sair de uma masmorra sem morrer]", "celular"),
    Triple("Mensagem de voz", "[#voz 0:23 | transcrição]", "celular"),
    Triple("Tela de bloqueio", "[#bloqueio 03:14 | notificação | notificação]", "celular"),
    Triple("Mensagem apagada", "[#apagada texto]", "celular"),
    Triple("Digitando…", "[#digitando Kang | mensagem]", "celular"),
    Triple("Diário com data", "[#diario 12 de março de 1894 | texto]", "epoca"),
    Triple("Cartaz de procurado", "[#procurado Jack \"Cinzas\" Moreau | \$ 5.000 | crimes]", "epoca"),
    Triple("Aviso de conteúdo", "[#aviso violência, morte de personagem, linguagem forte]", "estrutura"),
    Triple("Salto no tempo", "[#salto 3 anos depois]", "estrutura"),
    Triple("Texto colorido", "{cor:vermelho: texto}", "inline"),
    Triple("Sangue escorrendo", "{sangue: texto}", "inline"),
    Triple("Veneno", "{veneno: texto}", "inline"),
    Triple("Raio", "{raio: texto}", "inline"),
    Triple("Sombra", "{sombra: texto}", "inline"),
    Triple("Texto quebrando", "{quebra: texto}", "inline"),
    Triple("Levado pelo vento", "{vento: texto}", "inline"),
    Triple("Ouro", "{ouro: texto}", "inline"),
    Triple("Coração batendo", "{coracao: texto}", "inline"),
    Triple("Janela do Sistema (qualquer cor)", "[#sistema:vermelho Título | texto]", "rpg"),
    Triple("Baú de recompensa", "[#bau Baú de Ouro | 300 G | Espada Rúnica | Poção x3]", "rpg"),
    Triple("Ranking", "[#ranking Torre | 1 Kang 9820 ↑2 | 2 Hana 9400 ↓1 | 3 Você 8990 novo]", "rpg"),
    Triple("Tempo da missão", "[#tempo 00:00:15 | Fuja da masmorra]", "rpg"),
    Triple("Ficha de inimigo", "[#inimigo Lobo das Cinzas | Nv 18 | fraco: gelo | forte: fogo | descrição]", "rpg"),
    Triple("Dantian (Qi acumulado)", "[#dantian 72% | Fundação, estágio médio]", "murim"),
    Triple("Tribulação celestial", "[#tribulacao 4/9 | A quarta onda caiu]", "murim"),
    Triple("Regras", "[#regras Regras do turno da noite | 1. Não atenda o telefone | 2. … | 3. …]", "terror"),
    Triple("Foto antiga", "[#foto legenda]", "terror"),
    Triple("Linha do tempo", "[#linha 22:00 Saiu do bar | 23:14 ??? | 00:30 Corpo encontrado]", "misterio"),
    Triple("Saco de prova", "[#prova Nº 7 | Luva de couro, mão esquerda | Margem do rio]", "misterio"),
    Triple("Contrato mágico", "[#contrato Termos | assinatura]", "fantasia"),
    Triple("Voz de IA", "[#ia ÁRTEMIS | texto]", "scifi"),
    Triple("Grupo de mensagens", "[#grupo Família 🏠 | Mãe: texto | Pai: texto | eu: texto]", "celular"),
    Triple("Localização ao vivo", "[#localizacao Hana | Torre Norte, 13º andar | ao vivo]", "celular"),
    Triple("Abertura de capítulo", "[#capitulo 12 | O Rei sem Nome]", "estrutura"),
    Triple("No próximo capítulo", "[#proximo texto]", "estrutura"),
)
