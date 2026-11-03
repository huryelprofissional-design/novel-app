package com.huryel.novely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.AoVivo
import com.huryel.novely.data.Marcas
import com.huryel.novely.data.ParVivo
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Serifa
import com.huryel.novely.ui.theme.forma
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Sala de escrita ao vivo: cada um escreve num parágrafo, todo mundo vê na hora, com chat ao lado. */
@Composable
fun TelaAoVivo(pid: String, cid: String, nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val eu = app.conta.usuario.collectAsState().value?.uid ?: ""
    val meuNome = app.prefs.autorNome.valor.ifBlank { app.conta.usuario.value?.displayName ?: "Eu" }
    val pars by remember(pid, cid) { AoVivo.paragrafos(pid, cid) }.collectAsState(initial = null)
    val gente by remember(pid, cid) { AoVivo.gente(pid, cid) }.collectAsState(initial = emptyList())
    val chat by remember(pid, cid) { AoVivo.chat(pid, cid) }.collectAsState(initial = emptyList())
    var foco by remember { mutableStateOf<String?>(null) }
    var local by remember { mutableStateOf(TextFieldValue("")) }
    var mostrarChat by remember { mutableStateOf(false) }
    var vistos by remember { mutableIntStateOf(0) }
    var trazer by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // o capítulo do autor neste aparelho (só ele traz o texto para a obra)
    val capLocal by produceState<com.huryel.novely.data.Capitulo?>(null, cid) {
        value = app.repo.dao.capituloNuvem(cid)?.takeIf { c -> app.repo.dao.obraAgora(c.obraId)?.minha == true }
    }
    val titulo by produceState("Escrever junto", pid, cid) { value = AoVivo.abertas(pid).firstOrNull { it.cid == cid }?.titulo ?: "Escrever junto" }

    // presença: avisa que estou aqui (e em qual parágrafo) e sai ao fechar
    LaunchedEffect(foco) { while (true) { AoVivo.estouAqui(pid, cid, eu, meuNome, foco ?: ""); delay(20_000) } }
    DisposableEffect(Unit) { onDispose { foco?.let { AoVivo.soltar(pid, cid, it) }; AoVivo.sair(pid, cid, eu) } }
    // enquanto escrevo: mantém a trava e grava o texto (pouco depois de parar de digitar)
    LaunchedEffect(foco) { val f = foco ?: return@LaunchedEffect; while (true) { delay(10_000); AoVivo.travar(pid, cid, f, eu, meuNome) } }
    LaunchedEffect(foco, local.text) {
        val f = foco ?: return@LaunchedEffect
        delay(600)
        val atual = pars?.firstOrNull { it.id == f } ?: return@LaunchedEffect
        if (atual.texto != local.text) AoVivo.escrever(pid, cid, f, local.text, eu, meuNome)
    }

    fun largar() {
        val f = foco ?: return
        val atual = pars?.firstOrNull { it.id == f }
        if (local.text.isBlank() && (pars?.size ?: 0) > 1) AoVivo.apagar(pid, cid, f)
        else { if (atual?.texto != local.text) AoVivo.escrever(pid, cid, f, local.text, eu, meuNome); AoVivo.soltar(pid, cid, f) }
        foco = null
    }
    fun pegar(par: ParVivo) {
        if (par.travadoPara(eu)) { android.widget.Toast.makeText(ctx, "${par.nomeEditando} está escrevendo aqui", android.widget.Toast.LENGTH_SHORT).show(); return }
        if (foco == par.id) return
        largar()
        foco = par.id
        local = TextFieldValue(par.texto, TextRange(par.texto.length))
        AoVivo.travar(pid, cid, par.id, eu, meuNome)
    }
    fun mudou(par: ParVivo, v: TextFieldValue) {
        if (!v.text.contains('\n')) { local = v; return }
        // Enter: o resto vira parágrafo novo logo abaixo
        val partes = v.text.split('\n')
        local = TextFieldValue(partes.first(), TextRange(partes.first().length))
        AoVivo.escrever(pid, cid, par.id, partes.first(), eu, meuNome)
        AoVivo.soltar(pid, cid, par.id)
        val lista = pars.orEmpty()
        val prox = lista.getOrNull(lista.indexOfFirst { it.id == par.id } + 1)?.ordem
        var antes = par.ordem
        var ultimo = par.id
        partes.drop(1).forEach { t ->
            val id = AoVivo.inserir(pid, cid, antes, prox, t, eu, meuNome)
            antes = if (prox == null) antes + 1 else (antes + prox) / 2
            ultimo = id
        }
        foco = ultimo
        local = TextFieldValue(partes.last(), TextRange(partes.last().length))
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Topo(titulo, nav) {
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp), verticalAlignment = Alignment.CenterVertically) {
                gente.filter { it.online }.take(5).forEach { g ->
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(g.cor)), contentAlignment = Alignment.Center) {
                        Text(g.nome.take(1).uppercase(), fontWeight = FontWeight.ExtraBold, color = Color.Black, fontSize = 13.sp)
                    }
                }
            }
            val novas = (chat.size - vistos).coerceAtLeast(0)
            TextButton(onClick = { mostrarChat = !mostrarChat; vistos = chat.size }) { Text(if (novas > 0 && !mostrarChat) "💬 $novas" else "💬", color = p.destaque, fontWeight = FontWeight.Bold) }
            if (capLocal != null) TextButton(onClick = { largar(); trazer = true }) { Text("⤓ Trazer", color = p.destaque, fontWeight = FontWeight.Bold) }
        }
        val online = gente.filter { it.online && it.uid != eu }
        Text(if (online.isEmpty()) "Só você por aqui. Mande o convite de coautor para escreverem juntos." else "Aqui agora: " + online.joinToString { it.nome },
            fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(horizontal = 20.dp))
        val lista = pars
        if (lista == null) { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.destaque) }; return@Column }
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), rememberLazyListState(), contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(lista, key = { it.id }) { par ->
                    val travado = par.travadoPara(eu)
                    val quemEsta = gente.filter { it.online && it.uid != eu && it.par == par.id }
                    Row(Modifier.fillMaxWidth().clip(forma(8.dp)).background(if (foco == par.id) p.superficie else Color.Transparent)) {
                        Box(Modifier.width(3.dp).heightIn(min = 22.dp).fillMaxHeight().background(if (par.cor != 0L) Color(par.cor) else p.borda))
                        Column(Modifier.weight(1f).padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)) {
                            val estilo = TextStyle(fontFamily = Serifa, fontSize = 17.sp, lineHeight = 27.sp, color = if (travado) p.texto.copy(alpha = 0.55f) else p.texto)
                            if (foco == par.id) {
                                val fr = remember { FocusRequester() }
                                var teveFoco by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { fr.requestFocus() }
                                BasicTextField(local, { mudou(par, it) }, textStyle = estilo, cursorBrush = SolidColor(p.destaque),
                                    modifier = Modifier.fillMaxWidth().focusRequester(fr).onFocusChanged {
                                        if (it.isFocused) teveFoco = true else if (teveFoco && foco == par.id) largar()
                                    })
                            }
                            else Text(Marcas.limpar(par.texto).ifEmpty { " " }, style = estilo, modifier = Modifier.fillMaxWidth().clickable { pegar(par) })
                            if (travado) Text("✎ ${par.nomeEditando} está escrevendo…", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(AoVivo.corDe(par.editando)))
                            else if (quemEsta.isNotEmpty()) Text("👀 ${quemEsta.joinToString { it.nome }}", fontSize = 11.sp, color = p.textoSuave)
                        }
                    }
                }
                item {
                    TextButton(onClick = {
                        largar()
                        val id = AoVivo.inserir(pid, cid, lista.lastOrNull()?.ordem ?: 0.0, null, "", eu, meuNome)
                        foco = id; local = TextFieldValue("")
                    }) { Text("+ Novo parágrafo", color = p.destaque, fontWeight = FontWeight.Bold) }
                }
                item {
                    Text("Toque num parágrafo para escrever nele. A cor na lateral mostra quem escreveu por último. Enter cria um parágrafo novo.",
                        fontSize = 11.sp, color = p.textoSuave, fontStyle = FontStyle.Italic)
                }
            }
            if (mostrarChat) ChatAoVivo(chat, eu, Modifier.align(Alignment.BottomCenter)) { AoVivo.mandar(pid, cid, eu, meuNome, it) }
        }
    }
    if (trazer) AlertDialog(onDismissRequest = { trazer = false },
        title = { Text("Trazer para a obra?") },
        text = { Text("O texto desta sala vira o texto do capítulo. A versão de antes fica no histórico de versões do capítulo.") },
        confirmButton = {
            TextButton(onClick = {
                trazer = false
                val c = capLocal ?: return@TextButton
                escopo.launch {
                    val t = AoVivo.textoFinal(pid, cid)
                    app.repo.gravarTexto(c, t)
                    android.widget.Toast.makeText(ctx, "Capítulo atualizado com o texto da sala", android.widget.Toast.LENGTH_SHORT).show()
                }
            }) { Text("Trazer", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = { trazer = false }) { Text("Cancelar") } })
}

@Composable
private fun ChatAoVivo(msgs: List<com.huryel.novely.data.MsgVivo>, eu: String, modifier: Modifier, mandar: (String) -> Unit) {
    val p = LocalPaleta.current
    var texto by remember { mutableStateOf("") }
    val estado = rememberLazyListState()
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) estado.animateScrollToItem(msgs.size - 1) }
    Column(modifier.fillMaxWidth().heightIn(max = 380.dp).padding(10.dp).clip(forma(20.dp)).background(p.superficie).padding(12.dp)) {
        Text("Chat da sala", fontWeight = FontWeight.ExtraBold)
        LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 260.dp), estado, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (msgs.isEmpty()) item { Text("Combine a cena por aqui.", color = p.textoSuave, fontSize = 13.sp) }
            items(msgs, key = { it.id }) { m ->
                val meu = m.uid == eu
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (meu) Alignment.End else Alignment.Start) {
                    if (!meu) Text(m.nome, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(AoVivo.corDe(m.uid)))
                    Text(m.texto, fontSize = 14.sp, color = if (meu) p.sobreDestaque else p.texto,
                        modifier = Modifier.clip(forma(14.dp)).background(if (meu) p.destaque else p.borda).padding(horizontal = 12.dp, vertical = 7.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(texto, { texto = it }, placeholder = { Text("Mensagem") }, singleLine = true, modifier = Modifier.weight(1f))
            TextButton(onClick = { val t = texto.trim(); if (t.isNotEmpty()) { mandar(t); texto = "" } }) { Text("Enviar", color = p.destaque, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Autor abre (ou reabre) a sala do capítulo e entra nela. */
@Composable
fun TelaAbrirAoVivo(capId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(capId) {
        try {
            delay(700) // o editor termina de salvar
            val c = app.repo.dao.capituloAgora(capId) ?: error("capítulo não encontrado")
            val o = app.repo.dao.obraAgora(c.obraId) ?: error("obra não encontrada")
            if (!o.minha) error("só o autor abre a sala")
            app.colab.garantir(o)
            AoVivo.abrir(o.nuvemId, c.nuvemId, c.titulo, o.titulo, app.repo.lerTexto(c.id), app.colab.meuUid)
            nav.navigate("aovivo/${o.nuvemId}/${c.nuvemId}") { popUpTo("vivo/$capId") { inclusive = true } }
        } catch (e: Exception) { erro = "Não deu para abrir a sala: ${e.message}" }
    }
    Column(Modifier.fillMaxSize()) {
        Topo("Escrever junto", nav)
        Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (erro == null) { CircularProgressIndicator(color = p.destaque); Text("Abrindo a sala…", color = p.textoSuave) }
            else Text(erro!!, color = p.textoSuave)
            Text("Quem for coautor da obra vê a sala em Escrever › Colaborar e escreve com você na hora.", fontSize = 12.sp, color = p.textoSuave)
        }
    }
}

/** Salas abertas de uma obra (para quem colabora). */
@Composable
fun SalasAoVivo(pid: String, nav: NavHostController, autor: Boolean) {
    val p = LocalPaleta.current
    var salas by remember { mutableStateOf<List<com.huryel.novely.data.SalaVivo>>(emptyList()) }
    var versao by remember { mutableIntStateOf(0) }
    val escopo = rememberCoroutineScope()
    LaunchedEffect(pid, versao) { salas = AoVivo.abertas(pid) }
    if (salas.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("● AO VIVO AGORA", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF4D4D))
        salas.forEach { s ->
            Row(Modifier.fillMaxWidth().clip(forma(14.dp)).background(p.superficie).clickable { nav.navigate("aovivo/${s.pid}/${s.cid}") }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.titulo, fontWeight = FontWeight.Bold)
                    Text("Toque para entrar e escrever junto", fontSize = 12.sp, color = p.textoSuave)
                }
                if (autor) TextButton(onClick = { escopo.launch { AoVivo.fechar(s.pid, s.cid); versao++ } }) { Text("Fechar sala", color = p.textoSuave) }
                else Text("›", fontSize = 22.sp, color = p.textoSuave)
            }
        }
    }
}
