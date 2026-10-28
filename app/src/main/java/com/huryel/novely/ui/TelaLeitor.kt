package com.huryel.novely.ui

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Capitulo
import com.huryel.novely.data.Texto
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Paleta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// ───────── Blocos de texto ─────────

sealed interface Bloco {
    val plano: String
    data class Titulo(override val plano: String) : Bloco
    data class Paragrafo(val texto: AnnotatedString, val fala: Boolean) : Bloco { override val plano: String get() = texto.text }
    data object Separador : Bloco { override val plano = "" }
}

private val iniciosDeFala = listOf("—", "–", "―", "-", "“", "\"", "«")

fun ehFala(linha: String): Boolean {
    val l = linha.trimStart()
    return iniciosDeFala.any { l.startsWith(it) } && l.length > 1 && !l.startsWith("---")
}

/** **negrito** e *itálico* */
fun estilizar(linha: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    var negrito = false
    var italico = false
    while (i < linha.length) {
        if (linha.startsWith("**", i)) { negrito = !negrito; i += 2; continue }
        if (linha[i] == '*') { italico = !italico; i++; continue }
        val ini = i
        while (i < linha.length && linha[i] != '*') i++
        pushStyle(SpanStyle(fontWeight = if (negrito) FontWeight.Bold else null, fontStyle = if (italico) FontStyle.Italic else null))
        append(linha, ini, i)
        pop()
    }
}

/** Texto do capítulo → blocos (um por parágrafo; parágrafos gigantes viram vários blocos). Fora da thread principal. */
fun montarBlocos(texto: String): List<Bloco> {
    val saida = ArrayList<Bloco>(texto.length / 300 + 4)
    var anteriorVazia = true
    texto.lineSequence().forEach { bruta ->
        val linha = bruta.trim()
        val podeSerTitulo = anteriorVazia
        anteriorVazia = linha.isEmpty()
        when {
            linha.isEmpty() -> {}
            podeSerTitulo && Texto.ehTituloCapitulo(linha) && !linha.startsWith("#") -> saida.add(Bloco.Titulo(Texto.tituloLimpo(linha)))
            linha == "* * *" || linha == "***" || linha == "---" || linha == "◆" -> saida.add(Bloco.Separador)
            linha.startsWith("#") -> saida.add(Bloco.Titulo(linha.trimStart('#').trim()))
            linha.length > 2500 -> {
                val fala = ehFala(linha)
                Texto.dividirEmPedacos(linha, 1500).forEach { saida.add(Bloco.Paragrafo(estilizar(it.trim()), fala)) }
            }
            else -> saida.add(Bloco.Paragrafo(estilizar(linha), ehFala(linha)))
        }
    }
    return saida
}

// ───────── Leitura em voz alta (motor de voz do próprio Android, funciona offline se a voz estiver instalada) ─────────

class Voz(ctx: android.content.Context, private val aoTerminar: (String) -> Unit) {
    var pronta by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    private lateinit var tts: TextToSpeech
    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts.setLanguage(Locale("pt", "BR"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.language = Locale.getDefault()
                pronta = true
            } else erro = "Nenhum motor de voz disponível. Instale “Serviços de voz do Google” na Play Store."
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { if (id != null) aoTerminar(id) }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) {}
        })
    }
    private var vozEscolhida: String = ""

    fun falar(texto: String, id: String, velocidade: Float) {
        aplicarVoz()
        tts.setSpeechRate(velocidade)
        tts.speak(texto.take(3900), TextToSpeech.QUEUE_FLUSH, null, id)
    }

    /** Só vozes do Brasil que funcionam sem internet. */
    fun vozes(): List<android.speech.tts.Voice> = try {
        (tts.voices?.toList() ?: emptyList())
            .filter { it.locale.language == "pt" && it.locale.country.equals("BR", true) && !it.isNetworkConnectionRequired && !it.features.contains("notInstalled") }
            .sortedWith(compareBy({ generoVoz(it.name) ?: "z" }, { it.name }))
    } catch (e: Exception) { emptyList() }

    fun usarVoz(nome: String) { vozEscolhida = nome; aplicarVoz() }

    /** Reaplica a voz antes de cada fala: alguns motores esquecem a escolha depois de stop(). */
    private fun aplicarVoz() {
        if (vozEscolhida.isEmpty()) return
        try {
            val v = tts.voices?.firstOrNull { it.name == vozEscolhida } ?: return
            if (tts.voice?.name != v.name) tts.voice = v
        } catch (_: Exception) {}
    }

    fun parar() { tts.stop() }
    fun liberar() { tts.stop(); tts.shutdown() }
}

/** "Mulher" / "Homem" pelo nome técnico da voz (Google, Samsung e outros); null se não der para saber. */
fun generoVoz(nome: String): String? {
    val n = nome.lowercase()
    return when {
        "female" in n || "feminin" in n -> "Mulher"
        "male" in n || "masculin" in n -> "Homem"
        Regex("smt[f]\\d").containsMatchIn(n) -> "Mulher"
        Regex("smt[m]\\d").containsMatchIn(n) -> "Homem"
        "-afs" in n || "-pte" in n || "pt-br-language" in n -> "Mulher"
        "-ptd" in n || "-ptz" in n -> "Homem"
        else -> null
    }
}

// ───────── Tela ─────────

private enum class Painel { NENHUM, VOZ, BUSCA }

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun TelaLeitor(capId: Long, nav: NavHostController, consultaInicial: String = "", ocorrenciaInicial: Int = 0) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val prefs = app.prefs
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val ctx = LocalContext.current

    val tamanho by prefs.tamanhoP.estado.collectAsState()
    val entrelinhas by prefs.entrelinhas.estado.collectAsState()
    val margem by prefs.margem.estado.collectAsState()
    val fonte by prefs.fonte.estado.collectAsState()
    val destacarFalas by prefs.destacarFalas.estado.collectAsState()
    val modoPaginas by prefs.modoPaginas.estado.collectAsState()
    val telaLigada by prefs.telaLigada.estado.collectAsState()
    val velocidade by prefs.velocidadeVoz.estado.collectAsState()
    val narrador by prefs.narrador.estado.collectAsState()

    var cap by remember { mutableStateOf<Capitulo?>(null) }
    var blocos by remember { mutableStateOf<List<Bloco>?>(null) }
    var lista by remember { mutableStateOf<List<Capitulo>>(emptyList()) }
    var controles by remember { mutableStateOf(true) }
    var painel by remember { mutableStateOf(if (consultaInicial.isNotEmpty()) Painel.BUSCA else Painel.NENHUM) }
    var mostrarAjustes by remember { mutableStateOf(false) }
    var mostrarCapitulos by remember { mutableStateOf(false) }
    var mostrarSobre by remember { mutableStateOf(false) }

    LaunchedEffect(capId) {
        val c = repo.capitulo(capId).filterNotNull().first()
        cap = c
        blocos = withContext(Dispatchers.Default) { montarBlocos(repo.lerTexto(capId)) }
        lista = repo.dao.capitulosAgora(c.obraId)
    }

    val view = LocalView.current
    DisposableEffect(telaLigada) {
        view.keepScreenOn = telaLigada
        onDispose { view.keepScreenOn = false }
    }

    val c = cap
    val b = blocos
    if (c == null || b == null) {
        Box(Modifier.fillMaxSize().background(p.fundo)) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = p.destaque, trackColor = p.fundo)
        }
        return
    }
    val obra by remember(c.obraId) { repo.obra(c.obraId) }.collectAsState(initial = null)
    val idx = lista.indexOfFirst { it.id == c.id }
    val anterior = lista.getOrNull(idx - 1)
    val proximo = lista.getOrNull(idx + 1)
    fun irPara(outro: Capitulo) = nav.navigate("leitor/${outro.id}") { popUpTo(ROTA_LEITOR) { inclusive = true } }

    val estado = rememberLazyListState(c.posicaoBloco.coerceIn(0, b.size + 1), c.posicaoDeslocamento)
    val progresso by remember {
        derivedStateOf {
            val total = b.size + 2
            val ultimo = estado.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (ultimo >= total - 1) 1f else ((estado.firstVisibleItemIndex + 1).toFloat() / total).coerceIn(0f, 1f)
        }
    }
    val jaLido = c.progresso >= 0.98f
    LaunchedEffect(estado) {
        snapshotFlow { estado.firstVisibleItemIndex to estado.firstVisibleItemScrollOffset }
            .debounce(700)
            .collect { (i, d) -> repo.salvarPosicao(c, if (jaLido) 1f else progresso, i, d) }
    }
    DisposableEffect(Unit) {
        onDispose {
            val i = estado.firstVisibleItemIndex; val d = estado.firstVisibleItemScrollOffset; val pr = progresso
            app.escopo.launch { repo.salvarPosicao(c, if (jaLido) 1f else pr, i, d) }
        }
    }

    // ── Voz ──
    var falando by remember { mutableStateOf(-1) }
    var tocando by remember { mutableStateOf(false) }
    val falaveis = remember(b) { b.indices.filter { b[it] !is Bloco.Separador && b[it].plano.isNotBlank() } }
    val voz = remember {
        Voz(ctx) { id ->
            val atual = id.toIntOrNull() ?: return@Voz
            val prox = falaveis.firstOrNull { it > atual }
            if (prox == null) { tocando = false; falando = -1 } else falando = prox
        }
    }
    DisposableEffect(Unit) { onDispose { voz.liberar() } }
    LaunchedEffect(voz.pronta, narrador) { if (voz.pronta) voz.usarVoz(narrador) }
    LaunchedEffect(falando, tocando) {
        if (tocando && falando >= 0) {
            voz.falar(b[falando].plano, falando.toString(), velocidade)
            val vis = estado.layoutInfo.visibleItemsInfo
            val item = falando + 1
            if (vis.none { it.index == item } || vis.lastOrNull()?.index == item) estado.animateScrollToItem(item)
        }
    }
    fun tocar() {
        if (falando < 0) falando = falaveis.firstOrNull { it >= (estado.firstVisibleItemIndex - 1).coerceAtLeast(0) } ?: return
        tocando = true
    }
    fun pausar() { tocando = false; voz.parar() }

    // ── Busca ──
    var consulta by remember { mutableStateOf(consultaInicial) }
    val normalizados = remember(b) { lazy { b.map { Texto.normalizar(it.plano) } } }
    val achados by remember(b) {
        derivedStateOf {
            val q = Texto.normalizar(consulta.trim())
            if (q.length < 2) emptyList() else {
                val r = ArrayList<Pair<Int, Int>>()
                normalizados.value.forEachIndexed { bi, t ->
                    var i = t.indexOf(q)
                    while (i >= 0 && r.size < 5000) { r.add(bi to i); i = t.indexOf(q, i + q.length) }
                }
                r
            }
        }
    }
    var achadoAtual by remember { mutableStateOf(ocorrenciaInicial) }
    LaunchedEffect(achados, achadoAtual) {
        achados.getOrNull(achadoAtual)?.let { (bi, _) -> estado.animateScrollToItem(bi + 1) }
    }

    val familia = if (fonte == "SEM_SERIFA") FontFamily.SansSerif else FontFamily.Serif
    val estiloBase = TextStyle(fontFamily = familia, fontSize = tamanho.sp, lineHeight = (tamanho * entrelinhas).sp, color = p.texto)
    val corNarracao = if (destacarFalas) p.texto.copy(alpha = 0.86f) else p.texto
    val q = Texto.normalizar(consulta.trim())

    Box(Modifier.fillMaxSize().background(p.fundo)) {
        LazyColumn(
            state = estado,
            modifier = Modifier.fillMaxSize().pointerInput(modoPaginas) {
                detectTapGestures { pos ->
                    val w = size.width
                    if (modoPaginas && pos.x < w * 0.28f) escopo.launch { estado.animateScrollBy(-size.height * 0.88f) }
                    else if (modoPaginas && pos.x > w * 0.72f) escopo.launch { estado.animateScrollBy(size.height * 0.88f) }
                    else controles = !controles
                }
            },
            contentPadding = PaddingValues(start = margem.dp, end = margem.dp, top = 0.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy((tamanho * 0.85f).dp),
        ) {
            item(key = "cab") {
                CabecalhoCapitulo(c, obra, anterior, lista.take(idx + 1).count { Texto.tipoEspecial(it.titulo) == null }, margem, p) { mostrarSobre = true }
            }
            itemsIndexed(b, contentType = { _, x -> x::class }) { i, bloco ->
                val ativo = i == falando
                val fundoAtivo = if (ativo) Modifier.clip(RoundedCornerShape(10.dp)).background(p.destaque.copy(alpha = 0.14f)) else Modifier
                when (bloco) {
                    is Bloco.Titulo -> TituloNoTexto(bloco.plano, p, fundoAtivo)
                    is Bloco.Separador -> Text("◆   ◆   ◆", color = p.destaque, textAlign = TextAlign.Center, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    is Bloco.Paragrafo -> {
                        val texto = if (q.length >= 2) destacarBusca(bloco.texto, normalizados.value[i], q, p, achados.getOrNull(achadoAtual)?.takeIf { it.first == i }?.second) else bloco.texto
                        if (bloco.fala && destacarFalas) Fala(texto, estiloBase, p, fundoAtivo)
                        else Text(texto, style = estiloBase.copy(color = corNarracao), modifier = fundoAtivo)
                    }
                }
            }
            item(key = "fim") { FimCapitulo(b.isEmpty(), proximo, ::irPara) }
        }

        // barra de progresso fininha no topo
        Box(Modifier.align(Alignment.TopStart).fillMaxWidth(progresso).height(2.dp).background(p.destaque))

        AnimatedVisibility(controles, Modifier.align(Alignment.TopCenter), enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
            Surface(color = p.superficie.copy(alpha = 0.97f)) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                    Column(Modifier.weight(1f)) {
                        Text(c.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${(progresso * 100).toInt()}% · cap. ${idx + 1} de ${lista.size}", fontSize = 12.sp, color = p.textoSuave)
                    }
                    IconButton(onClick = { mostrarSobre = true }) { Icon(Icons.Filled.Info, "Sobre a obra") }
                    IconButton(onClick = { nav.navigate("editor/${c.id}") }) { Icon(Icons.Filled.Edit, "Editar capítulo") }
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            AnimatedVisibility(controles && painel == Painel.VOZ, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                PainelVoz(voz, tocando, velocidade, { if (tocando) pausar() else tocar() },
                    { val ant = falaveis.lastOrNull { it < falando }; if (ant != null) falando = ant },
                    { val px = falaveis.firstOrNull { it > falando }; if (px != null) falando = px },
                    { prefs.velocidadeVoz.definir(it) }, narrador, { prefs.narrador.definir(it); voz.usarVoz(it); if (tocando && falando >= 0) voz.falar(b[falando].plano, falando.toString(), velocidade) })
            }
            AnimatedVisibility(controles && painel == Painel.BUSCA, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                PainelBusca(consulta, { consulta = it; achadoAtual = 0 }, achados.size, achadoAtual,
                    { if (achados.isNotEmpty()) achadoAtual = (achadoAtual - 1 + achados.size) % achados.size },
                    { if (achados.isNotEmpty()) achadoAtual = (achadoAtual + 1) % achados.size },
                    { consulta = ""; painel = Painel.NENHUM })
            }
            AnimatedVisibility(controles, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                Surface(color = p.superficie.copy(alpha = 0.97f)) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().height(68.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        BotaoBarra("Ouvir", painel == Painel.VOZ || tocando, { Icon(Icons.Filled.PlayArrow, null) }) { painel = if (painel == Painel.VOZ) Painel.NENHUM else Painel.VOZ }
                        BotaoBarra("Buscar", painel == Painel.BUSCA, { Icon(Icons.Filled.Search, null) }) { painel = if (painel == Painel.BUSCA) Painel.NENHUM else Painel.BUSCA }
                        BotaoBarra("Capítulos", false, { Icon(Icons.AutoMirrored.Filled.List, null) }) { mostrarCapitulos = true }
                        BotaoBarra("Leitura", false, { Text("Aa", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp) }) { mostrarAjustes = true }
                    }
                }
            }
        }
        if (!controles) {
            Text("${(progresso * 100).toInt()}%", fontSize = 11.sp, color = p.textoSuave.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(12.dp))
        }
    }

    if (mostrarSobre) ModalBottomSheet(onDismissRequest = { mostrarSobre = false }, containerColor = p.superficie) {
        obra?.let { o -> SobreObra(o, lista, c, p) { mostrarSobre = false; mostrarCapitulos = true } }
    }
    if (mostrarAjustes) ModalBottomSheet(onDismissRequest = { mostrarAjustes = false }, containerColor = p.superficie) { AjustesLeitura() }
    if (mostrarCapitulos) ModalBottomSheet(onDismissRequest = { mostrarCapitulos = false }, containerColor = p.superficie) {
        val estadoLista = rememberLazyListState((idx - 3).coerceAtLeast(0))
        LazyColumn(state = estadoLista, modifier = Modifier.fillMaxHeight(0.7f), contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(lista, key = { _, x -> x.id }) { i, x ->
                val atual = x.id == c.id
                Row(
                    Modifier.fillMaxWidth().clickable { mostrarCapitulos = false; if (!atual) irPara(x) }
                        .background(if (atual) p.borda else Color.Transparent).padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${i + 1}", color = if (atual) p.destaque else p.textoSuave, modifier = Modifier.width(48.dp), fontWeight = FontWeight.Bold)
                    Text(x.titulo, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (x.progresso >= 0.98f && !atual) p.textoSuave else p.texto)
                }
            }
        }
    }
}

/** Abertura do capítulo: banner da obra, capa, nome da obra, rótulo, título, tempo e resumos. */
@Composable
private fun CabecalhoCapitulo(c: Capitulo, obra: com.huryel.novely.data.Obra?, anterior: Capitulo?, numero: Int, margem: Float, p: Paleta, abrirSobre: () -> Unit) {
    val especial = Texto.tipoEspecial(c.titulo)
    var verResumo by remember { mutableStateOf(false) }
    var verAnterior by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // banner sangrando até as bordas da tela, mesmo com margens de leitura
        Box(
            Modifier.layout { m, cs ->
                val extra = (margem * 2).dp.roundToPx()
                val w = cs.maxWidth + extra
                val pl = m.measure(cs.copy(minWidth = w, maxWidth = w))
                layout(cs.maxWidth, pl.height) { pl.place(-extra / 2, 0) }
            }.height(250.dp),
        ) {
            if (obra != null) {
                if (obra.banner != null) ImagemEnquadrada(obra.banner, obra.bannerX, obra.bannerY, obra.bannerZoom, obra.corBase, Modifier.fillMaxSize(), 1200)
                else Box(Modifier.fillMaxSize().background(Color(obra.corBase))) { ArteCapa(obra.titulo + "·leitor", obra.corBase) }
            }
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(0f to p.fundo.copy(alpha = 0.35f), 0.55f to p.fundo.copy(alpha = 0.55f), 1f to p.fundo)))
            if (obra != null) Row(Modifier.align(Alignment.BottomStart).padding(horizontal = margem.dp).padding(bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
                CapaObra(obra.titulo, obra.capa, obra.capaX, obra.capaY, obra.capaZoom, obra.corBase,
                    Modifier.width(64.dp).height(94.dp).clickable(onClick = abrirSobre), 9.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).clickable(onClick = abrirSobre)) {
                    Text(obra.titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Sobre a obra  ›", fontSize = 12.sp, color = p.destaque, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (especial != null) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("◆", color = p.destaque, fontSize = 22.sp)
                Text(especial, color = p.destaque, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp)
                val sub = c.titulo.substringAfter('—', c.titulo.substringAfter(':', "")).trim()
                Text(sub.ifEmpty { especial.lowercase().replaceFirstChar { it.uppercase() } },
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, lineHeight = 40.sp, fontStyle = FontStyle.Italic), textAlign = TextAlign.Center)
            }
        } else {
            Text("CAPÍTULO $numero", style = MaterialTheme.typography.labelSmall, color = p.destaque)
            Text(c.titulo.ifEmpty { "Sem título" }, style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, lineHeight = 35.sp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Etiqueta("${formatarPalavras(c.palavras.toLong())} palavras", p)
            Etiqueta("~${(c.palavras / 230).coerceAtLeast(1)} min", p)
            if (c.progresso in 0.02f..0.97f) Etiqueta("${(c.progresso * 100).toInt()}% lido", p, destaque = true)
        }
        if (anterior != null && anterior.resumo.isNotBlank()) {
            CaixaResumo("ANTERIORMENTE", anterior.resumo, verAnterior, p) { verAnterior = !verAnterior }
        }
        if (c.resumo.isNotBlank()) {
            CaixaResumo("NESTE CAPÍTULO", c.resumo, verResumo, p) { verResumo = !verResumo }
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(48.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(p.destaque))
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Etiqueta(t: String, p: Paleta, destaque: Boolean = false) {
    Text(t, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (destaque) p.sobreDestaque else p.textoSuave,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (destaque) p.destaque else p.superficie).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
private fun CaixaResumo(rotulo: String, texto: String, aberto: Boolean, p: Paleta, alternar: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.superficie).clickable(onClick = alternar)
            .animateContentSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rotulo, style = MaterialTheme.typography.labelSmall, color = p.destaque, modifier = Modifier.weight(1f))
            Text(if (aberto) "ocultar" else "mostrar", fontSize = 12.sp, color = p.textoSuave)
        }
        if (aberto) Text(texto, fontSize = 15.sp, lineHeight = 22.sp, color = p.texto)
    }
}

/** Painel "Sobre a obra": banner, capa, sinopse, números e progresso. */
@Composable
private fun SobreObra(o: com.huryel.novely.data.Obra, lista: List<Capitulo>, atual: Capitulo, p: Paleta, verCapitulos: () -> Unit) {
    val palavras = lista.sumOf { it.palavras.toLong() }
    val lidos = lista.count { it.progresso >= 0.98f }
    var sinopseAberta by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().height(170.dp)) {
            if (o.banner != null) ImagemEnquadrada(o.banner, o.bannerX, o.bannerY, o.bannerZoom, o.corBase, Modifier.fillMaxSize(), 1200)
            else Box(Modifier.fillMaxSize().background(Color(o.corBase))) { ArteCapa(o.titulo + "·sobre", o.corBase) }
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(0.4f to Color.Transparent, 1f to p.superficie)))
        }
        Row(Modifier.padding(horizontal = 20.dp).offset(y = (-56).dp), verticalAlignment = Alignment.Bottom) {
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(96.dp).height(140.dp), 12.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(nomeStatus(o.status).uppercase(), style = MaterialTheme.typography.labelSmall, color = p.destaque)
                Text(o.titulo, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp).offset(y = (-44).dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta("${lista.size} capítulos", p)
                Etiqueta("${formatarPalavras(palavras)} palavras", p)
                Etiqueta(if (lista.isNotEmpty()) "${lidos * 100 / lista.size}% lido" else "—", p, destaque = true)
            }
            Text("SINOPSE", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
            if (o.descricao.isBlank()) Text("Sem sinopse ainda. Adicione na página da obra › ⋮ › Editar título e descrição.", color = p.textoSuave, fontSize = 14.sp)
            else Text(o.descricao, fontSize = 15.sp, lineHeight = 23.sp, maxLines = if (sinopseAberta) Int.MAX_VALUE else 6, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.animateContentSize().clickable { sinopseAberta = !sinopseAberta })
            if (atual.resumo.isNotBlank()) {
                Text("ESTE CAPÍTULO", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
                Text(atual.resumo, fontSize = 14.sp, lineHeight = 21.sp, color = p.textoSuave)
            }
            OutlinedButton(onClick = verCapitulos, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Ver todos os capítulos") }
        }
    }
}

/** Títulos dentro do texto: prólogo/epílogo centralizados com ◆; capítulos em âmbar. */
@Composable
private fun TituloNoTexto(t: String, p: Paleta, extra: Modifier) {
    val esp = Texto.tipoEspecial(t)
    if (esp != null) {
        Column(extra.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("◆", color = p.destaque, fontSize = 20.sp)
            Text(esp, color = p.destaque, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp)
            val sub = t.substringAfter('—', t.substringAfter(':', "")).trim()
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp, lineHeight = 34.sp, fontStyle = FontStyle.Italic),
                textAlign = TextAlign.Center)
            Box(Modifier.width(48.dp).height(2.dp).background(p.destaque.copy(alpha = 0.6f)))
        }
    } else {
        Column(extra.fillMaxWidth().padding(top = 22.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(t, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 29.sp), color = p.destaque)
            Box(Modifier.width(40.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(p.destaque))
        }
    }
}

@Composable
private fun Fala(texto: AnnotatedString, estilo: TextStyle, p: Paleta, extra: Modifier) {
    // travessão/aspas em âmbar + barra lateral forte + fundo levemente tingido: dá pra ver na hora que alguém fala
    val colorido = remember(texto, p) {
        buildAnnotatedString {
            append(texto)
            val t = texto.text
            var fim = 0
            while (fim < t.length && (t[fim] in "—–―-“\"«" || t[fim] == ' ')) fim++
            if (fim > 0) addStyle(SpanStyle(color = p.destaque, fontWeight = FontWeight.ExtraBold), 0, fim)
            // travessões no meio da fala ("— disse ela. —") também em âmbar
            var k = fim
            while (k < t.length) { if (t[k] == '—') addStyle(SpanStyle(color = p.destaque), k, k + 1); k++ }
        }
    }
    Row(
        extra.height(IntrinsicSize.Min).clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
            .background(p.destaque.copy(alpha = 0.07f)),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(p.destaque.copy(alpha = 0.9f)))
        Spacer(Modifier.width(12.dp))
        Text(colorido, style = estilo.copy(color = p.texto, fontWeight = FontWeight.Medium), modifier = Modifier.padding(vertical = 6.dp).padding(end = 10.dp))
    }
}

private fun destacarBusca(base: AnnotatedString, normal: String, q: String, p: Paleta, atual: Int?): AnnotatedString {
    if (!normal.contains(q)) return base
    return buildAnnotatedString {
        append(base)
        var i = normal.indexOf(q)
        while (i >= 0) {
            val fim = (i + q.length).coerceAtMost(base.length)
            if (i < fim) addStyle(SpanStyle(background = if (i == atual) p.destaque.copy(alpha = 0.75f) else p.destaque.copy(alpha = 0.3f)), i, fim)
            i = normal.indexOf(q, i + q.length)
        }
    }
}

@Composable
private fun FimCapitulo(vazio: Boolean, proximo: Capitulo?, ir: (Capitulo) -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (vazio) Text("Este capítulo ainda está vazio.", color = p.textoSuave)
        val pulso = androidx.compose.animation.core.rememberInfiniteTransition(label = "fim")
        val brilho by pulso.animateFloat(0.45f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1400), androidx.compose.animation.core.RepeatMode.Reverse), label = "brilho")
        Text("◆", color = p.destaque.copy(alpha = brilho), fontSize = 26.sp)
        Text("FIM DO CAPÍTULO", style = MaterialTheme.typography.labelSmall, color = p.textoSuave, letterSpacing = 4.sp)
        if (proximo != null) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.superficie)
                    .border(1.dp, p.destaque.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).clickable { ir(proximo) }.padding(18.dp),
            ) {
                Text("PRÓXIMO CAPÍTULO", style = MaterialTheme.typography.labelSmall, color = p.destaque)
                Text(proximo.titulo, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${formatarPalavras(proximo.palavras.toLong())} palavras  ›", color = p.textoSuave, fontSize = 13.sp)
            }
        } else Text("Você chegou ao último capítulo.", color = p.textoSuave)
    }
}

@Composable
private fun BotaoBarra(rotulo: String, ativo: Boolean, icone: @Composable () -> Unit, aoTocar: () -> Unit) {
    val p = LocalPaleta.current
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = aoTocar).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides if (ativo) p.destaque else p.textoSuave) { icone() }
        Text(rotulo, fontSize = 11.sp, color = if (ativo) p.destaque else p.textoSuave, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PainelVoz(voz: Voz, tocando: Boolean, velocidade: Float, alternar: () -> Unit, voltar: () -> Unit, avancar: () -> Unit, definirVel: (Float) -> Unit,
                      narrador: String, definirNarrador: (String) -> Unit) {
    val p = LocalPaleta.current
    var escolhendo by remember { mutableStateOf(false) }
    Column(
        Modifier.padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(p.superficie)
            .border(1.dp, p.destaque.copy(alpha = 0.35f), RoundedCornerShape(22.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(voz.erro ?: if (tocando) "Lendo em voz alta…" else "Ouvir a partir daqui", fontWeight = FontWeight.Bold, color = p.destaque, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { definirVel(when { velocidade < 0.95f -> 1.0f; velocidade < 1.15f -> 1.25f; velocidade < 1.4f -> 1.5f; velocidade < 1.9f -> 2.0f; else -> 0.8f }) }) {
                Text("${"%.2f".format(velocidade).trimEnd('0').trimEnd(',', '.')}×", fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = voltar) { Icon(Icons.Filled.KeyboardArrowUp, "Parágrafo anterior") }
            FilledIconButton(onClick = alternar, enabled = voz.pronta, modifier = Modifier.size(58.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = p.destaque, contentColor = p.sobreDestaque)) {
                if (tocando) Text("❚❚", fontWeight = FontWeight.Bold) else Icon(Icons.Filled.PlayArrow, "Tocar")
            }
            IconButton(onClick = avancar) { Icon(Icons.Filled.KeyboardArrowDown, "Próximo parágrafo") }
            Box {
                TextButton(onClick = { escolhendo = true }, enabled = voz.pronta) { Text("Narrador", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                DropdownMenu(escolhendo, { escolhendo = false }, modifier = Modifier.heightIn(max = 380.dp)) {
                    val lista = remember(voz.pronta) { voz.vozes() }
                    if (lista.isEmpty()) DropdownMenuItem(text = { Text("Nenhuma voz brasileira offline. Instale em Configurações › Saída de texto para voz.") }, onClick = { escolhendo = false })
                    var mulher = 0; var homem = 0; var outra = 0
                    lista.forEach { v ->
                        val g = generoVoz(v.name)
                        val rotulo = when (g) {
                            "Mulher" -> "♀ Mulher · voz ${++mulher}"
                            "Homem" -> "♂ Homem · voz ${++homem}"
                            else -> "◦ Voz ${++outra}"
                        }
                        DropdownMenuItem(
                            text = { Text(rotulo, fontWeight = if (v.name == narrador) FontWeight.Bold else FontWeight.Normal, color = if (v.name == narrador) p.destaque else p.texto) },
                            onClick = { escolhendo = false; definirNarrador(v.name) },
                        )
                    }
                }
            }
        }
        if (narrador.isNotEmpty()) Text("Toque em “Narrador” para trocar a voz. Para mais vozes: Configurações do Android › Saída de texto para voz.", fontSize = 11.sp, color = p.textoSuave)
    }
}

@Composable
private fun PainelBusca(consulta: String, mudar: (String) -> Unit, total: Int, atual: Int, anterior: () -> Unit, proximo: () -> Unit, fechar: () -> Unit) {
    val p = LocalPaleta.current
    Row(
        Modifier.padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.superficie)
            .border(1.dp, p.destaque.copy(alpha = 0.35f), RoundedCornerShape(18.dp)).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = p.destaque)
        TextField(
            value = consulta, onValueChange = mudar, singleLine = true, placeholder = { Text("Buscar no capítulo") },
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { proximo() }),
            modifier = Modifier.weight(1f),
        )
        if (consulta.length >= 2) Text(if (total == 0) "0" else "${atual + 1}/$total", fontSize = 12.sp, color = p.textoSuave)
        IconButton(onClick = anterior) { Icon(Icons.Filled.KeyboardArrowUp, "Anterior") }
        IconButton(onClick = proximo) { Icon(Icons.Filled.KeyboardArrowDown, "Próximo") }
        IconButton(onClick = fechar) { Icon(Icons.Filled.Close, "Fechar busca") }
    }
}

/** Painel “Aa”: aparência da leitura. Também usado na tela de Ajustes. */
@Composable
fun AjustesLeitura() {
    val prefs = NovelyApp.instancia.prefs
    val p = LocalPaleta.current
    val tema by prefs.temaP.estado.collectAsState()
    val tamanho by prefs.tamanhoP.estado.collectAsState()
    val entrelinhas by prefs.entrelinhas.estado.collectAsState()
    val margem by prefs.margem.estado.collectAsState()
    val fonte by prefs.fonte.estado.collectAsState()
    val falas by prefs.destacarFalas.estado.collectAsState()
    val paginas by prefs.modoPaginas.estado.collectAsState()
    val ligada by prefs.telaLigada.estado.collectAsState()
    val cores = SliderDefaults.colors(thumbColor = p.destaque, activeTrackColor = p.destaque, inactiveTrackColor = p.borda)

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Leitura", style = MaterialTheme.typography.headlineSmall)
        Text("TEMA", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.huryel.novely.ui.theme.TemaLeitura.entries.forEach { t ->
                val sel = t.name == tema
                Box(
                    Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(14.dp)).background(t.paleta.fundo)
                        .border(if (sel) 2.dp else 1.dp, if (sel) p.destaque else p.borda, RoundedCornerShape(14.dp))
                        .clickable { prefs.definirTema(t.name) },
                    contentAlignment = Alignment.Center,
                ) { Text(t.paleta.nome, color = t.paleta.texto, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Com serifa", fonte == "SERIFA") { prefs.fonte.definir("SERIFA") }
            Chip("Sem serifa", fonte == "SEM_SERIFA") { prefs.fonte.definir("SEM_SERIFA") }
        }
        LinhaSlider("Tamanho", "${tamanho.toInt()}", tamanho, 14f..30f, cores) { prefs.tamanhoP.definir(it) }
        LinhaSlider("Entrelinhas", "%.1f".format(entrelinhas), entrelinhas, 1.3f..2.2f, cores) { prefs.entrelinhas.definir(it) }
        LinhaSlider("Margens", "${margem.toInt()}", margem, 8f..48f, cores) { prefs.margem.definir(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Rolagem contínua", !paginas) { prefs.modoPaginas.definir(false) }
            Chip("Páginas por toque", paginas) { prefs.modoPaginas.definir(true) }
        }
        if (paginas) Text("Toque na borda direita para avançar e na esquerda para voltar.", fontSize = 12.sp, color = p.textoSuave)
        LinhaInterruptor("Destacar falas de personagens", falas) { prefs.destacarFalas.definir(it) }
        LinhaInterruptor("Manter tela ligada lendo", ligada) { prefs.telaLigada.definir(it) }
    }
}

@Composable
private fun LinhaSlider(rotulo: String, valorTxt: String, v: Float, faixa: ClosedFloatingPointRange<Float>, cores: SliderColors, mudar: (Float) -> Unit) {
    val p = LocalPaleta.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, modifier = Modifier.width(96.dp), fontSize = 14.sp)
        Slider(v, mudar, valueRange = faixa, colors = cores, modifier = Modifier.weight(1f))
        Text(valorTxt, modifier = Modifier.width(36.dp), textAlign = TextAlign.End, fontSize = 13.sp, color = p.textoSuave)
    }
}

@Composable
fun LinhaInterruptor(rotulo: String, valor: Boolean, mudar: (Boolean) -> Unit) {
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { mudar(!valor) }, verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(valor, mudar, colors = SwitchDefaults.colors(checkedThumbColor = p.sobreDestaque, checkedTrackColor = p.destaque))
    }
}
