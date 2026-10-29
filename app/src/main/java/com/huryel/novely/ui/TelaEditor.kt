package com.huryel.novely.ui

import com.huryel.novely.ui.theme.forma
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.ArquivosTexto
import com.huryel.novely.data.Capitulo
import com.huryel.novely.data.Texto
import com.huryel.novely.data.Marcas
import com.huryel.novely.data.TipoFicha
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Paleta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Editor para capítulos gigantes: o texto é dividido em pedaços de ~3 mil caracteres, cada um num
 * campo próprio dentro de uma lista preguiçosa. Só os pedaços visíveis existem na tela. Ao salvar,
 * os pedaços são unidos de volta — o texto resultante é idêntico ao original mais suas edições.
 */
private class Pedaco(val id: Int, valor: TextFieldValue) {
    var valor by mutableStateOf(valor)
    val foco = FocusRequester()
}

private data class Passo(val pedacoId: Int, val valor: TextFieldValue)
private data class Marca(val pedaco: Int, val pos: Int, val rotulo: String, val tipo: Int) // 0 título, 1 cena

private enum class PainelEd { NENHUM, BUSCA }

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun TelaEditor(capId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val prefs = app.prefs
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val fonte by prefs.fonte.estado.collectAsState()
    val meta by prefs.metaDiaria.estado.collectAsState()
    val hoje by prefs.palavrasHoje.estado.collectAsState()
    val sequencia by prefs.sequencia.estado.collectAsState()
    val snack = remember { SnackbarHostState() }

    var cap by remember { mutableStateOf<Capitulo?>(null) }
    val pedacos = remember { mutableStateListOf<Pedaco>() }
    var carregado by remember { mutableStateOf(false) }
    var versao by remember { mutableStateOf(0) }
    var versaoSalva by remember { mutableStateOf(0) }
    var palavras by remember { mutableStateOf(0) }
    var estadoSalvo by remember { mutableStateOf("Salvo") }
    var focado by remember { mutableStateOf(-1) }
    var proximoId by remember { mutableStateOf(0) }
    var ocupado by remember { mutableStateOf(false) } // dividindo/recarregando: ignora digitação

    var menu by remember { mutableStateOf(false) }
    var modoFoco by rememberSaveable { mutableStateOf(false) }
    var repeticoes by rememberSaveable { mutableStateOf(false) }
    var painel by remember { mutableStateOf(PainelEd.NENHUM) }
    var dialogo by remember { mutableStateOf("") } // resumo | notas | meta | juntar
    var mostrarMapa by remember { mutableStateOf(false) }

    // desfazer / refazer
    val desfazer = remember { ArrayDeque<Passo>() }
    val refazer = remember { ArrayDeque<Passo>() }
    var ultimoPasso by remember { mutableStateOf(0L) }
    var ultimoPedacoPasso by remember { mutableStateOf(-1) }
    var contadorPilhas by remember { mutableStateOf(0) } // força recomposição dos botões

    val lista = rememberLazyListState()

    fun carregarTexto(texto: String) {
        pedacos.clear()
        Texto.dividirEmPedacos(texto, 3000).forEach { pedacos.add(Pedaco(proximoId++, TextFieldValue(it))) }
        if (pedacos.isEmpty()) pedacos.add(Pedaco(proximoId++, TextFieldValue("")))
        desfazer.clear(); refazer.clear(); contadorPilhas++
    }

    LaunchedEffect(capId) {
        val c = repo.capitulo(capId).filterNotNull().first()
        if (repo.dao.obraAgora(c.obraId)?.trancada == true) {
            // obra publicada por outra pessoa: só leitura
            nav.navigate("leitor/$capId") { popUpTo("editor/{id}") { inclusive = true } }
            return@LaunchedEffect
        }
        cap = c
        val texto = repo.lerTexto(capId)
        withContext(Dispatchers.Default) { Texto.dividirEmPedacos(texto, 3000) } // aquece fora da thread principal
        carregarTexto(texto)
        palavras = c.palavras
        carregado = true
        // voltar onde parou
        val (pi, cur) = prefs.posicaoEditor(capId)
        if (pi in pedacos.indices) {
            lista.scrollToItem(pi)
            if (cur >= 0) {
                val pd = pedacos[pi]
                pd.valor = pd.valor.copy(selection = TextRange(cur.coerceIn(0, pd.valor.text.length)))
            }
        }
    }

    fun textoCompleto(): String {
        val sb = StringBuilder()
        pedacos.forEach { sb.append(it.valor.text) }
        return sb.toString()
    }

    // Salvamento automático: 1,5 s depois da última tecla.
    LaunchedEffect(carregado) {
        if (!carregado) return@LaunchedEffect
        snapshotFlow { versao }.debounce(1500).collect { v ->
            val c = cap ?: return@collect
            if (v == versaoSalva || ocupado) return@collect
            estadoSalvo = "Salvando…"
            val t = textoCompleto()
            val novos = repo.gravarTexto(c, t)
            if (novos.isNotEmpty()) escopo.launch { snack.showSnackbar("✓ Ficha criada: ${novos.joinToString(", ")}") }
            palavras = withContext(Dispatchers.Default) { Texto.contarPalavras(t) }
            versaoSalva = v
            estadoSalvo = "Salvo"
        }
    }

    // Grava ao sair da tela / minimizar e lembra onde parou.
    val dono = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(dono) {
        fun salvarJa() {
            val c = cap ?: return
            val f = focado.takeIf { it in pedacos.indices } ?: lista.firstVisibleItemIndex.coerceIn(0, (pedacos.size - 1).coerceAtLeast(0))
            if (f in pedacos.indices) prefs.salvarPosicaoEditor(c.id, f, if (focado == f) pedacos[f].valor.selection.start else -1)
            if (versao == versaoSalva || ocupado) return
            val t = textoCompleto()
            versaoSalva = versao
            app.escopo.launch { repo.gravarTexto(c, t) }
        }
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) salvarJa() }
        dono.lifecycle.addObserver(obs)
        onDispose { dono.lifecycle.removeObserver(obs); salvarJa() }
    }

    fun recarregar(id: Long) = nav.navigate("editor/$id") { popUpTo("editor/{id}") { inclusive = true } }

    /** Salva o que está na tela e só depois roda [acao] (usado antes de mexer em outros capítulos). */
    fun salvarEDepois(acao: suspend (Capitulo, String) -> Unit) {
        val c = cap ?: return
        ocupado = true
        versaoSalva = versao
        val t = textoCompleto()
        app.escopo.launch {
            repo.gravarTexto(c, t)
            acao(c, t)
        }
    }

    // ── Capítulo automático ──
    fun verificarTitulo(i: Int, posNl: Int) {
        val c = cap ?: return
        val t = pedacos[i].valor.text
        val ini = t.lastIndexOf('\n', posNl - 1) + 1
        val linha = t.substring(ini, posNl)
        if (!Texto.ehTituloEstrito(linha)) return
        val antes = StringBuilder()
        for (k in 0 until i) antes.append(pedacos[k].valor.text)
        antes.append(t, 0, ini)
        val textoAntes = antes.toString()
        val depois = StringBuilder(t.substring(posNl + 1))
        for (k in i + 1 until pedacos.size) depois.append(pedacos[k].valor.text)
        val titulo = Texto.tituloLimpo(linha)
        if (textoAntes.isBlank()) {
            // título no topo: renomeia este capítulo
            carregarTexto(depois.toString())
            val novo = c.copy(titulo = titulo)
            cap = novo
            app.escopo.launch { repo.salvarCapitulo(novo) }
            versao++
            escopo.launch { snack.showSnackbar("Capítulo renomeado: $titulo") }
        } else {
            ocupado = true
            versaoSalva = versao
            estadoSalvo = "Criando capítulo…"
            app.escopo.launch {
                val id = repo.dividirCapitulo(c, textoAntes.trimEnd() + "\n", titulo, depois.toString().trimStart('\n'))
                withContext(Dispatchers.Main) { recarregar(id) }
            }
        }
    }

    fun separarTudo(silencioso: Boolean) {
        salvarEDepois { c, t ->
            val n = repo.separarPorTitulos(c, t)
            withContext(Dispatchers.Main) {
                if (n == 0 && repo.dao.capituloAgora(c.id)?.titulo == c.titulo) {
                    ocupado = false
                    if (!silencioso) snack.showSnackbar("Nenhuma linha de título encontrada (ex.: “Capítulo 2 — Nome”).")
                } else recarregar(c.id)
            }
        }
    }

    fun empilhar(pd: Pedaco, antigo: TextFieldValue) {
        val agora = System.currentTimeMillis()
        if (agora - ultimoPasso > 900 || ultimoPedacoPasso != pd.id) {
            desfazer.addLast(Passo(pd.id, antigo))
            if (desfazer.size > 200) desfazer.removeFirst()
            refazer.clear()
            contadorPilhas++
        }
        ultimoPasso = agora
        ultimoPedacoPasso = pd.id
    }

    fun aoMudar(i: Int, pd: Pedaco, novo: TextFieldValue) {
        if (ocupado) return
        val antigo = pd.valor
        if (novo.text != antigo.text) { empilhar(pd, antigo); versao++ }
        pd.valor = novo
        if (novo.text == antigo.text) return
        val nlAntes = antigo.text.count { it == '\n' }
        val nlDepois = novo.text.count { it == '\n' }
        if (nlDepois - nlAntes > 1) {
            // colou várias linhas: se houver títulos, separa automaticamente
            if (novo.text.lineSequence().any { Texto.ehTituloEstrito(it) }) separarTudo(silencioso = true)
        } else if (nlDepois > nlAntes) {
            // Enter (mesmo quando o teclado envia a palavra e o Enter juntos)
            val cur = novo.selection.start
            val posNl = if (cur > 0 && novo.text.getOrNull(cur - 1) == '\n') cur - 1 else novo.text.lastIndexOf('\n', (cur - 1).coerceAtLeast(0))
            if (posNl >= 0) verificarTitulo(i, posNl)
        }
    }

    fun aplicarPasso(origem: ArrayDeque<Passo>, destino: ArrayDeque<Passo>) {
        val passo = origem.removeLastOrNull() ?: return
        val pd = pedacos.firstOrNull { it.id == passo.pedacoId } ?: return
        destino.addLast(Passo(pd.id, pd.valor))
        pd.valor = passo.valor
        ultimoPedacoPasso = -1
        versao++
        contadorPilhas++
    }

    fun inserir(marca: String, envolver: Boolean) {
        val i = focado.takeIf { it in pedacos.indices } ?: pedacos.lastIndex
        val pd = pedacos[i]
        val v = pd.valor
        val sel = v.selection
        empilhar(pd, v); ultimoPedacoPasso = -1
        val novo: TextFieldValue = if (envolver && !sel.collapsed) {
            val t = v.text.substring(0, sel.min) + marca + v.text.substring(sel.min, sel.max) + marca + v.text.substring(sel.max)
            TextFieldValue(t, TextRange(sel.max + marca.length * 2))
        } else {
            val ins = if (envolver) marca + marca else marca
            val t = v.text.substring(0, sel.min) + ins + v.text.substring(sel.max)
            TextFieldValue(t, TextRange(sel.min + if (envolver) marca.length else ins.length))
        }
        pd.valor = novo
        versao++
    }

    /** Travessão de fala: começa um parágrafo novo com "— " se o cursor não estiver numa linha vazia. */
    fun novaFala() {
        val i = focado.takeIf { it in pedacos.indices } ?: pedacos.lastIndex
        val v = pedacos[i].valor
        val c = v.selection.min
        val inicioLinha = c == 0 || v.text[c - 1] == '\n'
        val linhaVazia = inicioLinha && (c == v.text.length || v.text[c] == '\n')
        inserir(if (linhaVazia) "— " else if (inicioLinha) "\n— " else "\n\n— ", false)
    }

    /** Vai até uma posição: rola, foca o pedaço e seleciona o trecho. */
    fun irPara(pedaco: Int, pos: Int, tamanho: Int = 0) {
        if (pedaco !in pedacos.indices) return
        escopo.launch {
            lista.scrollToItem(pedaco)
            delay(60)
            val pd = pedacos[pedaco]
            val ini = pos.coerceIn(0, pd.valor.text.length)
            pd.valor = pd.valor.copy(selection = TextRange(ini, (ini + tamanho).coerceAtMost(pd.valor.text.length)))
            try { pd.foco.requestFocus() } catch (_: Exception) {}
        }
    }

    /** Desce o máximo possível, sem procurar cursor (evita pular para o meio do texto). */
    fun irAoFimMaximo() {
        escopo.launch {
            val total = lista.layoutInfo.totalItemsCount
            if (total > 0) lista.scrollToItem(total - 1)
            repeat(3) { lista.scrollBy(1_000_000f); delay(16) }
        }
    }

    // ── Busca e substituir ──
    var consulta by remember { mutableStateOf("") }
    var substituto by remember { mutableStateOf("") }
    var ignorarCaixa by remember { mutableStateOf(true) }
    var achadoAtual by remember { mutableStateOf(0) }
    val qNorm = if (ignorarCaixa) Texto.normalizar(consulta) else consulta
    val achados by remember {
        derivedStateOf {
            val q = if (ignorarCaixa) Texto.normalizar(consulta) else consulta
            if (consulta.length < 2) emptyList() else {
                val r = ArrayList<Pair<Int, Int>>()
                pedacos.forEachIndexed { pi, pd ->
                    val t = if (ignorarCaixa) Texto.normalizar(pd.valor.text) else pd.valor.text
                    var k = t.indexOf(q)
                    while (k >= 0 && r.size < 3000) { r.add(pi to k); k = t.indexOf(q, k + q.length) }
                }
                r
            }
        }
    }
    fun irAchado(n: Int) {
        if (achados.isEmpty()) return
        achadoAtual = (n + achados.size) % achados.size
        val (pi, pos) = achados[achadoAtual]
        irPara(pi, pos, consulta.length)
    }
    fun substituirAtual() {
        val (pi, pos) = achados.getOrNull(achadoAtual) ?: return
        val pd = pedacos[pi]
        empilhar(pd, pd.valor); ultimoPedacoPasso = -1
        val t = pd.valor.text
        pd.valor = TextFieldValue(t.substring(0, pos) + substituto + t.substring(pos + consulta.length), TextRange(pos + substituto.length))
        versao++
    }
    fun substituirNoCapitulo(): Int {
        var n = 0
        pedacos.forEach { pd ->
            val t = pd.valor.text
            val novo = t.replace(consulta, substituto, ignoreCase = ignorarCaixa)
            if (novo != t) {
                n += Regex(Regex.escape(consulta), if (ignorarCaixa) setOf(RegexOption.IGNORE_CASE) else emptySet()).findAll(t).count()
                empilhar(pd, pd.valor); ultimoPedacoPasso = -1
                pd.valor = TextFieldValue(novo)
            }
        }
        if (n > 0) versao++
        return n
    }

    // ── Mapa do capítulo ──
    fun marcas(): List<Marca> {
        val r = ArrayList<Marca>()
        pedacos.forEachIndexed { pi, pd ->
            val t = pd.valor.text
            var ini = 0
            while (ini <= t.length) {
                var fim = t.indexOf('\n', ini); if (fim == -1) fim = t.length
                val l = t.substring(ini, fim).trim()
                if (Texto.ehTituloEstrito(l) || l.startsWith("#")) r.add(Marca(pi, ini, Texto.tituloLimpo(l), 0))
                else if (l == "* * *" || l == "***" || l == "◆") {
                    val prox = t.substring(fim).trimStart().lineSequence().firstOrNull()?.take(70) ?: ""
                    r.add(Marca(pi, ini, prox.ifEmpty { "Nova cena" }, 1))
                }
                ini = fim + 1
            }
        }
        return r
    }

    // ── Seleção atual ──
    val pdFocado = pedacos.getOrNull(focado)
    // ── @Personagem / &Lugar: sugestões enquanto digita ──
    val fichasObra by remember(cap?.obraId) { cap?.let { repo.fichas(it.obraId) } ?: kotlinx.coroutines.flow.flowOf(emptyList()) }.collectAsState(initial = emptyList())
    val digitandoMarca = pdFocado?.valor?.let { v ->
        if (!v.selection.collapsed) null else { val cur = v.selection.start.coerceIn(0, v.text.length); Marcas.digitando(v.text.substring((cur - 60).coerceAtLeast(0), cur)) }
    }
    fun completarMarca(simbolo: Char, termo: String, nome: String, definir: Boolean) {
        val i = focado.takeIf { it in pedacos.indices } ?: return
        val pd = pedacos[i]; val v = pd.valor; val cur = v.selection.start
        val ini = (cur - termo.length - 1).coerceAtLeast(0)
        val ins = "$simbolo$nome" + if (definir) ", " else " "
        aoMudar(i, pd, TextFieldValue(v.text.substring(0, ini) + ins + v.text.substring(cur), TextRange(ini + ins.length)))
    }
    val selecao = pdFocado?.valor?.selection
    val textoSelecionado = if (pdFocado != null && selecao != null && !selecao.collapsed)
        pdFocado.valor.text.substring(selecao.min, selecao.max) else ""

    fun moverSelecao(direcao: Int) {
        val pd = pdFocado ?: return
        val sel = selecao ?: return
        if (sel.collapsed) return
        val trecho = textoSelecionado
        empilhar(pd, pd.valor); ultimoPedacoPasso = -1
        pd.valor = TextFieldValue(pd.valor.text.removeRange(sel.min, sel.max), TextRange(sel.min))
        versao++
        salvarEDepois { c, _ ->
            val alvo = repo.moverTrecho(c, trecho, direcao)
            withContext(Dispatchers.Main) {
                ocupado = false
                if (alvo == null) {
                    // não há capítulo nessa direção: desfaz
                    aplicarPasso(desfazer, refazer)
                    snack.showSnackbar(if (direcao < 0) "Não há capítulo anterior." else "Não há próximo capítulo.")
                } else snack.showSnackbar("Trecho movido para “${alvo.titulo.ifEmpty { "sem título" }}”.")
            }
        }
    }

    val familia = when (fonte) { "SEM_SERIFA" -> FontFamily.SansSerif; "SERIFA" -> FontFamily.Serif; else -> p.fonteLeitura }
    val estilo = TextStyle(fontFamily = familia, fontSize = 17.sp, lineHeight = 29.sp, color = p.texto)

    // ───────── Diálogos ─────────
    cap?.let { c ->
        when (dialogo) {
            "resumo" -> DialogoTexto("Resumo do capítulo", "O que acontece neste capítulo (2–4 frases)", c.resumo, multilinha = true,
                aoFechar = { dialogo = "" }) { r -> dialogo = ""; val n = c.copy(resumo = r.trim()); cap = n; app.escopo.launch { repo.salvarCapitulo(n) } }
            "notas" -> DialogoTexto("Notas do autor", "Ideias, lembretes, pendências (só você vê)", c.notas, multilinha = true,
                aoFechar = { dialogo = "" }) { r -> dialogo = ""; val n = c.copy(notas = r.trim()); cap = n; app.escopo.launch { repo.salvarCapitulo(n) } }
            "meta" -> DialogoTexto("Meta diária de palavras", "Palavras por dia", meta.toInt().toString(), aoFechar = { dialogo = "" }) { r ->
                dialogo = ""; r.filter { it.isDigit() }.toIntOrNull()?.takeIf { it > 0 }?.let { prefs.metaDiaria.definir(it.toFloat()) }
            }
            "juntar" -> DialogoConfirmar("Juntar com o próximo capítulo?", "O texto do próximo capítulo vem para o fim deste. O próximo vai para a Lixeira (dá para recuperar por 30 dias).", "Juntar",
                { dialogo = "" }) {
                dialogo = ""
                salvarEDepois { cc, t ->
                    val ok = repo.juntarComProximo(cc, t)
                    withContext(Dispatchers.Main) { if (ok) recarregar(cc.id) else { ocupado = false; snack.showSnackbar("Não há próximo capítulo.") } }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(p.fundo).imePadding()) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(!modoFoco, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Topo(cap?.titulo?.ifEmpty { "Página em branco" } ?: "", nav) {
                        Text(estadoSalvo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (estadoSalvo == "Salvo") Color(0xFF6FC2B4) else p.destaque, modifier = Modifier.padding(end = 2.dp))
                        IconButton(onClick = { painel = if (painel == PainelEd.BUSCA) PainelEd.NENHUM else PainelEd.BUSCA }) { Icon(Icons.Filled.Search, "Buscar e substituir") }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Mais opções") }
                            MenuEditor(menu, { menu = false }, cap, repeticoes, nav,
                                aoFoco = { modoFoco = true }, aoRepeticoes = { repeticoes = !repeticoes }, aoMapa = { mostrarMapa = true },
                                aoDialogo = { dialogo = it }, aoSeparar = { separarTudo(false) },
                                aoMarcar = { c -> val n = c.copy(rascunho = !c.rascunho); cap = n; app.escopo.launch { repo.salvarCapitulo(n) } })
                        }
                    }
                    LinhaStatus(palavras, cap, textoSelecionado, hoje, meta, sequencia, p) { dialogo = "meta" }
                }
            }
            AnimatedVisibility(painel == PainelEd.BUSCA && !modoFoco) {
                PainelBuscaEditor(consulta, { consulta = it; achadoAtual = 0 }, substituto, { substituto = it }, ignorarCaixa, { ignorarCaixa = it },
                    achados.size, achadoAtual, { irAchado(achadoAtual - 1) }, { irAchado(achadoAtual + 1) },
                    aoSubstituir = { substituirAtual() },
                    aoSubstituirCapitulo = { val n = substituirNoCapitulo(); escopo.launch { snack.showSnackbar("$n substituição(ões) neste capítulo.") } },
                    aoSubstituirObra = {
                        val de = consulta; val para = substituto; val ic = ignorarCaixa
                        salvarEDepois { c, _ ->
                            val n = repo.substituirNaObra(c.obraId, de, para, ic)
                            withContext(Dispatchers.Main) { snack.showSnackbar("$n substituição(ões) na obra inteira."); recarregar(c.id) }
                        }
                    },
                    aoFechar = { painel = PainelEd.NENHUM; consulta = "" }, p = p)
            }
            if (!carregado) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp), color = p.destaque, trackColor = p.fundo)
            } else {
                val realce = remember(p, qNorm, repeticoes, ignorarCaixa) { RealceEditor(p, if (consulta.length >= 2) qNorm else "", ignorarCaixa, repeticoes) }
                LazyColumn(state = lista, modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = if (modoFoco) 48.dp else 12.dp)) {
                    itemsIndexed(pedacos, key = { _, x -> x.id }) { i, pd ->
                        BasicTextField(
                            value = pd.valor,
                            onValueChange = { novo -> aoMudar(i, pd, novo) },
                            textStyle = estilo,
                            cursorBrush = SolidColor(p.destaque),
                            visualTransformation = realce,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth().focusRequester(pd.foco).onFocusChanged { if (it.isFocused) focado = i },
                        )
                    }
                    item { Spacer(Modifier.height(240.dp)) }
                }
                AnimatedVisibility(!modoFoco) {
                    Column(Modifier.fillMaxWidth().background(p.superficie)) {
                        if (textoSelecionado.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${Texto.contarPalavras(textoSelecionado)} palavras selecionadas", fontSize = 12.sp, color = p.destaque, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { moverSelecao(-1) }) { Text("‹ Mover p/ cap. anterior", fontSize = 12.sp) }
                                TextButton(onClick = { moverSelecao(1) }) { Text("Mover p/ próximo ›", fontSize = 12.sp) }
                            }
                        }
                        digitandoMarca?.let { (simb, termo) ->
                            val lugar = simb == '&'
                            val opcoes = fichasObra.filter { (it.tipo == TipoFicha.LUGAR) == lugar && (termo.isBlank() || it.nome.contains(termo.trim(), true) || it.apelidos.contains(termo.trim(), true)) }.take(12)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (lugar) "LUGARES" else "PERSONAGENS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = p.textoSuave)
                                opcoes.forEach { f ->
                                    Text(f.nome, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (lugar) p.destaque else p.destaque2,
                                        modifier = Modifier.clip(forma(14.dp)).background(p.fundo).clickable { completarMarca(simb, termo, f.nome, false) }
                                            .padding(horizontal = 12.dp, vertical = 7.dp))
                                }
                                if (termo.trim().length >= 2 && opcoes.none { it.nome.equals(termo.trim(), true) }) {
                                    Text("+ Criar “${termo.trim()}”", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.sobreDestaque,
                                        modifier = Modifier.clip(forma(14.dp)).background(p.destaque).clickable { completarMarca(simb, termo, termo.trim(), true) }
                                            .padding(horizontal = 12.dp, vertical = 7.dp))
                                }
                                if (opcoes.isEmpty() && termo.trim().length < 2) Text("digite o nome… (vírgula depois do nome = descrever)", fontSize = 12.sp, color = p.textoSuave)
                            }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            BotaoFerramenta("@", p, FontWeight.ExtraBold, cor = p.destaque2) { inserir("@", false) }
                            BotaoFerramenta("&", p, FontWeight.ExtraBold, cor = p.destaque) { inserir("&", false) }
                            key(contadorPilhas) {
                                BotaoFerramenta("↶", p, ativo = desfazer.isNotEmpty()) { aplicarPasso(desfazer, refazer) }
                                BotaoFerramenta("↷", p, ativo = refazer.isNotEmpty()) { aplicarPasso(refazer, desfazer) }
                            }
                            BotaoFerramenta("—", p, cor = p.destaque) { novaFala() }
                            BotaoFerramenta("B", p, FontWeight.ExtraBold) { inserir("**", true) }
                            BotaoFerramenta("I", p, estiloFonte = FontStyle.Italic) { inserir("*", true) }
                            BotaoFerramenta("“ ”", p) { inserir("“", false) }
                            BotaoFerramenta("◆", p, cor = p.destaque) { inserir("\n\n* * *\n\n", false) }
                            BotaoFerramenta("H", p) { inserir("\n# ", false) }
                            BotaoFerramenta("☰", p) { mostrarMapa = true }
                        }
                    }
                }
            }
        }

        // ir ao início / fim
        if (carregado && !modoFoco) {
            Column(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = if (textoSelecionado.isNotEmpty()) 124.dp else 76.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BotaoRedondoPequeno(Icons.Filled.KeyboardArrowUp, "Ir ao início", p) { irPara(0, 0) }
                BotaoRedondoPequeno(Icons.Filled.KeyboardArrowDown, "Ir ao fim", p) { irAoFimMaximo() }
            }
        }
        if (modoFoco) {
            Text("Sair do foco", fontSize = 12.sp, color = p.textoSuave, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(10.dp).clip(forma(12.dp))
                    .background(p.superficie).clickable { modoFoco = false }.padding(horizontal = 12.dp, vertical = 8.dp))
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }

    if (mostrarMapa) ModalBottomSheet(onDismissRequest = { mostrarMapa = false }, containerColor = p.superficie) {
        val m = remember { marcas() }
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text("Mapa do capítulo", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
            Text("Títulos e trocas de cena (◆). Toque para ir até lá.", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            LazyColumn(Modifier.fillMaxHeight(0.7f)) {
                item {
                    Row(Modifier.fillMaxWidth().clickable { mostrarMapa = false; irPara(0, 0) }.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text("⤒  Início", color = p.textoSuave)
                    }
                }
                items(m) { mc ->
                    Row(Modifier.fillMaxWidth().clickable { mostrarMapa = false; irPara(mc.pedaco, mc.pos) }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (mc.tipo == 0) "§" else "◆", color = p.destaque, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                        Text(mc.rotulo, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (mc.tipo == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                if (m.isEmpty()) item { Text("Nenhum título ou troca de cena ainda.", color = p.textoSuave, modifier = Modifier.padding(20.dp)) }
                item {
                    Row(Modifier.fillMaxWidth().clickable { mostrarMapa = false; irAoFimMaximo() }
                        .padding(horizontal = 20.dp, vertical = 12.dp)) { Text("⤓  Fim", color = p.textoSuave) }
                }
            }
        }
    }
}

@Composable
private fun MenuEditor(
    aberto: Boolean, fechar: () -> Unit, cap: Capitulo?, repeticoes: Boolean, nav: NavHostController,
    aoFoco: () -> Unit, aoRepeticoes: () -> Unit, aoMapa: () -> Unit, aoDialogo: (String) -> Unit, aoSeparar: () -> Unit, aoMarcar: (Capitulo) -> Unit,
) {
    DropdownMenu(aberto, fechar) {
        DropdownMenuItem(text = { Text("Modo foco") }, onClick = { fechar(); aoFoco() })
        DropdownMenuItem(text = { Text("Mapa do capítulo") }, onClick = { fechar(); aoMapa() })
        DropdownMenuItem(text = { Text(if (repeticoes) "✓ Destacar repetições" else "Destacar repetições") }, onClick = { fechar(); aoRepeticoes() })
        HorizontalDivider()
        DropdownMenuItem(text = { Text("Notas do autor" + if (cap?.notas?.isNotBlank() == true) " ●" else "") }, onClick = { fechar(); aoDialogo("notas") })
        DropdownMenuItem(text = { Text("Resumo do capítulo") }, onClick = { fechar(); aoDialogo("resumo") })
        cap?.let { c -> DropdownMenuItem(text = { Text("Personagens e lugares") }, onClick = { fechar(); nav.navigate("fichas/${c.obraId}") }) }
        HorizontalDivider()
        DropdownMenuItem(text = { Text("Separar capítulos pelos títulos") }, onClick = { fechar(); aoSeparar() })
        DropdownMenuItem(text = { Text("Juntar com o próximo capítulo") }, onClick = { fechar(); aoDialogo("juntar") })
        cap?.let { c -> DropdownMenuItem(text = { Text(if (c.rascunho) "Marcar como concluído" else "Voltar para rascunho") }, onClick = { fechar(); aoMarcar(c) }) }
        HorizontalDivider()
        cap?.let { c ->
            DropdownMenuItem(text = { Text("Ver como leitor") }, onClick = { fechar(); nav.navigate("leitor/${c.id}") })
            DropdownMenuItem(text = { Text("Histórico de versões") }, onClick = { fechar(); nav.navigate("historico/${c.id}") })
        }
        DropdownMenuItem(text = { Text("Meta diária") }, onClick = { fechar(); aoDialogo("meta") })
    }
}

@Composable
private fun LinhaStatus(palavras: Int, cap: Capitulo?, selecionado: String, hoje: Long, meta: Float, sequencia: Long, p: Paleta, aoMeta: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${"%,d".format(palavras).replace(',', '.')} palavras" + if (cap?.rascunho == true) " · rascunho" else " · concluído",
            fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.weight(1f))
        Row(Modifier.clip(forma(10.dp)).clickable(onClick = aoMeta).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            val f = (hoje / meta.coerceAtLeast(1f)).coerceIn(0f, 1f)
            Box(Modifier.width(54.dp).height(5.dp).clip(forma(3.dp)).background(p.borda)) {
                Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(if (f >= 1f) Color(0xFF6FC2B4) else p.destaque))
            }
            Spacer(Modifier.width(6.dp))
            Text("hoje $hoje/${meta.toInt()}" + if (sequencia > 0) " · 🔥$sequencia" else "", fontSize = 11.sp, color = p.textoSuave)
        }
    }
}

@Composable
private fun PainelBuscaEditor(
    consulta: String, mudar: (String) -> Unit, substituto: String, mudarSub: (String) -> Unit, ignorarCaixa: Boolean, mudarCaixa: (Boolean) -> Unit,
    total: Int, atual: Int, anterior: () -> Unit, proximo: () -> Unit,
    aoSubstituir: () -> Unit, aoSubstituirCapitulo: () -> Unit, aoSubstituirObra: () -> Unit, aoFechar: () -> Unit, p: Paleta,
) {
    var confirmarObra by remember { mutableStateOf(false) }
    val cores = TextFieldDefaults.colors(focusedContainerColor = p.fundo, unfocusedContainerColor = p.fundo,
        focusedIndicatorColor = p.destaque, unfocusedIndicatorColor = p.borda)
    Column(Modifier.fillMaxWidth().background(p.superficie).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(consulta, mudar, singleLine = true, placeholder = { Text("Buscar") }, colors = cores, modifier = Modifier.weight(1f))
            Text(if (consulta.length < 2) "" else if (total == 0) "0" else "${atual + 1}/$total", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(horizontal = 6.dp))
            IconButton(onClick = anterior) { Icon(Icons.Filled.KeyboardArrowUp, "Anterior") }
            IconButton(onClick = proximo) { Icon(Icons.Filled.KeyboardArrowDown, "Próximo") }
            IconButton(onClick = aoFechar) { Icon(Icons.Filled.Close, "Fechar") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(substituto, mudarSub, singleLine = true, placeholder = { Text("Substituir por") }, colors = cores, modifier = Modifier.weight(1f))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(if (ignorarCaixa) "Aa ignorado" else "Aa exato", !ignorarCaixa) { mudarCaixa(!ignorarCaixa) }
            TextButton(onClick = aoSubstituir, enabled = total > 0) { Text("Substituir") }
            TextButton(onClick = aoSubstituirCapitulo, enabled = total > 0) { Text("Todos no capítulo") }
            TextButton(onClick = { confirmarObra = true }, enabled = consulta.length >= 2) { Text("Na obra inteira") }
        }
    }
    if (confirmarObra) DialogoConfirmar("Substituir na obra inteira?", "Troca “$consulta” por “$substituto” em todos os capítulos desta obra. Uma versão de cada capítulo alterado fica no histórico.",
        "Substituir tudo", { confirmarObra = false }) { confirmarObra = false; aoSubstituirObra() }
}

@Composable
private fun BotaoRedondoPequeno(icone: androidx.compose.ui.graphics.vector.ImageVector, desc: String, p: Paleta, acao: () -> Unit) {
    Box(Modifier.size(40.dp).clip(forma(20.dp)).background(p.superficie).border(1.dp, p.borda, forma(20.dp)).clickable(onClick = acao),
        contentAlignment = Alignment.Center) { Icon(icone, desc, tint = p.textoSuave) }
}

@Composable
private fun BotaoFerramenta(rotulo: String, p: Paleta, peso: FontWeight = FontWeight.Bold, estiloFonte: FontStyle = FontStyle.Normal, cor: Color = p.texto, ativo: Boolean = true, acao: () -> Unit) {
    Box(Modifier.size(46.dp).clip(forma(12.dp)).clickable(enabled = ativo, onClick = acao), contentAlignment = Alignment.Center) {
        Text(rotulo, fontWeight = peso, fontStyle = estiloFonte, fontSize = 18.sp, color = if (ativo) cor else p.textoSuave.copy(alpha = 0.35f), fontFamily = FontFamily.Serif)
    }
}

private val palavrasComuns = setOf("para", "como", "mais", "mas", "porque", "quando", "ainda", "muito", "esse", "essa", "isso", "este", "esta",
    "isto", "aquele", "aquela", "pelo", "pela", "seus", "suas", "eles", "elas", "dele", "dela", "nada", "tudo", "outro", "outra", "sobre",
    "entre", "depois", "antes", "também", "tambem", "então", "entao", "onde", "cada", "mesmo", "mesma", "estava", "tinha", "foram", "seria")

/**
 * Colore o editor sem mudar o texto: falas (— ou “) com travessão em destaque, títulos de capítulo maiores,
 * marcas ** * discretas, resultados da busca e (opcional) palavras repetidas muito perto.
 */
private class RealceEditor(private val p: Paleta, private val busca: String, private val ignorarCaixa: Boolean, private val repeticoes: Boolean) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val t = text.text
        val r = buildAnnotatedString {
            append(t)
            var inicio = 0
            while (inicio <= t.length) {
                var fim = t.indexOf('\n', inicio)
                if (fim == -1) fim = t.length
                var k = inicio
                while (k < fim && t[k] == ' ') k++
                if (k < fim) {
                    val c = t[k]
                    val linha = t.substring(k, fim)
                    if (Texto.ehTituloEstrito(linha)) {
                        val esp = Texto.tipoEspecial(linha) != null
                        addStyle(SpanStyle(color = p.destaque, fontWeight = FontWeight.Bold, fontSize = 1.3.em,
                            fontStyle = if (esp) FontStyle.Italic else FontStyle.Normal, letterSpacing = if (esp) 0.08.em else 0.em), k, fim)
                    } else if (c == '—' || c == '–' || c == '“' || (c == '-' && k + 1 < fim && t[k + 1] == ' ')) {
                        addStyle(SpanStyle(color = p.destaque, fontWeight = FontWeight.Bold), k, k + 1)
                    } else if (c == '#') {
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = p.destaque), k, fim)
                    } else if (linha.startsWith("* * *")) {
                        addStyle(SpanStyle(color = p.destaque), k, fim)
                    }
                }
                var j = t.indexOf('*', inicio)
                while (j in inicio until fim) {
                    addStyle(SpanStyle(color = p.textoSuave.copy(alpha = 0.6f)), j, j + 1)
                    j = t.indexOf('*', j + 1)
                }
                inicio = fim + 1
            }
            if ('@' in t || '&' in t) Marcas.REGRA.findAll(t).forEach { m ->
                val fimNome = m.groups[2]!!.range.last + 1
                addStyle(SpanStyle(color = if (m.groupValues[1] == "&") p.destaque else p.destaque2, fontWeight = FontWeight.Bold), m.range.first, fimNome)
                m.groups[3]?.let { g -> addStyle(SpanStyle(color = p.textoSuave), g.range.first, g.range.last + 1) }
            }
            if (busca.isNotEmpty()) {
                val base = if (ignorarCaixa) Texto.normalizar(t) else t
                var i = base.indexOf(busca)
                while (i >= 0) {
                    addStyle(SpanStyle(background = p.destaque.copy(alpha = 0.45f)), i, (i + busca.length).coerceAtMost(t.length))
                    i = base.indexOf(busca, i + busca.length)
                }
            }
            if (repeticoes) {
                val ultima = HashMap<String, Pair<Int, Int>>() // palavra -> (índice da palavra, início)
                var n = 0
                var i = 0
                while (i < t.length) {
                    if (t[i].isLetter()) {
                        val ini = i
                        while (i < t.length && t[i].isLetter()) i++
                        if (i - ini >= 4) {
                            val w = Texto.normalizar(t.substring(ini, i))
                            if (w !in palavrasComuns) {
                                val ant = ultima[w]
                                if (ant != null && n - ant.first <= 30) {
                                    val estilo = SpanStyle(textDecoration = TextDecoration.Underline, color = Color(0xFFFF8A65))
                                    addStyle(estilo, ant.second, ant.second + (i - ini))
                                    addStyle(estilo, ini, i)
                                }
                                ultima[w] = n to ini
                            }
                        }
                        n++
                    } else i++
                }
            }
        }
        return TransformedText(r, OffsetMapping.Identity)
    }
    override fun equals(other: Any?) = other is RealceEditor && other.p == p && other.busca == busca && other.repeticoes == repeticoes && other.ignorarCaixa == ignorarCaixa
    override fun hashCode() = p.hashCode() * 31 + busca.hashCode() + repeticoes.hashCode()
}

// ───────── Histórico de versões ─────────

@Composable
fun TelaHistorico(capId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    var versoes by remember { mutableStateOf<List<ArquivosTexto.Versao>?>(null) }
    var escolhida by remember { mutableStateOf<ArquivosTexto.Versao?>(null) }
    LaunchedEffect(capId) { versoes = repo.textos.versoes(capId) }
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }

    Column(Modifier.fillMaxSize()) {
        Topo("Histórico de versões", nav)
        Text("Uma cópia é guardada antes de salvar, no máximo a cada 10 minutos (até 30 versões). Toque numa versão para comparar ou restaurar.",
            color = p.textoSuave, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
        val v = versoes
        if (v == null) LinearProgressIndicator(Modifier.fillMaxWidth(), color = p.destaque, trackColor = p.fundo)
        else if (v.isEmpty()) Text("Ainda não há versões antigas deste capítulo.", color = p.textoSuave, modifier = Modifier.padding(20.dp))
        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(v) { ver ->
                Column(Modifier.fillMaxWidth().clip(forma(16.dp)).background(p.superficie).clickable { escolhida = ver }.padding(14.dp)) {
                    Text(fmt.format(Date(ver.quando)), fontWeight = FontWeight.Bold)
                    Text("${ver.palavras} palavras", fontSize = 12.sp, color = p.destaque)
                    Text(ver.inicio, fontSize = 13.sp, color = p.textoSuave, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    escolhida?.let { ver ->
        AlertDialog(
            onDismissRequest = { escolhida = null },
            title = { Text(fmt.format(Date(ver.quando))) },
            text = { Text("Comparar com o texto atual ou restaurar esta versão? Ao restaurar, o texto atual também vai para o histórico.") },
            confirmButton = {
                TextButton(onClick = {
                    escolhida = null
                    app.escopo.launch {
                        repo.textos.guardarVersaoSePreciso(capId, forcar = true)
                        val t = withContext(Dispatchers.IO) { ver.arquivo.readText(Charsets.UTF_8) }
                        repo.textos.gravar(capId, t)
                        repo.dao.atualizarPalavras(capId, Texto.contarPalavras(t), System.currentTimeMillis())
                        launch(Dispatchers.Main) { nav.navigate("editor/$capId") { popUpTo("editor/{id}") { inclusive = true } } }
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { escolhida = null }) { Text("Cancelar") }
                    TextButton(onClick = { escolhida = null; nav.navigate("comparar/$capId/${ver.quando}") }) { Text("Comparar") }
                }
            },
        )
    }
}

/** Diferença por parágrafo entre uma versão antiga e o texto atual. */
private data class LinhaDiff(val tipo: Int, val texto: String) // 0 igual, -1 removida, 1 adicionada

private fun diferenca(a: List<String>, b: List<String>): List<LinhaDiff> {
    val n = a.size; val m = b.size
    if (n.toLong() * m > 4_000_000) { // grande demais para LCS completa: mostra só o que não existe do outro lado
        val sa = a.toHashSet(); val sb = b.toHashSet()
        return a.filter { it !in sb }.map { LinhaDiff(-1, it) } + b.filter { it !in sa }.map { LinhaDiff(1, it) }
    }
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
    val r = ArrayList<LinhaDiff>()
    var i = 0; var j = 0
    while (i < n && j < m) {
        when {
            a[i] == b[j] -> { r.add(LinhaDiff(0, a[i])); i++; j++ }
            dp[i + 1][j] >= dp[i][j + 1] -> { r.add(LinhaDiff(-1, a[i])); i++ }
            else -> { r.add(LinhaDiff(1, b[j])); j++ }
        }
    }
    while (i < n) r.add(LinhaDiff(-1, a[i++]))
    while (j < m) r.add(LinhaDiff(1, b[j++]))
    return r
}

@Composable
fun TelaComparar(capId: Long, quando: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    var linhas by remember { mutableStateOf<List<LinhaDiff>?>(null) }
    LaunchedEffect(capId, quando) {
        val ver = app.repo.textos.versoes(capId).firstOrNull { it.quando == quando }
        val antigo = ver?.let { withContext(Dispatchers.IO) { it.arquivo.readText(Charsets.UTF_8) } } ?: ""
        val atual = app.repo.lerTexto(capId)
        linhas = withContext(Dispatchers.Default) {
            val d = diferenca(antigo.lines().map { it.trim() }.filter { it.isNotEmpty() }, atual.lines().map { it.trim() }.filter { it.isNotEmpty() })
            // mostra só as mudanças com 1 parágrafo de contexto
            d.filterIndexed { k, l -> l.tipo != 0 || d.getOrNull(k - 1)?.tipo?.let { it != 0 } == true || d.getOrNull(k + 1)?.tipo?.let { it != 0 } == true }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Topo("Comparar versões", nav)
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("− removido", color = Color(0xFFE57373), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("+ adicionado", color = Color(0xFF6FC2B4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        val l = linhas
        if (l == null) LinearProgressIndicator(Modifier.fillMaxWidth(), color = p.destaque, trackColor = p.fundo)
        else if (l.none { it.tipo != 0 }) Text("Nenhuma diferença: a versão é igual ao texto atual.", color = p.textoSuave, modifier = Modifier.padding(20.dp))
        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(l) { d ->
                val (fundo, cor) = when (d.tipo) {
                    -1 -> Color(0x22E57373) to Color(0xFFE57373)
                    1 -> Color(0x226FC2B4) to Color(0xFF6FC2B4)
                    else -> Color.Transparent to p.textoSuave
                }
                Text((if (d.tipo == -1) "− " else if (d.tipo == 1) "+ " else "") + d.texto, fontSize = 14.sp, lineHeight = 21.sp, color = if (d.tipo == 0) p.textoSuave else p.texto,
                    textDecoration = if (d.tipo == -1) TextDecoration.LineThrough else null,
                    modifier = Modifier.fillMaxWidth().clip(forma(10.dp)).background(fundo).border(if (d.tipo != 0) 1.dp else 0.dp, cor.copy(alpha = 0.4f), forma(10.dp)).padding(10.dp))
            }
        }
    }
}
