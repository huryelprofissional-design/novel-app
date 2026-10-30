package com.huryel.novely.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.EstadoAtualizacao
import com.huryel.novely.data.ResumoObra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.launch

// ───────────────────────── Ícones de traço (iguais aos mockups) ─────────────────────────

private fun icone(nome: String, vararg caminhos: String): ImageVector =
    ImageVector.Builder(nome, 24.dp, 24.dp, 24f, 24f).apply {
        caminhos.forEach { d ->
            addPath(addPathNodes(d), stroke = SolidColor(Color.White), strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }.build()

object Icones {
    val inicio = icone("inicio", "M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")
    val biblioteca = icone("biblioteca", "M4 4h4v16H4z", "M10 4h4v16h-4z", "M16 5l3.5-1 2.5 15.5-3.5 1z")
    val escrever = icone("escrever", "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4")
    val descobrir = icone("descobrir", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0", "M15.5 8.5l-2 5-5 2 2-5z")
    val perfil = icone("perfil", "M8 8a4 4 0 1 0 8 0a4 4 0 1 0-8 0", "M4 21a8 8 0 0 1 16 0")
    val sino = icone("sino", "M6 16V11a6 6 0 0 1 12 0v5l2 2H4z", "M10 21h4")
    val lupa = icone("lupa", "M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0", "M20 20l-4-4")
    val mais = icone("mais", "M12 5v14", "M5 12h14")
}

/** Troca de aba sem empilhar telas. */
fun irAba(nav: NavHostController, rota: String) {
    if (rota == "inicio") irInicio(nav) else nav.navigate(rota) {
        popUpTo("inicio") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// ───────────────────────── Peças comuns ─────────────────────────

@Composable
fun Avatar(tamanho: Dp, modifier: Modifier = Modifier) {
    val prefs = NovelyApp.instancia.prefs
    val foto by prefs.fotoPerfil.estado.collectAsState()
    val nome by prefs.autorNome.estado.collectAsState()
    val p = LocalPaleta.current
    val img = rememberImagem(foto.ifEmpty { null }, 400)
    Box(modifier.size(tamanho).clip(CircleShape).background(Brush.linearGradient(listOf(p.destaque, Color(0xFF7A3B8F)))), contentAlignment = Alignment.Center) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(nome.trim().take(1).uppercase().ifEmpty { "?" }, fontWeight = FontWeight.ExtraBold, fontSize = (tamanho.value * 0.42f).sp, color = p.sobreDestaque)
    }
}

/** Topo das abas: logo (ou título) + lupa + sino + foto. No Perfil: lupa + engrenagem. */
@Composable
fun TopoAba(titulo: String?, nav: NavHostController, noPerfil: Boolean = false) {
    val p = LocalPaleta.current
    val estadoAt by NovelyApp.instancia.atualizador.estado.collectAsState()
    val novidades by NovelyApp.instancia.prefs.novidades.estado.collectAsState()
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (titulo == null) Text(buildAnnotatedString { append("Novely"); withStyle(SpanStyle(color = p.destaque)) { append(".") } },
            fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = (-0.5).sp, modifier = Modifier.weight(1f))
        else Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = { nav.navigate("buscar") }) { Icon(Icones.lupa, "Buscar", tint = p.texto) }
        if (noPerfil) IconButton(onClick = { nav.navigate("ajustes") }) { Icon(Icons.Filled.Settings, "Ajustes", tint = p.texto) }
        else {
            IconButton(onClick = { nav.navigate("avisos") }) {
                Box {
                    Icon(Icones.sino, "Avisos", tint = p.texto)
                    if (estadoAt is EstadoAtualizacao.Disponivel || novidades.isNotBlank()) Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(p.destaque))
                }
            }
            Avatar(34.dp, Modifier.clickable { irAba(nav, "perfil") })
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun CabecalhoSecao(titulo: String, link: String? = null, aoTocar: () -> Unit = {}) {
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (link != null) Text(link, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.destaque, modifier = Modifier.clickable(onClick = aoTocar))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CapaItem(o: ResumoObra, largura: Dp, sub: String?, selo: String? = null, segurar: (() -> Unit)? = null, abrir: () -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.width(largura).combinedClickable(onClick = abrir, onLongClick = segurar)) {
        Box {
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.fillMaxWidth().aspectRatio(0.7f), (largura.value / 9).sp)
            if (selo != null) Text(selo, fontSize = 10.sp, fontWeight = FontWeight.Black, color = p.sobreDestaque,
                modifier = Modifier.padding(7.dp).clip(RoundedCornerShape(6.dp)).background(p.destaque).padding(horizontal = 6.dp, vertical = 2.dp))
        }
        Text(o.titulo, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
        if (sub != null) Text(sub, fontSize = 11.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun faltam(o: ResumoObra): String = when {
    o.totalCapitulos == 0 -> "sem capítulos"
    o.lidos >= o.totalCapitulos -> "em dia · ${o.totalCapitulos} caps"
    else -> "${o.lidos}/${o.totalCapitulos} · faltam ${o.totalCapitulos - o.lidos}"
}

// ───────────────────────── INÍCIO ─────────────────────────

private val SECOES = linkedMapOf(
    "CONT" to "Continuar lendo", "ATALHO" to "Atalho: novo capítulo", "LENDO" to "Na sua estante",
    "QUERO" to "Quero ler", "RECEBIDAS" to "De outros autores", "LIDAS" to "Lidas",
)

@Composable
fun TelaInicioAba(nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val obras by remember { repo.resumos() }.collectAsState(initial = null)
    val secoesTxt by app.prefs.secoesInicio.estado.collectAsState()
    val secoes = secoesTxt.split(',').toSet()
    var criando by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf(false) }

    val lista = obras
    if (lista == null) { Box(Modifier.fillMaxSize()); return }
    if (lista.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            TopoAba(null, nav)
            EstanteVaziaAba({ criando = true }, { nav.navigate("importar") })
        }
    } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item { TopoAba(null, nav) }
        val continuar = lista.filter { it.lidaEm > 0 && it.ultimoCapituloId != null }.maxByOrNull { it.lidaEm }
        if ("CONT" in secoes && continuar != null) item { FaixaContinuar(continuar) { nav.navigate("leitor/${continuar.ultimoCapituloId}") } }
        val escrita = lista.filter { it.minha && !it.trancada }.maxByOrNull { it.atualizadaEm }
        if ("ATALHO" in secoes && escrita != null) item {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 10.dp)) {
                Text("✎  Novo capítulo em ${escrita.titulo}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(p.superficie).border(1.dp, p.borda, RoundedCornerShape(18.dp))
                        .clickable { escopo.launch { val id = repo.novoCapitulo(escrita.id, "Capítulo ${escrita.totalCapitulos + 1}", null); nav.navigate("editor/$id") } }
                        .padding(horizontal = 14.dp, vertical = 9.dp))
            }
        }
        fun fileira(chave: String, titulo: String, itens: List<ResumoObra>, sub: (ResumoObra) -> String, selo: (ResumoObra) -> String? = { null }) {
            if (chave !in secoes || itens.isEmpty()) return
            item(key = chave) {
                CabecalhoSecao(titulo, "Ver tudo ›") { irAba(nav, "biblioteca") }
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(itens, key = { it.id }) { o -> CapaItem(o, 122.dp, sub(o), selo(o)) { nav.navigate("obra/${o.id}") } }
                }
            }
        }
        val deOutros = lista.filter { !it.minha }
        fileira("LENDO", "Na sua estante", deOutros.filter { it.status == StatusObra.LENDO }.sortedByDescending { it.lidaEm }, ::faltam) { o ->
            if (o.totalCapitulos - o.lidos in 1..99 && o.lidaEm > 0) "+${o.totalCapitulos - o.lidos}" else null
        }
        fileira("QUERO", "Quero ler", deOutros.filter { it.status == StatusObra.QUERO }, { "${it.totalCapitulos} caps" })
        fileira("RECEBIDAS", "De outros autores", deOutros.filter { it.trancada }, { if (it.autor.isNotBlank()) "por ${it.autor}" else "publicada" })
        fileira("LIDAS", "Lidas", deOutros.filter { it.status == StatusObra.CONCLUIDA }.sortedByDescending { it.lidaEm }, { "${it.totalCapitulos} caps" })
        item {
            Text("Editar início", fontSize = 13.sp, color = p.textoSuave, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp).clickable { editando = true }.padding(12.dp))
        }
    }

    if (editando) AlertDialog(
        onDismissRequest = { editando = false },
        title = { Text("O que aparece no Início") },
        text = {
            Column {
                SECOES.forEach { (k, nome) ->
                    Row(Modifier.fillMaxWidth().clickable {
                        val novo = if (k in secoes) secoes - k else secoes + k
                        app.prefs.secoesInicio.definir(SECOES.keys.filter { it in novo }.joinToString(","))
                    }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(k in secoes, null)
                        Text(nome, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { editando = false }) { Text("Pronto") } },
    )
    if (criando) DialogoCriarObra(nav) { criando = false }
}

@Composable
private fun FaixaContinuar(o: ResumoObra, abrir: () -> Unit) {
    val p = LocalPaleta.current
    val cap by remember(o.ultimoCapituloId) { NovelyApp.instancia.repo.capitulo(o.ultimoCapituloId ?: -1) }.collectAsState(initial = null)
    val c = cap
    Row(
        Modifier.padding(horizontal = 18.dp).padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.superficie)
            .border(1.dp, p.borda, RoundedCornerShape(16.dp)).clickable(onClick = abrir).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(44.dp).height(62.dp), 6.sp)
        Column(Modifier.weight(1f)) {
            Text("CONTINUAR LENDO", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = p.destaque)
            Text(o.titulo, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            if (c != null) {
                val min = ((1f - c.progresso) * c.palavras / 230f).toInt().coerceAtLeast(1)
                Text("${c.titulo.ifEmpty { "Capítulo" }} · faltam $min min", fontSize = 11.5.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.padding(top = 7.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(p.borda)) {
                    Box(Modifier.fillMaxWidth(c.progresso.coerceIn(0f, 1f)).fillMaxHeight().background(p.destaque))
                }
            }
        }
        Box(Modifier.size(42.dp).clip(CircleShape).background(p.destaque), contentAlignment = Alignment.Center) {
            Text("▶", color = p.sobreDestaque, fontSize = 15.sp)
        }
    }
}

@Composable
private fun EstanteVaziaAba(criar: () -> Unit, importar: () -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Sua estante está vazia", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text("Escreva sua primeira história ou traga uma novel (TXT/EPUB).", color = p.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = criar, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(top = 26.dp).fillMaxWidth().height(54.dp)) { Text("Criar obra", fontWeight = FontWeight.ExtraBold) }
        OutlinedButton(onClick = importar, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(top = 10.dp).fillMaxWidth().height(54.dp)) { Text("Importar TXT ou EPUB") }
    }
}

@Composable
fun DialogoCriarObra(nav: NavHostController, fechar: () -> Unit) {
    val repo = NovelyApp.instancia.repo
    val escopo = rememberCoroutineScope()
    DialogoNovaObra(aoFechar = fechar) { titulo, status, emBranco ->
        fechar()
        escopo.launch {
            val id = repo.criarObra(titulo, "", status)
            nav.navigate("obra/$id")
            if (emBranco) nav.navigate("editor/${repo.novoCapitulo(id, "", null, emBranco = true)}")
        }
    }
}

// ───────────────────────── BIBLIOTECA ─────────────────────────

@Composable
fun TelaBibliotecaAba(nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val obras by remember { app.repo.resumos() }.collectAsState(initial = emptyList())
    val colecoes by remember { app.repo.colecoes() }.collectAsState(initial = emptyList())
    val ord by app.prefs.ordenacao.estado.collectAsState()
    var aba by rememberSaveable { mutableStateOf("LENDO") }
    var colecao by rememberSaveable { mutableStateOf("") }
    var grade by rememberSaveable { mutableStateOf(true) }
    var menuOrd by remember { mutableStateOf(false) }
    var mover by remember { mutableStateOf<ResumoObra?>(null) }

    val deOutros = obras.filter { !it.minha }
    val porAba = mapOf(
        "LENDO" to deOutros.filter { it.status == StatusObra.LENDO },
        "QUERO" to deOutros.filter { it.status == StatusObra.QUERO },
        "LIDOS" to deOutros.filter { it.status == StatusObra.CONCLUIDA },
        "ARQUIVOS" to deOutros.filter { !it.trancada },
    )
    val nomesOrd = linkedMapOf("LEITURA" to "Lidas por último", "RECENTE" to "Atualizadas", "TITULO" to "Título (A–Z)",
        "TAMANHO" to "Maiores", "PROGRESSO" to "Mais lidas", "CRIACAO" to "Adicionadas")
    val lista = (porAba[aba] ?: emptyList()).filter { colecao.isEmpty() || it.colecao == colecao }.let { l ->
        when (ord) {
            "TITULO" -> l.sortedBy { it.titulo.lowercase() }
            "TAMANHO" -> l.sortedByDescending { it.totalPalavras }
            "PROGRESSO" -> l.sortedByDescending { if (it.totalCapitulos > 0) it.lidos.toFloat() / it.totalCapitulos else 0f }
            "CRIACAO" -> l.sortedByDescending { it.criadaEm }
            "RECENTE" -> l.sortedByDescending { it.atualizadaEm }
            else -> l.sortedByDescending { it.lidaEm }
        }
    }

    Column(Modifier.fillMaxSize()) {
    TopoAba("Biblioteca", nav)
    LazyVerticalGrid(GridCells.Fixed(if (grade) 3 else 1), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(if (grade) 18.dp else 6.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    listOf("LENDO" to "Lendo", "QUERO" to "Quero ler", "LIDOS" to "Lidos", "ARQUIVOS" to "Meus arquivos").forEach { (k, n) ->
                        val on = aba == k
                        Column(Modifier.clickable { aba = k }.padding(top = 6.dp)) {
                            Text("$n · ${porAba[k]?.size ?: 0}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) p.texto else p.textoSuave.copy(alpha = 0.7f))
                            Box(Modifier.padding(top = 10.dp).height(2.dp).width(if (on) 40.dp else 0.dp).background(p.destaque))
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(p.borda))
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        TextButton(onClick = { menuOrd = true }) { Text("⇅ ${nomesOrd[ord] ?: "Lidas por último"}", color = p.textoSuave, fontSize = 13.sp) }
                        DropdownMenu(menuOrd, { menuOrd = false }) {
                            nomesOrd.forEach { (k, n) -> DropdownMenuItem(text = { Text(n, color = if (k == ord) p.destaque else p.texto) }, onClick = { menuOrd = false; app.prefs.ordenacao.definir(k) }) }
                        }
                    }
                    if (aba == "ARQUIVOS") TextButton(onClick = { nav.navigate("importar") }) { Text("+ Importar", color = p.destaque, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { grade = !grade }) { Text(if (grade) "☰" else "▦", fontSize = 18.sp, color = p.textoSuave) }
                }
                if (colecoes.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Todas", colecao.isEmpty()) { colecao = "" }
                    colecoes.forEach { c -> Chip("▣ $c", colecao == c) { colecao = if (colecao == c) "" else c } }
                }
                if (lista.isEmpty()) Text(when (aba) {
                    "QUERO" -> "Segure o dedo numa capa e escolha “Quero ler” para guardar aqui."
                    "LIDOS" -> "As obras que você terminar aparecem aqui."
                    "ARQUIVOS" -> "TXT e EPUB que você importou. Só você vê."
                    else -> "Nada sendo lido. Importe uma novel ou abra uma obra recebida."
                }, color = p.textoSuave, fontSize = 14.sp, modifier = Modifier.padding(vertical = 24.dp))
            }
        }
        items(lista, key = { it.id }) { o ->
            if (grade) Column {
                CapaItem(o, 200.dp, if (o.status == StatusObra.CONCLUIDA) "lida" else faltam(o), segurar = { mover = o }) { nav.navigate("obra/${o.id}") }
                if (o.totalCapitulos > 0 && o.status == StatusObra.LENDO) Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(p.borda)) {
                    Box(Modifier.fillMaxWidth(o.lidos.toFloat() / o.totalCapitulos).fillMaxHeight().background(p.destaque))
                }
            } else Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { nav.navigate("obra/${o.id}") }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(52.dp).height(76.dp), 8.sp)
                Column(Modifier.weight(1f)) {
                    Text(o.titulo, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(faltam(o) + if (o.autor.isNotBlank()) " · por ${o.autor}" else "", fontSize = 12.sp, color = p.textoSuave, maxLines = 1)
                }
                TextButton(onClick = { mover = o }) { Text("⋮", color = p.textoSuave) }
            }
        }
    }
    }

    mover?.let { o ->
        AlertDialog(onDismissRequest = { mover = null }, title = { Text(o.titulo, maxLines = 2) },
            text = {
                Column {
                    listOf(StatusObra.LENDO to "Lendo", StatusObra.QUERO to "Quero ler", StatusObra.CONCLUIDA to "Lido").forEach { (s, n) ->
                        Row(Modifier.fillMaxWidth().clickable {
                            mover = null
                            app.escopo.launch { app.repo.dao.obraAgora(o.id)?.let { app.repo.salvarObra(it.copy(status = s)) } }
                        }.padding(vertical = 12.dp)) {
                            Text((if (o.status == s) "✓  " else "     ") + n, fontWeight = if (o.status == s) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mover = null }) { Text("Fechar") } })
    }
}

// ───────────────────────── ESCREVER ─────────────────────────

@Composable
fun TelaEscreverAba(nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val obras by remember { repo.resumos() }.collectAsState(initial = emptyList())
    val hoje by app.prefs.palavrasHoje.estado.collectAsState()
    val meta by app.prefs.metaDiaria.estado.collectAsState()
    val seq by app.prefs.sequencia.estado.collectAsState()
    val minhas = obras.filter { it.minha && !it.trancada }.sortedByDescending { it.atualizadaEm }
    var criando by remember { mutableStateOf(false) }
    var menuNovo by remember { mutableStateOf(false) }
    var escolherFichas by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item { TopoAba("Escrever", nav) }
            item {
                val f = (hoje.toFloat() / meta.coerceAtLeast(1f)).coerceIn(0f, 1f)
                Row(Modifier.padding(horizontal = 18.dp).padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.superficie)
                    .border(1.dp, p.borda, RoundedCornerShape(16.dp)).clickable { nav.navigate("ajustes/escrita") }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).drawBehind {
                        val s = 5.dp.toPx()
                        drawArc(p.borda, 0f, 360f, false, topLeft = Offset(s / 2, s / 2), size = Size(size.width - s, size.height - s), style = Stroke(s))
                        drawArc(p.destaque, -90f, 360f * f, false, topLeft = Offset(s / 2, s / 2), size = Size(size.width - s, size.height - s), style = Stroke(s, cap = StrokeCap.Round))
                    }, contentAlignment = Alignment.Center) { Text("${(f * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("Meta de hoje", fontWeight = FontWeight.ExtraBold)
                        Text("${formatarPalavras(hoje)} de ${formatarPalavras(meta.toLong())} palavras" + if (seq > 0) " · 🔥 $seq dias" else "", fontSize = 12.sp, color = p.textoSuave)
                    }
                }
            }
            item { CabecalhoSecao("Minhas obras", "+ Nova") { criando = true } }
            if (minhas.isEmpty()) item {
                Text("Você ainda não tem obras suas. Toque em + para começar.", color = p.textoSuave, modifier = Modifier.padding(horizontal = 18.dp))
            }
            items(minhas, key = { it.id }) { o ->
                Row(Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.superficie)
                    .border(1.dp, p.borda, RoundedCornerShape(16.dp)).clickable { nav.navigate("obra/${o.id}") }.padding(12.dp)) {
                    CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(74.dp).height(106.dp), 9.sp)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(when (o.status) { StatusObra.CONCLUIDA -> "CONCLUÍDA"; else -> "EM ANDAMENTO" }, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp, color = if (o.status == StatusObra.CONCLUIDA) p.destaque2 else p.destaque)
                        Text(o.titulo, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                        Text("${o.totalCapitulos} caps · ${formatarPalavras(o.totalPalavras)} palavras", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 3.dp))
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✎ Escrever", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.sobreDestaque,
                                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(p.destaque).clickable {
                                    escopo.launch {
                                        val caps = repo.dao.capitulosAgora(o.id)
                                        val alvo = caps.maxByOrNull { it.atualizadoEm }?.id ?: repo.novoCapitulo(o.id, "", null, emBranco = true)
                                        nav.navigate("editor/$alvo")
                                    }
                                }.padding(horizontal = 12.dp, vertical = 7.dp))
                            Text("+ Capítulo", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, p.borda, RoundedCornerShape(14.dp)).clickable {
                                    escopo.launch { nav.navigate("editor/${repo.novoCapitulo(o.id, "Capítulo ${o.totalCapitulos + 1}", null)}") }
                                }.padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                    }
                }
            }
            item { CabecalhoSecao("Ferramentas") }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Ferramenta("Gerador de nomes", "pessoas, lugares, técnicas") { nav.navigate("nomes") }
                    Ferramenta("Personagens", "@ e & no texto") {
                        if (minhas.size == 1) nav.navigate("fichas/${minhas[0].id}") else escolherFichas = true
                    }
                    Ferramenta("Estatísticas", "leitura e escrita") { nav.navigate("estatisticas") }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(18.dp)) {
            Box(Modifier.size(58.dp).shadow(14.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(p.destaque).clickable { menuNovo = true },
                contentAlignment = Alignment.Center) { Icon(Icones.mais, "Nova", tint = p.sobreDestaque, modifier = Modifier.size(28.dp)) }
            DropdownMenu(menuNovo, { menuNovo = false }) {
                DropdownMenuItem(text = { Text("Nova obra") }, onClick = { menuNovo = false; criando = true })
                DropdownMenuItem(text = { Text("Página em branco (escrever já)") }, onClick = {
                    menuNovo = false
                    escopo.launch {
                        val id = repo.criarObra("Nova história", "", StatusObra.ESCREVENDO)
                        nav.navigate("obra/$id"); nav.navigate("editor/${repo.novoCapitulo(id, "", null, emBranco = true)}")
                    }
                })
            }
        }
    }
    if (criando) DialogoCriarObra(nav) { criando = false }
    if (escolherFichas) AlertDialog(onDismissRequest = { escolherFichas = false }, title = { Text("Personagens de qual obra?") },
        text = {
            Column {
                if (minhas.isEmpty()) Text("Crie uma obra primeiro.")
                minhas.forEach { o -> Text(o.titulo, modifier = Modifier.fillMaxWidth().clickable { escolherFichas = false; nav.navigate("fichas/${o.id}") }.padding(vertical = 12.dp)) }
            }
        }, confirmButton = { TextButton(onClick = { escolherFichas = false }) { Text("Fechar") } })
}

@Composable
private fun Ferramenta(titulo: String, sub: String, acao: () -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.width(150.dp).clip(RoundedCornerShape(16.dp)).background(p.superficie).border(1.dp, p.borda, RoundedCornerShape(16.dp))
        .clickable(onClick = acao).padding(12.dp)) {
        Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Text(sub, fontSize = 11.sp, color = p.textoSuave, modifier = Modifier.padding(top = 4.dp))
    }
}

// ───────────────────────── DESCOBRIR ─────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelaDescobrirAba(nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val obras by remember { app.repo.resumos() }.collectAsState(initial = emptyList())
    val recebidas = obras.filter { it.trancada }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item { TopoAba("Descobrir", nav) }
        item {
            Row(Modifier.padding(horizontal = 18.dp).padding(top = 4.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).background(p.superficie)
                .border(1.dp, p.borda, RoundedCornerShape(14.dp)).clickable { nav.navigate("buscar") }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icones.lupa, null, tint = p.textoSuave, modifier = Modifier.size(18.dp))
                Text("Obras, autores, tags…", color = p.textoSuave, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
            }
        }
        item {
            Column(Modifier.padding(18.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3B2A4A), Color(0xFF120A10)))).padding(20.dp)) {
                Text("EM BREVE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = p.sobreDestaque,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(p.destaque).padding(horizontal = 8.dp, vertical = 3.dp))
                Text("Catálogo de obras", fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(top = 12.dp))
                Text("Quando a conta e a nuvem chegarem, aqui aparecem as obras publicadas por outras pessoas: em alta, novidades, ranking da semana, gêneros e tags.",
                    fontSize = 13.sp, color = p.textoSuave, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item { CabecalhoSecao("Recebidas por arquivo") }
        if (recebidas.isEmpty()) item {
            Text("Quando alguém te mandar uma obra publicada (.novely), toque no arquivo e escolha “Abrir com Novely”. Ela aparece aqui.",
                color = p.textoSuave, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 18.dp))
        } else item {
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recebidas, key = { it.id }) { o -> CapaItem(o, 112.dp, if (o.autor.isNotBlank()) "por ${o.autor}" else faltam(o)) { nav.navigate("obra/${o.id}") } }
            }
        }
        item { CabecalhoSecao("Gêneros") }
        item {
            FlowRow(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Fantasia", "Murim", "Romance", "Sistema", "Reencarnação", "Terror", "Ação", "Mistério").forEach { g ->
                    Text(g, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.textoSuave,
                        modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(p.superficie).border(1.dp, p.borda, RoundedCornerShape(18.dp))
                            .padding(horizontal = 13.dp, vertical = 8.dp))
                }
            }
        }
    }
}

// ───────────────────────── PERFIL ─────────────────────────

@Composable
fun TelaPerfilAba(nav: NavHostController) {
    val app = NovelyApp.instancia
    val prefs = app.prefs
    val p = LocalPaleta.current
    val obras by remember { app.repo.resumos() }.collectAsState(initial = emptyList())
    val nome by prefs.autorNome.estado.collectAsState()
    val bio by prefs.bio.estado.collectAsState()
    val seq by prefs.sequencia.estado.collectAsState()
    var editando by remember { mutableStateOf(false) }
    val minhas = obras.filter { it.minha && !it.trancada }
    val lendo = obras.filter { !it.minha && it.status == StatusObra.LENDO }.sortedByDescending { it.lidaEm }
    val horasLidas = remember { prefs.estatisticas().values.sumOf { it.first } / 3600f }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item { TopoAba("Perfil", nav, noPerfil = true) }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(88.dp, Modifier.clickable { editando = true })
                Text(nome.ifBlank { "Seu nome de autor" }, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(top = 12.dp),
                    color = if (nome.isBlank()) p.textoSuave else p.texto)
                val estSinc by app.sinc.estado.collectAsState()
                val ultSinc by app.prefs.ultimaSinc.estado.collectAsState()
                Text("☁ " + textoSinc(estSinc, ultSinc), fontSize = 11.sp, color = p.textoSuave.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp).clickable { nav.navigate("ajustes/conta") })
                if (bio.isNotBlank()) Text(bio, fontSize = 13.sp, color = p.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 30.dp, vertical = 4.dp))
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { editando = true }, shape = RoundedCornerShape(14.dp)) { Text("Editar perfil", color = p.texto) }
                    OutlinedButton(onClick = { nav.navigate("estatisticas") }, shape = RoundedCornerShape(14.dp)) { Text("Estatísticas", color = p.texto) }
                }
                Text("Ver meu perfil público ›", fontSize = 13.sp, color = p.destaque, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp).clickable { nav.navigate("autor/${app.pub.meuUid}") })
            }
        }
        item {
            Row(Modifier.padding(horizontal = 18.dp).padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.superficie)
                .border(1.dp, p.borda, RoundedCornerShape(16.dp))) {
                listOf(
                    "${minhas.size}" to "obras", formatarPalavras(minhas.sumOf { it.totalPalavras }) to "palavras",
                    "%.1f h".format(horasLidas).replace('.', ',') to "lendo", "$seq🔥" to "dias",
                ).forEach { (n, r) ->
                    Column(Modifier.weight(1f).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(n, fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Text(r, fontSize = 11.sp, color = p.textoSuave, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
        if (minhas.isNotEmpty()) {
            item { CabecalhoSecao("Minhas obras") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(minhas, key = { it.id }) { o -> CapaItem(o, 110.dp, "${o.totalCapitulos} caps") { nav.navigate("obra/${o.id}") } }
                }
            }
        }
        if (lendo.isNotEmpty()) {
            item { CabecalhoSecao("Lendo agora") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(lendo, key = { it.id }) { o -> CapaItem(o, 92.dp, null) { nav.navigate("obra/${o.id}") } }
                }
            }
        }
    }
    if (editando) DialogoPerfil { editando = false }
}

@Composable
private fun DialogoPerfil(fechar: () -> Unit) {
    val app = NovelyApp.instancia
    val prefs = app.prefs
    var nome by remember { mutableStateOf(prefs.autorNome.valor) }
    var bio by remember { mutableStateOf(prefs.bio.valor) }
    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) app.escopo.launch {
            val antigo = prefs.fotoPerfil.valor
            app.repo.imagens.importar(uri, 600)?.let { prefs.fotoPerfil.definir(it); prefs.perfilAlteradoEm.definir(System.currentTimeMillis()); if (antigo.isNotEmpty()) java.io.File(antigo).delete() }
        }
    }
    AlertDialog(onDismissRequest = fechar, title = { Text("Editar perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(76.dp, Modifier.clickable { seletor.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                Text("Toque na foto para trocar", fontSize = 12.sp, color = LocalPaleta.current.textoSuave)
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome de autor") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(bio, { bio = it.take(160) }, label = { Text("Bio (o que você escreve)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = {
            prefs.autorNome.definir(nome.trim()); prefs.bio.definir(bio.trim()); prefs.perfilAlteradoEm.definir(System.currentTimeMillis())
            app.escopo.launch { app.sinc.sincronizar() }; fechar()
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = fechar) { Text("Cancelar") } })
}

// ───────────────────────── AJUSTES (engrenagem do Perfil) ─────────────────────────

@Composable
fun TelaAjustesNova(nav: NavHostController) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Topo("Ajustes", nav)
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ItemAjuste("Escrita", "Meta diária de palavras") { nav.navigate("ajustes/escrita") }
            ItemAjuste("Conta", "Entrar com Google ou e-mail (em breve)") { nav.navigate("ajustes/conta") }
            ItemAjuste("Backup e restauração", "Cópia de segurança das suas obras") { nav.navigate("backup") }
            ItemAjuste("Importar TXT / EPUB", "Trazer novels para a biblioteca") { nav.navigate("importar") }
            ItemAjuste("Atualizações e problemas", "Versão ${NovelyApp.instancia.atualizador.nomeInstalado}") { nav.navigate("atualizar") }
            ItemAjuste("Sobre", "O que é o Novely") { nav.navigate("ajustes/sobre") }
            Text("A leitura (fonte, tamanho, páginas…) se ajusta no botão Aa dentro do leitor.", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun ItemAjuste(titulo: String, sub: String, acao: () -> Unit) {
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.superficie).border(1.dp, p.borda, RoundedCornerShape(16.dp))
        .clickable(onClick = acao).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(sub, fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 2.dp))
        }
        Text("›", fontSize = 22.sp, color = p.textoSuave)
    }
}

@Composable
fun TelaAjusteSecao(secao: String, nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        when (secao) {
            "escrita" -> {
                Topo("Escrita", nav)
                val meta by app.prefs.metaDiaria.estado.collectAsState()
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Row { Text("Meta diária", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text("${meta.toInt()} palavras", color = p.destaque, fontWeight = FontWeight.Bold) }
                    Slider(meta, { app.prefs.metaDiaria.definir((it / 50).toInt() * 50f) }, valueRange = 100f..5000f,
                        colors = SliderDefaults.colors(thumbColor = p.destaque, activeTrackColor = p.destaque, inactiveTrackColor = p.borda))
                    Text("A meta conta as palavras novas que você escreve no dia. Bater a meta todo dia aumenta a sequência 🔥.", fontSize = 13.sp, color = p.textoSuave)
                    Text("Personagens: escreva @Nome, descrição… no texto. Lugares: &Nome, descrição…", fontSize = 13.sp, color = p.textoSuave, modifier = Modifier.padding(top = 14.dp))
                }
            }
            "conta" -> {
                Topo("Conta", nav)
                val usuario by app.conta.usuario.collectAsState()
                val estado by app.sinc.estado.collectAsState()
                val ultima by app.prefs.ultimaSinc.estado.collectAsState()
                var sair by remember { mutableStateOf(false) }
                Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(usuario?.email ?: "", fontWeight = FontWeight.Bold)
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.superficie).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("☁ Nuvem", fontWeight = FontWeight.ExtraBold)
                        Text(textoSinc(estado, ultima), fontSize = 13.sp, color = if (estado is com.huryel.novely.data.EstadoSinc.Erro) Color(0xFFE57373) else p.textoSuave)
                        Button(onClick = { app.escopo.launch { app.sinc.sincronizar() } }, enabled = estado !is com.huryel.novely.data.EstadoSinc.Sincronizando,
                            colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.fillMaxWidth()) { Text("Sincronizar agora", fontWeight = FontWeight.Bold) }
                    }
                    Text("Suas obras, capítulos, personagens, grifos e progresso de leitura ficam guardados na nuvem e aparecem em qualquer aparelho onde você entrar com esta conta. Sem internet, tudo continua funcionando e sobe depois.",
                        fontSize = 13.sp, color = p.textoSuave)
                    OutlinedButton(onClick = { sair = true }, modifier = Modifier.fillMaxWidth()) { Text("Sair da conta", color = Color(0xFFE57373)) }
                }
                if (sair) DialogoConfirmar("Sair da conta?", "As obras continuam neste aparelho. Para usar de novo, entre com a mesma conta.", "Sair", { sair = false }) {
                    sair = false
                    app.escopo.launch { app.sinc.sincronizar(); app.conta.sair() }
                }
            }
            else -> {
                Topo("Sobre", nav)
                Text("Novely ${app.atualizador.nomeInstalado}\n\nApp para escrever, organizar e ler novels gigantes. Suas obras ficam neste aparelho; a internet só é usada para procurar atualizações.",
                    color = p.textoSuave, modifier = Modifier.padding(18.dp))
            }
        }
    }
}

fun textoSinc(e: com.huryel.novely.data.EstadoSinc, ultima: Long): String = when (e) {
    is com.huryel.novely.data.EstadoSinc.Sincronizando -> e.mensagem
    is com.huryel.novely.data.EstadoSinc.Erro -> e.mensagem
    else -> if (ultima == 0L) "Ainda não sincronizado." else {
        val min = (System.currentTimeMillis() - ultima) / 60000
        "Sincronizado " + when { min < 1 -> "agora"; min < 60 -> "há $min min"; min < 1440 -> "há ${min / 60} h"; else -> "há ${min / 1440} dias" }
    }
}

// ───────────────────────── AVISOS (sino) ─────────────────────────

@Composable
fun TelaAvisos(nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val estado by app.atualizador.estado.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Topo("Avisos", nav)
        val v = (estado as? EstadoAtualizacao.Disponivel)?.v
        val conflitos by app.prefs.conflitos.estado.collectAsState()
        val linhas = conflitos.split('\n').filter { it.isNotBlank() }.mapNotNull { l -> l.split('|').takeIf { it.size >= 2 } }
        if (v != null) ItemAvisoLinha("✦ Versão nova do Novely (build ${v.numero})", "Toque para atualizar") { nav.navigate("atualizar") }
        linhas.reversed().forEach { (cap, titulo) ->
            ItemAvisoLinha("⚠ Texto diferente em “$titulo”", "Você mudou este capítulo em dois aparelhos. Ficou o daqui; o outro está no Histórico — toque para comparar e escolher.") {
                nav.navigate("historico/$cap")
            }
        }
        val novidades by app.prefs.novidades.estado.collectAsState()
        val novas = novidades.split('\n').filter { it.isNotBlank() }.mapNotNull { l -> l.split('|').takeIf { it.size >= 3 } }
        novas.reversed().forEach { (id, titulo, n) ->
            if (id.startsWith("pub:")) ItemAvisoLinha("✦ Obra nova — $titulo", "Toque para ver no catálogo") { nav.navigate("pub/${id.removePrefix("pub:")}") }
            else ItemAvisoLinha("✦ $n capítulo(s) novo(s) em “$titulo”", "Já baixados — toque para abrir a obra") { nav.navigate("obra/$id") }
        }
        if (novas.isNotEmpty()) TextButton(onClick = { app.prefs.novidades.definir("") }, modifier = Modifier.padding(horizontal = 10.dp)) { Text("Marcar novidades como vistas", color = p.textoSuave) }
        if (linhas.isNotEmpty()) TextButton(onClick = { app.prefs.conflitos.definir("") }, modifier = Modifier.padding(horizontal = 10.dp)) { Text("Limpar avisos", color = p.textoSuave) }
        if (v == null && linhas.isEmpty() && novas.isEmpty()) Text("Nada novo por aqui. Quando as obras que você segue tiverem capítulos novos, eles aparecem aqui.",
            color = p.textoSuave, modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun ItemAvisoLinha(titulo: String, sub: String, acao: () -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.superficie).clickable(onClick = acao).padding(16.dp)) {
        Text(titulo, fontWeight = FontWeight.Bold, color = p.destaque)
        Text(sub, fontSize = 12.sp, color = p.textoSuave)
    }
}
