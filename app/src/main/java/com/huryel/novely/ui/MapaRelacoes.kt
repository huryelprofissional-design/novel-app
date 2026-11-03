package com.huryel.novely.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.data.Biblia
import com.huryel.novely.data.TipoFicha
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.forma
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Distribui os personagens: quem divide mais cenas fica perto (força de mola + repulsão), sempre igual para a mesma obra. */
private fun distribuir(ids: List<Long>, rels: List<Biblia.Relacao>): Map<Long, Offset> {
    val n = ids.size
    if (n == 0) return emptyMap()
    val idx = ids.withIndex().associate { it.value to it.index }
    val x = DoubleArray(n) { 0.5 + 0.35 * kotlin.math.cos(it * 2 * Math.PI / n) }
    val y = DoubleArray(n) { 0.5 + 0.35 * kotlin.math.sin(it * 2 * Math.PI / n) }
    val k = sqrt(0.6 / n)
    val arestas = rels.mapNotNull { r -> val a = idx[r.a] ?: return@mapNotNull null; val b = idx[r.b] ?: return@mapNotNull null; Triple(a, b, 1 + kotlin.math.ln(1.0 + r.cenas)) }
    val voltas = 350
    repeat(voltas) { v ->
        val dx = DoubleArray(n); val dy = DoubleArray(n)
        for (i in 0 until n) for (j in i + 1 until n) {
            val ex = x[i] - x[j]; val ey = y[i] - y[j]; val d = sqrt(ex * ex + ey * ey).coerceAtLeast(0.01)
            val f = k * k / d
            dx[i] += ex / d * f; dy[i] += ey / d * f; dx[j] -= ex / d * f; dy[j] -= ey / d * f
        }
        for ((a, b, w) in arestas) {
            val ex = x[a] - x[b]; val ey = y[a] - y[b]; val d = sqrt(ex * ex + ey * ey).coerceAtLeast(0.01)
            val f = d * d / k * w * 0.5
            dx[a] -= ex / d * f; dy[a] -= ey / d * f; dx[b] += ex / d * f; dy[b] += ey / d * f
        }
        // puxa de leve para o centro (grupos soltos não fogem)
        for (i in 0 until n) { dx[i] += (0.5 - x[i]) * 0.02; dy[i] += (0.5 - y[i]) * 0.02 }
        val t = 0.08 * (1 - v / voltas.toDouble()) + 0.002
        for (i in 0 until n) {
            val d = sqrt(dx[i] * dx[i] + dy[i] * dy[i]).coerceAtLeast(1e-9)
            x[i] = (x[i] + dx[i] / d * minOf(d, t)).coerceIn(0.06, 0.94)
            y[i] = (y[i] + dy[i] / d * minOf(d, t)).coerceIn(0.06, 0.94)
        }
    }
    return ids.associateWith { Offset(x[idx[it]!!].toFloat(), y[idx[it]!!].toFloat()) }
}

/** Mapa de relações: arraste para mover, pinça para aproximar, toque num personagem, e a barra volta no tempo. */
@Composable
fun MapaRelacoes(res: Biblia.Resultado) {
    val p = LocalPaleta.current
    val pessoas = remember(res) { res.retratos.filter { it.ficha.tipo == TipoFicha.PERSONAGEM && it.total > 0 }.take(40) }
    val ids = remember(pessoas) { pessoas.map { it.ficha.id }.toSet() }
    val rels = remember(res, ids) { res.relacoes.filter { it.a in ids && it.b in ids } }
    // até 40 pessoas: a conta leva poucos milissegundos
    val pos = remember(res) { distribuir(pessoas.map { it.ficha.id }, rels) }
    val n = res.caps.size
    var ate by remember(res) { mutableIntStateOf((n - 1).coerceAtLeast(0)) }
    var sel by remember { mutableStateOf<Long?>(null) }
    var escala by remember { mutableFloatStateOf(1f) }
    var desloc by remember { mutableStateOf(Offset.Zero) }
    val medidor = rememberTextMeasurer()
    val mortes = remember(res) { res.retratos.associate { rt -> rt.ficha.id to (rt.fatos.firstOrNull { it.tipo == Biblia.Tipo.MORTE }?.cap ?: Int.MAX_VALUE) } }
    fun ate(arr: IntArray) = (0..minOf(ate, arr.size - 1)).sumOf { arr[it] }
    val mencoes = remember(res, ate) { pessoas.associate { it.ficha.id to ate(it.porCap) } }
    val forcas = remember(res, ate) { rels.associateWith { ate(it.porCap) } }
    val maiorM = (mencoes.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val maiorF = (forcas.values.maxOrNull() ?: 1).coerceAtLeast(1)

    if (pessoas.size < 2) { Text("O mapa aparece quando dois ou mais personagens dividem cenas.", color = p.textoSuave, modifier = Modifier.padding(20.dp)); return }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).clip(forma(22.dp)).background(p.superficie).clipToBounds()) {
            val posAtual = pos
            Canvas(Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> escala = (escala * zoom).coerceIn(0.5f, 4f); desloc += pan } }
                .pointerInput(posAtual, mencoes) {
                    detectTapGestures { t ->
                        val lado = minOf(size.width, size.height) * 0.9f
                        val centro = Offset(size.width / 2f, size.height / 2f)
                        sel = posAtual?.entries?.filter { (mencoes[it.key] ?: 0) > 0 }?.minByOrNull { (id, o) -> ((centro + (o - Offset(0.5f, 0.5f)) * lado * escala + desloc) - t).getDistance() }
                            ?.takeIf { (_, o) -> ((centro + (o - Offset(0.5f, 0.5f)) * lado * escala + desloc) - t).getDistance() < 40.dp.toPx() }?.key
                    }
                }) {
                val ps = posAtual ?: return@Canvas
                val lado = minOf(size.width, size.height) * 0.9f
                val centro = Offset(size.width / 2f, size.height / 2f)
                fun tela(id: Long) = centro + (ps[id]!! - Offset(0.5f, 0.5f)) * lado * escala + desloc
                // linhas
                forcas.forEach { (rl, f) ->
                    if (f == 0) return@forEach
                    val foco = sel == null || rl.a == sel || rl.b == sel
                    drawLine(corRelacao(rl.tipo).copy(alpha = if (foco) 0.85f else 0.12f), tela(rl.a), tela(rl.b),
                        strokeWidth = (1.5f + 6f * f / maiorF) * density / 2.2f, cap = StrokeCap.Round)
                }
                // pessoas
                pessoas.forEach { rt ->
                    val id = rt.ficha.id
                    val m = mencoes[id] ?: 0
                    if (m == 0) return@forEach
                    val c = tela(id)
                    val raio = (9f + 15f * sqrt(m / maiorM.toFloat())) * density / 2.2f * escala.coerceIn(0.8f, 1.6f)
                    val morto = (mortes[id] ?: Int.MAX_VALUE) <= ate
                    val foco = sel == null || sel == id || rels.any { (it.a == sel && it.b == id || it.b == sel && it.a == id) && (forcas[it] ?: 0) > 0 }
                    val cor = if (morto) Color(0xFF6B6B6B) else p.destaque
                    drawCircle(cor.copy(alpha = if (foco) 1f else 0.25f), raio, c)
                    if (sel == id) drawCircle(Color.White, raio + 4f, c, style = Stroke(3f))
                    val nome = (if (morto) "✝ " else "") + rt.ficha.nome.split(' ').let { if (it.size > 1 && it.last().length > 2) it.drop(1).joinToString(" ") else it.first() }
                    val txt = medidor.measure(nome, TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.texto.copy(alpha = if (foco) 1f else 0.3f)))
                    drawText(txt, topLeft = Offset(c.x - txt.size.width / 2f, c.y + raio + 3f))
                }
            }
            Text("arraste · pinça para zoom · toque num nome", fontSize = 10.sp, color = p.textoSuave, modifier = Modifier.align(Alignment.TopCenter).padding(8.dp))
        }
        if (n > 1) Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            Text("Até o cap. ${ate + 1}: ${res.caps.getOrNull(ate)?.titulo ?: ""}", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Slider(value = ate.toFloat(), onValueChange = { ate = it.roundToInt() }, valueRange = 0f..(n - 1).toFloat(),
                colors = SliderDefaults.colors(thumbColor = p.destaque, activeTrackColor = p.destaque))
        }
        sel?.let { id ->
            val rt = pessoas.firstOrNull { it.ficha.id == id } ?: return@let
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 90.dp).clip(forma(18.dp)).background(p.superficie).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(rt.ficha.nome, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("${mencoes[id]} menções até aqui · ${rt.falas} falas na obra", fontSize = 12.sp, color = p.textoSuave)
                rels.filter { (it.a == id || it.b == id) && (forcas[it] ?: 0) > 0 }.sortedByDescending { forcas[it] }.take(5).forEach { rl ->
                    val outro = res.ficha(if (rl.a == id) rl.b else rl.a)?.nome ?: "?"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(corRelacao(rl.tipo)))
                        Text("  $outro — ${rl.tipo}, ${forcas[rl]} cenas", fontSize = 13.sp)
                    }
                }
            } 
        } ?: Spacer(Modifier.height(90.dp))
    }
}
