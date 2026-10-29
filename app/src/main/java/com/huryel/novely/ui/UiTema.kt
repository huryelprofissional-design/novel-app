package com.huryel.novely.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.ResumoObra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Paleta
import com.huryel.novely.ui.theme.TemaLeitura
import com.huryel.novely.ui.theme.forma
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ───────────────────────── Fundo e barra de navegação de cada tema ─────────────────────────

/** Neon ganha brilhos coloridos no fundo; os outros, cor sólida. */
@Composable
fun Modifier.fundoTema(): Modifier {
    val p = LocalPaleta.current
    return this.background(p.fundo).then(
        if (!p.brilho) Modifier else Modifier.drawBehind {
            val w = size.width; val h = size.height
            drawCircle(Brush.radialGradient(listOf(p.destaque2.copy(alpha = 0.45f), Color.Transparent), Offset(w * 0.1f, h * 0.02f), w * 0.85f), w * 0.85f, Offset(w * 0.1f, h * 0.02f))
            drawCircle(Brush.radialGradient(listOf(p.destaque3.copy(alpha = 0.28f), Color.Transparent), Offset(w * 1.05f, h * 0.32f), w * 0.75f), w * 0.75f, Offset(w * 1.05f, h * 0.32f))
            drawCircle(Brush.radialGradient(listOf(p.destaque.copy(alpha = 0.18f), Color.Transparent), Offset(w * 0.3f, h * 1.02f), w * 0.8f), w * 0.8f, Offset(w * 0.3f, h * 1.02f))
        },
    )
}

@Composable
fun BarraTema(abas: List<Triple<String, String, ImageVector>>, atual: String?, ir: (String) -> Unit) {
    val p = LocalPaleta.current
    when (p.nome) {
        "Papel" -> Row(
            Modifier.fillMaxWidth().background(p.fundo).navigationBarsPadding().padding(horizontal = 26.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            abas.forEach { (r, rot, _) ->
                val sel = r == atual
                Text(rot, fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) p.texto else p.textoSuave,
                    textDecoration = if (sel) TextDecoration.Underline else null,
                    modifier = Modifier.clickable { ir(r) }.padding(vertical = 8.dp, horizontal = 4.dp))
            }
        }
        "Terminal" -> Row(
            Modifier.fillMaxWidth().background(p.fundo).navigationBarsPadding().padding(16.dp).border(2.dp, p.texto),
        ) {
            abas.forEachIndexed { i, (r, rot, _) ->
                val sel = r == atual
                if (i > 0) Box(Modifier.width(2.dp).height(48.dp).background(p.texto))
                Box(Modifier.weight(1f).height(48.dp).background(if (sel) p.texto else Color.Transparent).clickable { ir(r) }, contentAlignment = Alignment.Center) {
                    Text(rot.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (sel) p.fundo else p.texto)
                }
            }
        }
        "Neon" -> Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier.fillMaxWidth().height(62.dp).clip(forma(22.dp)).background(p.superficie.copy(alpha = 0.92f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), forma(22.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
            ) {
                abas.forEach { (r, rot, ic) ->
                    val sel = r == atual
                    val cor by animateColorAsState(if (sel) p.destaque else p.textoSuave, label = "cor")
                    Column(Modifier.clip(forma(14.dp)).clickable { ir(r) }.padding(horizontal = 14.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(ic, rot, tint = cor, modifier = Modifier.size(22.dp))
                        Box(Modifier.padding(top = 4.dp).size(if (sel) 5.dp else 0.dp).clip(CircleShape).background(p.destaque))
                    }
                }
            }
        }
        else -> NavigationBar(containerColor = p.superficie) {
            abas.forEach { (r, rot, ic) ->
                NavigationBarItem(
                    selected = r == atual, onClick = { ir(r) },
                    icon = { Icon(ic, contentDescription = null) }, label = { Text(rot) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = p.destaque, selectedTextColor = p.destaque, indicatorColor = p.borda,
                        unselectedIconColor = p.textoSuave, unselectedTextColor = p.textoSuave,
                    ),
                )
            }
        }
    }
}

// ───────────────────────── Escolha de tema (Ajustes) ─────────────────────────

@Composable
fun SeletorTema(atual: TemaLeitura, escolher: (TemaLeitura) -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("TEMA DO APP", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
        TemaLeitura.entries.forEach { t ->
            val q = t.paleta
            val sel = t == atual
            Row(
                Modifier.fillMaxWidth().clip(forma(18.dp)).background(q.fundo)
                    .border(if (sel) 2.dp else 1.dp, if (sel) p.destaque else p.borda, forma(18.dp))
                    .clickable { escolher(t) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(q.destaque, q.destaque2, q.destaque3).forEach { c -> Box(Modifier.size(14.dp, 34.dp).background(c)) }
                }
                Column(Modifier.weight(1f)) {
                    Text(if (q.maiusculas) q.nome.uppercase() else q.nome, fontFamily = q.fonteTitulo, fontSize = 20.sp, color = q.texto,
                        fontWeight = if (q.fonteTitulo == com.huryel.novely.ui.theme.ArchivoBlack) FontWeight.Normal else FontWeight.Bold)
                    Text(q.descricao, fontFamily = q.fonteUi, fontSize = 12.sp, color = q.textoSuave)
                }
                if (sel) Text("✓", color = q.destaque, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Text("O tema muda o app inteiro: cores, letras, formatos, o Início e o jeito das falas no leitor.", fontSize = 12.sp, color = p.textoSuave)
    }
}

// ───────────────────────── utilidades ─────────────────────────

private fun destaque(lista: List<ResumoObra>): ResumoObra =
    lista.filter { it.lidaEm > 0 && it.ultimoCapituloId != null }.maxByOrNull { it.lidaEm } ?: lista.first()

private fun abrir(o: ResumoObra, nav: NavHostController) =
    if (o.ultimoCapituloId != null) nav.navigate("leitor/${o.ultimoCapituloId}") else nav.navigate("obra/${o.id}")

private fun estadoCurto(o: ResumoObra): String = when (o.status) {
    StatusObra.ESCREVENDO -> "escrevendo"
    StatusObra.CONCLUIDA -> "lida"
    else -> if (o.totalCapitulos > 0) "${o.lidos * 100 / o.totalCapitulos}%" else "nova"
}

@Composable
private fun ordenadas(lista: List<ResumoObra>): List<ResumoObra> {
    val ord by NovelyApp.instancia.prefs.ordenacao.estado.collectAsState()
    return when (ord) {
        "TITULO" -> lista.sortedBy { it.titulo.lowercase() }
        "TAMANHO" -> lista.sortedByDescending { it.totalPalavras }
        "PROGRESSO" -> lista.sortedByDescending { if (it.totalCapitulos > 0) it.lidos.toFloat() / it.totalCapitulos else 0f }
        "CRIACAO" -> lista.sortedByDescending { it.criadaEm }
        else -> lista
    }
}

// ───────────────────────── PAPEL ─────────────────────────

@Composable
fun InicioPapel(lista: List<ResumoObra>, nav: NavHostController) {
    val p = LocalPaleta.current
    val repo = NovelyApp.instancia.repo
    val d = destaque(lista)
    val cap by remember(d.ultimoCapituloId) { repo.capitulo(d.ultimoCapituloId ?: -1) }.collectAsState(initial = null)
    val outras = ordenadas(lista).filter { it.id != d.id }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 26.dp, end = 26.dp, bottom = 120.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Novely", fontFamily = p.fonteTitulo, fontStyle = FontStyle.Italic, fontSize = 24.sp, modifier = Modifier.weight(1f))
                Box(Modifier.size(38.dp).clip(CircleShape).border(1.dp, p.texto, CircleShape).clickable { nav.navigate("buscar") }, contentAlignment = Alignment.Center) {
                    Text("⌕", fontSize = 16.sp)
                }
            }
        }
        item {
            Column(Modifier.padding(top = 44.dp)) {
                Text(if (d.ultimoCapituloId != null) "CONTINUAR" else "OBRA RECENTE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = p.destaque)
                Text(d.titulo, fontFamily = p.fonteTitulo, fontSize = 44.sp, lineHeight = 46.sp, modifier = Modifier.padding(top = 8.dp).clickable { nav.navigate("obra/${d.id}") })
                cap?.let { c ->
                    Text("capítulo ${c.titulo.ifEmpty { "sem título" }}", fontFamily = p.fonteTitulo, fontStyle = FontStyle.Italic, fontSize = 18.sp,
                        color = p.textoSuave, modifier = Modifier.padding(top = 10.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(2.dp).background(p.borda)) {
                            Box(Modifier.fillMaxWidth(c.progresso.coerceIn(0f, 1f)).fillMaxHeight().background(p.texto))
                        }
                        Text("  ${(c.progresso * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(
                    Modifier.padding(top = 22.dp).fillMaxWidth().height(54.dp).clip(CircleShape).background(p.texto).clickable { abrir(d, nav) },
                    contentAlignment = Alignment.Center,
                ) { Text(if (d.ultimoCapituloId != null) "Abrir onde parei" else "Abrir obra", color = p.fundo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
            }
        }
        if (outras.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 46.dp).drawBehind {
                    drawLine(p.texto, Offset(0f, size.height), Offset(size.width, size.height), 2f)
                }.padding(bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
                    Text("Estante", fontFamily = p.fonteTitulo, fontSize = 22.sp, modifier = Modifier.weight(1f))
                    Text("${lista.size} obras", fontSize = 12.sp, color = p.textoSuave)
                }
            }
            items(outras, key = { it.id }) { o ->
                Row(Modifier.fillMaxWidth().clickable { nav.navigate("obra/${o.id}") }.drawBehind {
                    drawLine(p.borda, Offset(0f, size.height), Offset(size.width, size.height), 2f)
                }.padding(vertical = 16.dp), verticalAlignment = Alignment.Bottom) {
                    Text(o.titulo, fontFamily = p.fonteTitulo, fontSize = 19.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (o.status == StatusObra.CONCLUIDA) p.textoSuave else p.texto)
                    Text(estadoCurto(o), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (o.status == StatusObra.ESCREVENDO) p.destaque else p.textoSuave)
                }
            }
        }
    }
}

// ───────────────────────── NEON ─────────────────────────

@Composable
fun InicioNeon(lista: List<ResumoObra>, nav: NavHostController) {
    val p = LocalPaleta.current
    val repo = NovelyApp.instancia.repo
    val prefs = NovelyApp.instancia.prefs
    val seq by prefs.sequencia.estado.collectAsState()
    val d = destaque(lista)
    val cap by remember(d.ultimoCapituloId) { repo.capitulo(d.ultimoCapituloId ?: -1) }.collectAsState(initial = null)
    val prog by animateFloatAsState(cap?.progresso ?: 0f, tween(1000), label = "p")
    val gradiente = Brush.linearGradient(listOf(p.destaque, p.destaque2, p.destaque3))
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("novely", fontFamily = p.fonteTitulo, fontWeight = FontWeight.Black, fontSize = 24.sp,
                    style = LocalTextStyle.current.copy(brush = gradiente), modifier = Modifier.weight(1f))
                if (seq > 0) Text("⚡ $seq", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clip(forma(14.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 7.dp))
            }
        }
        item {
            Box(Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(forma(30.dp)).background(gradiente).padding(2.dp)) {
                Row(
                    Modifier.fillMaxWidth().clip(forma(28.dp)).background(p.superficie).clickable { abrir(d, nav) }.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CapaObra(d.titulo, d.capa, d.capaX, d.capaY, d.capaZoom, d.corBase,
                        Modifier.width(98.dp).height(146.dp).shadow(24.dp, forma(18.dp), ambientColor = p.destaque2, spotColor = p.destaque2), 11.sp)
                    Column(Modifier.weight(1f).height(146.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(if (d.ultimoCapituloId != null) "CONTINUAR" else "RECENTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = p.destaque)
                            Text(d.titulo, fontFamily = p.fonteTitulo, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            Text(cap?.titulo?.ifEmpty { "sem título" } ?: "", fontSize = 12.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                                Box(Modifier.fillMaxWidth(prog.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(p.destaque, p.destaque2))))
                            }
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.size(44.dp).shadow(16.dp, CircleShape, ambientColor = p.destaque, spotColor = p.destaque).clip(CircleShape).background(p.destaque),
                                contentAlignment = Alignment.Center) { Text("▶", color = p.sobreDestaque, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }
        item { Text("estante", fontFamily = p.fonteTitulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 22.dp, top = 26.dp, bottom = 12.dp)) }
        item {
            val cores = listOf(p.destaque, p.destaque2, p.destaque3)
            LazyRow(contentPadding = PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(ordenadas(lista), key = { _, o -> o.id }) { i, o ->
                    Column(Modifier.width(112.dp).clickable { nav.navigate("obra/${o.id}") }) {
                        CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
                            Modifier.fillMaxWidth().height(164.dp).shadow(14.dp, forma(20.dp), ambientColor = cores[i % 3], spotColor = cores[i % 3]), 12.sp)
                        Text(o.titulo, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        Text(if (o.status == StatusObra.ESCREVENDO) "✎ escrevendo" else estadoCurto(o), fontSize = 11.sp,
                            color = if (o.status == StatusObra.ESCREVENDO) p.destaque3 else p.textoSuave)
                    }
                }
            }
        }
    }
}

// ───────────────────────── TERMINAL ─────────────────────────

@Composable
fun InicioTerminal(lista: List<ResumoObra>, nav: NavHostController) {
    val p = LocalPaleta.current
    val repo = NovelyApp.instancia.repo
    val prefs = NovelyApp.instancia.prefs
    val hoje by prefs.palavrasHoje.estado.collectAsState()
    val seq by prefs.sequencia.estado.collectAsState()
    val d = destaque(lista)
    val cap by remember(d.ultimoCapituloId) { repo.capitulo(d.ultimoCapituloId ?: -1) }.collectAsState(initial = null)
    val relogio = remember { SimpleDateFormat("EEE dd.MM · HH:mm", Locale("pt", "BR")).format(Date()).uppercase() }
    var cursor by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(550); cursor = !cursor } }
    val ord = ordenadas(lista)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 120.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp)) {
                Text("● NOVELY", color = p.destaque, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(relogio, color = p.textoSuave, fontSize = 12.sp)
            }
        }
        item {
            Column(Modifier.padding(top = 20.dp).fillMaxWidth().border(2.dp, p.texto).clickable { nav.navigate("obra/${d.id}") }.padding(16.dp)) {
                Row {
                    Text(if (d.status == StatusObra.ESCREVENDO) "[ESCREVENDO]" else "[LENDO]", fontSize = 11.sp, color = p.textoSuave, modifier = Modifier.weight(1f))
                    Text("${d.lidos}/${d.totalCapitulos}", fontSize = 11.sp, color = p.textoSuave)
                }
                Text(d.titulo.uppercase(), fontFamily = p.fonteTitulo, fontSize = 32.sp, lineHeight = 31.sp, modifier = Modifier.padding(top = 10.dp),
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text("> ${cap?.titulo?.lowercase()?.ifEmpty { "sem título" } ?: "abrir"}" + if (cursor) "_" else " ", fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val f = (cap?.progresso ?: 0f).coerceIn(0f, 1f)
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(14.dp)) {
                    if (f > 0f) Box(Modifier.weight(f).fillMaxHeight().background(p.destaque))
                    if (f < 1f) Box(Modifier.weight(1f - f).fillMaxHeight().drawBehind {
                        var x = 0f
                        while (x < size.width) { drawRect(p.borda, Offset(x, 0f), androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)); x += 6.dp.toPx() }
                    })
                }
                Row(Modifier.padding(top = 6.dp)) {
                    Text("${((cap?.progresso ?: 0f) * 100).toInt()}%", fontSize = 11.sp, color = p.textoSuave, modifier = Modifier.weight(1f))
                    cap?.let { c -> Text("~${((1 - c.progresso) * c.palavras / 230).toInt().coerceAtLeast(1)} MIN", fontSize = 11.sp, color = p.textoSuave) }
                }
            }
            Row(Modifier.fillMaxWidth().offset(y = (-2).dp)) {
                Box(Modifier.weight(1f).background(p.destaque).clickable { abrir(d, nav) }.padding(16.dp)) {
                    Text("▶ CONTINUAR", color = p.sobreDestaque, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Box(Modifier.width(64.dp).border(2.dp, p.texto).clickable { nav.navigate("obra/${d.id}") }.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text("☰", fontSize = 15.sp)
                }
            }
        }
        item {
            Row(Modifier.padding(top = 26.dp, bottom = 8.dp)) {
                Text("// ESTANTE", fontSize = 11.sp, color = p.textoSuave, modifier = Modifier.weight(1f))
                Text("%02d ITENS".format(lista.size), fontSize = 11.sp, color = p.textoSuave)
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(p.texto))
        }
        itemsIndexed(ord, key = { _, o -> o.id }) { i, o ->
            val lida = o.status == StatusObra.CONCLUIDA
            Row(Modifier.fillMaxWidth().clickable { nav.navigate("obra/${o.id}") }.drawBehind {
                drawLine(p.borda, Offset(0f, size.height), Offset(size.width, size.height), 2f)
            }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("%02d".format(i + 1), color = if (lida) p.textoSuave else p.destaque, modifier = Modifier.width(26.dp))
                Text(o.titulo.uppercase(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (lida) p.textoSuave else p.texto, textDecoration = if (lida) TextDecoration.LineThrough else null)
                if (o.status == StatusObra.ESCREVENDO) Text("EDIT", fontSize = 11.sp, color = p.fundo, modifier = Modifier.background(p.texto).padding(horizontal = 6.dp, vertical = 2.dp))
                else Text(if (lida) "OK" else estadoCurto(o).uppercase(), fontSize = 11.sp, color = p.textoSuave)
            }
        }
        item {
            Row(Modifier.padding(top = 22.dp).fillMaxWidth().border(2.dp, p.texto).height(IntrinsicSize.Min)) {
                listOf("$hoje" to "PALAVRAS HOJE", "$seq" to "DIAS SEGUIDOS", formatarPalavras(lista.sumOf { it.totalPalavras }).uppercase() to "NA ESTANTE")
                    .forEachIndexed { i, (v, r) ->
                        if (i > 0) Box(Modifier.width(2.dp).fillMaxHeight().background(p.texto))
                        Column(Modifier.weight(1f).padding(12.dp)) {
                            Text(v, fontFamily = p.fonteTitulo, fontSize = 20.sp, color = if (i == 0) p.destaque else p.texto, maxLines = 1)
                            Text(r, fontSize = 9.sp, color = p.textoSuave)
                        }
                    }
            }
        }
    }
}
