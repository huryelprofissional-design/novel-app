package com.huryel.novely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.ArquivosTexto
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Editor para capítulos gigantes: o texto é dividido em pedaços de ~3 mil caracteres, cada um num
 * campo próprio dentro de uma lista preguiçosa. Só os pedaços visíveis existem na tela, então digitar
 * num capítulo de 100 mil palavras custa o mesmo que num de mil. Ao salvar, os pedaços são unidos de
 * volta — o texto resultante é idêntico ao original mais suas edições.
 */
private class Pedaco(val id: Int, valor: TextFieldValue) {
    var valor by mutableStateOf(valor)
}

@OptIn(FlowPreview::class)
@Composable
fun TelaEditor(capId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    val fonte by app.prefs.fonte.estado.collectAsState()

    var cap by remember { mutableStateOf<Capitulo?>(null) }
    val pedacos = remember { mutableStateListOf<Pedaco>() }
    var carregado by remember { mutableStateOf(false) }
    var versao by remember { mutableStateOf(0) }       // muda a cada edição
    var versaoSalva by remember { mutableStateOf(0) }
    var palavras by remember { mutableStateOf(0) }
    var estadoSalvo by remember { mutableStateOf("Salvo") }
    var focado by remember { mutableStateOf(-1) }
    var menu by remember { mutableStateOf(false) }
    var editandoResumo by remember { mutableStateOf(false) }
    var proximoId by remember { mutableStateOf(0) }

    LaunchedEffect(capId) {
        val c = repo.capitulo(capId).filterNotNull().first()
        cap = c
        val texto = repo.lerTexto(capId)
        val partes = withContext(Dispatchers.Default) { Texto.dividirEmPedacos(texto, 3000) }
        pedacos.clear()
        partes.forEach { pedacos.add(Pedaco(proximoId++, TextFieldValue(it))) }
        if (pedacos.isEmpty()) pedacos.add(Pedaco(proximoId++, TextFieldValue("")))
        palavras = c.palavras
        carregado = true
    }

    fun textoCompleto(): String {
        val sb = StringBuilder()
        pedacos.forEach { sb.append(it.valor.text) }
        return sb.toString()
    }

    // Salvamento automático: 1,5 s depois da última tecla (não grava a cada tecla).
    LaunchedEffect(carregado) {
        if (!carregado) return@LaunchedEffect
        snapshotFlow { versao }.debounce(1500).collect { v ->
            val c = cap ?: return@collect
            if (v == versaoSalva) return@collect
            estadoSalvo = "Salvando…"
            val t = textoCompleto()
            repo.gravarTexto(c, t)
            palavras = withContext(Dispatchers.Default) { Texto.contarPalavras(t) }
            versaoSalva = v
            estadoSalvo = "Salvo"
        }
    }

    // Garante a gravação ao sair da tela ou mandar o app para o fundo.
    val dono = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(dono) {
        fun salvarJa() {
            val c = cap ?: return
            if (versao == versaoSalva) return
            val t = textoCompleto()
            versaoSalva = versao
            app.escopo.launch { repo.gravarTexto(c, t) }
        }
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) salvarJa() }
        dono.lifecycle.addObserver(obs)
        onDispose { dono.lifecycle.removeObserver(obs); salvarJa() }
    }

    fun inserir(marca: String, envolver: Boolean) {
        val i = focado.takeIf { it in pedacos.indices } ?: pedacos.lastIndex
        val pd = pedacos[i]
        val v = pd.valor
        val sel = v.selection
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

    val familia = if (fonte == "SEM_SERIFA") FontFamily.SansSerif else FontFamily.Serif
    val estilo = TextStyle(fontFamily = familia, fontSize = 17.sp, lineHeight = 29.sp, color = p.texto)
    val realce = remember(p) { RealceEditor(p) }
    val lista = rememberLazyListState()
    var dividindo by remember { mutableStateOf(false) }

    // Capítulo automático: "Capítulo 2 — Título" ou "Prólogo" numa linha própria (depois de uma linha
    // vazia, ou no começo do texto) + Enter → o que vem abaixo vira um capítulo novo.
    fun verificarTitulo(i: Int, posNl: Int) {
        val c = cap ?: return
        val t = pedacos[i].valor.text
        val ini = t.lastIndexOf('\n', posNl - 1) + 1
        val linha = t.substring(ini, posNl)
        if (!Texto.ehTituloCapitulo(linha)) return
        val antes = StringBuilder()
        for (k in 0 until i) antes.append(pedacos[k].valor.text)
        antes.append(t, 0, ini)
        val textoAntes = antes.toString()
        val noComeco = textoAntes.isBlank()
        if (!noComeco && !textoAntes.trimEnd(' ', '\t').endsWith("\n\n")) return
        val depois = StringBuilder(t.substring(posNl + 1))
        for (k in i + 1 until pedacos.size) depois.append(pedacos[k].valor.text)
        val titulo = Texto.tituloLimpo(linha)
        if (noComeco) {
            // título no topo: só renomeia este capítulo
            val resto = depois.toString()
            pedacos.clear()
            Texto.dividirEmPedacos(resto, 3000).forEach { pedacos.add(Pedaco(proximoId++, TextFieldValue(it))) }
            if (pedacos.isEmpty()) pedacos.add(Pedaco(proximoId++, TextFieldValue("")))
            val novo = c.copy(titulo = titulo)
            cap = novo
            app.escopo.launch { repo.salvarCapitulo(novo) }
            versao++
            estadoSalvo = "Capítulo renomeado: $titulo"
        } else {
            dividindo = true
            versaoSalva = versao
            estadoSalvo = "Criando capítulo…"
            app.escopo.launch {
                val id = repo.dividirCapitulo(c, textoAntes.trimEnd() + "\n", titulo, depois.toString().trimStart('\n'))
                withContext(Dispatchers.Main) { nav.navigate("editor/$id") { popUpTo("editor/{id}") { inclusive = true } } }
            }
        }
    }

    fun aoMudar(i: Int, pd: Pedaco, novo: TextFieldValue) {
        if (dividindo) return
        val antigo = pd.valor
        if (novo.text != antigo.text) versao++
        pd.valor = novo
        if (novo.text.length == antigo.text.length + 1 && novo.selection.collapsed) {
            val pos = novo.selection.start - 1
            if (pos >= 0 && pos < novo.text.length && novo.text[pos] == '\n') verificarTitulo(i, pos)
        }
    }

    if (editandoResumo) cap?.let { c ->
        DialogoResumo(c, { editandoResumo = false }) { r ->
            editandoResumo = false
            val novo = c.copy(resumo = r.trim())
            cap = novo
            app.escopo.launch { repo.salvarCapitulo(novo) }
        }
    }
    Column(Modifier.fillMaxSize().background(p.fundo).imePadding()) {
        Topo(cap?.titulo?.ifEmpty { "Página em branco" } ?: "", nav) {
            Text(estadoSalvo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = if (estadoSalvo == "Salvo") Color(0xFF6FC2B4) else p.destaque, modifier = Modifier.padding(end = 4.dp))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Mais opções") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Ver como leitor") }, onClick = { menu = false; nav.navigate("leitor/$capId") })
                    DropdownMenuItem(text = { Text("Separar capítulos pelos títulos") }, onClick = {
                        menu = false
                        val c = cap ?: return@DropdownMenuItem
                        dividindo = true
                        versaoSalva = versao
                        val t = textoCompleto()
                        app.escopo.launch {
                            val n = repo.separarPorTitulos(c, t)
                            withContext(Dispatchers.Main) {
                                if (n == 0 && repo.dao.capituloAgora(c.id)?.titulo == c.titulo) { dividindo = false; estadoSalvo = "Nenhum título encontrado" }
                                else nav.navigate("editor/${c.id}") { popUpTo("editor/{id}") { inclusive = true } }
                            }
                        }
                    })
                    DropdownMenuItem(text = { Text("Resumo do capítulo") }, onClick = { menu = false; editandoResumo = true })
                    DropdownMenuItem(text = { Text("Histórico de versões") }, onClick = { menu = false; nav.navigate("historico/$capId") })
                    cap?.let { c ->
                        DropdownMenuItem(text = { Text(if (c.rascunho) "Marcar como concluído" else "Voltar para rascunho") }, onClick = {
                            menu = false
                            app.escopo.launch { repo.salvarCapitulo(c.copy(rascunho = !c.rascunho)); cap = c.copy(rascunho = !c.rascunho) }
                        })
                    }
                }
            }
        }
        Text("${"%,d".format(palavras).replace(',', '.')} palavras" + if (cap?.rascunho == true) " · rascunho" else " · concluído",
            fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(horizontal = 20.dp))
        if (!carregado) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp), color = p.destaque, trackColor = p.fundo)
        } else {
            LazyColumn(state = lista, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                itemsIndexed(pedacos, key = { _, x -> x.id }) { i, pd ->
                    BasicTextField(
                        value = pd.valor,
                        onValueChange = { novo -> aoMudar(i, pd, novo) },
                        textStyle = estilo,
                        cursorBrush = SolidColor(p.destaque),
                        visualTransformation = realce,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focado = i },
                    )
                }
                item { Spacer(Modifier.height(240.dp)) }
            }
            Row(
                Modifier.fillMaxWidth().background(p.superficie).padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
            ) {
                BotaoFerramenta("B", p, FontWeight.ExtraBold) { inserir("**", true) }
                BotaoFerramenta("I", p, estiloFonte = FontStyle.Italic) { inserir("*", true) }
                BotaoFerramenta("H", p) { inserir("\n# ", false) }
                BotaoFerramenta("—", p, cor = p.destaque) { inserir("— ", false) }
                BotaoFerramenta("“ ”", p) { inserir("“", false) }
                BotaoFerramenta("◆", p, cor = p.destaque) { inserir("\n\n* * *\n\n", false) }
            }
        }
    }
}

@Composable
private fun DialogoResumo(c: Capitulo, aoFechar: () -> Unit, aoSalvar: (String) -> Unit) {
    DialogoTexto("Resumo do capítulo", "O que acontece neste capítulo (2–4 frases)", c.resumo, multilinha = true, aoFechar = aoFechar, aoConfirmar = aoSalvar)
}

@Composable
private fun BotaoFerramenta(rotulo: String, p: Paleta, peso: FontWeight = FontWeight.Bold, estiloFonte: FontStyle = FontStyle.Normal, cor: Color = p.texto, acao: () -> Unit) {
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = acao), contentAlignment = Alignment.Center) {
        Text(rotulo, fontWeight = peso, fontStyle = estiloFonte, fontSize = 18.sp, color = cor, fontFamily = FontFamily.Serif)
    }
}

/** Colore o editor sem mudar o texto: falas (— ou “) com travessão em destaque, marcas ** * # discretas. */
private class RealceEditor(private val p: Paleta) : VisualTransformation {
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
                    if (c == '—' || c == '–' || c == '“' || (c == '-' && k + 1 < fim && t[k + 1] == ' ')) {
                        addStyle(SpanStyle(color = p.destaque, fontWeight = FontWeight.Bold), k, k + 1)
                    } else if (Texto.ehTituloCapitulo(t.substring(k, fim))) {
                        val esp = Texto.tipoEspecial(t.substring(k, fim)) != null
                        addStyle(SpanStyle(color = p.destaque, fontWeight = FontWeight.Bold, fontSize = 1.3.em,
                            fontStyle = if (esp) FontStyle.Italic else FontStyle.Normal, letterSpacing = if (esp) 0.08.em else 0.em), k, fim)
                    } else if (c == '#') {
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = p.destaque), k, fim)
                    } else if (t.startsWith("* * *", k)) {
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
        }
        return TransformedText(r, OffsetMapping.Identity)
    }
    override fun equals(other: Any?) = other is RealceEditor && other.p == p
    override fun hashCode() = p.hashCode()
}

// ───────── Histórico de versões ─────────

@Composable
fun TelaHistorico(capId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    var versoes by remember { mutableStateOf<List<ArquivosTexto.Versao>?>(null) }
    var confirmar by remember { mutableStateOf<ArquivosTexto.Versao?>(null) }
    LaunchedEffect(capId) { versoes = repo.textos.versoes(capId) }
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }

    Column(Modifier.fillMaxSize()) {
        Topo("Histórico de versões", nav)
        Text("Uma cópia é guardada automaticamente antes de salvar, no máximo a cada 10 minutos (até 30 versões).",
            color = p.textoSuave, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
        val v = versoes
        if (v == null) LinearProgressIndicator(Modifier.fillMaxWidth(), color = p.destaque, trackColor = p.fundo)
        else if (v.isEmpty()) Text("Ainda não há versões antigas deste capítulo.", color = p.textoSuave, modifier = Modifier.padding(20.dp))
        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(v) { ver ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.superficie).clickable { confirmar = ver }.padding(14.dp)) {
                    Text(fmt.format(Date(ver.quando)), fontWeight = FontWeight.Bold)
                    Text("${ver.palavras} palavras", fontSize = 12.sp, color = p.destaque)
                    Text(ver.inicio, fontSize = 13.sp, color = p.textoSuave, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    confirmar?.let { ver ->
        AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text("Restaurar esta versão?") },
            text = { Text("O texto atual também é guardado no histórico antes, então dá para voltar atrás.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = null
                    app.escopo.launch {
                        repo.textos.guardarVersaoSePreciso(capId, forcar = true)
                        val t = withContext(Dispatchers.IO) { ver.arquivo.readText(Charsets.UTF_8) }
                        repo.textos.gravar(capId, t)
                        repo.dao.atualizarPalavras(capId, Texto.contarPalavras(t), System.currentTimeMillis())
                        launch(Dispatchers.Main) { nav.navigate("editor/$capId") { popUpTo("editor/{id}") { inclusive = true } } }
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Cancelar") } },
        )
    }
}
