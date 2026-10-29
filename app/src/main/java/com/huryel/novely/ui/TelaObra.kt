package com.huryel.novely.ui

import com.huryel.novely.ui.theme.forma
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Capitulo
import com.huryel.novely.data.Obra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.data.Volume
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.launch

private sealed interface Dialogo {
    data object EditarInfo : Dialogo
    data object NovoCapitulo : Dialogo
    data object NovoVolume : Dialogo
    data object ExcluirObra : Dialogo
    data class RenomearCap(val c: Capitulo) : Dialogo
    data class ExcluirCap(val c: Capitulo) : Dialogo
    data class MoverCap(val c: Capitulo) : Dialogo
    data class RenomearVol(val v: Volume) : Dialogo
    data class ExcluirVol(val v: Volume) : Dialogo
    data class MenuImagem(val tipo: String) : Dialogo
    data class Etiquetas(val c: Capitulo) : Dialogo
    data object Colecao : Dialogo
    data object Publicar : Dialogo
    data object MoverVarios : Dialogo
    data object ExcluirVarios : Dialogo
}

@Composable
fun TelaObra(obraId: Long, nav: NavHostController) {
    val repo = NovelyApp.instancia.repo
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val obra by remember(obraId) { repo.obra(obraId) }.collectAsState(initial = null)
    val volumes by remember(obraId) { repo.volumes(obraId) }.collectAsState(initial = emptyList())
    val capitulos by remember(obraId) { repo.capitulos(obraId) }.collectAsState(initial = emptyList())
    var dialogo by remember { mutableStateOf<Dialogo?>(null) }
    var menuObra by remember { mutableStateOf(false) }
    var tipoEscolhendo by remember { mutableStateOf("capa") }
    val snack = remember { SnackbarHostState() }

    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) escopo.launch {
            val ok = repo.trocarImagem(obraId, tipoEscolhendo, uri)
            if (ok) nav.navigate("imagem/$obraId/$tipoEscolhendo")
            else snack.showSnackbar("Não consegui abrir essa imagem.")
        }
    }
    val bk = NovelyApp.instancia.backup
    val app = NovelyApp.instancia
    val exportarNovely = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { u -> if (u != null) app.escopo.launch { bk.exportarBackup(u, obraId) } }
    val exportarEpub = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/epub+zip")) { u -> if (u != null) app.escopo.launch { bk.exportarEpub(obraId, u) } }
    val exportarTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { u -> if (u != null) app.escopo.launch { bk.exportarTexto(obraId, u) } }
    val estadoBk by bk.estado.collectAsState()
    LaunchedEffect(estadoBk) {
        when (val e = estadoBk) {
            is com.huryel.novely.data.EstadoBackup.Feito -> { bk.limpar(); snack.showSnackbar(e.mensagem) }
            is com.huryel.novely.data.EstadoBackup.Erro -> { bk.limpar(); snack.showSnackbar(e.mensagem) }
            is com.huryel.novely.data.EstadoBackup.Trabalhando -> snack.showSnackbar(e.mensagem)
            else -> {}
        }
    }
    fun nomeSeguro(o: Obra) = o.titulo.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().ifEmpty { "obra" }

    fun escolherImagem(tipo: String) {
        tipoEscolhendo = tipo
        seletor.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val o = obra ?: run { Box(Modifier.fillMaxSize()); return }
    val estilo = "CLASSICO"
    val selecionados = remember { mutableStateListOf<Long>() }
    val emSelecao = selecionados.isNotEmpty()
    var filtroTag by remember { mutableStateOf("") }
    val todasTags = remember(capitulos) { capitulos.flatMap { it.tags.split(',').map { t -> t.trim() } }.filter { it.isNotEmpty() }.distinct().sorted() }
    val visiveis = if (filtroTag.isEmpty()) capitulos else capitulos.filter { c -> c.tags.split(',').any { it.trim() == filtroTag } }
    fun alternarSel(id: Long) { if (id in selecionados) selecionados.remove(id) else selecionados.add(id) }
    androidx.activity.compose.BackHandler(enabled = emSelecao) { selecionados.clear() }
    val numeros = remember(capitulos) {
        var n = 0
        capitulos.associate { c -> c.id to (if (com.huryel.novely.data.Texto.tipoEspecial(c.titulo) == null) ++n else 0) }
    }

    Box(Modifier.fillMaxSize()) {
        val listaEstado = androidx.compose.foundation.lazy.rememberLazyListState()
        LazyColumn(Modifier.fillMaxSize(), state = listaEstado, contentPadding = PaddingValues(bottom = if (emSelecao) 110.dp else 40.dp)) {
            item {
                val alvo = capitulos.firstOrNull { it.id == o.ultimoCapituloId } ?: capitulos.firstOrNull { it.progresso < 0.98f } ?: capitulos.firstOrNull()
                CabecalhoObra(
                    estilo, o, capitulos, volumes, alvo,
                    { if (listaEstado.firstVisibleItemIndex == 0) listaEstado.firstVisibleItemScrollOffset.toFloat() else 0f },
                    AcoesObra(
                        voltar = { nav.popBackStack() }, menu = { menuObra = true },
                        banner = { if (!o.trancada) dialogo = Dialogo.MenuImagem("banner") }, capa = { if (!o.trancada) dialogo = Dialogo.MenuImagem("capa") },
                        ler = alvo?.let { al -> { nav.navigate("leitor/${al.id}") } },
                        novoCap = if (o.minha) ({ dialogo = Dialogo.NovoCapitulo }) else null, novoVol = { dialogo = Dialogo.NovoVolume },
                    ),
                )
            }

            if (todasTags.isNotEmpty()) item(key = "tags") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Todos", filtroTag.isEmpty()) { filtroTag = "" }
                    todasTags.forEach { t -> Chip("#$t", filtroTag == t) { filtroTag = if (filtroTag == t) "" else t } }
                }
            }
            val semVolume = visiveis.filter { c -> c.volumeId == null || volumes.none { it.id == c.volumeId } }
            if (semVolume.isNotEmpty() && volumes.isNotEmpty()) item { CabecalhoVolume("Sem volume", null, {}, {}) }
            items(semVolume, key = { "c${it.id}" }) { c -> LinhaCapitulo(c, o, nav, { dialogo = it }, escopo, estilo, numeros[c.id] ?: 0, c.id in selecionados, emSelecao) { alternarSel(c.id) } }
            volumes.forEach { v ->
                item(key = "v${v.id}") { CabecalhoVolume(v.titulo, v, { dialogo = Dialogo.RenomearVol(v) }, { dialogo = Dialogo.ExcluirVol(v) }) }
                val dele = visiveis.filter { it.volumeId == v.id }
                if (dele.isEmpty()) item(key = "vazio${v.id}") {
                    Text("Nenhum capítulo aqui. Use ⋮ › Mover para volume.", color = p.textoSuave, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
                items(dele, key = { "c${it.id}" }) { c -> LinhaCapitulo(c, o, nav, { dialogo = it }, escopo, estilo, numeros[c.id] ?: 0, c.id in selecionados, emSelecao) { alternarSel(c.id) } }
            }
            if (capitulos.isEmpty()) item {
                Text(if (o.minha) "Nenhum capítulo ainda. Toque em “+ Cap.” para começar." else "Nenhum capítulo ainda. Use ⋮ › Importar capítulos.", color = p.textoSuave, modifier = Modifier.padding(20.dp))
            }
        }

        Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)) {
            DropdownMenu(expanded = menuObra, onDismissRequest = { menuObra = false }) {
                val livre = !o.trancada
                if (o.minha && livre) DropdownMenuItem(text = { Text("✦ Publicar para leitores", color = p.destaque, fontWeight = FontWeight.Bold) }, onClick = { menuObra = false; dialogo = Dialogo.Publicar })
                if (livre) DropdownMenuItem(text = { Text("Editar título e descrição") }, onClick = { menuObra = false; dialogo = Dialogo.EditarInfo })
                DropdownMenuItem(text = { Text("Personagens e lugares") }, onClick = { menuObra = false; nav.navigate("fichas/$obraId") })
                DropdownMenuItem(text = { Text("Linha do tempo") }, onClick = { menuObra = false; nav.navigate("tempo/$obraId") })
                DropdownMenuItem(text = { Text("Marcadores e grifos") }, onClick = { menuObra = false; nav.navigate("marcadores/$obraId") })
                if (livre) DropdownMenuItem(text = { Text("Reordenar capítulos") }, onClick = { menuObra = false; nav.navigate("reordenar/$obraId") })
                DropdownMenuItem(text = { Text("Selecionar vários") }, onClick = { menuObra = false; capitulos.firstOrNull()?.let { selecionados.add(it.id) } })
                DropdownMenuItem(text = { Text("Coleção" + if (o.colecao.isNotEmpty()) ": ${o.colecao}" else "…") }, onClick = { menuObra = false; dialogo = Dialogo.Colecao })
                if (livre) {
                    DropdownMenuItem(text = { Text("Importar capítulos (TXT/EPUB)") }, onClick = { menuObra = false; nav.navigate("importar/$obraId") })
                    DropdownMenuItem(text = { Text("Trocar capa") }, onClick = { menuObra = false; escolherImagem("capa") })
                    DropdownMenuItem(text = { Text("Trocar banner") }, onClick = { menuObra = false; escolherImagem("banner") })
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Sou o autor (permitir editar)") },
                        leadingIcon = { if (o.minha) Icon(Icons.Filled.Check, null) },
                        onClick = { menuObra = false; escopo.launch { repo.salvarObra(o.copy(minha = !o.minha)) } },
                    )
                }
                HorizontalDivider()
                (if (livre) listOf(StatusObra.ESCREVENDO, StatusObra.LENDO, StatusObra.CONCLUIDA) else listOf(StatusObra.LENDO, StatusObra.CONCLUIDA)).forEach { s ->
                    DropdownMenuItem(
                        text = { Text("Marcar como ${nomeStatus(s).lowercase()}") },
                        leadingIcon = { if (o.status == s) Icon(Icons.Filled.Check, null) },
                        onClick = { menuObra = false; escopo.launch { repo.salvarObra(o.copy(status = s, minha = if (s == StatusObra.ESCREVENDO) true else o.minha)) } },
                    )
                }
                HorizontalDivider()
                if (livre) {
                    DropdownMenuItem(text = { Text("Exportar EPUB") }, onClick = { menuObra = false; exportarEpub.launch(nomeSeguro(o) + ".epub") })
                    DropdownMenuItem(text = { Text("Exportar TXT / Markdown") }, onClick = { menuObra = false; exportarTxt.launch(nomeSeguro(o) + ".md") })
                }
                DropdownMenuItem(text = { Text("Backup só desta obra") }, onClick = { menuObra = false; exportarNovely.launch(nomeSeguro(o) + ".novely") })
                HorizontalDivider()
                DropdownMenuItem(text = { Text(if (livre) "Excluir obra" else "Remover da biblioteca", color = Color(0xFFE57373)) }, onClick = { menuObra = false; dialogo = Dialogo.ExcluirObra })
            }
        }
        if (emSelecao) Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(p.superficie).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${selecionados.size} selecionado(s)", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                TextButton(onClick = { selecionados.clear(); selecionados.addAll(visiveis.map { it.id }) }) { Text("Todos") }
                TextButton(onClick = { selecionados.clear() }) { Text("Cancelar") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val ids = selecionados.toList()
                OutlinedButton(onClick = { escopo.launch { repo.marcarLidos(ids, true); selecionados.clear() } }) { Text("✓ Lidos") }
                OutlinedButton(onClick = { escopo.launch { repo.marcarLidos(ids, false); selecionados.clear() } }) { Text("Não lidos") }
                if (!o.trancada) {
                    OutlinedButton(onClick = { dialogo = Dialogo.MoverVarios }) { Text("Mover p/ volume") }
                    OutlinedButton(onClick = { dialogo = Dialogo.ExcluirVarios }) { Text("Excluir", color = Color(0xFFE57373)) }
                }
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter))
    }

    when (val d = dialogo) {
        null -> {}
        Dialogo.EditarInfo -> DialogoInfo(o, aoFechar = { dialogo = null }) { t, desc ->
            dialogo = null; escopo.launch { repo.salvarObra(o.copy(titulo = t.trim().ifEmpty { o.titulo }, descricao = desc.trim())) }
        }
        Dialogo.NovoCapitulo -> DialogoTexto("Novo capítulo", "Título", "Capítulo ${capitulos.size + 1}", "Criar", aoFechar = { dialogo = null }) { t ->
            dialogo = null
            escopo.launch {
                val volume = capitulos.lastOrNull()?.volumeId
                val id = repo.novoCapitulo(obraId, t, volume)
                if (o.status == StatusObra.ESCREVENDO) nav.navigate("editor/$id")
            }
        }
        Dialogo.NovoVolume -> DialogoTexto("Novo volume ou arco", "Nome", "Volume ${volumes.size + 1}", "Criar", aoFechar = { dialogo = null }) { t ->
            dialogo = null; escopo.launch { repo.novoVolume(obraId, t) }
        }
        Dialogo.ExcluirObra -> DialogoConfirmar("Excluir “${o.titulo}”?", "A obra sai da estante. Os ${capitulos.size} capítulos vão para a Lixeira por 30 dias (Ajustes › Lixeira).", "Excluir tudo", { dialogo = null }) {
            dialogo = null; escopo.launch { repo.excluirObra(o); nav.popBackStack() }
        }
        is Dialogo.RenomearCap -> DialogoTexto("Renomear capítulo", "Título", d.c.titulo, aoFechar = { dialogo = null }) { t ->
            dialogo = null; escopo.launch { repo.salvarCapitulo(d.c.copy(titulo = t.trim().ifEmpty { d.c.titulo })) }
        }
        is Dialogo.ExcluirCap -> DialogoConfirmar("Excluir “${d.c.titulo}”?", "Ele vai para a Lixeira e pode ser recuperado por 30 dias (Ajustes › Lixeira).", "Excluir", { dialogo = null }) {
            dialogo = null; escopo.launch { repo.excluirCapitulo(d.c) }
        }
        is Dialogo.MoverCap -> AlertDialog(
            onDismissRequest = { dialogo = null },
            title = { Text("Mover para…") },
            text = {
                Column {
                    (listOf<Volume?>(null) + volumes).forEach { v ->
                        Text(v?.titulo ?: "Sem volume", fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable {
                            dialogo = null; escopo.launch { repo.salvarCapitulo(d.c.copy(volumeId = v?.id)) }
                        }.padding(vertical = 14.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialogo = null }) { Text("Fechar") } },
        )
        is Dialogo.RenomearVol -> DialogoTexto("Renomear volume", "Nome", d.v.titulo, aoFechar = { dialogo = null }) { t ->
            dialogo = null; escopo.launch { repo.renomearVolume(d.v, t) }
        }
        is Dialogo.ExcluirVol -> DialogoConfirmar("Excluir “${d.v.titulo}”?", "Os capítulos NÃO serão apagados: eles vão para “Sem volume”.", "Excluir volume", { dialogo = null }) {
            dialogo = null; escopo.launch { repo.excluirVolume(d.v) }
        }
        is Dialogo.Etiquetas -> DialogoTexto("Etiquetas do capítulo", "Separe por vírgula (ex.: luta, revelação)", d.c.tags, aoFechar = { dialogo = null }) { t ->
            dialogo = null
            val limpo = t.split(',').map { it.trim().lowercase().removePrefix("#") }.filter { it.isNotEmpty() }.distinct().joinToString(",")
            escopo.launch { repo.salvarCapitulo(d.c.copy(tags = limpo)) }
        }
        Dialogo.Publicar -> DialogoPublicar(o.id, o.titulo) { dialogo = null }
        Dialogo.Colecao -> {
            val existentes by remember { repo.colecoes() }.collectAsState(initial = emptyList())
            var nova by remember { mutableStateOf(o.colecao) }
            AlertDialog(
                onDismissRequest = { dialogo = null },
                title = { Text("Coleção") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Agrupe obras na Biblioteca (ex.: Cultivo, Isekai, Minhas).", fontSize = 13.sp, color = p.textoSuave)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            existentes.forEach { c -> Chip(c, nova == c) { nova = c } }
                        }
                        OutlinedTextField(nova, { nova = it }, label = { Text("Nome da coleção") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = { TextButton(onClick = { dialogo = null; escopo.launch { repo.salvarObra(o.copy(colecao = nova.trim())) } }) { Text("Salvar") } },
                dismissButton = { TextButton(onClick = { dialogo = null; escopo.launch { repo.salvarObra(o.copy(colecao = "")) } }) { Text("Sem coleção") } },
            )
        }
        Dialogo.MoverVarios -> AlertDialog(
            onDismissRequest = { dialogo = null },
            title = { Text("Mover ${selecionados.size} para…") },
            text = {
                Column {
                    (listOf<Volume?>(null) + volumes).forEach { v ->
                        Text(v?.titulo ?: "Sem volume", fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable {
                            val ids = selecionados.toList(); dialogo = null; selecionados.clear()
                            escopo.launch { repo.moverParaVolume(ids, v?.id) }
                        }.padding(vertical = 14.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialogo = null }) { Text("Fechar") } },
        )
        Dialogo.ExcluirVarios -> DialogoConfirmar("Excluir ${selecionados.size} capítulo(s)?", "Eles vão para a Lixeira por 30 dias.", "Excluir", { dialogo = null }) {
            val ids = selecionados.toList(); dialogo = null; selecionados.clear()
            escopo.launch { ids.forEach { id -> repo.dao.capituloAgora(id)?.let { repo.excluirCapitulo(it) } } }
        }
        is Dialogo.MenuImagem -> {
            val nome = if (d.tipo == "banner") "banner" else "capa"
            val tem = if (d.tipo == "banner") o.banner != null else o.capa != null
            AlertDialog(
                onDismissRequest = { dialogo = null },
                title = { Text(nome.replaceFirstChar { it.uppercase() }) },
                text = {
                    Column {
                        listOfNotNull(
                            "Escolher imagem da galeria" to { escolherImagem(d.tipo) },
                            if (tem) "Ajustar enquadramento" to { nav.navigate("imagem/$obraId/${d.tipo}") } else null,
                            if (tem) "Remover imagem" to { escopo.launch { repo.removerImagem(obraId, d.tipo) }; Unit } else null,
                        ).forEach { (rotulo, acao) ->
                            Text(rotulo, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable { dialogo = null; acao() }.padding(vertical = 14.dp))
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { dialogo = null }) { Text("Fechar") } },
            )
        }
    }
}

@Composable
private fun Cabecalho(o: Obra, nav: NavHostController, desloc: () -> Float, aoMenu: () -> Unit, aoBanner: () -> Unit, aoCapa: () -> Unit) {
    val p = LocalPaleta.current
    Box(Modifier.fillMaxWidth().height(300.dp)) {
        ImagemEnquadrada(o.banner, o.bannerX, o.bannerY, o.bannerZoom, o.corBase,
            Modifier.fillMaxWidth().height(210.dp).graphicsLayer { translationY = desloc() * 0.45f }.clickable(onClick = aoBanner), ladoMax = 1400) {
            Text("Toque para colocar um banner", color = Color(0x99FFFFFF), fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
        }
        Box(Modifier.fillMaxWidth().height(210.dp).background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to p.fundo)))
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BotaoRedondo({ Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }) { nav.popBackStack() }
            BotaoRedondo({ Icon(Icons.Filled.MoreVert, "Opções da obra") }, aoMenu)
        }
        Row(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp), verticalAlignment = Alignment.Bottom) {
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
                Modifier.width(110.dp).height(160.dp).border(2.dp, p.fundo, forma(12.dp)).clickable(onClick = aoCapa), 13.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.padding(bottom = 6.dp)) {
                Text(nomeStatus(o.status).uppercase(), style = MaterialTheme.typography.labelSmall, color = p.destaque)
                Text(o.titulo, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 27.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun BotaoRedondo(icone: @Composable () -> Unit, aoTocar: () -> Unit) {
    Box(Modifier.size(44.dp).clip(forma(22.dp)).background(Color(0x99000000)).clickable(onClick = aoTocar), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { icone() }
    }
}

@Composable
private fun Estatistica(valor: String, rotulo: String, modifier: Modifier) {
    val p = LocalPaleta.current
    Column(modifier.clip(forma(14.dp)).background(p.superficie).padding(10.dp)) {
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(rotulo, color = p.textoSuave, fontSize = 12.sp)
    }
}

@Composable
private fun CabecalhoVolume(titulo: String, v: Volume?, aoRenomear: () -> Unit, aoExcluir: () -> Unit) {
    val p = LocalPaleta.current
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = p.destaque, modifier = Modifier.weight(1f))
        if (v != null) Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opções do volume", tint = p.textoSuave) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Renomear") }, onClick = { menu = false; aoRenomear() })
                DropdownMenuItem(text = { Text("Excluir volume") }, onClick = { menu = false; aoExcluir() })
            }
        }
    }
}

@Composable
private fun LinhaCapitulo(c: Capitulo, o: Obra, nav: NavHostController, abrir: (Dialogo) -> Unit, escopo: kotlinx.coroutines.CoroutineScope, estilo: String, numero: Int,
                          selecionado: Boolean, emSelecao: Boolean, alternar: () -> Unit) {
    val repo = NovelyApp.instancia.repo
    val p = LocalPaleta.current
    var menu by remember { mutableStateOf(false) }
    val atual = c.id == o.ultimoCapituloId
    val lido = c.progresso >= 0.98f
    LinhaCapEstilo(estilo, c, o, atual, lido, numero, abrir = { if (emSelecao) alternar() else nav.navigate("leitor/${c.id}") },
        selecionado = selecionado, emSelecao = emSelecao, segurar = alternar) {
        if (emSelecao) return@LinhaCapEstilo
        if (o.minha) IconButton(onClick = { nav.navigate("editor/${c.id}") }) { Icon(Icons.Filled.Edit, "Editar texto", tint = p.textoSuave) }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opções do capítulo", tint = p.textoSuave) }
            DropdownMenu(menu, { menu = false }) {
                val livre = !o.trancada
                if (!o.minha && livre) DropdownMenuItem(text = { Text("Corrigir erro no texto") }, onClick = { menu = false; nav.navigate("editor/${c.id}") })
                if (livre) DropdownMenuItem(text = { Text("Renomear") }, onClick = { menu = false; abrir(Dialogo.RenomearCap(c)) })
                if (o.minha && livre) DropdownMenuItem(text = { Text(if (c.rascunho) "Marcar como concluído" else "Voltar para rascunho") },
                    onClick = { menu = false; escopo.launch { repo.salvarCapitulo(c.copy(rascunho = !c.rascunho)) } })
                DropdownMenuItem(text = { Text(if (lido) "Marcar como não lido" else "Marcar como lido") },
                    onClick = { menu = false; escopo.launch { repo.salvarCapitulo(c.copy(progresso = if (lido) 0f else 1f)) } })
                if (livre) {
                    DropdownMenuItem(text = { Text("Subir") }, onClick = { menu = false; escopo.launch { repo.moverCapitulo(c, -1) } })
                    DropdownMenuItem(text = { Text("Descer") }, onClick = { menu = false; escopo.launch { repo.moverCapitulo(c, 1) } })
                    DropdownMenuItem(text = { Text("Mover para volume") }, onClick = { menu = false; abrir(Dialogo.MoverCap(c)) })
                }
                DropdownMenuItem(text = { Text("Etiquetas" + if (c.tags.isNotEmpty()) " (${c.tags.split(',').size})" else "") }, onClick = { menu = false; abrir(Dialogo.Etiquetas(c)) })
                if (livre) {
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Excluir", color = Color(0xFFE57373)) }, onClick = { menu = false; abrir(Dialogo.ExcluirCap(c)) })
                }
            }
        }
    }
}

@Composable
private fun DialogoInfo(o: Obra, aoFechar: () -> Unit, aoSalvar: (String, String) -> Unit) {
    var titulo by remember { mutableStateOf(o.titulo) }
    var desc by remember { mutableStateOf(o.descricao) }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Editar obra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(desc, { desc = it }, label = { Text("Descrição / sinopse") }, minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { aoSalvar(titulo, desc) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}
