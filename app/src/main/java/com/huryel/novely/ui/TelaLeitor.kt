package com.huryel.novely.ui

import androidx.compose.ui.graphics.drawscope.clipRect

import com.huryel.novely.ui.theme.forma
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
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
import androidx.compose.animation.core.animateFloat
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
import com.huryel.novely.data.Marcas
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Paleta
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import com.huryel.novely.data.StatusObra
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified

// ───────── Blocos de texto ─────────

sealed interface Bloco {
    val plano: String
    data class Titulo(override val plano: String) : Bloco

    data class Paragrafo(val texto: AnnotatedString, val fala: Boolean) : Bloco { override val plano: String get() = texto.text }
    data object Separador : Bloco { override val plano = "" }
    /** Efeito de escrita numa linha inteira (data/Fx.kt). */
    data class Efeito(val uso: com.huryel.novely.data.FxUso) : Bloco { override val plano: String get() = com.huryel.novely.data.Fx.plano(uso) }
}

private val iniciosDeFala = listOf("—", "–", "―", "-", "“", "\"", "«")

fun ehFala(linha: String): Boolean {
    val l = linha.trimStart()
    return iniciosDeFala.any { l.startsWith(it) } && l.length > 1 && !l.startsWith("---")
}

/** **negrito** e *itálico*. */
fun estilizar(linha: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    var negrito = false; var italico = false
    while (i < linha.length) {
        if (linha.startsWith("**", i)) { negrito = !negrito; i += 2; continue }
        if (linha[i] == '*') { italico = !italico; i++; continue }
        if (linha[i] == '{') {
            val m = com.huryel.novely.data.Fx.RX_INLINE.find(linha, i)?.takeIf { it.range.first == i }
            if (m != null && com.huryel.novely.data.Fx.conhecido(m.groupValues[1])) {
                val cs = m.groupValues[2].split('|').map { it.trim() }
                val def = com.huryel.novely.data.Fx.def(m.groupValues[1])!!
                pushStyle(SpanStyle(fontWeight = if (negrito) FontWeight.Bold else null, fontStyle = if (italico) FontStyle.Italic else null))
                if (def.tipo == com.huryel.novely.data.FxTipo.CORRIGE && cs.size > 1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color(0xFFE0445A))) { append(cs[0]) }
                    append(" ")
                }
                withStyle(estiloFxInline(m.groupValues[1])) { append(cs.last()) }
                pop()
                i = m.range.last + 1
                continue
            }
        }
        val ini = i
        i++
        while (i < linha.length && linha[i] != '*' && linha[i] != '{') i++
        pushStyle(SpanStyle(fontWeight = if (negrito) FontWeight.Bold else null, fontStyle = if (italico) FontStyle.Italic else null))
        append(linha, ini, i); pop()
    }
}

/** Texto do capítulo → blocos (um por parágrafo; parágrafos gigantes viram vários blocos). Fora da thread principal. */
/** Modo livro: cada página é uma lista de pedaços de blocos (parágrafos cortados por linha). */
private sealed interface ItemPag {
    data object Cabecalho : ItemPag
    data class Trecho(val bi: Int, val ini: Int, val fim: Int, val altura: Int) : ItemPag
    data object Fim : ItemPag
}
private class Pagina(val itens: List<ItemPag>) {
    val primeiroBloco: Int get() = itens.firstNotNullOfOrNull { (it as? ItemPag.Trecho)?.bi } ?: -1
}

/** origens (opcional) recebe, para cada bloco, a linha do texto de onde ele veio (para a prévia do editor achar o símbolo). */
fun montarBlocos(texto: String, origens: MutableList<Int>? = null): List<Bloco> {
    val saida = ArrayList<Bloco>(texto.length / 300 + 4)
    var nLinha = -1
    var anteriorVazia = true
    com.huryel.novely.data.Limpeza.limparTexto(texto).lineSequence().forEach { bruta ->
        if (origens != null) while (origens.size < saida.size) origens.add(nLinha)
        nLinha++
        val linha = Marcas.limpar(bruta).trim()
        val podeSerTitulo = anteriorVazia
        anteriorVazia = linha.isEmpty()
        when {
            linha.isEmpty() -> {}
            podeSerTitulo && Texto.ehTituloCapitulo(linha) && !linha.startsWith("#") -> saida.add(Bloco.Titulo(Texto.tituloLimpo(linha)))
            linha == "* * *" || linha == "***" || linha == "---" || linha == "◆" -> saida.add(Bloco.Separador)
            '[' in linha && com.huryel.novely.data.Fx.dividirLinha(linha).any { it is com.huryel.novely.data.FxUso } || com.huryel.novely.data.Fx.lerLinha(linha) != null ->
                com.huryel.novely.data.Fx.dividirLinha(linha).forEach { pd ->
                    if (pd is com.huryel.novely.data.FxUso) saida.add(Bloco.Efeito(pd)) else (pd as String).let { saida.add(Bloco.Paragrafo(estilizar(it), ehFala(it))) }
                }
            linha.startsWith("#") -> saida.add(Bloco.Titulo(linha.trimStart('#').trim()))
            linha.length > 2500 -> {
                val fala = ehFala(linha)
                Texto.dividirEmPedacos(linha, 1500).forEach { saida.add(Bloco.Paragrafo(estilizar(it.trim()), fala)) }
            }
            else -> saida.add(Bloco.Paragrafo(estilizar(linha), ehFala(linha)))
        }
    }
    if (origens != null) while (origens.size < saida.size) origens.add(nLinha)
    return saida
}

/** Um bloco desenhado do jeito do leitor, sem nomes, comentários nem voz (prévia do editor). */
@Composable
fun BlocoPrevia(bloco: Bloco, estilo: TextStyle, p: Paleta, modifier: Modifier = Modifier) {
    when (bloco) {
        is Bloco.Titulo -> TituloNoTexto(bloco.plano, p, modifier)
        is Bloco.Separador -> Box(modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) { SeparadorCena("", p.destaque) }
        is Bloco.Paragrafo -> if (bloco.fala) Fala(bloco.texto, estilo, p, modifier) else Text(bloco.texto, style = estilo, modifier = modifier)
        is Bloco.Efeito -> FxBloco(bloco.uso, estilo, modifier)
    }
}


// ───────── Leitura em voz alta (motor de voz do próprio Android, funciona offline se a voz estiver instalada) ─────────

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

/** Volta direto para o Início, limpando a pilha de telas. */
fun irInicio(nav: NavHostController) {
    nav.navigate("inicio") { popUpTo(0) { inclusive = true }; launchSingleTop = true }
}

// ───────── Tela ─────────

private enum class Painel { NENHUM, VOZ, BUSCA }

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun TelaLeitor(capId: Long, nav: NavHostController, consultaInicial: String = "", ocorrenciaInicial: Int = 0, blocoInicial: Int = -1) {
    // no texto, a cor da obra (ou cores calmas); de madrugada, âmbar fraco
    val app = NovelyApp.instancia
    val obraDoCap by produceState<com.huryel.novely.data.Obra?>(null, capId) {
        value = withContext(Dispatchers.IO) { app.repo.dao.capituloAgora(capId)?.let { app.repo.dao.obraAgora(it.obraId) } }
    }
    val cor = obraDoCap?.let { rememberCorObra(it.capa, it.corBase) }
    val noite by app.prefs.noiteProfunda.estado.collectAsState()
    CoresDeLeitura(cor, noite) { TelaLeitorCorpo(capId, nav, consultaInicial, ocorrenciaInicial, blocoInicial) }
}

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
private fun TelaLeitorCorpo(capId: Long, nav: NavHostController, consultaInicial: String, ocorrenciaInicial: Int, blocoInicial: Int) {
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
    val destacarNomes by prefs.destacarNomes.estado.collectAsState()
    val modoPaginas by prefs.modoPaginas.estado.collectAsState()
    val telaLigada by prefs.telaLigada.estado.collectAsState()
    val velocidade by prefs.velocidadeVoz.estado.collectAsState()
    val escurecer by prefs.escurecer.estado.collectAsState()
    val luzQuente by prefs.luzQuente.estado.collectAsState()
    val narrador by prefs.narrador.estado.collectAsState()

    var cap by remember { mutableStateOf<Capitulo?>(null) }
    var blocos by remember { mutableStateOf<List<Bloco>?>(null) }
    var lista by remember { mutableStateOf<List<Capitulo>>(emptyList()) }
    var controles by remember { mutableStateOf(true) }
    var painel by remember { mutableStateOf(if (consultaInicial.isNotEmpty()) Painel.BUSCA else Painel.NENHUM) }
    var mostrarAjustes by remember { mutableStateOf(false) }
    var mostrarCapitulos by remember { mutableStateOf(false) }
    var mostrarSobre by remember { mutableStateOf(false) }

    // tradução no aparelho: o idioma escolhido vale para os próximos capítulos também
    var original by remember { mutableStateOf<String?>(null) }
    val idiomaLeitura by prefs.idiomaLeitura.estado.collectAsState()
    var traduzindo by remember { mutableStateOf<String?>(null) }
    var escolherIdioma by remember { mutableStateOf(false) }
    val ctxT = LocalContext.current
    LaunchedEffect(capId) {
        val c = repo.capitulo(capId).filterNotNull().first()
        cap = c
        val t = repo.lerTexto(capId)
        if (idiomaLeitura.isEmpty()) blocos = withContext(Dispatchers.Default) { montarBlocos(t) }
        original = t
        lista = repo.dao.capitulosAgora(c.obraId)
    }
    LaunchedEffect(original, idiomaLeitura) {
        val t = original ?: return@LaunchedEffect
        if (idiomaLeitura.isEmpty()) { blocos = withContext(Dispatchers.Default) { montarBlocos(t) }; return@LaunchedEffect }
        traduzindo = "Preparando a tradução…"
        val trad = try {
            com.huryel.novely.data.Tradutor.traduzir(ctxT, "cap$capId", t, idiomaLeitura) { feitas, total ->
                traduzindo = if (total < 0) "Baixando o idioma (só na primeira vez)…" else "Traduzindo $feitas de $total…"
            }
        } catch (e: Exception) { traduzindo = null; android.widget.Toast.makeText(ctxT, "Não deu para traduzir: ${e.message}", android.widget.Toast.LENGTH_LONG).show(); prefs.idiomaLeitura.definir(""); t }
        blocos = withContext(Dispatchers.Default) { montarBlocos(trad) }
        traduzindo = null
    }

    val view = LocalView.current
    DisposableEffect(telaLigada) {
        view.keepScreenOn = telaLigada
        onDispose { view.keepScreenOn = false }
    }

    val c = cap
    val b = blocos
    if (c == null || b == null) {
        Box(Modifier.fillMaxSize().background(p.fundo), contentAlignment = Alignment.Center) {
            val pulso = androidx.compose.animation.core.rememberInfiniteTransition(label = "abrindo")
            val a by pulso.animateFloat(0.25f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(700), androidx.compose.animation.core.RepeatMode.Reverse), label = "a")
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("◆", color = p.destaque, fontSize = 30.sp, modifier = Modifier.graphicsLayer { alpha = a })
                traduzindo?.let { Text(it, color = p.textoSuave, fontSize = 13.sp) }
            }
        }
        return
    }
    // ignora mudanças só de data (salvar a posição mexe nelas) para não redesenhar o leitor inteiro
    val obra by remember(c.obraId) {
        repo.obra(c.obraId).map { it?.copy(lidaEm = 0, atualizadaEm = 0, ultimoCapituloId = null) }.distinctUntilChanged()
    }.collectAsState(initial = null)
    // obra publicada (minha ou baixada do catálogo): curtidas, comentários e contagem de leitura
    val social = obra?.let { ob ->
        when {
            ob.publicaId.isNotEmpty() && c.publicaId.isNotEmpty() -> ob.publicaId to c.publicaId
            ob.publicada && ob.minha && c.publicadoEm in 1..System.currentTimeMillis() -> ob.nuvemId to c.nuvemId
            else -> null
        }
    }
    LaunchedEffect(social) { if (social != null && obra?.publicaId?.isNotEmpty() == true) app.pub.contarLeitura(social.first, social.second) }
    val idx = lista.indexOfFirst { it.id == c.id }
    val anterior = lista.getOrNull(idx - 1)
    val proximo = lista.getOrNull(idx + 1)
    fun irPara(outro: Capitulo) = nav.navigate("leitor/${outro.id}") { popUpTo(ROTA_LEITOR) { inclusive = true } }

    val estado = rememberLazyListState(c.posicaoBloco.coerceIn(0, b.size + 1), c.posicaoDeslocamento)
    // modo livro
    var paginas by remember { mutableStateOf<List<Pagina>?>(null) }
    val paginador = rememberPagerState { paginas?.size ?: 1 }
    val emPaginas = modoPaginas && paginas != null
    fun paginaDoBloco(bi: Int): Int = paginas?.indexOfFirst { pg -> pg.itens.any { it is ItemPag.Trecho && it.bi == bi } }?.takeIf { it >= 0 } ?: 0
    suspend fun irParaBloco(bi: Int, animar: Boolean = true) {
        if (modoPaginas && paginas != null) { val pg = paginaDoBloco(bi); if (pg != paginador.currentPage) { if (animar) paginador.animateScrollToPage(pg) else paginador.scrollToPage(pg) } }
        else if (animar) estado.animateScrollToItem((bi + 1).coerceAtMost(b.size + 1)) else estado.scrollToItem((bi + 1).coerceAtMost(b.size + 1))
    }
    fun blocoAtual(): Int = if (modoPaginas && paginas != null) {
        val pgs = paginas!!; (paginador.currentPage downTo 0).firstNotNullOfOrNull { pgs.getOrNull(it)?.primeiroBloco?.takeIf { x -> x >= 0 } } ?: 0
    } else (estado.firstVisibleItemIndex - 1).coerceAtLeast(0)
    val parouAoAbrir = remember(c.id) { c.posicaoBloco }
    val marcasMapa = remember(b) {
        val tot = (b.size + 2).toFloat()
        val m = b.mapIndexedNotNull { i, bl -> when {
            bl is Bloco.Separador -> MarcaMapa((i + 1) / tot, 0, i)
            else -> null } }
        if (parouAoAbrir > 1 && c.progresso < 0.98f) m + MarcaMapa(parouAoAbrir / tot, 2, (parouAoAbrir - 1).coerceIn(0, (b.size - 1).coerceAtLeast(0))) else m
    }
    val progresso by remember {
        derivedStateOf {
            val pgs = paginas
            if (modoPaginas && pgs != null) {
                if (paginador.currentPage >= pgs.size - 1) 1f else ((paginador.currentPage + 1).toFloat() / pgs.size).coerceIn(0f, 1f)
            } else {
                val total = b.size + 2
                val ultimo = estado.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (ultimo >= total - 1) 1f else ((estado.firstVisibleItemIndex + 1).toFloat() / total).coerceIn(0f, 1f)
            }
        }
    }
    val jaLido = c.progresso >= 0.98f
    LaunchedEffect(blocoInicial) { if (blocoInicial >= 0 && !modoPaginas) estado.scrollToItem((blocoInicial + 1).coerceAtMost(b.size + 1)) }

    // ── Marcadores, fichas, folha do parágrafo ──
    val marcas by remember(c.id) { repo.marcadoresCap(c.id) }.collectAsState(initial = emptyList())
    // presos ao texto: se o autor mexeu nos parágrafos, o grifo segue o trecho (e o número novo é guardado)
    val ancoradas = remember(marcas, b) { val planos = b.map { it.plano }; marcas.map { it to com.huryel.novely.data.Ancora.ancorar(planos, it.bloco, it.trecho) } }
    val marcasPorBloco = remember(ancoradas) { ancoradas.associate { (m, bi) -> bi to m } }
    LaunchedEffect(ancoradas) { ancoradas.filter { (m, bi) -> bi != m.bloco }.forEach { (m, bi) -> repo.salvarMarcador(m.copy(bloco = bi)) } }
    val fichas by remember(c.obraId) { repo.fichas(c.obraId) }.collectAsState(initial = emptyList())
    LaunchedEffect(c.obraId) { repo.repararNomes(c.obraId) }
    var blocoSel by remember { mutableStateOf(-1) }
    var fichaVer by remember { mutableStateOf<com.huryel.novely.data.Ficha?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var termoVer by remember { mutableStateOf("") } // palavra tocada no texto que abriu a ficha

    // ── Rolagem automática ──
    val velRolagem by prefs.velocidadeRolagem.estado.collectAsState()
    var rolando by remember { mutableStateOf(false) }
    LaunchedEffect(rolando, velRolagem) {
        while (rolando) {
            withFrameNanos { }
            if (modoPaginas) { rolando = false; break }
            estado.scrollBy(velRolagem * 1.4f)
            if (!estado.canScrollForward) rolando = false
        }
    }

    // ── Estatísticas: tempo lendo e palavras lidas ──
    val progIni = remember { c.progresso }
    var inicioSessao by remember { mutableStateOf(System.currentTimeMillis()) }
    val donoCiclo = androidx.lifecycle.compose.LocalLifecycleOwner.current
    // até onde chegou nesta visita (para a estatística do autor de obras do catálogo)
    val maxBloco = remember(c.id) { intArrayOf(-1) }
    LaunchedEffect(c.id, paginas) { snapshotFlow { if (progresso >= 0.995f) b.size - 1 else blocoAtual() }.collect { if (it > maxBloco[0]) maxBloco[0] = it } }
    fun registrarSessao() {
        val seg = ((System.currentTimeMillis() - inicioSessao) / 1000).toInt().coerceIn(0, 3 * 3600)
        val pal = ((progresso - progIni).coerceAtLeast(0f) * c.palavras).toInt()
        if (seg >= 5) prefs.registrarLeitura(seg, pal)
        val s = social
        if (s != null && obra?.minha != true && seg >= 3) { val m = maxBloco[0]; app.escopo.launch { app.pub.registrarLeitura(s.first, s.second, m, b.size, seg) } }
        inicioSessao = System.currentTimeMillis()
    }
    DisposableEffect(donoCiclo) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) registrarSessao()
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) inicioSessao = System.currentTimeMillis()
        }
        donoCiclo.lifecycle.addObserver(obs)
        onDispose { donoCiclo.lifecycle.removeObserver(obs); registrarSessao() }
    }
    LaunchedEffect(estado) {
        snapshotFlow { if (modoPaginas) (if (paginas != null) blocoAtual() + 1 to -1 else -1 to -1) else estado.firstVisibleItemIndex to estado.firstVisibleItemScrollOffset }
            .debounce(700)
            .collect { (i, d) -> if (i >= 0) repo.salvarPosicao(c, if (jaLido) 1f else progresso, i, d.coerceAtLeast(0)) }
    }
    DisposableEffect(Unit) {
        onDispose {
            com.huryel.novely.data.Ambiente.parar() // ::som do capítulo para ao sair
            val emLivro = modoPaginas && paginas != null
            val i = if (emLivro) blocoAtual() + 1 else estado.firstVisibleItemIndex
            val d = if (emLivro) 0 else estado.firstVisibleItemScrollOffset; val pr = progresso
            app.escopo.launch { repo.salvarPosicao(c, if (jaLido) 1f else pr, i, d) }
        }
    }

    // ── Voz: o Narrador lê com uma voz por personagem e continua com a tela apagada ──
    val nar by com.huryel.novely.data.Narrador.estado.collectAsState()
    val vozPronta by com.huryel.novely.data.Narrador.pronta.collectAsState()
    val vozesPersonagens by prefs.vozesPersonagens.estado.collectAsState()
    val chaveVoz = "cap:${c.id}:${b.size}"
    val falando = if (nar.chave == chaveVoz) nar.bloco else -1
    val tocando = nar.chave == chaveVoz && nar.tocando
    val falaveis = remember(b) { b.indices.filter { b[it] !is Bloco.Separador && b[it].plano.isNotBlank() } }
    LaunchedEffect(Unit) { com.huryel.novely.data.Narrador.iniciar(ctx) }
    LaunchedEffect(vozPronta, narrador) { if (vozPronta) com.huryel.novely.data.Narrador.definirNarrador(narrador) }
    LaunchedEffect(velocidade, vozesPersonagens) { com.huryel.novely.data.Narrador.velocidade = velocidade; com.huryel.novely.data.Narrador.vozesPersonagens = vozesPersonagens }
    LaunchedEffect(falando) {
        if (tocando && falando >= 0) {
            if (modoPaginas) irParaBloco(falando) else {
                val vis = estado.layoutInfo.visibleItemsInfo
                val item = falando + 1
                if (vis.none { it.index == item } || vis.lastOrNull()?.index == item) estado.animateScrollToItem(item)
            }
        }
    }
    suspend fun prepararVoz() = com.huryel.novely.data.Narrador.carregar(chaveVoz, listOfNotNull(obra?.titulo, c.titulo).joinToString(" · "),
        b.map { it.plano }, b.map { it is Bloco.Paragrafo && it.fala }, c.obraId)
    fun tocar() {
        val ini = if (falando >= 0) falando else falaveis.firstOrNull { it >= blocoAtual() } ?: return
        escopo.launch { prepararVoz(); com.huryel.novely.data.Narrador.tocar(ini) }
    }
    fun pausar() = com.huryel.novely.data.Narrador.pausar()
    /** Ouvir a partir de um parágrafo escolhido (menu do parágrafo ou "Ouvir o que está na tela"). */
    fun tocarDe(bloco: Int) {
        val ini = falaveis.firstOrNull { it >= bloco } ?: return
        escopo.launch { prepararVoz(); com.huryel.novely.data.Narrador.tocar(ini) }
    }

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
        achados.getOrNull(achadoAtual)?.let { (bi, _) -> irParaBloco(bi) }
    }

    // ── Nomes de personagens/lugares destacados (toque abre a ficha) ──
    val ignoradosTxt by prefs.naoEhNome.estado.collectAsState()
    val ignorados = remember(ignoradosTxt) { prefs.ignorados() }
    val regraNomes = remember(fichas) {
        // nome completo e apelidos escritos pelo autor valem sempre; partes do nome entram só como palpite (confiança menor)
        val mapa = HashMap<String, com.huryel.novely.data.Ficha>()
        val palpites = HashSet<String>()
        fichas.forEach { f -> val n = f.nome.trim(); if (n.length >= 2) mapa[n] = f }
        fichas.forEach { f -> f.apelidos.split(',').map { it.trim() }.forEach { a -> if (a.length >= 2 && !mapa.containsKey(a)) mapa[a] = f } }
        // partes do nome que só um personagem tem ("Seo-jun" para "Kang Seo-jun"); "Seu", "Dona" etc. nunca entram
        Marcas.partesUnicas(fichas.associate { it.nome to it.id }).forEach { (parte, id) -> if (!mapa.containsKey(parte)) fichas.firstOrNull { it.id == id }?.let { mapa[parte] = it; palpites += parte } }
        Marcas.regexNomes(mapa.keys)?.let { Triple(it, mapa, palpites) }
    }
    fun comNomes(t: AnnotatedString): AnnotatedString {
        val (rx, mapa, palpites) = regraNomes ?: return t
        if (!destacarNomes) return t
        val achados = rx.findAll(t.text).toList()
        if (achados.isEmpty()) return t
        return buildAnnotatedString {
            append(t)
            achados.forEach { m ->
                val f = mapa[m.value] ?: return@forEach
                val conf = Marcas.confianca(t.text, m.range.first, m.value, m.value !in palpites, "${f.id}|${m.value}" in ignorados)
                if (conf < Marcas.LIMIAR_LEVE) return@forEach
                val estilo = if (conf >= Marcas.LIMIAR_FORTE) SpanStyle(color = p.destaque2, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.None)
                    else SpanStyle(textDecoration = TextDecoration.Underline) // palpite: sublinhado leve, sem cor
                addLink(LinkAnnotation.Clickable("ficha${f.id}", TextLinkStyles(estilo)) { fichaVer = f; termoVer = m.value }, m.range.first, m.range.last + 1)
            }
        }
    }
    val capitularLigado by prefs.capitular.estado.collectAsState()
    val primeiroParagrafo = remember(b, capitularLigado) { if (!capitularLigado) -1 else b.indexOfFirst { it is Bloco.Paragrafo && !(it as Bloco.Paragrafo).fala && it.plano.length > 40 } }
    val enfeite = obra?.enfeite ?: ""
    // comentários por trecho (obras do catálogo)
    var comentariosCap by remember { mutableStateOf<List<com.huryel.novely.data.Comentario>>(emptyList()) }
    var recarregarComs by remember { mutableIntStateOf(0) }
    LaunchedEffect(social, recarregarComs) { social?.let { (pid, cid) -> comentariosCap = try { app.pub.comentarios(pid, cid) } catch (_: Exception) { emptyList() } } }
    val comsPorBloco = remember(comentariosCap) { comentariosCap.filter { it.bloco >= 0 }.groupingBy { it.bloco }.eachCount() }
    var trechoComentar by remember { mutableIntStateOf(-1) }
    // maratona: no fim do capítulo, o próximo começa sozinho
    val maratona by prefs.maratona.estado.collectAsState()
    var contagem by remember(c.id) { mutableIntStateOf(-1) }
    val noFim by remember { derivedStateOf { progresso >= 0.995f } }
    // moeda por terminar capítulo de outro autor (uma vez por capítulo, até 10 por dia)
    LaunchedEffect(noFim, social) {
        val sc = social
        if (noFim && sc != null && obra?.minha != true) com.huryel.novely.data.Moedas.ganhar(1, "leitura", 10, "${sc.first}/${sc.second}")
        // missão "ler" e XP: um por capítulo de obra que não é sua
        if (noFim && obra != null && obra?.minha != true && !prefs.jaContado("xp/lido/${c.id}")) { prefs.marcarContado("xp/lido/${c.id}"); com.huryel.novely.data.Progresso.registrar("ler") }
    }
    LaunchedEffect(noFim, maratona, c.id) {
        if (!maratona || !noFim || proximo == null || contagem != -1) return@LaunchedEffect
        for (sgs in 5 downTo 1) { contagem = sgs; kotlinx.coroutines.delay(1000); if (contagem < 0) return@LaunchedEffect }
        contagem = -2; irPara(proximo)
    }

    // aviso "continuando de onde parou"
    var avisoRetomar by remember { mutableStateOf(c.posicaoBloco > 2 && blocoInicial < 0 && consultaInicial.isEmpty() && c.progresso < 0.98f) }
    LaunchedEffect(Unit) { if (avisoRetomar) { kotlinx.coroutines.delay(5000); avisoRetomar = false } }
    // entrada suave
    var entrou by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entrou = true }
    val alfaEntrada by androidx.compose.animation.core.animateFloatAsState(if (entrou) 1f else 0f, androidx.compose.animation.core.tween(420), label = "entrada")

    val familia = when (fonte) { "SEM_SERIFA" -> FontFamily.SansSerif; "SERIFA" -> FontFamily.Serif; else -> p.fonteLeitura }
    val estiloBase = TextStyle(fontFamily = familia, fontSize = tamanho.sp, lineHeight = (tamanho * entrelinhas).sp, color = p.texto)
    // Luar D1: narração em cinza calmo, fala em creme com fio ametista
    val corNarracao = if (destacarFalas) Color(0xFFB9B2C9) else p.texto
    val q = Texto.normalizar(consulta.trim())

    // texto decorado (nomes + capitular): o mesmo que é medido e desenhado
    fun decorado(i: Int): AnnotatedString {
        val bl = b[i] as? Bloco.Paragrafo ?: return AnnotatedString(b[i].plano)
        val t = comNomes(bl.texto)
        return if (i == primeiroParagrafo) capitular(t, p, tamanho) else t
    }
    val medidor = rememberTextMeasurer(cacheSize = 0)
    val densidade = androidx.compose.ui.platform.LocalDensity.current
    var areaLivro by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val estiloLivro = estiloBase.copy(lineBreak = LineBreak.Simple)
    val estiloTituloLivro = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 29.sp)
    LaunchedEffect(modoPaginas, b, areaLivro, tamanho, entrelinhas, margem, fonte, destacarFalas, destacarNomes, regraNomes) {
        if (!modoPaginas) { paginas?.let { pgs -> val bi = blocoAtual(); paginas = null; estado.scrollToItem((bi + 1).coerceAtMost(b.size + 1)) }; return@LaunchedEffect }
        if (areaLivro.width == 0) return@LaunchedEffect
        // de onde continuar: posição atual da lista/página, ou a salva ao abrir
        val ancora = when {
            paginas != null -> blocoAtual()
            blocoInicial >= 0 -> blocoInicial
            else -> (estado.firstVisibleItemIndex - 1).coerceAtLeast(-1)
        }
        val novas = withContext(Dispatchers.Default) {
            paginar(b, ::decorado, medidor, densidade, areaLivro, margem, tamanho, estiloLivro,
                estiloTituloLivro, destacarFalas, primeiroParagrafo)
        }
        paginas = novas
        val alvo = if (ancora < 0) 0 else novas.indexOfFirst { pg -> pg.itens.any { it is ItemPag.Trecho && it.bi == ancora } }.coerceAtLeast(0)
        paginador.scrollToPage(alvo.coerceIn(0, novas.size - 1))
    }
    var origemLivro by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val limitesTrechos = remember { HashMap<Pair<Int, Int>, androidx.compose.ui.geometry.Rect>() }

    val vidro = rememberVidro()
    Box(Modifier.fillMaxSize().background(p.fundo)) {
        Box(Modifier.fillMaxSize().background(p.fundo).fonteVidro(vidro)) {
        if (modoPaginas) Box(
            Modifier.fillMaxSize()
                .onGloballyPositioned { areaLivro = it.size; origemLivro = it.positionInRoot() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { pos ->
                            val raiz = pos + origemLivro
                            val bi = limitesTrechos.entries.firstOrNull { it.key.first == paginador.currentPage && it.value.contains(raiz) }?.key?.second ?: -1
                            if (bi in b.indices && b[bi] !is Bloco.Separador) blocoSel = bi
                        },
                        onTap = { pos ->
                            val w = size.width
                            val n = paginas?.size ?: 1
                            when {
                                pos.x < w * 0.25f -> escopo.launch { if (paginador.currentPage > 0) paginador.animateScrollToPage(paginador.currentPage - 1) }
                                pos.x > w * 0.75f -> escopo.launch { if (paginador.currentPage < n - 1) paginador.animateScrollToPage(paginador.currentPage + 1) }
                                else -> controles = !controles
                            }
                        },
                    )
                },
        ) {
            val pgs = paginas
            if (pgs == null) Text("◆", color = p.destaque, fontSize = 26.sp, modifier = Modifier.align(Alignment.Center))
            else HorizontalPager(paginador, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { it }) { pg ->
                val pagina = pgs[pg]
                Box(Modifier.fillMaxSize().graphicsLayer {
                    // efeito de folha: a página de baixo aparece levemente menor e mais escura
                    val off = ((paginador.currentPage - pg) + paginador.currentPageOffsetFraction).coerceIn(-1f, 1f)
                    val a = kotlin.math.abs(off)
                    scaleX = 1f - a * 0.06f; scaleY = 1f - a * 0.06f; alpha = 1f - a * 0.35f
                }.background(p.fundo)) {
                    if (pagina.itens.firstOrNull() == ItemPag.Fim) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = margem.dp, vertical = 20.dp)) {
                            FimCapitulo(b.isEmpty(), proximo, obra, idx + 1, lista.size, lista.count { it.progresso >= 0.98f || it.id == c.id }, ::irPara,
                                voltarObra = { nav.popBackStack() },
                                concluir = obra?.takeIf { it.status == StatusObra.LENDO }?.let { o -> { app.escopo.launch { repo.dao.obraAgora(o.id)?.let { atual -> repo.salvarObra(atual.copy(status = StatusObra.CONCLUIDA)) } } } }, social = social, atualFim = c)
                        }
                    } else Column(
                        Modifier.fillMaxSize().padding(start = margem.dp, end = margem.dp, top = TOPO_LIVRO.dp, bottom = BASE_LIVRO.dp),
                        verticalArrangement = Arrangement.spacedBy((tamanho * 0.85f).dp),
                    ) {
                        pagina.itens.forEach { item ->
                            when (item) {
                                ItemPag.Cabecalho -> CabecalhoPagina(c, obra, lista.take(idx + 1).count { Texto.tipoEspecial(it.titulo) == null }, p) { mostrarSobre = true }
                                is ItemPag.Trecho -> {
                                    val bl = b[item.bi]
                                    val alturaDp = with(densidade) { item.altura.toDp() }
                                    val mk = marcasPorBloco[item.bi]
                                    val fundo = Modifier.onGloballyPositioned { limitesTrechos[pg to item.bi] = it.boundsInRoot() }
                                        .then(if (item.bi == falando) Modifier.clip(forma(10.dp)).background(p.destaque.copy(alpha = 0.14f)) else Modifier)
                                        .then(if (mk != null && mk.cor.isNotEmpty()) Modifier.clip(forma(6.dp)).background((coresGrifo[mk.cor] ?: p.destaque).copy(alpha = 0.22f)) else Modifier)
                                    when (bl) {
                                        is Bloco.Titulo -> Box(Modifier.height(alturaDp).then(fundo)) { TituloNoTexto(bl.plano, p, Modifier) }
                                        is Bloco.Separador -> Box(Modifier.fillMaxWidth().height(alturaDp), contentAlignment = Alignment.Center) { SeparadorCena(enfeite, p.destaque) }
                                        is Bloco.Efeito -> Box(Modifier.fillMaxWidth().height(alturaDp).then(fundo), contentAlignment = Alignment.Center) { FxBloco(bl.uso, estiloLivro) }
                                        is Bloco.Paragrafo -> {
                                            val inteiro = remember(item.bi, regraNomes, destacarNomes, tamanho) { decorado(item.bi) }
                                            val comBusca = if (q.length >= 2) destacarBusca(inteiro, normalizados.value[item.bi], q, p, achados.getOrNull(achadoAtual)?.takeIf { it.first == item.bi }?.second) else inteiro
                                            val trecho0 = comBusca.subSequence(item.ini.coerceAtMost(comBusca.length), item.fim.coerceAtMost(comBusca.length))
                                            val trecho = trecho0
                                            if (bl.fala && destacarFalas) Fala(trecho, estiloLivro, p, fundo)
                                            else Text(trecho, style = estiloLivro.copy(color = corNarracao),
                                                modifier = fundo.then(if (item.bi == primeiroParagrafo && item.ini == 0) Modifier.padding(top = (tamanho * 0.5f).dp) else Modifier))
                                        }
                                    }
                                }
                                ItemPag.Fim -> {}
                            }
                        }
                    }
                    Text("${pg + 1} / ${pgs.size}", fontSize = 11.sp, color = p.textoSuave.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
                }
            }
        }
        else LazyColumn(
            state = estado,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = alfaEntrada; translationY = (1f - alfaEntrada) * 40f }.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { pos ->
                        val item = estado.layoutInfo.visibleItemsInfo.firstOrNull { pos.y >= it.offset && pos.y < it.offset + it.size }
                        val bi = (item?.index ?: 0) - 1
                        if (bi in b.indices && b[bi] !is Bloco.Separador) { rolando = false; blocoSel = bi }
                    },
                    onTap = { pos ->
                        val w = size.width
                        if (rolando) rolando = false
                        else controles = !controles
                    },
                )
            },
            contentPadding = PaddingValues(start = margem.dp, end = margem.dp, top = 0.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy((tamanho * 0.85f).dp),
        ) {
            item(key = "cab") {
                CabecalhoCapitulo(c, obra, anterior, lista.take(idx + 1).count { Texto.tipoEspecial(it.titulo) == null }, idx + 1, lista.size, margem, p) { mostrarSobre = true }
            }
            // "Anteriormente…": resumo do capítulo anterior ao começar um capítulo
            if (anterior != null && anterior.resumo.isNotBlank() && c.progresso < 0.05f) item(key = "anterior") {
                CartaoAnteriormente(anterior, p)
            }
            itemsIndexed(b, contentType = { _, x -> x::class }) { i, bloco ->
                val ativo = i == falando
                val mk = marcasPorBloco[i]
                val fundoAtivo = (if (ativo) Modifier.clip(forma(10.dp)).background(p.destaque.copy(alpha = 0.14f)) else Modifier)
                    .then(if (mk != null && mk.cor.isNotEmpty()) Modifier.clip(forma(6.dp)).background((coresGrifo[mk.cor] ?: p.destaque).copy(alpha = 0.22f)) else Modifier)
                    .then(if (i == parouAoAbrir - 1 && c.progresso < 0.98f && parouAoAbrir > 1) Modifier.drawBehind {
                        // fita ametista onde você parou (R3)
                        val x = size.width + 10.dp.toPx(); val w = 10.dp.toPx(); val h = 34.dp.toPx()
                        drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(x, -6.dp.toPx()); lineTo(x + w, -6.dp.toPx()); lineTo(x + w, h); lineTo(x + w / 2, h - 7.dp.toPx()); lineTo(x, h); close() }, Color(0xFF9D7BFF))
                    } else Modifier)
                    .then(if (mk != null) Modifier.drawBehind {
                        val x = size.width + 6.dp.toPx()
                        drawRect(coresGrifo[mk.cor] ?: p.destaque, topLeft = androidx.compose.ui.geometry.Offset(x, 2f), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 18.dp.toPx()))
                    } else Modifier)
                when (bloco) {
                    is Bloco.Titulo -> TituloNoTexto(bloco.plano, p, fundoAtivo)
                    is Bloco.Separador -> Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) { SeparadorCena(enfeite, p.destaque) }
                    is Bloco.Efeito -> FxBloco(bloco.uso, estiloBase, fundoAtivo)
                    is Bloco.Paragrafo -> {
                        val base = if (q.length >= 2) destacarBusca(bloco.texto, normalizados.value[i], q, p, achados.getOrNull(achadoAtual)?.takeIf { it.first == i }?.second) else bloco.texto
                        val texto = remember(base, regraNomes, destacarNomes, i == primeiroParagrafo, tamanho) {
                            val t = comNomes(base)
                            if (i == primeiroParagrafo) capitular(t, p, tamanho) else t
                        }
                        val comEfeitos = texto
                        run {
                        Column {
                            if (bloco.fala && destacarFalas) Fala(comEfeitos, estiloBase, p, fundoAtivo)
                            else Text(comEfeitos, style = estiloBase.copy(color = corNarracao), modifier = fundoAtivo.then(if (i == primeiroParagrafo) Modifier.padding(top = (tamanho * 0.5f).dp) else Modifier))
                            val n = comsPorBloco[i] ?: 0
                            if (n > 0) Text("💬 $n", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.sobreDestaque,
                                modifier = Modifier.align(Alignment.End).padding(top = 4.dp).clip(RoundedCornerShape(10.dp)).background(p.destaque)
                                    .clickable { trechoComentar = i }.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        }
                    }
                }
            }
            item(key = "fim") {
                FimCapitulo(b.isEmpty(), proximo, obra, idx + 1, lista.size, lista.count { it.progresso >= 0.98f || it.id == c.id }, ::irPara,
                    voltarObra = { nav.popBackStack() },
                    concluir = obra?.takeIf { it.status == StatusObra.LENDO }?.let { o -> { app.escopo.launch { repo.dao.obraAgora(o.id)?.let { atual -> repo.salvarObra(atual.copy(status = StatusObra.CONCLUIDA)) } } } }, social = social, atualFim = c)
            }
        }

        }

        // brilho reduzido e luz quente (por cima do texto, sem bloquear toques)
        if (escurecer > 0.01f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = escurecer)))
        if (luzQuente > 0.01f) Box(Modifier.fillMaxSize().background(Color(0xFFFF8A2A).copy(alpha = luzQuente * 0.28f)))

        // avisos rápidos (ex.: nome que não abre mais a ficha)
        LaunchedEffect(aviso) { if (aviso != null) { kotlinx.coroutines.delay(3500); aviso = null } }
        AvisoDeslizante(aviso, Modifier.align(Alignment.TopCenter)) { aviso = null }
        // lua do capítulo: enche conforme a leitura (K3)
        androidx.compose.foundation.Canvas(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 12.dp, end = 14.dp).size(18.dp)) {
            val r = size.minDimension / 2
            drawCircle(Color(0xFF1C1530), r)
            clipRect(right = size.width * progresso.coerceIn(0f, 1f)) { drawCircle(Color(0xFFEFE8FF), r) }
            drawCircle(Color(0x669D7BFF), r + 3f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        }

        traduzindo?.let { msg ->
            Row(Modifier.align(Alignment.Center).clip(forma(18.dp)).background(p.superficie).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), color = p.destaque, strokeWidth = 2.dp)
                Text(msg, fontWeight = FontWeight.SemiBold)
            }
        }
        if (escolherIdioma) AlertDialog(onDismissRequest = { escolherIdioma = false }, confirmButton = { TextButton(onClick = { escolherIdioma = false }) { Text("Fechar") } },
            title = { Text("Ler em outro idioma") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Traduz no próprio celular, grátis. Na primeira vez baixa o idioma (~30 MB). Serve também para ler em português uma obra importada em inglês ou coreano.",
                        fontSize = 12.sp, color = p.textoSuave)
                    (listOf("" to "Original (sem traduzir)") + com.huryel.novely.data.Tradutor.IDIOMAS).forEach { (cod, nome) ->
                        Text(nome, fontWeight = if (cod == idiomaLeitura) FontWeight.ExtraBold else FontWeight.Normal, color = if (cod == idiomaLeitura) p.destaque else p.texto,
                            modifier = Modifier.fillMaxWidth().clickable { escolherIdioma = false; prefs.idiomaLeitura.definir(cod) }.padding(vertical = 11.dp))
                    }
                }
            })
        val semMovimento = reduzirAnimacoes()
        AnimatedVisibility(controles, Modifier.align(Alignment.TopCenter), enter = if (semMovimento) fadeIn() else slideInVertically { -it } + fadeIn(), exit = if (semMovimento) fadeOut() else slideOutVertically { -it } + fadeOut()) {
            // HUD superior: barra preta sólida (o texto não aparece por baixo), título em serifa e progresso numa linha fina
            Column(Modifier.fillMaxWidth().background(Color.Black).pointerInput(Unit) { detectTapGestures { } }) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(start = 4.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotaoHud(IconesHud.voltar, "Voltar", 22.dp) { nav.popBackStack() }
                    Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                        Text(c.titulo, fontFamily = com.huryel.novely.ui.theme.InstrumentSerif, fontSize = 20.sp, lineHeight = 22.sp, color = Color(0xFFEDE6DA), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val restam = ((1f - progresso) * c.palavras / 230f).toInt()
                        Text(("cap. ${idx + 1} de ${lista.size}" + if (restam >= 1 && progresso < 0.99f) " · faltam ~$restam min" else "").uppercase(),
                            fontFamily = com.huryel.novely.ui.theme.PlexMono, fontSize = 9.5.sp, letterSpacing = 1.4.sp, lineHeight = 12.sp, color = Color.White.copy(alpha = .5f), maxLines = 1)
                    }
                    if (idiomaLeitura.isEmpty()) BotaoHud(IconesHud.web, "Traduzir capítulo") { escolherIdioma = true }
                    else Box(Modifier.size(32.dp).clip(CircleShape).clickable { escolherIdioma = true }.semantics { contentDescription = "Idioma da leitura: $idiomaLeitura" }, contentAlignment = Alignment.Center) {
                        Text(idiomaLeitura.uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = p.destaque) }
                    BotaoHud(IconesHud.info, "Sobre a obra") { mostrarSobre = true }
                    if (obra?.minha == true) BotaoHud(IconesHud.editar, "Editar capítulo") { nav.navigate("editor/${c.id}") }
                }
                // progresso do capítulo: linha fina com a porcentagem
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BarraComMapa(progresso, marcasMapa, p.destaque, Modifier.weight(1f)) { bi -> escopo.launch { irParaBloco(bi) } }
                    Text("${(progresso * 100).toInt()}%", fontFamily = com.huryel.novely.ui.theme.PlexMono, fontSize = 10.sp, color = Color.White.copy(alpha = .6f),
                        modifier = Modifier.padding(start = 10.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
            }
        }

        AnimatedVisibility(avisoRetomar, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 86.dp),
            enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut()) {
            Row(
                Modifier.shadow(10.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(p.superficie)
                    .border(1.dp, p.destaque.copy(alpha = 0.45f), RoundedCornerShape(50)).padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("↺  Continuando de onde parou · ${(c.progresso * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { avisoRetomar = false; escopo.launch { if (modoPaginas) paginador.animateScrollToPage(0) else estado.animateScrollToItem(0) } }) { Text("Do início", color = p.destaque, fontSize = 13.sp) }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            AnimatedVisibility(controles && painel == Painel.VOZ, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                PainelVoz(vozPronta, tocando, velocidade, { if (tocando) pausar() else tocar() },
                    { val ant = falaveis.lastOrNull { it < falando }; if (ant != null) com.huryel.novely.data.Narrador.irPara(ant) },
                    { val px = falaveis.firstOrNull { it > falando }; if (px != null) com.huryel.novely.data.Narrador.irPara(px) },
                    { prefs.velocidadeVoz.definir(it) }, narrador, { prefs.narrador.definir(it) },
                    nar.timerFim, { min -> com.huryel.novely.data.Narrador.timer(min) },
                    vozesPersonagens, { prefs.vozesPersonagens.definir(it) }, c.obraId, { prepararVoz() },
                    daTela = if (falando >= 0 && kotlin.math.abs(blocoAtual() - falando) > 2) ({ tocarDe(blocoAtual()) }) else null,
                    quem = com.huryel.novely.data.Narrador.falante(falando))
            }
            AnimatedVisibility(controles && painel == Painel.BUSCA, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                PainelBusca(consulta, { consulta = it; achadoAtual = 0 }, achados.size, achadoAtual,
                    { if (achados.isNotEmpty()) achadoAtual = (achadoAtual - 1 + achados.size) % achados.size },
                    { if (achados.isNotEmpty()) achadoAtual = (achadoAtual + 1) % achados.size },
                    { consulta = ""; painel = Painel.NENHUM })
            }
            AnimatedVisibility(controles, enter = if (semMovimento) fadeIn() else slideInVertically { it } + fadeIn(), exit = if (semMovimento) fadeOut() else slideOutVertically { it } + fadeOut()) {
                // HUD inferior: barra preta sólida de ponta a ponta, rótulos em mono
                Column(Modifier.fillMaxWidth().background(Color.Black).pointerInput(Unit) { detectTapGestures { } }) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().height(66.dp).padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                        val ocultosHud by prefs.ocultosHud.estado.collectAsState()
                        val esconde = remember(ocultosHud) { ocultosHud.split(',').toSet() }
                        if ("OUVIR" !in esconde) BotaoBarra("Ouvir", painel == Painel.VOZ || tocando, IconesHud.ouvir) { painel = if (painel == Painel.VOZ) Painel.NENHUM else Painel.VOZ }
                        if ("BUSCAR" !in esconde) BotaoBarra("Buscar", painel == Painel.BUSCA, IconesHud.buscar) { painel = if (painel == Painel.BUSCA) Painel.NENHUM else Painel.BUSCA }
                        if ("CAPITULOS" !in esconde) BotaoBarra("Capítulos", false, IconesHud.capitulos) { mostrarCapitulos = true }
                        if (!modoPaginas && "ROLAR" !in esconde) BotaoBarra("Rolar", rolando, IconesHud.rolar) { rolando = !rolando; if (rolando) { controles = false; painel = Painel.NENHUM } }
                        BotaoBarra("Leitura", false, IconesHud.leitura) { mostrarAjustes = true }
                    }
                }
            }
        }
        // ficha do personagem: cartão de vidro por cima do texto
        fichaVer?.let { f ->
            androidx.activity.compose.BackHandler { fichaVer = null }
            Box(Modifier.fillMaxSize().background(Color(0x40000000)).pointerInput(Unit) { detectTapGestures { fichaVer = null } }) {
                Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(10.dp).fillMaxWidth()
                    .vidroLiquido(vidro, 32.dp, tinta = Color(0xE610131A)).pointerInput(Unit) { detectTapGestures { } }) {
                    Column {
                        VerFicha(f, c.ordem, idx + 1)
                        // o autor corrige o reconhecimento: esta palavra não é este personagem (e pode desfazer)
                        val termo = termoVer
                        val ehObraMinha = obra?.publicaId.isNullOrEmpty()
                        if (ehObraMinha && termo.isNotBlank() && !termo.equals(f.nome.trim(), true)) {
                            TextButton(onClick = { prefs.ignorarNome(f.id, termo); fichaVer = null; aviso = "“$termo” não vai mais abrir ${f.nome.trim()}" },
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)) { Text("“$termo” aqui não é ${f.nome.trim()}", color = Color(0xFFFF9D9D)) }
                        }
                        val meusIgnorados = ignorados.filter { it.startsWith("${f.id}|") }.map { it.substringAfter('|') }
                        if (ehObraMinha && meusIgnorados.isNotEmpty()) Row(Modifier.padding(start = 20.dp, end = 12.dp, bottom = 12.dp).horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Não reconhece:", color = Color.White.copy(alpha = .6f), fontSize = 12.sp)
                            meusIgnorados.forEach { t -> TextButton(onClick = { prefs.voltarNome(f.id, t) }) { Text("$t ✕", fontSize = 12.sp, color = Color.White) } }
                        }
                    }
                }
            }
        }
        if (contagem > 0) Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 110.dp).vidroLiquido(vidro, 26.dp, tinta = Color(0xCC10131A))
            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Próximo capítulo em $contagem…", fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = { contagem = -3 }) { Text("Cancelar", color = p.destaque) }
        }
        if (!controles) Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(12.dp)) {
            PorcentagemLeitura { progresso }
        }
    }

    if (mostrarSobre) ModalBottomSheet(onDismissRequest = { mostrarSobre = false }, containerColor = p.superficie) {
        obra?.let { o -> SobreObra(o, lista, c, p) { mostrarSobre = false; mostrarCapitulos = true } }
    }
    if (blocoSel >= 0 && blocoSel in b.indices) ModalBottomSheet(onDismissRequest = { blocoSel = -1 }, containerColor = p.superficie) {
        FolhaParagrafo(b[blocoSel].plano, marcasPorBloco[blocoSel], c.obraId, c.id, blocoSel, obra?.titulo ?: "", c.titulo, fichas,
            fechar = { blocoSel = -1 }, abrirFicha = { blocoSel = -1; fichaVer = it },
            comentar = if (social != null) ({ val bi = blocoSel; blocoSel = -1; trechoComentar = bi }) else null,
            corObra = obra?.corBase ?: 0, capa = obra?.capa,
            ouvirDaqui = { val bi = blocoSel; tocarDe(bi) })
    }
    if (trechoComentar >= 0 && social != null && trechoComentar in b.indices) ModalBottomSheet(onDismissRequest = { trechoComentar = -1; recarregarComs++ }, containerColor = p.superficie) {
        FolhaComentarios(social.first, social.second, trechoComentar, b[trechoComentar].plano) { }
    }
    // (ficha agora abre como vidro dentro da tela, acima)
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

/** Abertura do capítulo: banner sangrando, capa no centro, número, título, tempo, progresso da obra e resumos. */
@Composable
private fun CabecalhoCapitulo(c: Capitulo, obra: com.huryel.novely.data.Obra?, anterior: Capitulo?, numero: Int, posicao: Int, total: Int, margem: Float, p: Paleta, abrirSobre: () -> Unit) {
    var verResumo by remember { mutableStateOf(true) }
    var verAnterior by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // abertura V2: número romano, título em serifa, linha laranja (U9)
        AberturaV2(c, obra, numero, posicao, total, abrirSobre)
        if (anterior != null && anterior.resumo.isNotBlank()) {
            CaixaResumo("ANTERIORMENTE", anterior.resumo, verAnterior, p) { verAnterior = !verAnterior }
        }
        if (c.resumo.isNotBlank()) {
            CaixaResumo("NESTE CAPÍTULO", c.resumo, verResumo, p) { verResumo = !verResumo }
        }
        Spacer(Modifier.height(6.dp))
    }
}

/** Primeira letra grande em âmbar e primeiras palavras em negrito, como abertura de livro. */
private fun capitular(t: AnnotatedString, p: Paleta, tamanho: Float): AnnotatedString {
    val txt = t.text
    val ini = txt.indexOfFirst { it.isLetterOrDigit() }
    if (ini < 0) return t
    var fimPalavras = ini; var palavras = 0
    while (fimPalavras < txt.length && palavras < 4) { if (txt[fimPalavras] == ' ') palavras++; fimPalavras++ }
    return buildAnnotatedString {
        append(t)
        addStyle(SpanStyle(fontSize = (tamanho * 2.6f).sp, color = Ed.laranja, fontFamily = com.huryel.novely.ui.theme.InstrumentSerif, fontWeight = FontWeight.Normal), ini, ini + 1)
        if (fimPalavras > ini + 1) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, color = p.texto), ini + 1, fimPalavras.coerceAtMost(txt.length))
    }
}

@Composable
private fun Etiqueta(t: String, p: Paleta, destaque: Boolean = false) {
    Text(t, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (destaque) p.sobreDestaque else p.textoSuave,
        modifier = Modifier.clip(forma(10.dp)).background(if (destaque) p.destaque else p.superficie).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
private fun CaixaResumo(rotulo: String, texto: String, aberto: Boolean, p: Paleta, alternar: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(forma(16.dp)).background(p.superficie).clickable(onClick = alternar)
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
            var verCapa2 by remember { mutableStateOf(false) }
            if (verCapa2) VisorCapa(o) { verCapa2 = false }
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(96.dp).height(140.dp).clickable { verCapa2 = true }, 12.sp)
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
            Box(Modifier.width(40.dp).height(3.dp).clip(forma(2.dp)).background(p.destaque))
        }
    }
}

@Composable
private fun Fala(texto: AnnotatedString, estilo: TextStyle, p: Paleta, extra: Modifier) {
    // travessões/aspas da fala na cor de destaque; o "invólucro" muda conforme o tema
    val colorido = remember(texto, p) {
        buildAnnotatedString {
            append(texto)
            val t = texto.text
            var fim = 0
            while (fim < t.length && (t[fim] in "—–―-“\"«" || t[fim] == ' ')) fim++
            val cor = if (p.fala == com.huryel.novely.ui.theme.EstiloFala.BALAO) p.destaque3 else p.destaque
            if (p.fala != com.huryel.novely.ui.theme.EstiloFala.FIO) {
                if (fim > 0) addStyle(SpanStyle(color = cor, fontWeight = FontWeight.ExtraBold), 0, fim)
                var k = fim
                while (k < t.length) { if (t[k] == '—') addStyle(SpanStyle(color = cor), k, k + 1); k++ }
            }
        }
    }
    when (p.fala) {
        com.huryel.novely.ui.theme.EstiloFala.FIO -> Row(extra.height(IntrinsicSize.Min)) {
            Box(Modifier.width(2.dp).fillMaxHeight().padding(vertical = 3.dp).background(p.destaque))
            Spacer(Modifier.width(12.dp))
            Text(colorido, style = estilo.copy(color = p.texto))
        }
        com.huryel.novely.ui.theme.EstiloFala.BALAO -> Text(colorido, style = estilo.copy(color = p.texto),
            modifier = extra.fillMaxWidth().clip(forma(18.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(p.destaque2.copy(alpha = 0.20f), p.destaque3.copy(alpha = 0.12f))))
                .border(1.dp, p.destaque2.copy(alpha = 0.4f), forma(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp))
        com.huryel.novely.ui.theme.EstiloFala.ETIQUETA -> Row(extra) {
            Text("FALA", fontFamily = p.fonteUi, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = p.sobreDestaque,
                modifier = Modifier.padding(top = 6.dp).background(p.destaque).padding(horizontal = 5.dp, vertical = 1.dp))
            Spacer(Modifier.width(10.dp))
            Text(colorido, style = estilo.copy(color = p.texto))
        }
        com.huryel.novely.ui.theme.EstiloFala.ITALICO -> Text(texto, style = estilo.copy(color = p.texto, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), modifier = extra)
        else -> Row(
            extra.height(IntrinsicSize.Min).clip(forma(topEnd = 12.dp, bottomEnd = 12.dp)).background(p.destaque.copy(alpha = 0.07f)),
        ) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(p.destaque.copy(alpha = 0.9f)))
            Spacer(Modifier.width(12.dp))
            Text(colorido, style = estilo.copy(color = p.texto, fontWeight = FontWeight.Medium), modifier = Modifier.padding(vertical = 6.dp).padding(end = 10.dp))
        }
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
private fun FimCapitulo(vazio: Boolean, proximo: Capitulo?, obra: com.huryel.novely.data.Obra?, posicao: Int, total: Int, lidos: Int,
                        ir: (Capitulo) -> Unit, voltarObra: () -> Unit, concluir: (() -> Unit)?, social: Pair<String, String>? = null, atualFim: Capitulo? = null) {
    val deOutro = obra != null && !obra.minha
    FimV2(vazio, atualFim ?: return, posicao, proximo, if (proximo != null) posicao + 1 else 0, social, deOutro || obra?.publicada == true, ir, voltarObra, concluir)
}

@Composable
private fun RowScope.BotaoBarra(rotulo: String, ativo: Boolean, icone: androidx.compose.ui.graphics.vector.ImageVector, aoTocar: () -> Unit) {
    val p = LocalPaleta.current
    val toque = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val apertado by toque.collectIsPressedAsState()
    val escala by animateFloatAsState(if (apertado && !reduzirAnimacoes()) 0.92f else 1f, label = "aperto")
    val cor = if (ativo) p.destaque else Color.White.copy(alpha = 0.6f)
    Column(
        Modifier.weight(1f).fillMaxHeight().graphicsLayer { scaleX = escala; scaleY = escala }
            .clickable(toque, null, role = androidx.compose.ui.semantics.Role.Button, onClick = aoTocar)
            .semantics { selected = ativo },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(4.dp).clip(CircleShape).background(if (ativo) p.destaque else Color.Transparent))
        Icon(icone, null, tint = cor, modifier = Modifier.padding(top = 5.dp).size(20.dp))
        Text(rotulo.uppercase(), fontFamily = com.huryel.novely.ui.theme.PlexMono, fontSize = 9.sp, letterSpacing = 1.2.sp, color = cor, maxLines = 1, modifier = Modifier.padding(top = 5.dp))
    }
}

/** Botão redondo de ícone do HUD superior (32 dp). */
@Composable
private fun BotaoHud(icone: androidx.compose.ui.graphics.vector.ImageVector, descricao: String, tam: androidx.compose.ui.unit.Dp = 18.dp, aoTocar: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClickLabel = descricao, onClick = aoTocar), contentAlignment = Alignment.Center) {
        Icon(icone, descricao, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(tam))
    }
}

@Composable
private fun PainelVoz(pronta: Boolean, tocando: Boolean, velocidade: Float, alternar: () -> Unit, voltar: () -> Unit, avancar: () -> Unit, definirVel: (Float) -> Unit,
                      narrador: String, definirNarrador: (String) -> Unit, timerFim: Long, definirTimer: (Int) -> Unit,
                      vozesPersonagens: Boolean, definirVozesPersonagens: (Boolean) -> Unit, obraId: Long, preparar: suspend () -> Unit,
                      daTela: (() -> Unit)? = null, quem: String = "Narrador") {
    val p = LocalPaleta.current
    var escolhendo by remember { mutableStateOf(false) }
    var elenco by remember { mutableStateOf(false) }
    val escopoV = rememberCoroutineScope()
    var agora by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timerFim) { while (timerFim > 0) { agora = System.currentTimeMillis(); kotlinx.coroutines.delay(15_000) } }
    Column(
        Modifier.padding(12.dp).fillMaxWidth().background(Color.Black)
            .border(1.dp, Ed.linha).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FalandoAgora(if (tocando) quem else "Ouvir", tocando)
        Text(com.huryel.novely.data.Narrador.erro ?: if (tocando) "Continua com a tela apagada" else "Ouvir a partir daqui", fontFamily = com.huryel.novely.ui.theme.PlexMono, fontSize = 10.sp, letterSpacing = 1.2.sp, color = Ed.suave)
        // a voz está num lugar e a tela em outro: ouvir o que está na tela
        if (daTela != null) Text("▶  Ouvir a partir do que está na tela", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0B0B0E),
            modifier = Modifier.fillMaxWidth().clip(forma(14.dp)).background(Color.White).clickable(onClick = daTela).padding(horizontal = 14.dp, vertical = 10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { definirVel(when { velocidade < 0.95f -> 1.0f; velocidade < 1.15f -> 1.25f; velocidade < 1.4f -> 1.5f; velocidade < 1.9f -> 2.0f; else -> 0.8f }) }) {
                Text("${"%.2f".format(velocidade).trimEnd('0').trimEnd(',', '.')}×", fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = voltar) { Icon(Icons.Filled.KeyboardArrowUp, "Parágrafo anterior") }
            FilledIconButton(onClick = alternar, enabled = pronta, modifier = Modifier.size(58.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = p.destaque, contentColor = p.sobreDestaque)) {
                if (tocando) Text("❚❚", fontWeight = FontWeight.Bold) else Icon(Icons.Filled.PlayArrow, "Tocar")
            }
            IconButton(onClick = avancar) { Icon(Icons.Filled.KeyboardArrowDown, "Próximo parágrafo") }
            Box {
                TextButton(onClick = { escolhendo = true }, enabled = pronta) { Text("Narrador", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                DropdownMenu(escolhendo, { escolhendo = false }, modifier = Modifier.heightIn(max = 380.dp)) {
                    val lista = remember(pronta) { com.huryel.novely.data.Narrador.vozesDisponiveis() }
                    com.huryel.novely.data.VozNeural.VOZES.filter { com.huryel.novely.data.VozNeural.instalada(it) }.forEach { v ->
                        DropdownMenuItem(text = { Text("✦ " + v.nome + " (Novely)", fontWeight = if (v.id == narrador) FontWeight.Bold else FontWeight.Normal, color = if (v.id == narrador) p.destaque else p.texto) },
                            onClick = { escolhendo = false; definirNarrador(v.id) })
                    }
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("⏾ Timer:", fontSize = 12.sp, color = p.textoSuave)
            listOf(0 to "off", 15 to "15 min", 30 to "30", 60 to "60").forEach { (m, t) ->
                val ativo = if (m == 0) timerFim == 0L else timerFim > 0 && ((timerFim - agora) / 60_000 + 1) in (m - 14)..m
                Text(t, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (ativo) p.sobreDestaque else p.texto,
                    modifier = Modifier.clip(forma(10.dp)).background(if (ativo) p.destaque else p.borda).clickable { definirTimer(m) }.padding(horizontal = 10.dp, vertical = 6.dp))
            }
            if (timerFim > 0) Text("faltam ${((timerFim - agora) / 60_000 + 1)} min", fontSize = 11.sp, color = p.textoSuave)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Uma voz para cada personagem", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("O app descobre quem fala pelo “disse Fulano”", fontSize = 11.sp, color = p.textoSuave)
            }
            Switch(vozesPersonagens, definirVozesPersonagens)
        }
        var vozesNovely by remember { mutableStateOf(false) }
        Text("✦ Vozes do Novely · mais humanas, sem internet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B0B0E),
            modifier = Modifier.fillMaxWidth().clip(forma(14.dp)).background(Color.White).clickable { vozesNovely = true }.padding(horizontal = 14.dp, vertical = 11.dp))
        if (vozesPersonagens) TextButton(onClick = { escopoV.launch { preparar(); elenco = true } }, enabled = pronta) { Text("🎭 Elenco: escolher as vozes", color = p.destaque, fontWeight = FontWeight.Bold) }
        if (vozesNovely) DialogoVozesNovely(narrador, velocidade, { definirNarrador(it); vozesNovely = false }) { vozesNovely = false }
    }
    if (elenco) DialogoElenco(obraId) { elenco = false }
}

/** Elenco: a voz e o tom de cada personagem que fala no capítulo, com amostra. */
@Composable
private fun DialogoElenco(obraId: Long, fechar: () -> Unit) {
    val p = LocalPaleta.current
    val nar = com.huryel.novely.data.Narrador
    val vozes = remember { nar.vozesDisponiveis() }
    var versao by remember { mutableIntStateOf(0) }
    AlertDialog(onDismissRequest = fechar, confirmButton = { TextButton(onClick = fechar) { Text("Pronto") } },
        title = { Text("Elenco") },
        text = {
            if (nar.nomesElenco.isEmpty()) Text("Ninguém fala neste capítulo com nome reconhecido. Use “— … — disse Fulano” e crie a ficha do personagem.", color = p.textoSuave)
            else androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(nar.nomesElenco.entries.toList(), key = { it.key }) { (id, nome) ->
                    key(versao) {
                        val v = nar.vozDe(id) ?: com.huryel.novely.data.VozPersonagem("", 1f)
                        var abrir by remember { mutableStateOf(false) }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(nome, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                TextButton(onClick = { nar.amostra(v, "Eu sou $nome. É assim que eu falo.") }) { Text("▶ Ouvir") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    val i = vozes.indexOfFirst { it.name == v.voz }
                                    val g = vozes.getOrNull(i)?.let { nar.generoDaVoz(it.name) }
                                    val nv0 = com.huryel.novely.data.VozNeural.voz(v.voz)
                                    Text(if (nv0 != null) "✦ ${nv0.nome}" else (if (g == 'F') "♀ " else if (g == 'M') "♂ " else "") + if (i >= 0) "Voz ${i + 1}" else "Narrador", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clip(forma(10.dp)).background(p.borda).clickable { abrir = true }.padding(horizontal = 10.dp, vertical = 6.dp))
                                    DropdownMenu(abrir, { abrir = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                                        com.huryel.novely.data.VozNeural.VOZES.filter { com.huryel.novely.data.VozNeural.instalada(it) }.forEach { n ->
                                            DropdownMenuItem(text = { Text("✦ ${n.nome} · ${n.descricao}") },
                                                onClick = { abrir = false; val nv = v.copy(voz = n.id, tom = 1f); nar.mudarVoz(obraId, id, nv); versao++; nar.amostra(nv, "Assim?") })
                                        }
                                        vozes.forEachIndexed { k, vz ->
                                            val gg = nar.generoDaVoz(vz.name)
                                            DropdownMenuItem(text = { Text((if (gg == 'F') "♀ Mulher" else if (gg == 'M') "♂ Homem" else "◦") + " · voz ${k + 1}") },
                                                onClick = { abrir = false; val nv = v.copy(voz = vz.name); nar.mudarVoz(obraId, id, nv); versao++; nar.amostra(nv, "Assim?") })
                                        }
                                    }
                                }
                                listOf(0.82f to "grave", 1f to "normal", 1.18f to "agudo").forEach { (t, rot) ->
                                    val ativo = kotlin.math.abs(v.tom - t) < 0.09f
                                    Text(rot, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (ativo) p.sobreDestaque else p.texto,
                                        modifier = Modifier.clip(forma(10.dp)).background(if (ativo) p.destaque else p.borda)
                                            .clickable { val nv = v.copy(tom = t); nar.mudarVoz(obraId, id, nv); versao++; nar.amostra(nv, "Assim?") }.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                }
            }
        })
}

@Composable
private fun PainelBusca(consulta: String, mudar: (String) -> Unit, total: Int, atual: Int, anterior: () -> Unit, proximo: () -> Unit, fechar: () -> Unit) {
    val p = LocalPaleta.current
    Row(
        Modifier.padding(12.dp).fillMaxWidth().clip(forma(18.dp)).background(p.superficie)
            .border(1.dp, p.destaque.copy(alpha = 0.35f), forma(18.dp)).padding(start = 12.dp, end = 4.dp),
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Padrão", fonte == "TEMA") { prefs.fonte.definir("TEMA") }
            Chip("Com serifa", fonte == "SERIFA") { prefs.fonte.definir("SERIFA") }
            Chip("Sem serifa", fonte == "SEM_SERIFA") { prefs.fonte.definir("SEM_SERIFA") }
        }
        LinhaSlider("Tamanho", "${tamanho.toInt()}", tamanho, 14f..30f, cores) { prefs.tamanhoP.definir(it) }
        LinhaSlider("Entrelinhas", "%.1f".format(entrelinhas), entrelinhas, 1.3f..2.2f, cores) { prefs.entrelinhas.definir(it) }
        LinhaSlider("Margens", "${margem.toInt()}", margem, 8f..48f, cores) { prefs.margem.definir(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Rolagem contínua", !paginas) { prefs.modoPaginas.definir(false) }
            Chip("Páginas (livro)", paginas) { prefs.modoPaginas.definir(true) }
        }
        if (paginas) Text("Vire a página deslizando para o lado ou tocando nas bordas. Toque no meio para mostrar os botões.", fontSize = 12.sp, color = p.textoSuave)
        // botões da barra de baixo (Leitura sempre fica: é por ela que se volta aqui)
        val ocultos by prefs.ocultosHud.estado.collectAsState()
        Text("BOTÕES DA BARRA", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("OUVIR" to "Ouvir", "BUSCAR" to "Buscar", "CAPITULOS" to "Capítulos", "ROLAR" to "Rolar").forEach { (k, n) ->
                val on = k !in ocultos.split(',')
                Chip((if (on) "✓ " else "") + n, on) {
                    val l = ocultos.split(',').filter { it.isNotBlank() }.toMutableSet(); if (on) l.add(k) else l.remove(k); prefs.ocultosHud.definir(l.joinToString(","))
                }
            }
        }
        LinhaInterruptor("Destacar falas de personagens", falas) { prefs.destacarFalas.definir(it) }
        val nomes by prefs.destacarNomes.estado.collectAsState()
        LinhaInterruptor("Destacar nomes das fichas (toque abre)", nomes) { prefs.destacarNomes.definir(it) }
        LinhaInterruptor("Manter tela ligada lendo", ligada) { prefs.telaLigada.definir(it) }
        val noite by prefs.noiteProfunda.estado.collectAsState()
        val capit by prefs.capitular.estado.collectAsState()
        val marat by prefs.maratona.estado.collectAsState()
        val corObra by prefs.corDaObra.estado.collectAsState()
        LinhaInterruptor("Noite profunda (âmbar fraco, sem luz azul)", noite) { prefs.noiteProfunda.definir(it) }
        LinhaInterruptor("Cores da capa da obra", corObra) { prefs.corDaObra.definir(it) }
        LinhaInterruptor("Primeira letra grande (capitular)", capit) { prefs.capitular.definir(it) }
        LinhaInterruptor("Maratona: próximo capítulo começa sozinho", marat) { prefs.maratona.definir(it) }
        val esc by prefs.escurecer.estado.collectAsState()
        val quente by prefs.luzQuente.estado.collectAsState()
        val vel by prefs.velocidadeRolagem.estado.collectAsState()
        LinhaSlider("Escurecer", "${(esc * 100).toInt()}%", esc, 0f..0.8f, cores) { prefs.escurecer.definir(it) }
        LinhaSlider("Luz quente", "${(quente * 100).toInt()}%", quente, 0f..1f, cores) { prefs.luzQuente.definir(it) }
        LinhaSlider("Rolagem auto", "%.1f".format(vel), vel, 0.3f..4f, cores) { prefs.velocidadeRolagem.definir(it) }
    }
}

@Composable
private fun LinhaSlider(rotulo: String, valorTxt: String, v: Float, faixa: ClosedFloatingPointRange<Float>, cores: SliderColors, mudar: (Float) -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rotulo, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.texto)
            Text(valorTxt, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.destaque)
        }
        Slider(v, mudar, valueRange = faixa, colors = cores, modifier = Modifier.fillMaxWidth().height(32.dp))
    }
}

@Composable
fun LinhaInterruptor(rotulo: String, valor: Boolean, mudar: (Boolean) -> Unit) {
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { mudar(!valor) }, verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.texto)
        Switch(valor, mudar, colors = SwitchDefaults.colors(checkedThumbColor = p.sobreDestaque, checkedTrackColor = p.destaque,
            uncheckedThumbColor = p.textoSuave, uncheckedTrackColor = p.superficie, uncheckedBorderColor = p.borda))
    }
}


// ───────── Modo livro: paginação ─────────

private const val TOPO_LIVRO = 26
private const val BASE_LIVRO = 34
private const val CAB_LIVRO = 196

/**
 * Corta o capítulo em páginas que cabem exatamente na tela: mede cada parágrafo com a mesma fonte,
 * largura e entrelinhas do desenho e divide parágrafos longos no fim de uma linha.
 */
private fun paginar(
    b: List<Bloco>, decorado: (Int) -> AnnotatedString, medidor: TextMeasurer, d: androidx.compose.ui.unit.Density,
    area: androidx.compose.ui.unit.IntSize, margem: Float, tamanho: Float, estilo: TextStyle, estiloTitulo: TextStyle,
    falas: Boolean, primeiro: Int,
): List<Pagina> = with(d) {
    val largura = (area.width - 2 * margem.dp.toPx()).toInt().coerceAtLeast(100)
    val altura = (area.height - (TOPO_LIVRO + BASE_LIVRO + 6).dp.toPx()).toInt().coerceAtLeast(200)
    val esp = (tamanho * 0.85f).dp.roundToPx()
    val paginas = ArrayList<Pagina>()
    var atual = ArrayList<ItemPag>()
    var y = 0
    fun fechar() { if (atual.isNotEmpty()) paginas.add(Pagina(atual)); atual = ArrayList(); y = 0 }
    fun antes() = if (atual.isEmpty()) 0 else esp
    fun colocar(item: ItemPag, h: Int) { y += antes() + h; atual.add(item) }
    fun inteiro(item: ItemPag, h: Int) { if (y + antes() + h > altura && atual.isNotEmpty()) fechar(); colocar(item, h) }

    colocar(ItemPag.Cabecalho, CAB_LIVRO.dp.roundToPx())
    b.forEachIndexed { i, bl ->
        when (bl) {
            is Bloco.Titulo -> {
                val especial = Texto.tipoEspecial(bl.plano) != null
                val lay = medidor.measure(bl.plano, estiloTitulo, constraints = Constraints(maxWidth = largura))
                inteiro(ItemPag.Trecho(i, 0, 0, lay.size.height + (if (especial) 110 else 44).dp.roundToPx()), lay.size.height + (if (especial) 110 else 44).dp.roundToPx())
            }
            is Bloco.Separador -> inteiro(ItemPag.Trecho(i, 0, 0, 40.dp.roundToPx()), 40.dp.roundToPx())
            is Bloco.Efeito -> inteiro(ItemPag.Trecho(i, 0, 0, 176.dp.roundToPx()), 176.dp.roundToPx())
            is Bloco.Paragrafo -> {
                val fala = bl.fala && falas
                val w = if (fala) largura - 26.dp.roundToPx() else largura
                val st = if (fala) estilo.copy(fontWeight = FontWeight.Medium) else estilo
                val lay = medidor.measure(decorado(i), st, constraints = Constraints(maxWidth = w.coerceAtLeast(60)))
                var l0 = 0
                while (l0 < lay.lineCount) {
                    val extra = (if (fala) 12.dp.roundToPx() else 0) + (if (i == primeiro && l0 == 0) (tamanho * 0.5f).dp.roundToPx() else 0)
                    val livre = altura - y - antes() - extra
                    val topo = lay.getLineTop(l0)
                    var l = l0 - 1
                    while (l + 1 < lay.lineCount && lay.getLineBottom(l + 1) - topo <= livre) l++
                    if (l < l0) {
                        if (atual.isNotEmpty()) { fechar(); continue }
                        l = l0 // nem uma linha cabe (fonte enorme): coloca assim mesmo
                    }
                    val h = kotlin.math.ceil(lay.getLineBottom(l) - topo).toInt() + extra
                    colocar(ItemPag.Trecho(i, lay.getLineStart(l0), lay.getLineEnd(l), h), h)
                    l0 = l + 1
                    if (l0 < lay.lineCount) fechar()
                }
            }
        }
    }
    fechar()
    paginas.add(Pagina(listOf(ItemPag.Fim)))
    paginas
}

/** Abertura compacta do capítulo na primeira página do modo livro. */
@Composable
private fun CabecalhoPagina(c: Capitulo, obra: com.huryel.novely.data.Obra?, numero: Int, p: Paleta, abrirSobre: () -> Unit) {
    val especial = Texto.tipoEspecial(c.titulo)
    Column(Modifier.fillMaxWidth().height(CAB_LIVRO.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (obra != null) Text(obra.titulo.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = p.textoSuave,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable(onClick = abrirSobre).padding(4.dp))
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp).height(1.dp).background(p.destaque.copy(alpha = 0.6f)))
            Text(if (especial != null) "  $especial  " else "  CAPÍTULO $numero  ", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp, color = p.destaque)
            Box(Modifier.width(28.dp).height(1.dp).background(p.destaque.copy(alpha = 0.6f)))
        }
        Spacer(Modifier.height(10.dp))
        val titulo = if (especial != null) c.titulo.substringAfter('—', c.titulo.substringAfter(':', "")).trim().ifEmpty { c.titulo } else c.titulo.ifEmpty { "Sem título" }
        Text(titulo, style = MaterialTheme.typography.displaySmall.copy(fontSize = 27.sp, lineHeight = 32.sp, fontStyle = if (especial != null) FontStyle.Italic else FontStyle.Normal),
            textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(12.dp))
        Text("◷ ${(c.palavras / 230).coerceAtLeast(1)} min · ${formatarPalavras(c.palavras.toLong())} palavras", fontSize = 12.sp, color = p.textoSuave)
        Spacer(Modifier.height(14.dp))
        Text("◆   ◆   ◆", color = p.destaque.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

/** Isolado: a porcentagem muda ao rolar sem recompor o leitor inteiro. */
@Composable
private fun PorcentagemLeitura(progresso: () -> Float) {
    val pct by remember { derivedStateOf { (progresso() * 100).toInt() } }
    Text("$pct%", fontSize = 11.sp, color = LocalPaleta.current.textoSuave.copy(alpha = 0.6f))
}

private val REACOES = listOf("chorei" to "😭", "fogo" to "🔥", "choque" to "😱", "risada" to "😂", "mente" to "🤯")

/** Reações rápidas do fim do capítulo (obra do catálogo): toque para reagir, toque de novo para tirar. */
@Composable
internal fun Reacoes(pid: String, cid: String) {
    val p = LocalPaleta.current
    val pub = NovelyApp.instancia.pub
    var contas by remember(cid) { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var minha by remember(cid) { mutableStateOf<String?>(null) }
    val escopo = rememberCoroutineScope()
    LaunchedEffect(cid) { try { val (c, m) = pub.reacoes(pid, cid); contas = c; minha = m } catch (_: Exception) {} }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.superficie).padding(14.dp)) {
        Text("O que achou?", fontSize = 12.sp, color = p.textoSuave, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            REACOES.forEach { (k, e) ->
                val eu = minha == k
                val escala by androidx.compose.animation.core.animateFloatAsState(if (eu) 1.15f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = 500f), label = "r")
                Column(Modifier.graphicsLayer { scaleX = escala; scaleY = escala }.clip(RoundedCornerShape(16.dp))
                    .background(if (eu) LARANJA_FX.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f))
                    .border(if (eu) 1.5.dp else 0.dp, if (eu) LARANJA_FX else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable {
                        val antiga = minha; val nova = if (eu) null else k
                        minha = nova
                        contas = contas.toMutableMap().apply { antiga?.let { this[it] = ((this[it] ?: 1) - 1).coerceAtLeast(0) }; nova?.let { this[it] = (this[it] ?: 0) + 1 } }
                        escopo.launch { try { pub.reagir(pid, cid, nova, antiga) } catch (_: Exception) {} }
                    }.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(e, fontSize = 26.sp)
                    Text("${contas[k] ?: 0}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.textoSuave)
                }
            }
        }
    }
}

private val MISSAO = Regex("^\\[([xX✓ ]?)\\]")
private val BARRA_STATUS = Regex("^([\\p{L}][\\p{L} ]{0,14}?)\\s*:?\\s*(\\d+)\\s*/\\s*(\\d+)$")

@Composable
private fun AvisoDeslizante(texto: String?, modifier: Modifier, fechar: () -> Unit) {
    var ultimo by remember { mutableStateOf("") }
    texto?.let { ultimo = it }
    androidx.compose.animation.AnimatedVisibility(texto != null, modifier.statusBarsPadding().padding(top = 70.dp, start = 20.dp, end = 20.dp),
        enter = slideInVertically(androidx.compose.animation.core.spring(0.55f, 300f)) { -it * 3 } + fadeIn(), exit = slideOutVertically { -it * 3 } + fadeOut()) {
        Text("🔔  $ultimo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDDF3FF), textAlign = TextAlign.Center,
            modifier = Modifier.shadow(20.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF12233A), Color(0xFF0B1626))))
                .border(1.dp, Color(0xFF5CC8FF), RoundedCornerShape(14.dp)).clickable(onClick = fechar).padding(horizontal = 18.dp, vertical = 12.dp))
    }
}

