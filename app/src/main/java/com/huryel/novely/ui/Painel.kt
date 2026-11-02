package com.huryel.novely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Marcador
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.Laranja
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ───────────────────────── PAINEL DO ESCRITOR ─────────────────────────

private val PALAVRAS_COMUNS = setOf(
    "para", "como", "mais", "mas", "que", "ele", "ela", "eles", "elas", "isso", "isto", "aquilo", "este", "esta", "esse", "essa", "aquele", "aquela", "pelo", "pela",
    "pelos", "pelas", "num", "numa", "dos", "das", "nos", "nas", "uma", "uns", "umas", "com", "sem", "por", "sobre", "entre", "até", "ainda", "também", "muito",
    "muita", "quando", "onde", "porque", "então", "depois", "antes", "sempre", "nunca", "nada", "tudo", "cada", "outro", "outra", "mesmo", "mesma", "seu", "sua",
    "seus", "suas", "dele", "dela", "deles", "delas", "meu", "minha", "você", "vocês", "eram", "estava", "estavam", "tinha", "tinham", "foram", "fosse", "seria",
    "pode", "podia", "está", "são", "era", "foi", "ser", "ter", "tem", "não", "sim", "lhe", "lhes", "aos", "às", "já", "só", "bem", "tão", "qual", "quem",
)

@Composable
fun TelaPainel(nav: NavHostController) {
    val app = NovelyApp.instancia
    val prefs = app.prefs
    val p = LocalPaleta.current
    val seq by prefs.sequencia.estado.collectAsState()
    val meta by prefs.metaDiaria.estado.collectAsState()
    val hoje by prefs.palavrasHoje.estado.collectAsState()
    val dias = remember(hoje) {
        val m = prefs.escritaDias(); val f = SimpleDateFormat("yyyyMMdd", Locale.US); val nome = SimpleDateFormat("EEE", Locale("pt", "BR"))
        (6 downTo 0).map { d -> val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -d) }; nome.format(c.time).take(3).replaceFirstChar { it.uppercase() } to (m[f.format(c.time)] ?: 0) }
    }
    val horas = remember(hoje) { prefs.escritaHoras() }
    // melhor janela de 3 horas
    val melhor = remember(horas) { (0 until 24).maxByOrNull { h -> (0..2).sumOf { horas[(h + it) % 24] } } ?: 22 }
    val totalSemana = dias.sumOf { it.second }
    val metaSemana = (meta * 7).toInt()
    var repetidas by remember { mutableStateOf<Pair<String, List<Pair<String, Int>>>?>(null) }
    LaunchedEffect(Unit) {
        repetidas = withContext(Dispatchers.IO) {
            val minhas = app.repo.resumos().first().filter { it.minha && !it.trancada }.sortedByDescending { it.atualizadaEm }
            val cap = minhas.firstOrNull()?.let { o -> app.repo.dao.capitulosAgora(o.id).maxByOrNull { it.atualizadoEm } } ?: return@withContext null
            val texto = app.repo.lerTexto(cap.id).lowercase()
            val cont = HashMap<String, Int>()
            Regex("[\\p{L}]{4,}").findAll(com.huryel.novely.data.Marcas.limpar(texto)).forEach { m -> if (m.value !in PALAVRAS_COMUNS) cont[m.value] = (cont[m.value] ?: 0) + 1 }
            cap.titulo to cont.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        }
    }
    val maior = (dias.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 30.dp)) {
        Topo("", nav)
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Painel do escritor", fontWeight = FontWeight.Black, fontSize = 30.sp)
            Column(Modifier.fillMaxWidth().vidro(RoundedCornerShape(26.dp)).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("últimos 7 dias", fontSize = 11.sp, color = p.textoSuave)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("%,d".format(totalSemana).replace(',', '.'), fontWeight = FontWeight.Black, fontSize = 30.sp)
                            Text("  palavras", fontSize = 13.sp, color = p.textoSuave, modifier = Modifier.padding(bottom = 5.dp))
                        }
                    }
                    Text("🔥 $seq dias", fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Laranja.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 8.dp))
                }
                Row(Modifier.fillMaxWidth().height(130.dp).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    dias.forEachIndexed { i, (d, v) ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            if (v > 0) Text(if (v >= 1000) "${v / 1000}k" else "$v", fontSize = 9.sp, color = p.textoSuave)
                            Box(Modifier.fillMaxWidth().fillMaxHeight((v.toFloat() / maior * 0.78f).coerceAtLeast(0.04f)).clip(RoundedCornerShape(10.dp))
                                .background(if (i == 6) Brush.verticalGradient(listOf(Color(0xFFFF9A5A), Color(0xFFC13DFF))) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.12f)))))
                            Text(d, fontSize = 10.sp, color = p.textoSuave, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CartaoNumero("melhor horário", if (horas.sum() == 0) "—" else "${melhor}h–${(melhor + 3) % 24}h", if (horas.sum() == 0) "escreva para descobrir" else "quando você mais escreve", Modifier.weight(1f))
                CartaoNumero("meta da semana", "${(totalSemana * 100 / metaSemana.coerceAtLeast(1)).coerceAtMost(999)}%",
                    if (totalSemana >= metaSemana) "batida! 🎉" else "faltam ${"%,d".format(metaSemana - totalSemana).replace(',', '.')}", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CartaoNumero("total escrito", "%,d".format(prefs.totalEscrito.valor).replace(',', '.'), "palavras no app", Modifier.weight(1f))
                CartaoNumero("maior sequência", "${prefs.maiorSequencia.valor.coerceAtLeast(seq)} dias", "batendo a meta", Modifier.weight(1f))
            }
            repetidas?.let { (titulo, l) ->
                if (l.isNotEmpty()) Column(Modifier.fillMaxWidth().vidro(RoundedCornerShape(22.dp)).padding(16.dp)) {
                    Text("palavras que você mais repetiu em “$titulo”", fontSize = 11.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val topo = l.first().second.coerceAtLeast(1)
                    l.forEach { (w, n) ->
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(w, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(90.dp), maxLines = 1)
                            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f))) {
                                Box(Modifier.fillMaxWidth(n.toFloat() / topo).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Laranja))
                            }
                            Text("  $n×", fontSize = 11.sp, color = p.textoSuave)
                        }
                    }
                }
            }
            Text("Ver estatísticas de leitura ›", fontSize = 13.sp, color = p.textoSuave, modifier = Modifier.clickable { nav.navigate("estatisticas") }.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun CartaoNumero(rotulo: String, valor: String, sub: String, modifier: Modifier) {
    val p = LocalPaleta.current
    Column(modifier.vidro(RoundedCornerShape(22.dp)).padding(16.dp)) {
        Text(rotulo, fontSize = 11.sp, color = p.textoSuave)
        Text(valor, fontWeight = FontWeight.Black, fontSize = 22.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        Text(sub, fontSize = 11.sp, color = p.textoSuave, maxLines = 1)
    }
}

// ───────────────────────── CONQUISTAS ─────────────────────────

data class Conquista(val id: String, val icone: String, val titulo: String, val cor: Color, val ganhou: Boolean)

/** Calcula as conquistas com o que o app já sabe (palavras, leitura, sequência…) e guarda as ganhas. */
suspend fun calcularConquistas(): List<Conquista> = withContext(Dispatchers.IO) {
    val app = NovelyApp.instancia; val prefs = app.prefs
    val obras = app.repo.resumos().first()
    val minhas = obras.filter { it.minha && !it.trancada }
    val palavras = maxOf(prefs.totalEscrito.valor, minhas.sumOf { it.totalPalavras })
    val capsLidos = obras.filter { !it.minha }.sumOf { it.lidos }
    val horasLidas = prefs.estatisticas().values.sumOf { it.first } / 3600
    val grifos = try { app.repo.dao.sincMarcadores().size } catch (_: Exception) { 0 }
    val seq = maxOf(prefs.maiorSequencia.valor, prefs.sequencia.valor)
    val l = listOf(
        Conquista("p1k", "✎", "Primeiras\n1.000 palavras", Color(0xFFFF9A5A), palavras >= 1000),
        Conquista("p10k", "✎", "10 mil\npalavras", Color(0xFFFF7A3A), palavras >= 10_000),
        Conquista("p50k", "✎", "50 mil\npalavras", Color(0xFFFF6A2B), palavras >= 50_000),
        Conquista("p100k", "✒", "100 mil\npalavras", Color(0xFFE0452B), palavras >= 100_000),
        Conquista("s7", "🔥", "7 dias\nseguidos", Color(0xFFFF8A2A), seq >= 7),
        Conquista("s30", "🔥", "30 dias\nseguidos", Color(0xFFE85A1F), seq >= 30),
        Conquista("l10", "👁", "10 capítulos\nlidos", Color(0xFF5AB0FF), capsLidos >= 10),
        Conquista("l100", "👁", "100 capítulos\nlidos", Color(0xFF3A8BFF), capsLidos >= 100),
        Conquista("h10", "☾", "10 horas\nlendo", Color(0xFF7A8BFF), horasLidas >= 10),
        Conquista("fim", "✓", "Leu uma obra\ninteira", Color(0xFF6FC2B4), obras.any { !it.minha && it.status == StatusObra.CONCLUIDA }),
        Conquista("pub", "★", "Primeira obra\npublicada", Color(0xFFFFD54A), minhas.any { it.publicada }),
        Conquista("com", "💬", "Primeiro\ncomentário", Color(0xFFC13DFF), prefs.comentou.valor),
        Conquista("g10", "❝", "10 trechos\ngrifados", Color(0xFFE88AA8), grifos >= 10),
        Conquista("c20", "▤", "Obra com\n20 capítulos", Color(0xFF4FD1B8), minhas.any { it.totalCapitulos >= 20 }),
    )
    val ganhas = l.filter { it.ganhou }.joinToString(",") { it.id }
    if (ganhas != prefs.conquistas.valor) prefs.conquistas.definir(ganhas)
    l
}

@Composable
fun SecaoConquistas() {
    val p = LocalPaleta.current
    var l by remember { mutableStateOf<List<Conquista>>(emptyList()) }
    LaunchedEffect(Unit) { l = calcularConquistas() }
    if (l.isEmpty()) return
    Text("Conquistas", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(top = 22.dp))
    Text("${l.count { it.ganhou }} de ${l.size}", fontSize = 12.sp, color = p.textoSuave)
    Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        l.sortedByDescending { it.ganhou }.forEach { c ->
            Column(Modifier.width(84.dp).alpha(if (c.ganhou) 1f else 0.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(58.dp).then(if (c.ganhou) Modifier.shadow(14.dp, CircleShape, ambientColor = c.cor, spotColor = c.cor) else Modifier).clip(CircleShape)
                    .background(if (c.ganhou) Brush.radialGradient(listOf(c.cor, c.cor.copy(alpha = 0.45f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f)))),
                    contentAlignment = Alignment.Center) { Text(c.icone, fontSize = 22.sp, color = Color.White) }
                Text(c.titulo, fontSize = 10.5.sp, lineHeight = 13.sp, color = Color(0xFFCCCCCC), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

// ───────────────────────── CITAÇÕES FAVORITAS (grifos) ─────────────────────────

@Composable
fun SecaoCitacoes() {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val ctx = LocalContext.current
    var l by remember { mutableStateOf<List<Triple<Marcador, String, String>>>(emptyList()) }
    LaunchedEffect(Unit) {
        l = withContext(Dispatchers.IO) {
            val dao = app.repo.dao
            dao.sincMarcadores().filter { it.trecho.length > 20 }.sortedByDescending { it.criadoEm }.take(12).map { m ->
                Triple(m, dao.obraAgora(m.obraId)?.titulo ?: "", dao.capituloAgora(m.capId)?.titulo ?: "")
            }
        }
    }
    if (l.isEmpty()) return
    Text("Citações favoritas", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        l.forEach { (m, obra, cap) ->
            val cor = coresGrifo[m.cor] ?: Color(0xFFE9A65A)
            Column(Modifier.width(260.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(cor.copy(alpha = 0.22f), Color(0xFF161616))))
                .border(1.dp, cor.copy(alpha = 0.3f), RoundedCornerShape(22.dp)).padding(16.dp)) {
                Text("“${m.trecho.take(220)}”", fontFamily = p.fonteLeitura, fontSize = 15.sp, lineHeight = 21.sp, color = Color.White, maxLines = 6, overflow = TextOverflow.Ellipsis)
                Text("$obra · $cap", fontSize = 11.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                Text("↗ Compartilhar", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, modifier = Modifier.padding(top = 10.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color.White).clickable { compartilharTrecho(ctx, m.trecho, obra, cap, p) }.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}
