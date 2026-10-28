package com.huryel.novely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Capitulo
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Serifa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ───────────────────────── Ajuste de capa / banner ─────────────────────────

@Composable
fun TelaAjusteImagem(obraId: Long, tipo: String, nav: NavHostController) {
    val repo = NovelyApp.instancia.repo
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val obra by remember(obraId) { repo.obra(obraId) }.collectAsState(initial = null)
    val o = obra ?: return
    val banner = tipo == "banner"
    var x by remember(o.id) { mutableFloatStateOf(if (banner) o.bannerX else o.capaX) }
    var y by remember(o.id) { mutableFloatStateOf(if (banner) o.bannerY else o.capaY) }
    var zoom by remember(o.id) { mutableFloatStateOf(if (banner) o.bannerZoom else o.capaZoom) }
    var tamanho by remember { mutableStateOf(IntSize(1, 1)) }

    Column(Modifier.fillMaxSize()) {
        Topo(if (banner) "Ajustar banner" else "Ajustar capa", nav)
        Text("Arraste para mover · faça pinça (ou use a barra) para aproximar.",
            color = p.textoSuave, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val moldura = if (banner) Modifier.fillMaxWidth().aspectRatio(1.86f) else Modifier.fillMaxHeight(0.9f).aspectRatio(2f / 3f)
            ImagemEnquadrada(
                if (banner) o.banner else o.capa, x, y, zoom, o.corBase,
                moldura.clip(RoundedCornerShape(if (banner) 0.dp else 14.dp))
                    .onSizeChanged { tamanho = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestoZoom, _ ->
                            zoom = (zoom * gestoZoom).coerceIn(1f, 4f)
                            x = (x - pan.x / tamanho.width * 1.4f / zoom).coerceIn(0f, 1f)
                            y = (y - pan.y / tamanho.height * 1.4f / zoom).coerceIn(0f, 1f)
                        }
                    },
                ladoMax = 1600,
            )
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ZOOM", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
            Slider(zoom, { zoom = it }, valueRange = 1f..4f,
                colors = SliderDefaults.colors(thumbColor = p.destaque, activeTrackColor = p.destaque, inactiveTrackColor = p.borda))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { x = 0.5f; y = 0.5f; zoom = 1f }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Centralizar") }
                Button(
                    onClick = { escopo.launch { repo.ajustarImagem(obraId, tipo, x, y, zoom); nav.popBackStack() } },
                    colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Salvar", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ───────────────────────── Leitor ─────────────────────────

private sealed interface Bloco {
    data class Titulo(val texto: String) : Bloco
    data class Paragrafo(val texto: AnnotatedString) : Bloco
    data object Separador : Bloco
}

/** Converte o Markdown simples do Novely em blocos (um por parágrafo). Roda fora da thread principal. */
private fun montarBlocos(texto: String): List<Bloco> {
    val saida = ArrayList<Bloco>()
    texto.lineSequence().forEach { bruta ->
        val linha = bruta.trim()
        when {
            linha.isEmpty() -> {}
            linha == "* * *" || linha == "***" || linha == "---" || linha == "◆" -> saida.add(Bloco.Separador)
            linha.startsWith("#") -> saida.add(Bloco.Titulo(linha.trimStart('#').trim()))
            else -> saida.add(Bloco.Paragrafo(estilizar(linha)))
        }
    }
    return saida
}

/** **negrito** e *itálico* */
fun estilizar(linha: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    var negrito = false
    var italico = false
    while (i < linha.length) {
        if (linha.startsWith("**", i)) { negrito = !negrito; i += 2; continue }
        if (linha[i] == '*' || linha[i] == '_') { italico = !italico; i++; continue }
        val ini = i
        while (i < linha.length && linha[i] != '*' && linha[i] != '_') i++
        pushStyle(SpanStyle(fontWeight = if (negrito) FontWeight.Bold else null, fontStyle = if (italico) FontStyle.Italic else null))
        append(linha, ini, i)
        pop()
    }
}

@OptIn(FlowPreview::class)
@Composable
fun TelaLeitor(capId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    val tamanho by app.prefs.tamanho.collectAsState()
    var cap by remember { mutableStateOf<Capitulo?>(null) }
    var blocos by remember { mutableStateOf<List<Bloco>?>(null) }
    var vizinhos by remember { mutableStateOf<Pair<Capitulo?, Capitulo?>>(null to null) }
    var controles by remember { mutableStateOf(true) }

    LaunchedEffect(capId) {
        val c = repo.capitulo(capId).filterNotNull().first()
        cap = c
        blocos = withContext(Dispatchers.Default) { montarBlocos(repo.lerTexto(capId)) }
        val lista = repo.dao.capitulosAgora(c.obraId)
        val i = lista.indexOfFirst { it.id == c.id }
        vizinhos = lista.getOrNull(i - 1) to lista.getOrNull(i + 1)
    }

    val c = cap
    val b = blocos
    Box(Modifier.fillMaxSize().background(p.fundo)) {
        if (c == null || b == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 2.dp), color = p.destaque, trackColor = p.fundo)
        } else {
            val estado = rememberLazyListState(c.posicaoBloco.coerceIn(0, b.size + 1), c.posicaoDeslocamento)
            fun progresso(): Float {
                val total = b.size + 2
                val info = estado.layoutInfo
                val ultimo = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                return if (ultimo >= total - 1) 1f else ((estado.firstVisibleItemIndex + 1).toFloat() / total).coerceIn(0f, 1f)
            }
            LaunchedEffect(estado) {
                snapshotFlow { estado.firstVisibleItemIndex to estado.firstVisibleItemScrollOffset }
                    .debounce(700)
                    .collect { (i, d) -> repo.salvarPosicao(c, maxOf(progresso(), if (c.progresso >= 0.98f) 1f else 0f), i, d) }
            }
            DisposableEffect(Unit) {
                onDispose {
                    val i = estado.firstVisibleItemIndex; val d = estado.firstVisibleItemScrollOffset; val pr = progresso()
                    app.escopo.launch { repo.salvarPosicao(c, maxOf(pr, if (c.progresso >= 0.98f) 1f else 0f), i, d) }
                }
            }
            val estiloTexto = TextStyle(fontFamily = Serifa, fontSize = tamanho.sp, lineHeight = (tamanho * 1.72f).sp, color = p.texto)
            LazyColumn(
                state = estado,
                modifier = Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { controles = !controles },
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 76.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy((tamanho * 0.9f).dp),
            ) {
                item(key = "cab") {
                    Text(c.titulo, style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp, lineHeight = 33.sp))
                }
                itemsIndexed(b, contentType = { _, x -> x::class }) { _, bloco ->
                    when (bloco) {
                        is Bloco.Titulo -> Text(bloco.texto, style = MaterialTheme.typography.headlineSmall)
                        is Bloco.Separador -> Text("◆  ◆  ◆", color = p.destaque, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        is Bloco.Paragrafo -> Text(bloco.texto, style = estiloTexto)
                    }
                }
                item(key = "fim") {
                    Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (b.isEmpty()) Text("Este capítulo ainda está vazio.", color = p.textoSuave)
                        vizinhos.second?.let { prox ->
                            Button(onClick = { nav.navigate("leitor/${prox.id}") { popUpTo("leitor/{id}") { inclusive = true } } },
                                colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                Text("Próximo: ${prox.titulo}", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } ?: Text("Fim dos capítulos disponíveis.", color = p.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (controles) {
                Surface(color = p.superficie.copy(alpha = 0.97f), modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                    Topo(c.titulo, nav) {
                        IconButton(onClick = { nav.navigate("editor/${c.id}") }) { Icon(Icons.Filled.Edit, "Editar capítulo") }
                    }
                }
                Surface(color = p.superficie.copy(alpha = 0.97f), modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = vizinhos.first != null, onClick = {
                            vizinhos.first?.let { nav.navigate("leitor/${it.id}") { popUpTo("leitor/{id}") { inclusive = true } } }
                        }) { Text("‹ Anterior") }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("${(progresso() * 100).toInt()}% · ${b.size} parágrafos", fontSize = 12.sp, color = p.textoSuave)
                        }
                        TextButton(enabled = vizinhos.second != null, onClick = {
                            vizinhos.second?.let { nav.navigate("leitor/${it.id}") { popUpTo("leitor/{id}") { inclusive = true } } }
                        }) { Text("Próximo ›") }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Editor (versão inicial; otimização para textos gigantes na Fase E) ─────────────────────────

@Composable
fun TelaEditor(capId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    var cap by remember { mutableStateOf<Capitulo?>(null) }
    var valor by remember { mutableStateOf<TextFieldValue?>(null) }
    var salvoTexto by remember { mutableStateOf("") }
    var estadoSalvo by remember { mutableStateOf("Salvo") }

    LaunchedEffect(capId) {
        val c = repo.capitulo(capId).filterNotNull().first()
        cap = c
        val t = repo.lerTexto(capId)
        salvoTexto = t
        valor = TextFieldValue(t, TextRange(t.length))
    }

    // Salvamento automático: espera 1,2 s sem digitar e grava (não grava a cada tecla).
    val textoAtual = valor?.text
    LaunchedEffect(textoAtual) {
        val c = cap ?: return@LaunchedEffect
        val t = textoAtual ?: return@LaunchedEffect
        if (t == salvoTexto) { estadoSalvo = "Salvo"; return@LaunchedEffect }
        estadoSalvo = "Editando…"
        delay(1200)
        estadoSalvo = "Salvando…"
        repo.gravarTexto(c, t)
        salvoTexto = t
        estadoSalvo = "Salvo"
    }

    // Garante a gravação ao sair da tela ou mandar o app para o fundo.
    val dono = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val textoMaisRecente by rememberUpdatedState(textoAtual)
    DisposableEffect(dono) {
        fun salvarJa() {
            val c = cap ?: return
            val t = textoMaisRecente ?: return
            if (t != salvoTexto) { salvoTexto = t; app.escopo.launch { repo.gravarTexto(c, t) } }
        }
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) salvarJa() }
        dono.lifecycle.addObserver(obs)
        onDispose { dono.lifecycle.removeObserver(obs); salvarJa() }
    }

    val v = valor
    Column(Modifier.fillMaxSize().imePadding()) {
        Topo(cap?.titulo ?: "", nav) {
            Text(estadoSalvo, fontSize = 12.sp, color = if (estadoSalvo == "Salvo") androidx.compose.ui.graphics.Color(0xFF6FC2B4) else p.textoSuave,
                modifier = Modifier.padding(end = 12.dp))
        }
        if (v == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = p.destaque, trackColor = p.fundo)
        } else {
            Text("${com.huryel.novely.data.ArquivosTexto.contarPalavras(v.text)} palavras", fontSize = 12.sp, color = p.textoSuave,
                modifier = Modifier.padding(horizontal = 20.dp))
            BasicTextField(
                value = v, onValueChange = { valor = it },
                textStyle = TextStyle(fontFamily = Serifa, fontSize = 17.sp, lineHeight = 29.sp, color = p.texto),
                cursorBrush = SolidColor(p.destaque),
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            )
            Row(Modifier.fillMaxWidth().background(p.superficie).padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("**" to "B", "*" to "I", "\n# " to "H", "\n\n* * *\n\n" to "◆", "— " to "—").forEach { (ins, rot) ->
                    TextButton(onClick = {
                        val sel = v.selection
                        val novo = if (!sel.collapsed && (ins == "**" || ins == "*"))
                            v.text.substring(0, sel.min) + ins + v.text.substring(sel.min, sel.max) + ins + v.text.substring(sel.max)
                        else v.text.substring(0, sel.min) + ins + v.text.substring(sel.max)
                        val pos = if (!sel.collapsed && (ins == "**" || ins == "*")) sel.max + ins.length * 2 else sel.min + ins.length
                        valor = TextFieldValue(novo, TextRange(pos))
                    }, modifier = Modifier.clip(RoundedCornerShape(10.dp))) {
                        Text(rot, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (rot == "◆") p.destaque else p.texto,
                            fontStyle = if (rot == "I") FontStyle.Italic else FontStyle.Normal)
                    }
                }
            }
        }
    }
}
