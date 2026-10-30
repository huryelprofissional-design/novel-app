package com.huryel.novely.ui

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.CapRevisao
import com.huryel.novely.data.Marcas
import com.huryel.novely.data.Membro
import com.huryel.novely.data.Nota
import com.huryel.novely.data.Sugestao
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.launch

private fun nomePapel(p: String) = if (p == "COAUTOR") "Coautor" else "Revisor"

// ───────────────────────── AUTOR: colaboração da obra ─────────────────────────

@Composable
fun TelaColaboracao(obraId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val colab = app.colab
    val repo = app.repo
    val p = LocalPaleta.current
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    val obra by remember(obraId) { repo.obra(obraId) }.collectAsState(initial = null)
    val caps by remember(obraId) { repo.capitulos(obraId) }.collectAsState(initial = emptyList())
    val o = obra ?: run { Box(Modifier.fillMaxSize()); return }
    var membros by remember { mutableStateOf<List<Membro>>(emptyList()) }
    var revisao by remember { mutableStateOf<List<CapRevisao>>(emptyList()) }
    var sugestoes by remember { mutableStateOf<List<Sugestao>>(emptyList()) }
    var codigo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var enviar by remember { mutableStateOf(false) }
    var verSug by remember { mutableStateOf<Sugestao?>(null) }
    var trabalhando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    LaunchedEffect(o.nuvemId, recarga) {
        try {
            membros = colab.membros(o.nuvemId); revisao = colab.capitulosRevisao(o.nuvemId); sugestoes = colab.sugestoes(o.nuvemId)
        } catch (e: Exception) { if (e.message?.contains("PERMISSION_DENIED") != true) erro = "Sem conexão." }
    }
    fun rodar(acao: suspend () -> Unit) { trabalhando = true; erro = null; escopo.launch { try { acao() } catch (e: Exception) { erro = e.message }; trabalhando = false; recarga++ } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item { Topo("Colaboração", nav) }
        item {
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Convide alguém para revisar ou escrever junto. Ninguém mexe no seu texto: revisores deixam notas; coautores mandam capítulos que você aceita ou não.",
                    fontSize = 13.sp, color = p.textoSuave, lineHeight = 19.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { rodar { codigo = colab.convidar(o, "REVISOR") to "REVISOR" } }, enabled = !trabalhando, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), shape = RoundedCornerShape(14.dp)) { Text("+ Revisor", fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = { rodar { codigo = colab.convidar(o, "COAUTOR") to "COAUTOR" } }, enabled = !trabalhando, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)) { Text("+ Coautor", color = p.texto, fontWeight = FontWeight.Bold) }
                }
                erro?.let { Text(it, color = Color(0xFFE57373), fontSize = 13.sp) }
            }
        }
        item { Titulo2("Pessoas (${membros.size})") }
        if (membros.isEmpty()) item { Text("Ninguém ainda.", color = p.textoSuave, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 18.dp)) }
        items(membros, key = { it.uid }) { m ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.nome, fontWeight = FontWeight.Bold)
                    Text(nomePapel(m.papel), fontSize = 12.sp, color = if (m.papel == "COAUTOR") p.destaque else p.destaque2)
                }
                TextButton(onClick = { rodar { colab.remover(o.nuvemId, m.uid) } }) { Text("Remover", color = p.textoSuave) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 22.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Em revisão", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { enviar = true }) { Text("+ Enviar capítulos", color = p.destaque, fontWeight = FontWeight.Bold) }
            }
        }
        if (revisao.isEmpty()) item { Text("Mande capítulos (até rascunhos) para quem você convidou ler e anotar.", color = p.textoSuave, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 18.dp)) }
        items(revisao, key = { "r" + it.id }) { c ->
            Row(Modifier.fillMaxWidth().clickable { nav.navigate("revisao/${o.nuvemId}/${c.id}") }.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(c.titulo, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (c.notas > 0) "✎ ${c.notas} nota(s)" else "sem notas ainda", fontSize = 12.sp, color = if (c.notas > 0) p.destaque else p.textoSuave)
                }
                TextButton(onClick = { rodar { colab.tirarRevisao(o.nuvemId, c.id) } }) { Text("Tirar", color = p.textoSuave) }
            }
        }
        item { Titulo2("Capítulos sugeridos (${sugestoes.size})") }
        if (sugestoes.isEmpty()) item { Text("Coautores mandam capítulos aqui. Você lê e decide.", color = p.textoSuave, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 18.dp)) }
        items(sugestoes, key = { "s" + it.id }) { s ->
            Column(Modifier.fillMaxWidth().clickable { verSug = s }.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(s.titulo.ifBlank { "Sem título" }, fontWeight = FontWeight.SemiBold)
                Text("de ${s.nome} · ${ArquivosPalavras(s.texto)} palavras", fontSize = 12.sp, color = p.textoSuave)
            }
        }
    }

    codigo?.let { (c, papel) ->
        AlertDialog(onDismissRequest = { codigo = null }, title = { Text("Convite de ${nomePapel(papel).lowercase()}") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Mande este código para a pessoa. No Novely dela: Escrever › Colaborar › Entrar com código.", fontSize = 13.sp, color = p.textoSuave)
                    Text(c, fontWeight = FontWeight.Black, fontSize = 36.sp, letterSpacing = 6.sp, color = p.destaque, modifier = Modifier.padding(top = 16.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "Te convidei como ${nomePapel(papel).lowercase()} de “${o.titulo}” no Novely. Código: $c\n(Escrever › Colaborar › Entrar com código)"), "Enviar convite"))
                }) { Text("Compartilhar", fontWeight = FontWeight.Bold) }
            }, dismissButton = { TextButton(onClick = { codigo = null }) { Text("Fechar") } })
    }
    if (enviar) {
        val marcados = remember { mutableStateListOf<Long>() }
        AlertDialog(onDismissRequest = { enviar = false }, title = { Text("Enviar para revisão") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(caps, key = { it.id }) { c ->
                        Row(Modifier.fillMaxWidth().clickable { if (c.id in marcados) marcados.remove(c.id) else marcados.add(c.id) }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(c.id in marcados, null)
                            Text(c.titulo.ifEmpty { "Sem título" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { enviar = false; val l = caps.filter { it.id in marcados }; rodar { colab.enviarRevisao(o, l) } }, enabled = marcados.isNotEmpty()) { Text("Enviar ${marcados.size}") } },
            dismissButton = { TextButton(onClick = { enviar = false }) { Text("Cancelar") } })
    }
    verSug?.let { s ->
        AlertDialog(onDismissRequest = { verSug = null }, title = { Text(s.titulo.ifBlank { "Sugestão" }) },
            text = { LazyColumn(Modifier.heightIn(max = 460.dp)) { item { Text("de ${s.nome}", color = p.destaque, fontSize = 12.sp); Text(Marcas.limpar(s.texto), fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp)) } } },
            confirmButton = { TextButton(onClick = { verSug = null; rodar { val id = colab.aceitar(o, s); nav.navigate("editor/$id") } }) { Text("Virar capítulo", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { verSug = null; rodar { colab.descartar(o.nuvemId, s) } }) { Text("Descartar", color = Color(0xFFE57373)) } })
    }
}

private fun ArquivosPalavras(t: String) = com.huryel.novely.data.ArquivosTexto.contarPalavras(t)

@Composable
private fun Titulo2(t: String) {
    Text(t, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 6.dp))
}

// ───────────────────────── MEMBRO: minhas colaborações ─────────────────────────

@Composable
fun TelaColaborar(nav: NavHostController) {
    val app = NovelyApp.instancia
    val colab = app.colab
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    var lista by remember { mutableStateOf(colab.minhas()) }
    var codigo by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item { Topo("Colaborar", nav) }
        item {
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Recebeu um código de convite? Digite aqui para revisar ou escrever junto com outro autor.", fontSize = 13.sp, color = p.textoSuave)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(codigo, { codigo = it.uppercase().take(6) }, placeholder = { Text("ABC123") }, singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    Button(onClick = {
                        ocupado = true; erro = null
                        escopo.launch { erro = colab.entrar(codigo); ocupado = false; if (erro == null) { codigo = ""; lista = colab.minhas() } }
                    }, enabled = codigo.length == 6 && !ocupado, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.height(54.dp)) { Text("Entrar") }
                }
                erro?.let { Text(it, color = Color(0xFFE57373), fontSize = 13.sp) }
            }
        }
        item { Titulo2("Obras onde colaboro") }
        if (lista.isEmpty()) item { Text("Nenhuma ainda.", color = p.textoSuave, modifier = Modifier.padding(horizontal = 18.dp)) }
        items(lista, key = { it.pid }) { m ->
            Row(Modifier.fillMaxWidth().clickable { nav.navigate("colab/${m.pid}") }.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.titulo, fontWeight = FontWeight.Bold)
                    Text("${nomePapel(m.papel)} · obra de ${m.autor}", fontSize = 12.sp, color = p.textoSuave)
                }
                Text("›", fontSize = 22.sp, color = p.textoSuave)
            }
        }
    }
}

@Composable
fun TelaColabObra(pid: String, nav: NavHostController) {
    val app = NovelyApp.instancia
    val colab = app.colab
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val minha = colab.minhas().firstOrNull { it.pid == pid }
    var caps by remember { mutableStateOf<List<CapRevisao>?>(null) }
    var sugerir by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var sairDlg by remember { mutableStateOf(false) }
    LaunchedEffect(pid) { caps = try { colab.capitulosRevisao(pid) } catch (e: Exception) { erro = "Sem acesso (o autor pode ter te removido)."; emptyList() } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item { Topo(minha?.titulo ?: "Colaboração", nav) }
        item {
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Você é ${nomePapel(minha?.papel ?: "REVISOR").lowercase()} desta obra de ${minha?.autor ?: "outro autor"}. Toque num capítulo para ler; segure um parágrafo para anotar.",
                    fontSize = 13.sp, color = p.textoSuave)
                if (minha?.papel == "COAUTOR") Button(onClick = { sugerir = true }, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text("✎ Escrever capítulo para sugerir", fontWeight = FontWeight.Bold) }
                erro?.let { Text(it, color = Color(0xFFE57373), fontSize = 13.sp) }
            }
        }
        item { Titulo2("Capítulos para revisar") }
        val l = caps
        if (l == null) item { CircularProgressIndicator(color = p.destaque, modifier = Modifier.padding(18.dp)) }
        else if (l.isEmpty()) item { Text("O autor ainda não mandou capítulos.", color = p.textoSuave, modifier = Modifier.padding(horizontal = 18.dp)) }
        else items(l, key = { it.id }) { c ->
            Row(Modifier.fillMaxWidth().clickable { nav.navigate("revisao/$pid/${c.id}") }.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(c.titulo, fontWeight = FontWeight.SemiBold)
                    Text("${c.notas} nota(s)", fontSize = 12.sp, color = p.textoSuave)
                }
            }
        }
        item { TextButton(onClick = { sairDlg = true }, modifier = Modifier.padding(12.dp)) { Text("Sair desta colaboração", color = Color(0xFFE57373)) } }
    }
    if (sugerir) {
        var titulo by remember { mutableStateOf("") }
        var texto by remember { mutableStateOf("") }
        var enviando by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { if (!enviando) sugerir = false }, title = { Text("Sugerir capítulo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(texto, { texto = it }, label = { Text("Texto (pode colar)") }, minLines = 8, maxLines = 14, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { enviando = true; escopo.launch { try { colab.sugerir(pid, titulo, texto); sugerir = false } catch (e: Exception) { erro = e.message }; enviando = false } },
                enabled = texto.isNotBlank() && !enviando) { Text("Enviar ao autor") } },
            dismissButton = { TextButton(onClick = { sugerir = false }) { Text("Cancelar") } })
    }
    if (sairDlg) DialogoConfirmar("Sair?", "Você deixa de ver os capítulos desta obra.", "Sair", { sairDlg = false }) {
        sairDlg = false; escopo.launch { colab.sair(pid); nav.popBackStack() }
    }
}

// ───────────────────────── Ler capítulo em revisão e anotar ─────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TelaRevisaoCap(pid: String, cid: String, nav: NavHostController) {
    val app = NovelyApp.instancia
    val colab = app.colab
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    var titulo by remember { mutableStateOf("") }
    var paragrafos by remember { mutableStateOf<List<String>?>(null) }
    var notas by remember { mutableStateOf<List<Nota>>(emptyList()) }
    var anotar by remember { mutableStateOf<String?>(null) }
    var verNotas by remember { mutableStateOf(false) }
    suspend fun carregarNotas() { notas = try { colab.notas(pid, cid) } catch (_: Exception) { emptyList() } }
    LaunchedEffect(pid, cid) {
        try {
            val c = colab.capitulosRevisao(pid).firstOrNull { it.id == cid } ?: run { paragrafos = emptyList(); return@LaunchedEffect }
            titulo = c.titulo
            paragrafos = Marcas.limpar(colab.textoRevisao(pid, c)).split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            carregarNotas()
        } catch (_: Exception) { paragrafos = emptyList() }
    }
    val comNota = notas.map { it.trecho }.toSet()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
            item { Topo(titulo, nav) }
            item { Text("Segure um parágrafo para deixar uma nota.", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) }
            val l = paragrafos
            if (l == null) item { CircularProgressIndicator(color = p.destaque, modifier = Modifier.padding(22.dp)) }
            else itemsIndexed(l) { _, par ->
                Text(par, fontFamily = p.fonteLeitura, fontSize = 17.sp, lineHeight = 27.sp,
                    modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { anotar = par })
                        .background(if (par.take(600) in comNota) p.destaque.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(horizontal = 22.dp, vertical = 7.dp))
            }
        }
        ExtendedFloatingActionButton(onClick = { verNotas = true }, containerColor = p.destaque, contentColor = p.sobreDestaque,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(18.dp)) { Text("✎ ${notas.size} nota(s)", fontWeight = FontWeight.Bold) }
    }
    anotar?.let { trecho ->
        var texto by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { anotar = null }, title = { Text("Nota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("“${trecho.take(200)}${if (trecho.length > 200) "…" else ""}”", fontSize = 13.sp, color = p.textoSuave)
                    OutlinedTextField(texto, { texto = it }, placeholder = { Text("Ex.: essa fala ficou estranha…") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { val t = texto; anotar = null; escopo.launch { try { colab.anotar(pid, cid, trecho, t); carregarNotas() } catch (_: Exception) {} } }, enabled = texto.isNotBlank()) { Text("Salvar nota") } },
            dismissButton = { TextButton(onClick = { anotar = null }) { Text("Cancelar") } })
    }
    if (verNotas) ModalBottomSheet(onDismissRequest = { verNotas = false }, containerColor = p.superficie) {
        LazyColumn(Modifier.fillMaxHeight(0.8f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Notas", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
            if (notas.isEmpty()) item { Text("Nenhuma nota ainda.", color = p.textoSuave) }
            items(notas, key = { it.id }) { n ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.fundo).border(1.dp, p.borda, RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Text(n.nome, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = p.destaque)
                    if (n.trecho.isNotBlank()) Text("“${n.trecho.take(160)}${if (n.trecho.length > 160) "…" else ""}”", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 4.dp))
                    Text(n.texto, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
                    Text("apagar", fontSize = 11.sp, color = p.textoSuave, textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clickable { escopo.launch { try { colab.apagarNota(pid, cid, n); carregarNotas() } catch (_: Exception) {} } })
                }
            }
        }
    }
}
