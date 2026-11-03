package com.huryel.novely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.forma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Números de um capítulo publicado, montados das leituras. */
data class NumerosCap(
    val titulo: String, val leitores: Int, val terminaram: Int, val minutosMedio: Float, val faixas: IntArray,
    val paradaBloco: Int, val paradaTrecho: String, val curtidas: Long, val comentarios: Long,
) { val conclusao get() = if (leitores == 0) 0f else terminaram / leitores.toFloat() }

data class NumerosObra(val leitores: Int, val ativos7: Int, val android: Int, val pc: Int, val caps: List<NumerosCap>)

/** Conta tudo a partir das leituras (puro: dá para testar). [textos] = parágrafos de cada capítulo, para mostrar onde param. */
fun calcularNumeros(
    caps: List<Triple<String, String, List<String>>>, // id público, título, parágrafos
    leituras: List<com.huryel.novely.data.Publicador.Leitura>,
    social: Map<String, Pair<Long, Long>> = emptyMap(), agora: Long = System.currentTimeMillis(),
): NumerosObra {
    val porCap = leituras.groupBy { it.cap }
    val numeros = caps.map { (id, titulo, pars) ->
        val ls = porCap[id].orEmpty()
        val terminou = { l: com.huryel.novely.data.Publicador.Leitura -> l.bloco >= (l.total * 0.95f).toInt() - 1 }
        val pararam = ls.filterNot(terminou)
        val faixas = IntArray(10)
        pararam.forEach { l -> faixas[((l.bloco + 0.5f) / l.total.coerceAtLeast(1) * 10).toInt().coerceIn(0, 9)]++ }
        // parágrafo onde mais gente parou (precisa de pelo menos 2 pessoas no mesmo lugar)
        val moda = pararam.groupingBy { it.bloco }.eachCount().filter { it.value >= 2 }.maxByOrNull { it.value }?.key ?: -1
        val trecho = pars.getOrNull(moda)?.trim()?.take(180) ?: ""
        val min = if (ls.isEmpty()) 0f else ls.sumOf { it.segundos }.toFloat() / ls.size / 60f
        val (cu, co) = social[id] ?: (0L to 0L)
        NumerosCap(titulo, ls.size, ls.count(terminou), min, faixas, moda, trecho, cu, co)
    }
    val porPessoa = leituras.groupBy { it.uid }
    return NumerosObra(
        leitores = porPessoa.size,
        ativos7 = porPessoa.count { (_, l) -> l.any { agora - it.em < 7 * 86_400_000L } },
        android = porPessoa.count { (_, l) -> l.any { it.plataforma == "android" } },
        pc = porPessoa.count { (_, l) -> l.any { it.plataforma == "pc" } },
        caps = numeros,
    )
}

@Composable
fun TelaEstatisticas(obraId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    var n by remember { mutableStateOf<NumerosObra?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(obraId) {
        try {
            n = withContext(Dispatchers.IO) {
                val o = app.repo.dao.obraAgora(obraId) ?: return@withContext null
                val pid = o.nuvemId
                val publicados = app.repo.dao.capitulosAgora(obraId).filter { it.publicadoEm in 1..System.currentTimeMillis() }
                val leituras = app.pub.leituras(pid)
                val soc = try { app.pub.capitulos(pid, autor = true).associate { it.id to (it.curtidas to it.comentarios) } } catch (_: Exception) { emptyMap() }
                val caps = publicados.map { c -> Triple(c.nuvemId, c.titulo, montarBlocos(app.repo.lerTexto(c.id)).map { it.plano }) }
                calcularNumeros(caps, leituras, soc)
            }
        } catch (e: Exception) {
            erro = if (e.message?.contains("PERMISSION", true) == true) "O Firebase ainda não deixa ler as estatísticas. Atualize as regras do Firestore (te mandei no chat)." else "Não deu para carregar: ${e.message}"
        }
    }
    Column(Modifier.fillMaxSize()) {
        Topo("Leitores", nav)
        val num = n
        when {
            erro != null -> Text(erro!!, color = p.textoSuave, modifier = Modifier.padding(20.dp))
            num == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.destaque) }
            else -> ConteudoEstatisticas(num)
        }
    }
}

@Composable
fun ConteudoEstatisticas(num: NumerosObra, abertoInicial: Int = -1) {
    val p = LocalPaleta.current
    var aberto by remember { mutableStateOf(abertoInicial) }
    val primeiro = num.caps.firstOrNull()?.leitores?.coerceAtLeast(1) ?: 1
    val maior = (num.caps.maxOfOrNull { it.leitores } ?: 1).coerceAtLeast(1)
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Numero("${num.leitores}", "leitores")
                Numero("${num.ativos7}", "nos últimos 7 dias")
                val conc = num.caps.filter { it.leitores > 0 }.map { it.conclusao }.average().takeIf { !it.isNaN() } ?: 0.0
                Numero("${(conc * 100).toInt()}%", "terminam o capítulo")
                Numero("${num.android} · ${num.pc}", "celular · PC")
            }
        }
        if (num.leitores == 0) item {
            Text("Ainda ninguém leu pela versão nova do app. Os números aparecem conforme as pessoas leem (só contam leitores, não você).",
                color = p.textoSuave, fontSize = 13.sp, modifier = Modifier.padding(vertical = 10.dp))
        }
        // insights
        val comLeitor = num.caps.withIndex().filter { it.value.leitores >= 3 }
        val queda = comLeitor.zipWithNext().maxByOrNull { (a, b) -> a.value.leitores - b.value.leitores }
        val segura = comLeitor.maxByOrNull { it.value.conclusao }
        if (queda != null && queda.first.value.leitores > queda.second.value.leitores) item {
            Dica("📉", "Maior perda: do cap. ${queda.first.index + 1} para o ${queda.second.index + 1}",
                "${queda.first.value.leitores - queda.second.value.leitores} pessoas não seguiram. Vale olhar o gancho do fim do cap. ${queda.first.index + 1}.")
        }
        if (segura != null) item { Dica("🔥", "O que mais prende: cap. ${segura.index + 1}", "${(segura.value.conclusao * 100).toInt()}% de quem começa termina “${segura.value.titulo}”.") }
        item { Text("CAPÍTULO A CAPÍTULO", style = MaterialTheme.typography.labelSmall, color = p.destaque, modifier = Modifier.padding(top = 8.dp)) }
        items(num.caps.withIndex().toList(), key = { it.index }) { (i, c) ->
            Column(Modifier.fillMaxWidth().clip(forma(16.dp)).background(p.superficie).clickable { aberto = if (aberto == i) -1 else i }.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", fontWeight = FontWeight.ExtraBold, color = p.destaque, modifier = Modifier.width(28.dp))
                    Text(c.titulo, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${c.leitores}", fontWeight = FontWeight.ExtraBold)
                    Text(" · ${(c.leitores * 100 / primeiro)}%", fontSize = 12.sp, color = p.textoSuave)
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(forma(3.dp)).background(p.borda)) {
                    Box(Modifier.fillMaxWidth(c.leitores / maior.toFloat()).fillMaxHeight().clip(forma(3.dp)).background(p.destaque))
                }
                Text("${(c.conclusao * 100).toInt()}% terminam · ${"%.1f".format(c.minutosMedio)} min em média · ♥ ${c.curtidas} · 💬 ${c.comentarios}", fontSize = 12.sp, color = p.textoSuave)
                if (aberto == i) {
                    Text("ONDE QUEM LARGOU PAROU (início → fim)", style = MaterialTheme.typography.labelSmall, color = p.destaque, modifier = Modifier.padding(top = 6.dp))
                    val topo = (c.faixas.maxOrNull() ?: 0).coerceAtLeast(1)
                    Row(Modifier.fillMaxWidth().height(34.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        c.faixas.forEach { v ->
                            Box(Modifier.weight(1f).fillMaxHeight().clip(forma(4.dp)).background(p.destaque.copy(alpha = 0.08f + 0.92f * v / topo)), contentAlignment = Alignment.Center) {
                                if (v > 0) Text("$v", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (v * 2 > topo) p.sobreDestaque else p.texto)
                            }
                        }
                    }
                    if (c.paradaTrecho.isNotEmpty()) {
                        Text("Mais gente para neste parágrafo:", fontSize = 12.sp, color = p.textoSuave)
                        Text("“${c.paradaTrecho}”", fontSize = 13.sp, fontStyle = FontStyle.Italic,
                            modifier = Modifier.fillMaxWidth().clip(forma(10.dp)).background(p.fundo.copy(alpha = 0.6f)).padding(10.dp))
                    } else if (c.leitores - c.terminaram == 0 && c.leitores > 0) Text("Todo mundo que começou terminou. 👏", fontSize = 12.sp, color = p.textoSuave)
                }
            }
        }
    }
}

@Composable
private fun Numero(valor: String, rotulo: String) {
    val p = LocalPaleta.current
    Column(Modifier.clip(forma(16.dp)).background(p.superficie).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = p.destaque)
        Text(rotulo, fontSize = 11.sp, color = p.textoSuave)
    }
}

@Composable
private fun Dica(icone: String, titulo: String, texto: String) {
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().clip(forma(16.dp)).background(p.superficie).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(icone, fontSize = 20.sp)
        Column {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(texto, fontSize = 12.5.sp, color = p.textoSuave)
        }
    }
}
