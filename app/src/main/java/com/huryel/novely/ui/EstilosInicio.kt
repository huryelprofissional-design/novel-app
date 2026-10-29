package com.huryel.novely.ui

import com.huryel.novely.ui.theme.forma
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.ResumoObra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Paleta
import com.huryel.novely.ui.theme.Serifa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

/** Seletor de estilo (Ajustes). */
@Composable
fun SeletorEstilo() {
    val prefs = NovelyApp.instancia.prefs
    val p = LocalPaleta.current
    val estilo by prefs.estilo.estado.collectAsState()
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ESTILO DO APP", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CINEMA" to "Cinema", "REVISTA" to "Revista", "ESTANTE" to "Estante").forEach { (id, nome) ->
                val sel = estilo == id
                Column(
                    Modifier.weight(1f).clip(forma(16.dp)).background(p.superficie)
                        .border(if (sel) 2.dp else 1.dp, if (sel) p.destaque else p.borda, forma(16.dp))
                        .clickable { prefs.estilo.definir(id) }.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MiniaturaEstilo(id, p)
                    Text(nome, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (sel) p.destaque else p.texto)
                }
            }
        }
        Text("Muda o Início e a página de cada obra. O leitor e o editor continuam iguais.", fontSize = 12.sp, color = p.textoSuave)
    }
}

@Composable
private fun MiniaturaEstilo(id: String, p: Paleta) {
    Box(Modifier.size(width = 56.dp, height = 70.dp).clip(forma(8.dp)).background(p.fundo).border(1.dp, p.borda, forma(8.dp))) {
        when (id) {
            "CINEMA" -> {
                Box(Modifier.fillMaxWidth().height(40.dp).background(Brush.verticalGradient(listOf(p.destaque.copy(alpha = 0.7f), p.fundo))))
                Row(Modifier.align(Alignment.BottomStart).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { Box(Modifier.size(12.dp, 18.dp).clip(forma(2.dp)).background(p.borda)) }
                }
            }
            "REVISTA" -> Column(Modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(p.texto))
                repeat(4) { Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0${it + 1}", fontSize = 6.sp, color = p.destaque, fontFamily = Serifa)
                    Spacer(Modifier.width(3.dp)); Box(Modifier.weight(1f).height(2.dp).background(p.borda))
                } }
            }
            else -> {
                Box(Modifier.align(Alignment.Center).offset(y = (-8).dp).size(22.dp, 32.dp).clip(forma(2.dp)).background(p.destaque.copy(alpha = 0.8f)))
                Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    listOf(14, 18, 12, 16, 15).forEach { h -> Box(Modifier.size(5.dp, h.dp).background(p.textoSuave.copy(alpha = 0.6f))) }
                }
            }
        }
    }
}

private fun dataHoje(): String = SimpleDateFormat("EEEE · dd MMM", Locale("pt", "BR")).format(Date()).uppercase()

private fun linhaObra(o: ResumoObra): String = when (o.status) {
    StatusObra.LENDO -> if (o.totalCapitulos > 0) "Lendo · ${o.lidos}/${o.totalCapitulos} capítulos" else "Lendo"
    StatusObra.CONCLUIDA -> "Concluída · ${o.totalCapitulos} capítulos"
    else -> "Escrevendo · ${o.totalCapitulos} capítulos · ${formatarPalavras(o.totalPalavras)} palavras"
}

private fun destaqueInicio(lista: List<ResumoObra>): ResumoObra =
    lista.filter { it.lidaEm > 0 && it.ultimoCapituloId != null }.maxByOrNull { it.lidaEm } ?: lista.first()

private fun abrirDestaque(o: ResumoObra, nav: NavHostController) {
    if (o.ultimoCapituloId != null) nav.navigate("leitor/${o.ultimoCapituloId}") else nav.navigate("obra/${o.id}")
}

// ───────────────────────── CINEMA ─────────────────────────

@Composable
fun InicioCinema(lista: List<ResumoObra>, nav: NavHostController) {
    val p = LocalPaleta.current
    val repo = NovelyApp.instancia.repo
    val d = destaqueInicio(lista)
    val obra by remember(d.id) { repo.obra(d.id) }.collectAsState(initial = null)
    val cap by remember(d.ultimoCapituloId) { repo.capitulo(d.ultimoCapituloId ?: -1) }.collectAsState(initial = null)
    val escrevendo = lista.filter { it.status == StatusObra.ESCREVENDO }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(480.dp)) {
                val ob = obra
                if (ob?.banner != null) ImagemEnquadrada(ob.banner, ob.bannerX, ob.bannerY, ob.bannerZoom, d.corBase, Modifier.fillMaxSize(), 1400)
                else Box(Modifier.fillMaxSize().background(Color(d.corBase))) { ArteCapa(d.titulo + "·cinema", d.corBase) }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to p.fundo.copy(alpha = 0.25f), 0.45f to Color.Transparent, 1f to p.fundo)))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom) {
                    Text("NOVELY", fontFamily = Serifa, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 4.sp, modifier = Modifier.weight(1f))
                    Text(dataHoje(), fontSize = 11.sp, letterSpacing = 1.sp, color = p.textoSuave)
                }
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (d.ultimoCapituloId != null) "CONTINUE DE ONDE PAROU" else "SUA OBRA MAIS RECENTE",
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = p.destaque)
                    Text(d.titulo, fontFamily = Serifa, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 40.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    cap?.let { c ->
                        Text("“${c.titulo.ifEmpty { "sem título" }}” · faltam ~${((1 - c.progresso) * c.palavras / 230).toInt().coerceAtLeast(1)} min",
                            fontFamily = Serifa, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                        Button(onClick = { abrirDestaque(d, nav) }, shape = forma(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = p.texto, contentColor = p.fundo),
                            modifier = Modifier.weight(1f).height(52.dp)) { Text("▶  Ler", fontWeight = FontWeight.ExtraBold) }
                        Button(onClick = { nav.navigate("obra/${d.id}") }, shape = forma(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = p.texto.copy(alpha = 0.14f), contentColor = p.texto),
                            modifier = Modifier.weight(1f).height(52.dp)) { Text("ⓘ  Ver obra", fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
        }
        if (escrevendo.isNotEmpty()) {
            item { TituloFileira("Escrevendo agora") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(escrevendo, key = { it.id }) { o -> CartaoCinema(o) { nav.navigate("obra/${o.id}") } }
                }
            }
        }
        item { TituloFileira("Sua estante") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(lista, key = { it.id }) { o -> PosterCinema(o) { nav.navigate("obra/${o.id}") } }
            }
        }
        val concluidas = lista.filter { it.status == StatusObra.CONCLUIDA }
        if (concluidas.isNotEmpty()) {
            item { TituloFileira("Concluídas") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(concluidas, key = { it.id }) { o -> PosterCinema(o) { nav.navigate("obra/${o.id}") } }
                }
            }
        }
    }
}

@Composable
private fun TituloFileira(t: String) {
    Text(t, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp))
}

@Composable
private fun CartaoCinema(o: ResumoObra, aoAbrir: () -> Unit) {
    val p = LocalPaleta.current
    val obra by remember(o.id) { NovelyApp.instancia.repo.obra(o.id) }.collectAsState(initial = null)
    Box(Modifier.width(230.dp).height(120.dp).clip(forma(12.dp)).clickable(onClick = aoAbrir)) {
        val ob = obra
        if (ob?.banner != null) ImagemEnquadrada(ob.banner, ob.bannerX, ob.bannerY, ob.bannerZoom, o.corBase, Modifier.fillMaxSize(), 700)
        else Box(Modifier.fillMaxSize().background(Color(o.corBase))) { ArteCapa(o.titulo + "·faixa", o.corBase) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color(0xDD000000))))
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(o.titulo, fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("✎ ${o.totalCapitulos} caps · ${formatarPalavras(o.totalPalavras)} palavras", fontSize = 11.sp, color = Color(0xFF9ED9CE))
        }
    }
}

@Composable
private fun PosterCinema(o: ResumoObra, aoAbrir: () -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.width(112.dp).clickable(onClick = aoAbrir)) {
        Box {
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.fillMaxWidth().height(168.dp), 13.sp)
            if (o.totalCapitulos > 0 && o.status == StatusObra.LENDO) {
                Text("${o.lidos * 100 / o.totalCapitulos}%", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = p.sobreDestaque,
                    modifier = Modifier.padding(6.dp).clip(forma(6.dp)).background(p.destaque).padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(o.titulo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ───────────────────────── REVISTA ─────────────────────────

@Composable
fun InicioRevista(lista: List<ResumoObra>, nav: NavHostController) {
    val p = LocalPaleta.current
    val repo = NovelyApp.instancia.repo
    val d = destaqueInicio(lista)
    val cap by remember(d.ultimoCapituloId) { repo.capitulo(d.ultimoCapituloId ?: -1) }.collectAsState(initial = null)
    var filtro by remember { mutableStateOf("TODAS") }
    val filtradas = if (filtro == "TODAS") lista else lista.filter { it.status == filtro }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 120.dp)) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
                    Text("NOVELY", fontFamily = Serifa, fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = 5.sp, modifier = Modifier.weight(1f))
                    Text(dataHoje(), fontSize = 10.sp, letterSpacing = 1.sp, color = p.textoSuave)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(p.texto))
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${lista.size} OBRAS", fontSize = 10.sp, letterSpacing = 1.sp, color = p.textoSuave)
                    Text("${lista.sumOf { it.totalCapitulos }} CAPÍTULOS", fontSize = 10.sp, letterSpacing = 1.sp, color = p.textoSuave)
                    Text("${formatarPalavras(lista.sumOf { it.totalPalavras }).uppercase()} PALAVRAS", fontSize = 10.sp, letterSpacing = 1.sp, color = p.textoSuave)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(p.borda))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clickable { abrirDestaque(d, nav) }.padding(vertical = 22.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (d.ultimoCapituloId != null) "CONTINUAR LEITURA" else "OBRA RECENTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = p.destaque)
                    Text(d.titulo, fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 33.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    cap?.let { c ->
                        Text("“${c.titulo.ifEmpty { "sem título" }}”" + if (c.resumo.isNotBlank()) " — ${c.resumo}" else "",
                            fontFamily = Serifa, fontStyle = FontStyle.Italic, fontSize = 15.sp, lineHeight = 21.sp, color = p.textoSuave, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f).height(1.dp).background(p.borda)) {
                                Box(Modifier.fillMaxWidth(c.progresso.coerceIn(0f, 1f)).height(3.dp).offset(y = (-1).dp).background(p.destaque))
                            }
                            Text("  ${(c.progresso * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.destaque)
                        }
                    }
                }
                CapaObra(d.titulo, d.capa, d.capaX, d.capaY, d.capaZoom, d.corBase, Modifier.width(92.dp).height(138.dp), 11.sp)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.borda))
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
                Text("Índice da estante", fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("TODAS" to "TODAS", StatusObra.ESCREVENDO to "ESCREVENDO", StatusObra.LENDO to "LENDO", StatusObra.CONCLUIDA to "CONCLUÍDAS").forEach { (id, t) ->
                    Text(t, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, color = if (filtro == id) p.destaque else p.textoSuave,
                        modifier = Modifier.clickable { filtro = id }.padding(vertical = 8.dp))
                }
            }
        }
        items(filtradas.size) { i ->
            val o = filtradas[i]
            Column(Modifier.fillMaxWidth().clickable { nav.navigate("obra/${o.id}") }) {
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("%02d".format(i + 1), fontFamily = Serifa, fontSize = 26.sp, color = p.destaque, modifier = Modifier.width(40.dp))
                    Column(Modifier.weight(1f)) {
                        Text(o.titulo, fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(linhaObra(o), fontSize = 12.sp, color = p.textoSuave, maxLines = 1)
                    }
                    CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(34.dp).height(50.dp), 5.sp)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(p.borda))
            }
        }
    }
}

// ───────────────────────── ESTANTE ─────────────────────────

@Composable
fun InicioEstante(lista: List<ResumoObra>, nav: NavHostController) {
    val p = LocalPaleta.current
    val inicial = lista.indexOf(destaqueInicio(lista)).coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = inicial) { lista.size }
    val atual = lista.getOrNull(pager.currentPage) ?: lista.first()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Novely", fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 26.sp, modifier = Modifier.weight(1f))
                Text("${lista.size} na estante", fontSize = 12.sp, color = p.textoSuave)
            }
        }
        item {
            Box(Modifier.fillMaxWidth().height(330.dp)
                .drawBehind { drawCircle(Brush.radialGradient(listOf(p.destaque.copy(alpha = 0.16f), Color.Transparent), center = Offset(size.width / 2, size.height * 0.45f), radius = size.width * 0.6f)) }) {
                HorizontalPager(
                    state = pager, pageSize = PageSize.Fixed(190.dp),
                    contentPadding = PaddingValues(horizontal = 100.dp), pageSpacing = 0.dp,
                    modifier = Modifier.fillMaxSize(),
                ) { pg ->
                    val o = lista[pg]
                    val desloc = (pager.currentPage - pg) + pager.currentPageOffsetFraction
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
                            Modifier.width(184.dp).height(276.dp)
                                .graphicsLayer {
                                    rotationY = (desloc * 38f).coerceIn(-45f, 45f)
                                    val e = 1f - (desloc.absoluteValue * 0.18f).coerceAtMost(0.3f)
                                    scaleX = e; scaleY = e
                                    alpha = 1f - (desloc.absoluteValue * 0.45f).coerceAtMost(0.6f)
                                    cameraDistance = 14f * density
                                }
                                .shadow(18.dp, forma(12.dp))
                                .clickable { nav.navigate("obra/${o.id}") }, 20.sp)
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(atual.titulo, fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(linhaObra(atual), fontSize = 12.sp, color = p.textoSuave)
                if (atual.totalCapitulos > 0 && atual.status != StatusObra.ESCREVENDO) {
                    Box(Modifier.width(160.dp).height(4.dp).clip(forma(2.dp)).background(p.borda)) {
                        Box(Modifier.fillMaxWidth(atual.lidos.toFloat() / atual.totalCapitulos).fillMaxHeight().background(p.destaque))
                    }
                }
                Button(onClick = { abrirDestaque(atual, nav) }, shape = forma(26.dp),
                    colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.padding(top = 6.dp).height(48.dp)) {
                    Text(if (atual.ultimoCapituloId != null) "▶  Continuar lendo" else "Abrir obra", fontWeight = FontWeight.ExtraBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    lista.indices.take(12).forEach { i ->
                        Box(Modifier.height(4.dp).width(if (i == pager.currentPage) 18.dp else 4.dp).clip(forma(2.dp))
                            .background(if (i == pager.currentPage) p.destaque else p.borda))
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 26.dp), verticalAlignment = Alignment.Bottom) {
                Text("Prateleira", fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
                Text("toque numa lombada", fontSize = 12.sp, color = p.destaque)
            }
        }
        item { Prateleira(lista, atual.id) { nav.navigate("obra/$it") } }
    }
}

@Composable
private fun Prateleira(lista: List<ResumoObra>, atualId: Long, abrir: (Long) -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(150.dp), verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            lista.forEach { o ->
                val alt = (110 + (o.titulo.hashCode().absoluteValue % 36)).dp
                val larg = (26 + (o.totalPalavras / 400_000).toInt().coerceAtMost(18)).dp
                Box(
                    Modifier.width(larg).height(alt).clip(forma(topStart = 3.dp, topEnd = 3.dp)).background(Color(o.corBase))
                        .drawBehind {
                            drawRect(Color.Black.copy(alpha = 0.25f), topLeft = Offset(size.width - 4f, 0f), size = androidx.compose.ui.geometry.Size(4f, size.height))
                            if (o.id == atualId) drawRect(p.destaque, size = androidx.compose.ui.geometry.Size(size.width, 8f))
                        }
                        .clickable { abrir(o.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(o.titulo, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF5EEE2), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.requiredWidth(alt - 14.dp).rotate(-90f))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(forma(2.dp)).background(p.borda))
    }
}
