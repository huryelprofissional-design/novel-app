package com.huryel.novely.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.ObraPublica
import com.huryel.novely.data.Progresso
import com.huryel.novely.data.ResumoObra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.Serifa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ───────── "Seu mês no Novely" ─────────

private class SlideRetro(val cores: List<Color>, val rotulo: String, val numero: Long?, val unidade: String, val frase: String, val obra: ResumoObra? = null)

private suspend fun montarRetro(): List<SlideRetro> = withContext(Dispatchers.IO) {
    val app = NovelyApp.instancia; val prefs = app.prefs
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    val dias = (0 until 30).map { fmt.format(java.util.Date(System.currentTimeMillis() - it * 86_400_000L)) }.toSet()
    val leit = prefs.estatisticas().filterKeys { it in dias }
    val segundos = leit.values.sumOf { it.first.toLong() }
    val palavrasLidas = leit.values.sumOf { it.second.toLong() }
    val diaForte = leit.maxByOrNull { it.value.first }
    val esc = prefs.escritaDias().filterKeys { it in dias }
    val escritas = esc.values.sumOf { it.toLong() }
    val horas = prefs.escritaHoras()
    val horaFav = horas.indices.maxByOrNull { horas[it] }?.takeIf { horas[it] > 0 }
    val obras = app.repo.resumos().first()
    val limite = System.currentTimeMillis() - 30 * 86_400_000L
    val doMes = obras.filter { !it.minha && it.lidaEm > limite }
    val obraMes = doMes.maxByOrNull { it.lidos }
    val info = Progresso.info(prefs.xp.valor)
    val mes = SimpleDateFormat("MMMM", Locale("pt", "BR")).format(java.util.Date()).replaceFirstChar { it.uppercase() }
    val l = mutableListOf<SlideRetro>()
    l += SlideRetro(listOf(Color(0xFFFF6A2B), Color(0xFF8E2DE2)), "RETROSPECTIVA", null, "", "Seu $mes\nno Novely")
    if (segundos >= 60) l += SlideRetro(listOf(Color(0xFF1E3C72), Color(0xFF2A5298)), "VOCÊ LEU", segundos / 60, "minutos",
        if (palavrasLidas > 0) "São umas ${"%,d".format(palavrasLidas).replace(',', '.')} palavras. Dá ${maxOf(1, palavrasLidas / 80_000)} livro${if (palavrasLidas >= 160_000) "s" else ""} inteiro${if (palavrasLidas >= 160_000) "s" else ""}." else "Cada minuto conta.")
    if (diaForte != null && diaForte.value.first >= 600) {
        val d = fmt.parse(diaForte.key)!!
        l += SlideRetro(listOf(Color(0xFF7F00FF), Color(0xFFE100FF)), "SEU DIA MAIS INTENSO", diaForte.value.first / 60L, "minutos num dia só",
            SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(d) + ". Maratona de respeito.")
    }
    if (doMes.isNotEmpty()) l += SlideRetro(listOf(Color(0xFF0F2027), Color(0xFF2C5364)), "OBRAS NO MÊS", doMes.size.toLong(), if (doMes.size == 1) "obra aberta" else "obras abertas",
        "${doMes.sumOf { it.lidos }} capítulos na estante.")
    if (obraMes != null) l += SlideRetro(listOf(Color(obraMes.corBase).copy(alpha = 1f), Color(0xFF0B0B10)), "A OBRA DO SEU MÊS", null, "", obraMes.titulo, obraMes)
    if (escritas > 0) l += SlideRetro(listOf(Color(0xFFFF512F), Color(0xFFDD2476)), "VOCÊ ESCREVEU", escritas, "palavras",
        "Em ${esc.size} ${if (esc.size == 1) "dia" else "dias"}" + (horaFav?.let { ". Sua hora favorita: ${it}h." } ?: "."))
    l += SlideRetro(listOf(Color(0xFFFFB347), Color(0xFFFF6A2B)), "SEU NÍVEL", info.nivel.toLong(), Progresso.titulo(info.nivel),
        "${prefs.xp.valor} XP no total · ${maxOf(prefs.sequencia.valor, 0)}🔥 de sequência")
    l += SlideRetro(listOf(Color(0xFF141414), Color(0xFF3B2A4A)), "ATÉ O MÊS QUE VEM", null, "", "Bora fazer o próximo\nser ainda maior.")
    l
}

@Composable
fun TelaRetro(nav: NavHostController) {
    val slides by produceState<List<SlideRetro>?>(null) { value = montarRetro() }
    val l = slides ?: run { Box(Modifier.fillMaxSize().background(Color.Black)); return }
    var i by remember { mutableIntStateOf(0) }
    val tempo = remember(i) { Animatable(0f) }
    var pausado by remember { mutableStateOf(false) }
    fun proximo() { if (i < l.lastIndex) i++ else nav.popBackStack() }
    LaunchedEffect(i, pausado) { if (!pausado) { tempo.animateTo(1f, tween(((1f - tempo.value) * 6000).toInt(), easing = LinearEasing)); proximo() } }
    val s = l[i]
    val giro = rememberInfiniteTransition(label = "retro")
    val ang by giro.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "ang")
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(s.cores, start = androidx.compose.ui.geometry.Offset(0f, 2000f * ang), end = androidx.compose.ui.geometry.Offset(1400f, 2000f * (1 - ang))))
        .pointerInput(i) {
            detectTapGestures(onPress = { pausado = true; tryAwaitRelease(); pausado = false },
                onTap = { pos -> if (pos.x < size.width * 0.3f) { if (i > 0) i-- } else proximo() })
        }) {
        if (i == 0 || i == l.lastIndex) Confete(i)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                l.indices.forEach { k ->
                    Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxWidth(if (k < i) 1f else if (k == i) tempo.value else 0f).fillMaxHeight().background(Color.White))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("NOVELY", fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                Text("✕", fontSize = 22.sp, color = Color.White, modifier = Modifier.clickable { nav.popBackStack() }.padding(10.dp))
            }
            key(i) {
                val entra = remember { Animatable(0f) }
                LaunchedEffect(Unit) { entra.animateTo(1f, spring(0.6f, 120f)) }
                Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp).graphicsLayer { alpha = entra.value.coerceIn(0f, 1f); translationY = (1 - entra.value) * 120f },
                    verticalArrangement = Arrangement.Center) {
                    Text(s.rotulo, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.85f))
                    if (s.obra != null) {
                        Box(Modifier.padding(vertical = 22.dp).align(Alignment.CenterHorizontally).rotate(-4f).shadow(30.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).size(180.dp, 260.dp)) {
                            CapaObra(s.obra.titulo, s.obra.capa, s.obra.capaX, s.obra.capaY, s.obra.capaZoom, s.obra.corBase, Modifier.fillMaxSize(), 16.sp)
                        }
                    }
                    if (s.numero != null) {
                        val conta = remember { Animatable(0f) }
                        LaunchedEffect(Unit) { conta.animateTo(1f, tween(1400, 200, FastOutSlowInEasing)) }
                        Text("%,d".format((s.numero * conta.value).toLong()).replace(',', '.'), fontSize = 86.sp, lineHeight = 90.sp, fontWeight = FontWeight.Black, color = Color.White,
                            modifier = Modifier.padding(top = 6.dp))
                        Text(s.unidade, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(s.frase, fontSize = if (s.numero == null && s.obra == null) 44.sp else 18.sp, lineHeight = if (s.numero == null && s.obra == null) 48.sp else 26.sp,
                        fontWeight = if (s.numero == null && s.obra == null) FontWeight.Black else FontWeight.Medium,
                        fontFamily = if (s.obra != null) Serifa else null, color = Color.White, textAlign = if (s.obra != null) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                }
            }
        }
    }
}

/** Banner no Perfil que abre a retrospectiva. */
@Composable
fun BannerRetro(abrir: () -> Unit) {
    val mes = remember { SimpleDateFormat("MMMM", Locale("pt", "BR")).format(java.util.Date()) }
    Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(Color(0xFFFF6A2B), Color(0xFF8E2DE2)))).brilho(3000).clickable(onClick = abrir).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("✨", fontSize = 28.sp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Seu $mes no Novely", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
            Text("Sua retrospectiva do mês, estilo stories", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
        }
        Text("▶", fontSize = 18.sp, color = Color.White)
    }
}

// ───────── Feed vertical de primeiros parágrafos ─────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelaFeed(nav: NavHostController) {
    val app = NovelyApp.instancia
    val lista by produceState<List<ObraPublica>?>(null) {
        value = try {
            val a = CacheCatalogo.pegar("ALTA"); val n = try { CacheCatalogo.pegar("NOVIDADES") } catch (_: Exception) { emptyList() }
            (a + n).distinctBy { it.id }.shuffled(java.util.Random(Calendar.getInstance().get(Calendar.DAY_OF_YEAR).toLong()))
        } catch (_: Exception) { emptyList() }
    }
    val l = lista
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (l == null) Text("Carregando…", color = Color.White, modifier = Modifier.align(Alignment.Center))
        else if (l.isEmpty()) Text("Sem conexão com o catálogo.", color = Color.White, modifier = Modifier.align(Alignment.Center))
        else {
            val pager = rememberPagerState { l.size }
            VerticalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { k -> PaginaFeed(l[k], pager.currentPage == k, nav) }
        }
        Row(Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✕", fontSize = 22.sp, color = Color.White, modifier = Modifier.clickable { nav.popBackStack() }.padding(10.dp))
            Text("Primeiras linhas", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color.White)
        }
    }
}

@Composable
private fun PaginaFeed(o: ObraPublica, ativa: Boolean, nav: NavHostController) {
    val app = NovelyApp.instancia
    val capa by produceState<ByteArray?>(null, o.id) { value = CachePoster.capa(o.id) }
    val img = imagemDeBytes(capa ?: o.capaMini)
    val cor = rememberCorBytes(capa ?: o.capaMini) ?: LARANJA_FX
    val inicio by produceState<String?>(null, o.id) {
        value = app.pub.inicioDaObra(o.id).split("\n").map { it.trim() }.filter { it.length > 1 && !it.startsWith("#") }.joinToString("\n\n").take(900)
    }
    // o texto vai aparecendo enquanto a página está na tela
    val revela = remember(o.id) { Animatable(0f) }
    LaunchedEffect(ativa, inicio) { if (ativa && inicio != null) revela.animateTo(1f, tween(2600, easing = LinearEasing)) }
    Box(Modifier.fillMaxSize()) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.55f; scaleX = 1.3f; scaleY = 1.3f }.blur(34.dp))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Black.copy(alpha = 0.75f), Color.Black.copy(alpha = 0.95f)))))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 64.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.rotate(-3f).shadow(16.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).size(64.dp, 92.dp).background(cor.copy(alpha = 0.5f))) {
                    if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(o.titulo, fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 23.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("por ${o.autorNome}", fontSize = 13.sp, color = Color(0xFFCCCCCC))
                    Text((o.generos + o.tags).take(3).joinToString(" · "), fontSize = 11.sp, color = cor, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            val t = inicio
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 22.dp)) {
                if (t == null) Text("…", color = Color.White, fontSize = 22.sp)
                else {
                    val n = (t.length * revela.value).toInt()
                    Text(t.take(n), fontFamily = Serifa, fontStyle = FontStyle.Normal, fontSize = 18.sp, lineHeight = 28.sp, color = Color(0xFFF2EEE6), overflow = TextOverflow.Ellipsis)
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(90.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(Color.White).brilho(2800).clickable { nav.navigate("pub/${o.id}") },
                    contentAlignment = Alignment.Center) { Text("Continuar lendo", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp) }
            }
            Text("deslize para a próxima ↑", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))
        }
    }
}
