package com.huryel.novely.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Capitulo
import com.huryel.novely.data.Obra
import com.huryel.novely.data.ResumoObra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.data.Texto
import com.huryel.novely.data.Volume
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Paleta

// ───────────────────────── Peças comuns ─────────────────────────

/** Banner da obra (ou arte gerada) com degradê que se funde ao fundo preto. */
@Composable
fun BannerFundo(o: Obra?, corBase: Long, titulo: String, modifier: Modifier) {
    val p = LocalPaleta.current
    Box(modifier) {
        if (o?.banner != null) ImagemEnquadrada(o.banner, o.bannerX, o.bannerY, o.bannerZoom, corBase, Modifier.fillMaxSize(), 1600)
        else Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(corBase), Color(0xFF120D16))))) { ArteCapa("$titulo·banner", corBase) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.3f to Color.Transparent, 0.55f to Color.Black.copy(alpha = 0.2f), 1f to p.fundo)))
    }
}

@Composable
private fun BotaoVidro(icone: ImageVector, desc: String, acao: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape).clickable(onClick = acao),
        contentAlignment = Alignment.Center,
    ) { Icon(icone, desc, tint = Color(0xFFE6DFD3), modifier = Modifier.size(21.dp)) }
}

@Composable
private fun Etiqueta(t: String) {
    Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6DFD3),
        modifier = Modifier.padding(7.dp).clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(alpha = 0.72f)).padding(horizontal = 7.dp, vertical = 3.dp))
}

@Composable
private fun Barra(f: Float, modifier: Modifier, altura: Int = 4) {
    val p = LocalPaleta.current
    val anim by animateFloatAsState(f.coerceIn(0f, 1f), tween(900), label = "barra")
    Box(modifier.height(altura.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.12f))) {
        Box(Modifier.fillMaxWidth(anim).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(p.destaque))
    }
}

/** Barra de navegação inferior. */
@Composable
fun BarraTema(abas: List<Triple<String, String, ImageVector>>, atual: String?, ir: (String) -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxWidth().background(Color(0xFF0A0908))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.borda))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(70.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            abas.forEach { (r, rot, ic) ->
                val sel = r == atual
                val cor = if (sel) p.destaque else Color(0xFF6F675B)
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp)).clickable { ir(r) }.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(ic, rot, tint = cor, modifier = Modifier.size(24.dp))
                    Text(rot, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = cor)
                }
            }
        }
    }
}

// ───────────────────────── Visualizador de capa (zoom) ─────────────────────────

@Composable
fun VisorCapa(o: Obra, fechar: () -> Unit) {
    val img = rememberImagem(o.capa, 2400)
    var escala by remember { mutableFloatStateOf(1f) }
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    Dialog(onDismissRequest = fechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        escala = (escala * zoom).coerceIn(1f, 5f)
                        if (escala > 1f) { dx += pan.x; dy += pan.y } else { dx = 0f; dy = 0f }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { if (escala > 1f) { escala = 1f; dx = 0f; dy = 0f } else escala = 2.5f })
                },
            contentAlignment = Alignment.Center,
        ) {
            val mod = Modifier.fillMaxWidth().padding(24.dp).graphicsLayer { scaleX = escala; scaleY = escala; translationX = dx; translationY = dy }
            if (img != null) Image(img, "Capa de ${o.titulo}", contentScale = ContentScale.Fit, modifier = mod)
            else CapaObra(o.titulo, null, .5f, .5f, 1f, o.corBase, mod.aspectRatio(2f / 3f), 28.sp)
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(o.titulo, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFE6DFD3), textAlign = TextAlign.Center)
                Text("pinça ou toque duplo para ampliar", fontSize = 12.sp, color = Color(0xFFA39A8B))
            }
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)) { BotaoVidro(Icons.Filled.Close, "Fechar", fechar) }
        }
    }
}

// ───────────────────────── INÍCIO (modelo B) ─────────────────────────

private fun destaque(lista: List<ResumoObra>): ResumoObra =
    lista.filter { it.lidaEm > 0 && it.ultimoCapituloId != null }.maxByOrNull { it.lidaEm } ?: lista.first()

@Composable
fun InicioClassico(lista: List<ResumoObra>, nav: NavHostController) {
    val p = LocalPaleta.current
    val repo = NovelyApp.instancia.repo
    val d = destaque(lista)
    val obra by remember(d.id) { repo.obra(d.id) }.collectAsState(initial = null)
    val cap by remember(d.ultimoCapituloId) { repo.capitulo(d.ultimoCapituloId ?: -1) }.collectAsState(initial = null)
    val outras = lista.filter { it.id != d.id }
    val lendo = outras.filter { it.status != StatusObra.ESCREVENDO }
    val escrevendo = lista.filter { it.status == StatusObra.ESCREVENDO }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
        item(key = "hero") {
            Box(Modifier.fillMaxWidth().height(344.dp)) {
                BannerFundo(obra, d.corBase, d.titulo, Modifier.fillMaxWidth().height(310.dp))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Novely", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFFE6DFD3))
                    Text(".", fontWeight = FontWeight.Black, fontSize = 24.sp, color = p.destaque)
                    Spacer(Modifier.weight(1f))
                    BotaoVidro(Icons.Filled.Search, "Buscar") { nav.navigate("buscar") }
                    Spacer(Modifier.width(10.dp))
                    BotaoVidro(Icons.Filled.Add, "Importar") { nav.navigate("importar") }
                }
                CapaObra(d.titulo, d.capa, d.capaX, d.capaY, d.capaZoom, d.corBase,
                    Modifier.align(Alignment.BottomCenter).width(150.dp).height(222.dp)
                        .shadow(18.dp, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { nav.navigate("obra/${d.id}") }, 16.sp)
            }
        }
        item(key = "info") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (d.ultimoCapituloId != null) "CONTINUAR LENDO" else "OBRA RECENTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, color = p.destaque)
                Text(d.titulo, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                cap?.let { c ->
                    val faltam = ((1 - c.progresso) * c.palavras / 230).toInt().coerceAtLeast(1)
                    Text("${c.titulo.ifEmpty { "Sem título" }} · faltam $faltam min", fontSize = 13.sp, color = p.textoSuave,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    Barra(c.progresso, Modifier.padding(top = 12.dp).width(240.dp))
                }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { if (d.ultimoCapituloId != null) nav.navigate("leitor/${d.ultimoCapituloId}") else nav.navigate("obra/${d.id}") },
                        shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("▶  Continuar", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
                    Button(
                        onClick = { nav.navigate("obra/${d.id}") }, shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF1A1815), p.texto), modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("Ver obra", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
            }
        }
        if (lendo.isNotEmpty()) {
            item(key = "s1") { Secao("Sua estante", "Ver todas (${lista.size}) ›") { nav.navigate("biblioteca") } }
            item(key = "r1") { Fileira(lendo, nav) }
        }
        if (escrevendo.isNotEmpty()) {
            item(key = "s2") { Secao("Escrevendo", "${escrevendo.size}") { nav.navigate("escrever") } }
            item(key = "r2") { Fileira(escrevendo, nav) }
        }
    }
}

@Composable
private fun Secao(titulo: String, extra: String, acao: () -> Unit) {
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(extra, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.destaque, modifier = Modifier.clickable(onClick = acao))
    }
}

@Composable
private fun Fileira(lista: List<ResumoObra>, nav: NavHostController) {
    val p = LocalPaleta.current
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(lista, key = { it.id }) { o ->
            Column(Modifier.width(112.dp).clickable { nav.navigate("obra/${o.id}") }) {
                Box {
                    CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.fillMaxWidth().height(158.dp), 12.sp)
                    val etiqueta = when {
                        o.status == StatusObra.ESCREVENDO -> "${o.totalCapitulos} caps"
                        o.totalCapitulos == 0 -> "Novo"
                        o.lidos == 0 -> "Novo"
                        else -> "Cap. ${o.lidos}"
                    }
                    Etiqueta(etiqueta)
                }
                Text(o.titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                Text(
                    when {
                        o.status == StatusObra.ESCREVENDO -> "escrevendo"
                        o.status == StatusObra.CONCLUIDA -> "lida"
                        o.totalCapitulos > 0 -> "${o.lidos * 100 / o.totalCapitulos}% lido"
                        else -> "sem capítulos"
                    }, fontSize = 12.sp, color = p.textoSuave,
                )
            }
        }
    }
}

// ───────────────────────── OBRA (clássico) ─────────────────────────

class AcoesObra(
    val voltar: () -> Unit, val menu: () -> Unit, val banner: () -> Unit, val capa: () -> Unit,
    val ler: (() -> Unit)?, val novoCap: (() -> Unit)?, val novoVol: () -> Unit,
)

@Composable
fun CabecalhoObra(estilo: String, o: Obra, caps: List<Capitulo>, volumes: List<Volume>, alvo: Capitulo?, desloc: () -> Float, a: AcoesObra) {
    val p = LocalPaleta.current
    val palavras = caps.sumOf { it.palavras.toLong() }
    val lidos = caps.count { it.progresso >= 0.98f }
    var sinopseAberta by remember { mutableStateOf(false) }
    var verCapa by remember { mutableStateOf(false) }
    var menuCapa by remember { mutableStateOf(false) }
    if (verCapa) VisorCapa(o) { verCapa = false }
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(330.dp)) {
            BannerFundo(o, o.corBase, o.titulo, Modifier.fillMaxWidth().height(300.dp).graphicsLayer { translationY = desloc() * 0.45f }.clickable(onClick = a.banner))
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                BotaoVidro(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", a.voltar)
                Spacer(Modifier.weight(1f))
                BotaoVidro(Icons.Filled.MoreVert, "Opções da obra", a.menu)
            }
            Row(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp), verticalAlignment = Alignment.Bottom) {
                Box {
                    CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
                        Modifier.width(120.dp).height(178.dp).shadow(14.dp, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable { menuCapa = true }, 14.sp)
                    DropdownMenu(menuCapa, { menuCapa = false }) {
                        DropdownMenuItem(text = { Text("Ver capa") }, onClick = { menuCapa = false; verCapa = true })
                        if (!o.trancada) DropdownMenuItem(text = { Text("Trocar ou ajustar capa") }, onClick = { menuCapa = false; a.capa() })
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.padding(bottom = 4.dp)) {
                    Text(nomeStatus(o.status).uppercase() + if (o.trancada) " · PUBLICADA" else if (!o.minha) " · SÓ LEITURA" else "", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, color = p.destaque)
                    Text(o.titulo, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, lineHeight = 26.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp))
                    if (o.autor.isNotBlank()) Text("por ${o.autor}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.texto.copy(alpha = 0.85f), modifier = Modifier.padding(top = 4.dp))
                    Text("${caps.size} caps · ${formatarPalavras(palavras)} palavras" + if (caps.isNotEmpty()) " · ${lidos * 100 / caps.size}% lido" else "",
                        fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { a.ler?.invoke() }, enabled = a.ler != null, shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Text("▶  " + when {
                        alvo == null -> "Sem capítulos"
                        alvo.progresso in 0.01f..0.97f -> "Continuar · ${alvo.titulo.ifEmpty { "sem título" }}"
                        else -> "Ler · ${alvo.titulo.ifEmpty { "sem título" }}"
                    }, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (a.novoCap != null) OutlinedButton(onClick = a.novoCap, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, p.borda)) { Text("+ Cap.", color = p.texto, fontWeight = FontWeight.Bold) }
            }
            if (o.descricao.isNotBlank()) Text(o.descricao, fontSize = 14.sp, lineHeight = 21.sp, color = p.textoSuave,
                maxLines = if (sinopseAberta) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 16.dp).animateContentSize().clickable { sinopseAberta = !sinopseAberta })
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Capítulos", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Box(Modifier.padding(top = 8.dp).width(84.dp).height(2.dp).background(p.destaque))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = a.novoVol) { Text("+ Volume", color = p.destaque) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.borda))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LinhaCapEstilo(
    estilo: String, c: Capitulo, o: Obra, atual: Boolean, lido: Boolean, numero: Int,
    abrir: () -> Unit, selecionado: Boolean = false, emSelecao: Boolean = false, segurar: () -> Unit = {},
    acoes: @Composable RowScope.() -> Unit,
) {
    val p = LocalPaleta.current
    val especial = Texto.tipoEspecial(c.titulo)
    val info = when {
        c.rascunho && o.status == StatusObra.ESCREVENDO -> "rascunho · ${formatarPalavras(c.palavras.toLong())} palavras"
        c.progresso in 0.01f..0.979f -> "${(c.progresso * 100).toInt()}% · faltam ${((1 - c.progresso) * c.palavras / 230).toInt().coerceAtLeast(1)} min"
        lido -> "lido"
        else -> "${formatarPalavras(c.palavras.toLong())} palavras"
    }.let { base ->
        if (!o.publicada || !o.minha) base else when {
            c.publicadoEm == 0L -> "○ não publicado · $base"
            c.publicadoEm > System.currentTimeMillis() -> "◷ agendado · $base"
            else -> "● publicado · $base"
        }
    }
    Row(
        Modifier.fillMaxWidth().background(if (selecionado) p.destaque.copy(alpha = 0.12f) else Color.Transparent)
            .combinedClickable(onClick = abrir, onLongClick = segurar).padding(start = 20.dp, top = 6.dp, bottom = 6.dp, end = if (emSelecao) 20.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emSelecao) {
            Box(Modifier.padding(end = 12.dp).size(22.dp).clip(CircleShape).background(if (selecionado) p.destaque else Color.Transparent)
                .border(2.dp, if (selecionado) p.destaque else p.borda, CircleShape), contentAlignment = Alignment.Center) {
                if (selecionado) Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Black, color = p.sobreDestaque)
            }
        }
        Text(if (especial != null) "◆" else "$numero", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
            color = if (atual) p.destaque else Color(0xFF6F675B), modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(c.titulo.ifEmpty { "Sem título" }, fontSize = 15.sp, fontWeight = if (atual) FontWeight.Bold else FontWeight.Medium,
                color = if (lido && !atual) Color(0xFF6F675B) else p.texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(info + c.tags.split(',').filter { it.isNotBlank() }.joinToString("") { "  #${it.trim()}" },
                fontSize = 12.sp, color = if (atual) p.destaque else if (info.startsWith("rascunho")) p.destaque2 else Color(0xFF6F675B),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        acoes()
    }
    Box(Modifier.padding(start = 20.dp, end = 20.dp).fillMaxWidth().height(1.dp).background(Color(0xFF15130F)))
}
