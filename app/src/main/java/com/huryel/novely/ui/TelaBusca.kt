package com.huryel.novely.ui

import com.huryel.novely.ui.theme.forma
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Buscador
import com.huryel.novely.data.ProgressoBusca
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Serifa
import kotlinx.coroutines.delay

@Composable
fun TelaBusca(nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val buscador = remember { Buscador(app.repo) }
    val obras by remember { app.repo.resumos() }.collectAsState(initial = emptyList())
    var consulta by rememberSaveable { mutableStateOf("") }
    var obraFiltro by rememberSaveable { mutableStateOf<Long?>(null) }
    var soTitulos by rememberSaveable { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<ProgressoBusca?>(null) }

    LaunchedEffect(consulta, obraFiltro, soTitulos) {
        resultado = null
        if (consulta.trim().length < 2) return@LaunchedEffect
        delay(300) // espera você parar de digitar
        buscador.buscar(consulta, obraFiltro, soTitulos).collect { resultado = it }
    }

    val todasFichas by produceState(emptyList<com.huryel.novely.data.Ficha>()) { value = try { app.repo.dao.sincFichas() } catch (_: Exception) { emptyList() } }
    val lugares = remember { listOf(Triple("Ajustes", "ajustes", "conta, aparência, meta diária"), Triple("Lixeira", "lixeira", "capítulos, fichas e obras apagados"),
        Triple("Backup e restauração", "backup", "cópia de segurança"), Triple("Importar TXT / EPUB", "importar", "trazer novels"), Triple("Relatório do mês", "estatisticas", "palavras e leitura"),
        Triple("Avisos", "avisos", "capítulos novos e conflitos"), Triple("Gerador de nomes", "nomes", "nomes para personagens"), Triple("Colaborar", "colaborar", "entrar numa obra com código")) }
    fun sem(t: String) = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()
    val q = consulta.trim(); val k = sem(q)
    fun marcar(t: String) = buildAnnotatedString {
        val i = sem(t).indexOf(k); if (i < 0 || k.isEmpty()) append(t) else { append(t.substring(0, i)); pushStyle(SpanStyle(color = Ed.laranja)); append(t.substring(i, (i + q.length).coerceAtMost(t.length))); pop(); append(t.substring((i + q.length).coerceAtMost(t.length))) }
    }
    val obrasAchadas = if (k.length < 2) emptyList() else obras.filter { sem(it.titulo).contains(k) }
    val fichasAchadas = if (k.length < 2) emptyList() else todasFichas.filter { sem(it.nome + " " + it.apelidos).contains(k) }.take(12)
    val lugaresAchados = if (k.length < 2) emptyList() else lugares.filter { sem(it.first + " " + it.third).contains(k) }

    Column(Modifier.fillMaxSize().background(Ed.fundo)) {
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().border(1.dp, Ed.creme).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⌕", color = Ed.suave, fontSize = 18.sp)
                Box(Modifier.weight(1f).padding(start = 10.dp)) {
                    if (consulta.isEmpty()) Text("Obra, capítulo, personagem, trecho…", color = Ed.suave, fontFamily = com.huryel.novely.ui.theme.InstrumentSerif, fontSize = 20.sp)
                    androidx.compose.foundation.text.BasicTextField(consulta, { consulta = it }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Ed.creme, fontFamily = com.huryel.novely.ui.theme.InstrumentSerif, fontSize = 20.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Ed.laranja), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), modifier = Modifier.fillMaxWidth())
                }
                if (consulta.isNotEmpty()) Text("✕", color = Ed.suave, modifier = Modifier.clickable { consulta = "" }.padding(start = 8.dp))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Todas as obras", obraFiltro == null) { obraFiltro = null }
                obras.take(12).forEach { o -> Chip(o.titulo.take(22), obraFiltro == o.id) { obraFiltro = o.id } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("No texto", !soTitulos) { soTitulos = false }
                Chip("Só títulos", soTitulos) { soTitulos = true }
            }
        }
        val r = resultado
        if (q.length < 2) {
            Text("Digite ao menos 2 letras. A busca ignora acentos e maiúsculas: “coracao” encontra “Coração”.",
                color = Ed.suave, fontSize = 14.sp, modifier = Modifier.padding(20.dp))
        } else LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp)) {
            if (obrasAchadas.isNotEmpty()) { item { GrupoBusca("Obras", obrasAchadas.size) }; items(obrasAchadas, key = { "ob${it.id}" }) { o -> ItemBusca(marcar(o.titulo), "sua biblioteca") { nav.navigate("obra/${o.id}") } } }
            if (fichasAchadas.isNotEmpty()) { item { GrupoBusca("Personagens e lugares", fichasAchadas.size) }
                items(fichasAchadas, key = { "fi${it.id}" }) { f -> ItemBusca(marcar(f.nome), (if (f.tipo == com.huryel.novely.data.TipoFicha.LUGAR) "lugar" else "personagem") + " · " + (obras.firstOrNull { it.id == f.obraId }?.titulo ?: "")) { nav.navigate("fichas/${f.obraId}") } } }
            if (lugaresAchados.isNotEmpty()) { item { GrupoBusca("No app", lugaresAchados.size) }; items(lugaresAchados, key = { "lu${it.second}" }) { (n, rota, d) -> ItemBusca(marcar(n), d) { nav.navigate(rota) } } }
            if (r != null) {
                val titulos = r.achados.filter { it.noTitulo }; val trechos = r.achados.filter { !it.noTitulo }
                item {
                    Text(if (r.terminou) "${r.achados.size}${if (r.achados.size >= 400) "+" else ""} nos capítulos" else "Procurando… ${r.lidos} de ${r.total} capítulos",
                        fontSize = 12.sp, color = Ed.suave, modifier = Modifier.padding(top = 16.dp))
                    if (!r.terminou && r.total > 0) LinearProgressIndicator(progress = { r.lidos.toFloat() / r.total }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        color = Ed.laranja, trackColor = Ed.linha, drawStopIndicator = {})
                }
                if (titulos.isNotEmpty()) { item { GrupoBusca("Capítulos", titulos.size) }
                    items(titulos, key = { "t${it.item.capId}-${it.ocorrencia}" }) { a -> ItemBusca(marcar(a.item.capTitulo), a.item.obraTitulo) { nav.navigate("leitor/${a.item.capId}") } } }
                if (trechos.isNotEmpty()) { item { GrupoBusca("Trechos", trechos.size) }
                    items(trechos, key = { "a${it.item.capId}-${it.ocorrencia}" }) { a ->
                        val trecho = buildAnnotatedString {
                            append(a.trecho)
                            val ini = a.inicio.coerceIn(0, a.trecho.length); val fim = (a.inicio + a.tamanho).coerceIn(ini, a.trecho.length)
                            addStyle(SpanStyle(color = Ed.laranja), ini, fim)
                        }
                        ItemBusca(trecho, "${a.item.obraTitulo} · ${a.item.capTitulo}") { nav.navigate("leitor/${a.item.capId}?q=${Uri.encode(q)}&n=${a.ocorrencia}") }
                    } }
                if (r.terminou && r.achados.isEmpty() && obrasAchadas.isEmpty() && fichasAchadas.isEmpty() && lugaresAchados.isEmpty()) item { Text("Nada encontrado.", color = Ed.suave, modifier = Modifier.padding(top = 20.dp)) }
            }
        }
    }
}

@Composable
private fun GrupoBusca(rot: String, n: Int) = Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp)) { Mono(rot, Modifier.weight(1f), tamanho = 10.sp); Mono("$n", tamanho = 10.sp) }

@Composable
private fun ItemBusca(t: androidx.compose.ui.text.AnnotatedString, sub: String, acao: () -> Unit) = Column(Modifier.fillMaxWidth().clickable(onClick = acao)
    .drawBehind { drawLine(Ed.linha, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1f) }.padding(vertical = 10.dp)) {
    Text(t, color = Ed.creme, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis); Mono(sub, Modifier.padding(top = 2.dp), tamanho = 9.sp)
}
