package com.huryel.novely.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Capitulo
import com.huryel.novely.data.Evento
import com.huryel.novely.data.ResumoObra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.launch
import kotlin.random.Random

// ───────────────────────── BIBLIOTECA ─────────────────────────

@Composable
fun TelaBiblioteca(nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val obras by remember { app.repo.resumos() }.collectAsState(initial = emptyList())
    val colecoes by remember { app.repo.colecoes() }.collectAsState(initial = emptyList())
    val ord by app.prefs.ordenacao.estado.collectAsState()
    var filtro by rememberSaveable2("TODAS")
    var colecao by rememberSaveable2("")
    var emLista by rememberSaveable2("GRADE")
    var menuOrd by remember { mutableStateOf(false) }

    val lista = obras
        .filter { filtro == "TODAS" || it.status == filtro }
        .filter { colecao.isEmpty() || it.colecao == colecao }
        .let { l ->
            when (ord) {
                "TITULO" -> l.sortedBy { it.titulo.lowercase() }
                "TAMANHO" -> l.sortedByDescending { it.totalPalavras }
                "PROGRESSO" -> l.sortedByDescending { if (it.totalCapitulos > 0) it.lidos.toFloat() / it.totalCapitulos else 0f }
                "CRIACAO" -> l.sortedByDescending { it.criadaEm }
                "LEITURA" -> l.sortedByDescending { it.lidaEm }
                else -> l
            }
        }
    val nomesOrd = linkedMapOf("RECENTE" to "Atualizadas", "LEITURA" to "Lidas por último", "TITULO" to "Título (A–Z)",
        "TAMANHO" to "Maiores", "PROGRESSO" to "Mais lidas", "CRIACAO" to "Adicionadas")

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (emLista == "GRADE") 3 else 1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(if (emLista == "GRADE") 18.dp else 4.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Biblioteca", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { menuOrd = true }) { Text("⇅ ${nomesOrd[ord] ?: "Atualizadas"}", fontWeight = FontWeight.SemiBold) }
                        DropdownMenu(menuOrd, { menuOrd = false }) {
                            nomesOrd.forEach { (k, n) ->
                                DropdownMenuItem(text = { Text(n, color = if (k == ord) p.destaque else p.texto) }, onClick = { menuOrd = false; app.prefs.ordenacao.definir(k) })
                            }
                        }
                    }
                    TextButton(onClick = { emLista = if (emLista == "GRADE") "LISTA" else "GRADE" }) { Text(if (emLista == "GRADE") "☰" else "▦", fontSize = 18.sp) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Todas · ${obras.size}", filtro == "TODAS") { filtro = "TODAS" }
                    Chip("Lendo", filtro == StatusObra.LENDO) { filtro = StatusObra.LENDO }
                    Chip("Escrevendo", filtro == StatusObra.ESCREVENDO) { filtro = StatusObra.ESCREVENDO }
                    Chip("Concluídas", filtro == StatusObra.CONCLUIDA) { filtro = StatusObra.CONCLUIDA }
                }
                if (colecoes.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Todas as coleções", colecao.isEmpty()) { colecao = "" }
                    colecoes.forEach { c -> Chip("▣ $c", colecao == c) { colecao = if (colecao == c) "" else c } }
                }
                if (lista.isEmpty()) Text("Nada aqui com esses filtros.", color = p.textoSuave, modifier = Modifier.padding(vertical = 20.dp))
            }
        }
        items(lista, key = { it.id }) { o ->
            if (emLista == "GRADE") Column(Modifier.clickable { nav.navigate("obra/${o.id}") }) {
                CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.fillMaxWidth().aspectRatio(2f / 3f), 12.sp)
                Text(o.titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                Text(resumoLinha(o), fontSize = 11.sp, color = p.textoSuave, maxLines = 1)
            } else Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { nav.navigate("obra/${o.id}") }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(52.dp).height(76.dp), 8.sp)
                Column(Modifier.weight(1f)) {
                    Text(o.titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(resumoLinha(o) + if (o.colecao.isNotEmpty()) " · ▣ ${o.colecao}" else "", fontSize = 12.sp, color = p.textoSuave, maxLines = 1)
                    if (o.totalCapitulos > 0 && o.status != StatusObra.ESCREVENDO) {
                        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(p.borda)) {
                            Box(Modifier.fillMaxWidth(o.lidos.toFloat() / o.totalCapitulos).fillMaxHeight().background(p.destaque))
                        }
                    }
                }
            }
        }
    }
}

private fun resumoLinha(o: ResumoObra) = when (o.status) {
    StatusObra.ESCREVENDO -> "✎ ${o.totalCapitulos} caps · ${formatarPalavras(o.totalPalavras)}"
    StatusObra.CONCLUIDA -> "lida · ${o.totalCapitulos} caps"
    else -> if (o.totalCapitulos > 0) "${o.lidos * 100 / o.totalCapitulos}% · ${o.lidos}/${o.totalCapitulos}" else "sem capítulos"
}

@Composable
private fun rememberSaveable2(inicial: String): MutableState<String> =
    androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(inicial) }

// ───────────────────────── LINHA DO TEMPO ─────────────────────────

@Composable
fun TelaLinhaTempo(obraId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    val eventos by remember(obraId) { repo.eventos(obraId) }.collectAsState(initial = emptyList())
    val caps by remember(obraId) { repo.capitulos(obraId) }.collectAsState(initial = emptyList())
    var editando by remember { mutableStateOf<Evento?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item { Topo("Linha do tempo", nav) }
            if (eventos.isEmpty()) item {
                Text("Registre os acontecimentos da história em ordem: batalhas, revelações, mortes, viagens… e ligue cada um ao capítulo onde acontece.",
                    color = p.textoSuave, modifier = Modifier.padding(20.dp))
            }
            itemsIndexed(eventos, key = { _, e -> e.id }) { i, e ->
                val cap = caps.firstOrNull { it.id == e.capId }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 20.dp)) {
                    Box(Modifier.width(26.dp).fillMaxHeight()) {
                        Box(Modifier.align(Alignment.TopCenter).width(2.dp).fillMaxHeight().background(if (i == eventos.lastIndex) Color.Transparent else p.borda).padding(top = 14.dp))
                        Box(Modifier.align(Alignment.TopCenter).padding(top = 16.dp).size(14.dp).clip(CircleShape).background(p.destaque))
                    }
                    Column(Modifier.weight(1f).padding(start = 10.dp, top = 8.dp, bottom = 14.dp).clip(RoundedCornerShape(16.dp)).background(p.superficie)
                        .clickable { editando = e }.padding(14.dp)) {
                        if (e.quando.isNotBlank()) Text(e.quando.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = p.destaque)
                        Text(e.titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (e.descricao.isNotBlank()) Text(e.descricao, fontSize = 14.sp, color = p.textoSuave, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp))
                        if (cap != null) Text("↗ ${cap.titulo.ifEmpty { "sem título" }}", fontSize = 12.sp, color = p.destaque, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp).clickable { nav.navigate("leitor/${cap.id}") })
                        Row(Modifier.padding(top = 4.dp)) {
                            TextButton(onClick = { app.escopo.launch { repo.moverEvento(eventos, e, -1) } }, enabled = i > 0) { Text("↑") }
                            TextButton(onClick = { app.escopo.launch { repo.moverEvento(eventos, e, 1) } }, enabled = i < eventos.lastIndex) { Text("↓") }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(onClick = { editando = Evento(obraId = obraId, titulo = "", ordem = 0.0) },
            containerColor = p.destaque, contentColor = p.sobreDestaque, shape = RoundedCornerShape(18.dp),
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)) { Text("＋ Evento", fontWeight = FontWeight.Bold) }
    }

    editando?.let { ini ->
        var e by remember(ini) { mutableStateOf(ini) }
        var escolherCap by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { editando = null },
            title = { Text(if (ini.id == 0L) "Novo evento" else "Editar evento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(e.titulo, { e = e.copy(titulo = it) }, label = { Text("O que aconteceu") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(e.quando, { e = e.copy(quando = it) }, label = { Text("Quando (ex.: Ano 3, Dia 12)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(e.descricao, { e = e.copy(descricao = it) }, label = { Text("Detalhes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    val cap = caps.firstOrNull { it.id == e.capId }
                    OutlinedButton(onClick = { escolherCap = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (cap != null) "Capítulo: ${cap.titulo.ifEmpty { "sem título" }}" else "Ligar a um capítulo…", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (ini.id != 0L) TextButton(onClick = { editando = null; app.escopo.launch { repo.excluirEvento(ini) } }) { Text("Excluir evento", color = Color(0xFFE57373)) }
                }
            },
            confirmButton = { TextButton(onClick = { if (e.titulo.isNotBlank()) { editando = null; app.escopo.launch { repo.salvarEvento(e) } } }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { editando = null }) { Text("Cancelar") } },
        )
        if (escolherCap) AlertDialog(
            onDismissRequest = { escolherCap = false },
            title = { Text("Capítulo") },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    item { Text("Nenhum", modifier = Modifier.fillMaxWidth().clickable { e = e.copy(capId = null); escolherCap = false }.padding(vertical = 12.dp)) }
                    items(caps, key = { it.id }) { c ->
                        Text(c.titulo.ifEmpty { "sem título" }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().clickable { e = e.copy(capId = c.id); escolherCap = false }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { escolherCap = false }) { Text("Fechar") } },
        )
    }
}

// ───────────────────────── REORDENAR (arrastar) ─────────────────────────

@Composable
fun TelaReordenar(obraId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    val itens = remember { mutableStateListOf<Capitulo>() }
    var carregou by remember { mutableStateOf(false) }
    LaunchedEffect(obraId) { itens.clear(); itens.addAll(repo.dao.capitulosAgora(obraId)); carregou = true }
    val estado = rememberLazyListState()
    var arrastando by remember { mutableStateOf<Long?>(null) }
    var deslocamento by remember { mutableFloatStateOf(0f) }
    var mudou by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Topo("Reordenar capítulos", nav) {
            TextButton(onClick = {
                val ids = itens.map { it.id }
                app.escopo.launch { repo.reordenar(ids) }
                nav.popBackStack()
            }, enabled = mudou) { Text("Salvar", fontWeight = FontWeight.Bold) }
        }
        Text("Segure um capítulo e arraste para cima ou para baixo.", fontSize = 13.sp, color = p.textoSuave, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        if (carregou) LazyColumn(state = estado, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            itemsIndexed(itens, key = { _, c -> c.id }) { i, c ->
                val ativo = arrastando == c.id
                Row(
                    Modifier.fillMaxWidth().zIndex(if (ativo) 1f else 0f)
                        .graphicsLayer { translationY = if (ativo) deslocamento else 0f; scaleX = if (ativo) 1.03f else 1f; scaleY = if (ativo) 1.03f else 1f }
                        .padding(vertical = 3.dp).shadow(if (ativo) 12.dp else 0.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp)).background(if (ativo) p.borda else p.superficie)
                        .pointerInput(c.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { arrastando = c.id; deslocamento = 0f },
                                onDragEnd = { arrastando = null; deslocamento = 0f },
                                onDragCancel = { arrastando = null; deslocamento = 0f },
                                onDrag = { mudanca, d ->
                                    mudanca.consume()
                                    deslocamento += d.y
                                    val idx = itens.indexOfFirst { it.id == c.id }
                                    val altura = size.height.toFloat() + 6.dp.toPx()
                                    if (deslocamento > altura * 0.6f && idx < itens.lastIndex) {
                                        itens.add(idx + 1, itens.removeAt(idx)); deslocamento -= altura; mudou = true
                                    } else if (deslocamento < -altura * 0.6f && idx > 0) {
                                        itens.add(idx - 1, itens.removeAt(idx)); deslocamento += altura; mudou = true
                                    }
                                },
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${i + 1}", fontWeight = FontWeight.ExtraBold, color = if (ativo) p.destaque else p.textoSuave, modifier = Modifier.width(40.dp))
                    Text(c.titulo.ifEmpty { "sem título" }, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("≡", fontSize = 20.sp, color = p.textoSuave)
                }
            }
        }
    }
}

// ───────────────────────── GERADOR DE NOMES (offline) ─────────────────────────

private object Nomes {
    val silabasOriente = listOf("ka", "ri", "shin", "yun", "hae", "jin", "mu", "ryu", "seo", "han", "ten", "lin", "xiao", "mei", "wei", "zhen", "kai", "sora", "en", "ga", "yeon", "hwa", "jun", "tae", "ming")
    val silabasOcidente = listOf("al", "ric", "el", "dor", "an", "thar", "is", "mar", "ven", "lor", "ae", "wyn", "ca", "rin", "dra", "kel", "os", "the", "ra", "vel", "ion", "bran", "syl", "mor", "eth")
    val silabasBr = listOf("ra", "fa", "el", "lu", "ca", "jo", "ão", "ma", "ri", "ta", "be", "ni", "ce", "lo", "di", "go", "vi", "na", "ru", "be", "ga", "te")
    val lugarPre = listOf("Pico", "Vale", "Cidade", "Torre", "Floresta", "Abismo", "Ilha", "Templo", "Portão", "Mar", "Planície", "Fortaleza", "Seita", "Reino", "Lago")
    val lugarPos = listOf("das Cinzas", "do Lótus Negro", "Celestial", "dos Mil Ventos", "Esquecido", "da Lua Partida", "de Jade", "do Dragão Adormecido", "Carmesim",
        "das Nove Névoas", "Sem Nome", "do Sol Poente", "de Ferro", "das Almas", "Eterno")
    val tecA = listOf("Lâmina", "Punho", "Palma", "Passo", "Espada", "Chama", "Selo", "Olho", "Canção", "Corte", "Escudo", "Garra", "Lança")
    val tecB = listOf("do Trovão Silencioso", "das Mil Sombras", "do Céu Partido", "da Lua Vazia", "do Dragão Ascendente", "Carmesim", "da Neve Eterna",
        "do Vazio", "da Fênix Negra", "dos Nove Infernos", "da Aurora", "do Vento Cortante", "Celestial")
    val titulos = listOf("o Imortal", "a Sem Coração", "o Errante", "da Espada Quebrada", "a Bruxa Cinzenta", "o Último Herdeiro", "o Demônio Celeste", "a Lâmina Fria")

    fun palavra(sil: List<String>, r: Random, min: Int = 2, max: Int = 3): String =
        (1..r.nextInt(min, max + 1)).joinToString("") { sil[r.nextInt(sil.size)] }.replaceFirstChar { it.uppercase() }

    fun gerar(tipo: String, r: Random): String = when (tipo) {
        "ORIENTE" -> palavra(silabasOriente, r, 1, 1) + " " + palavra(silabasOriente, r, 2, 2).let { it.substring(0, 1).uppercase() + it.substring(1) }
        "OCIDENTE" -> palavra(silabasOcidente, r) + if (r.nextInt(3) == 0) " " + palavra(silabasOcidente, r, 2, 3) else ""
        "BR" -> palavra(silabasBr, r, 2, 3)
        "LUGAR" -> lugarPre[r.nextInt(lugarPre.size)] + " " + lugarPos[r.nextInt(lugarPos.size)]
        "TECNICA" -> tecA[r.nextInt(tecA.size)] + " " + tecB[r.nextInt(tecB.size)]
        else -> palavra(if (r.nextBoolean()) silabasOriente else silabasOcidente, r) + ", " + titulos[r.nextInt(titulos.size)]
    }
}

@Composable
fun TelaGeradorNomes(nav: NavHostController) {
    val p = LocalPaleta.current
    val ctx = LocalContext.current
    var tipo by remember { mutableStateOf("ORIENTE") }
    var semente by remember { mutableStateOf(System.nanoTime()) }
    val nomes = remember(tipo, semente) { val r = Random(semente); List(12) { Nomes.gerar(tipo, r) }.distinct() }
    var copiado by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Topo("Gerador de nomes", nav)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ORIENTE" to "Oriental", "OCIDENTE" to "Fantasia", "BR" to "Brasileiro", "LUGAR" to "Lugares", "TECNICA" to "Técnicas", "TITULO" to "Com título")
                .forEach { (k, n) -> Chip(n, tipo == k) { tipo = k } }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(nomes) { n ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.superficie).clickable {
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("nome", n)); copiado = n
                }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(n, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text(if (copiado == n) "copiado ✓" else "copiar", fontSize = 12.sp, color = if (copiado == n) p.destaque else p.textoSuave)
                }
            }
        }
        Button(onClick = { semente = System.nanoTime(); copiado = "" }, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp)) {
            Text("↻  Gerar outros", fontWeight = FontWeight.ExtraBold)
        }
    }
}
