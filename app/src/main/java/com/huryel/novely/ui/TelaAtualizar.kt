package com.huryel.novely.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Diagnostico
import com.huryel.novely.data.EstadoAtualizacao
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ───────────────────────── Atualizações e problemas ─────────────────────────

@Composable
fun TelaAtualizar(nav: NavHostController) {
    val app = NovelyApp.instancia
    val at = app.atualizador
    val p = LocalPaleta.current
    val ctx = LocalContext.current
    val estado by at.estado.collectAsState()
    val auto by app.prefs.atualizarAuto.estado.collectAsState()
    var relatorio by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        relatorio = withContext(Dispatchers.IO) { Diagnostico.relatorio() }
        if (estado is EstadoAtualizacao.Parado || estado is EstadoAtualizacao.Erro || estado is EstadoAtualizacao.EmDia) at.verificar()
    }
    // volta da tela de permissão do Android: se já baixou, segue para instalar
    var pediuPermissao by remember { mutableStateOf(false) }
    val dono = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(dono) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME && pediuPermissao) {
                pediuPermissao = false
                (at.estado.value as? EstadoAtualizacao.Pronta)?.let { if (at.podeInstalar()) at.instalar(ctx, it.arquivo) }
            }
        }
        dono.lifecycle.addObserver(obs); onDispose { dono.lifecycle.removeObserver(obs) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Topo("Atualizações", nav)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.superficie).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("VERSÃO INSTALADA", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
                Text("Novely ${at.nomeInstalado}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                when (val e = estado) {
                    EstadoAtualizacao.Parado, EstadoAtualizacao.Verificando -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = p.destaque, strokeWidth = 2.dp)
                        Text("  Procurando versão nova…", color = p.textoSuave)
                    }
                    EstadoAtualizacao.EmDia -> {
                        Text("✓ Você está na versão mais nova.", color = p.destaque2, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(onClick = { app.escopo.launch { at.verificar() } }, modifier = Modifier.fillMaxWidth()) { Text("Procurar de novo") }
                    }
                    is EstadoAtualizacao.Erro -> {
                        Text(e.mensagem, color = Color(0xFFE57373))
                        OutlinedButton(onClick = { app.escopo.launch { at.verificar() } }, modifier = Modifier.fillMaxWidth()) { Text("Tentar de novo") }
                    }
                    is EstadoAtualizacao.Disponivel -> CartaoNovaVersao(e.v.numero, e.v.novidades, e.v.tamanho, "Baixar e instalar") {
                        app.escopo.launch { at.baixar(e.v) }
                    }
                    is EstadoAtualizacao.Baixando -> {
                        Text("Baixando build ${e.v.numero}… ${(e.progresso * 100).toInt()}%", fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator({ e.progresso }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = p.destaque, trackColor = p.borda)
                    }
                    is EstadoAtualizacao.Pronta -> CartaoNovaVersao(e.v.numero, e.v.novidades, 0, "Instalar agora") {
                        if (at.podeInstalar()) at.instalar(ctx, e.arquivo) else { pediuPermissao = true; at.abrirPermissao(ctx) }
                    }
                }
            }
            Text("Suas obras não são apagadas ao atualizar: a versão nova instala por cima.\nNa primeira vez, o Android pede para permitir que o Novely instale apps — é só ativar e voltar.",
                fontSize = 13.sp, color = p.textoSuave, lineHeight = 19.sp)
            LinhaInterruptor("Avisar quando tiver versão nova", auto) { app.prefs.atualizarAuto.definir(it) }

            HorizontalDivider(color = p.borda)
            Text("Problemas", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            val r = relatorio
            if (r == null) Text("Nenhum travamento ou fechamento registrado. O Novely anota sozinho se algo der errado.", color = p.textoSuave, fontSize = 14.sp)
            else {
                Text("O Novely registrou um problema. Mande o relatório para quem cuida do app — ele mostra exatamente onde travou.", fontSize = 14.sp, color = p.textoSuave)
                Text(r.take(900), fontSize = 11.sp, color = p.textoSuave, lineHeight = 14.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.superficie).padding(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { compartilharTexto(ctx, "Relatório do Novely", r) }, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.weight(1f)) { Text("Enviar relatório") }
                    OutlinedButton(onClick = { Diagnostico.apagar(); relatorio = null }, modifier = Modifier.weight(1f)) { Text("Apagar") }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun CartaoNovaVersao(numero: Int, novidades: String, tamanho: Long, botao: String, acao: () -> Unit) {
    val p = LocalPaleta.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("✦ Nova versão: build $numero" + if (tamanho > 0) " · ${tamanho / 1_000_000} MB" else "", color = p.destaque, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        if (novidades.isNotBlank()) Text(novidades.take(700), fontSize = 14.sp, lineHeight = 20.sp)
        Button(onClick = acao, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(botao, fontWeight = FontWeight.ExtraBold) }
    }
}

fun compartilharTexto(ctx: Context, assunto: String, texto: String) {
    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, assunto).putExtra(Intent.EXTRA_TEXT, texto)
    ctx.startActivity(Intent.createChooser(i, assunto))
}

// ───────────────────────── Avisos que aparecem em qualquer tela ─────────────────────────

/** Aviso de versão nova (no Início), relatório depois de travar/fechar e tratamento de "Abrir com Novely". */
@Composable
fun BoxScope.AvisosGlobais(nav: NavHostController, rota: String?) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val ctx = LocalContext.current
    val estado by app.atualizador.estado.collectAsState()

    // 1) arquivo recebido de outro app
    val recebido by app.arquivoRecebido.collectAsState()
    LaunchedEffect(recebido) {
        val uri = recebido ?: return@LaunchedEffect
        app.arquivoRecebido.value = null
        val tipo = withContext(Dispatchers.IO) { tipoRecebido(ctx, uri) }
        if (tipo == "NOVELY") { app.escopo.launch { app.backup.analisarRestauracao(uri) }; nav.navigate("backup") }
        else { app.escopo.launch { app.importador.analisar(uri) }; nav.navigate("importar") }
    }

    // 2) relatório de problema (uma vez por ocorrência)
    var problema by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val quando = withContext(Dispatchers.IO) { Diagnostico.ultimoProblemaEm() }
        if (quando > app.prefs.problemaVisto.valor) problema = withContext(Dispatchers.IO) { Diagnostico.relatorio() }
        if (quando > 0) app.prefs.problemaVisto.definir(quando)
    }
    problema?.let { r ->
        AlertDialog(
            onDismissRequest = { problema = null },
            title = { Text("O Novely teve um problema") },
            text = { Text("Da última vez o app travou ou fechou sozinho. Suas obras estão salvas. Se você mandar o relatório, dá para descobrir a causa e corrigir.") },
            confirmButton = { TextButton(onClick = { problema = null; compartilharTexto(ctx, "Relatório do Novely", r) }) { Text("Enviar relatório") } },
            dismissButton = { TextButton(onClick = { problema = null }) { Text("Agora não") } },
        )
    }

    // 3) versão nova disponível: janela uma vez por versão + faixa no Início
    val v = (estado as? EstadoAtualizacao.Disponivel)?.v
    var dispensada by remember { mutableStateOf(0) }
    val avisada by app.prefs.versaoAvisada.estado.collectAsState()
    if (v != null && avisada < v.numero) AlertDialog(
        onDismissRequest = { app.prefs.versaoAvisada.definir(v.numero.toLong()) },
        title = { Text("✦ Versão nova do Novely") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Build ${v.numero} está disponível. Atualizar não apaga suas obras.", fontWeight = FontWeight.SemiBold)
                if (v.novidades.isNotBlank()) Text(v.novidades.take(400), fontSize = 13.sp, color = p.textoSuave)
            }
        },
        confirmButton = { TextButton(onClick = { app.prefs.versaoAvisada.definir(v.numero.toLong()); nav.navigate("atualizar") }) { Text("Atualizar agora", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = { app.prefs.versaoAvisada.definir(v.numero.toLong()) }) { Text("Depois") } },
    )
    if (v != null && rota == "inicio" && dispensada != v.numero) {
        Row(
            Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .shadow(16.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(p.superficie)
                .border(1.dp, p.destaque.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .clickable { nav.navigate("atualizar") }.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("✦ Versão nova do Novely", fontWeight = FontWeight.ExtraBold, color = p.destaque, fontSize = 14.sp)
                Text("Toque para atualizar (build ${v.numero})", fontSize = 12.sp, color = p.textoSuave)
            }
            TextButton(onClick = { dispensada = v.numero }) { Text("Depois", color = p.textoSuave) }
        }
    }
}

/** "NOVELY" para obra publicada/backup; "TEXTO" para EPUB/TXT. */
private fun tipoRecebido(ctx: Context, uri: Uri): String {
    val nome = try { NovelyApp.instancia.importador.nomeDe(uri).lowercase() } catch (_: Exception) { "" }
    if (nome.endsWith(".novely")) return "NOVELY"
    if (nome.endsWith(".epub") || nome.endsWith(".txt") || nome.endsWith(".md")) return "TEXTO"
    val mime = ctx.contentResolver.getType(uri) ?: ""
    if ("epub" in mime || mime.startsWith("text/")) return "TEXTO"
    // zip sem nome conhecido: se tiver manifest.json, é do Novely
    return try {
        ctx.contentResolver.openInputStream(uri)?.use { ins ->
            val zip = java.util.zip.ZipInputStream(ins.buffered())
            var e = zip.nextEntry; var n = 0
            while (e != null && n < 4000) { if (e.name == "manifest.json") return@use "NOVELY"; if (e.name == "mimetype") return@use "TEXTO"; e = zip.nextEntry; n++ }
            "TEXTO"
        } ?: "TEXTO"
    } catch (_: Exception) { "TEXTO" }
}

// ───────────────────────── Publicar para leitores ─────────────────────────

@Composable
fun DialogoPublicar(obraId: Long, titulo: String, fechar: () -> Unit) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val ctx = LocalContext.current
    var autor by remember { mutableStateOf(app.prefs.autorNome.valor) }
    var rascunhos by remember { mutableStateOf(false) }
    var gerando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!gerando) fechar() },
        title = { Text("Publicar para leitores") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Gera um arquivo de “$titulo” para você mandar por WhatsApp, Telegram, Drive, e-mail… Quem abrir com o Novely pode ler, grifar e ouvir, mas não pode editar.",
                    fontSize = 14.sp, lineHeight = 20.sp)
                OutlinedTextField(autor, { autor = it }, label = { Text("Seu nome de autor") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { rascunhos = !rascunhos }) {
                    Checkbox(rascunhos, { rascunhos = it })
                    Text("Incluir capítulos em rascunho", fontSize = 14.sp)
                }
                Text("Não vão junto: suas notas, seu progresso e (se desmarcado) os rascunhos.", fontSize = 12.sp, color = p.textoSuave)
                if (gerando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = p.destaque, trackColor = p.borda)
                erro?.let { Text(it, color = Color(0xFFE57373), fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !gerando, onClick = {
                gerando = true; erro = null
                app.prefs.autorNome.definir(autor.trim())
                app.escopo.launch {
                    try {
                        val f = app.backup.publicar(obraId, autor, rascunhos)
                        withContext(Dispatchers.Main) {
                            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".arquivos", f)
                            val i = Intent(Intent.ACTION_SEND).setType("application/octet-stream").putExtra(Intent.EXTRA_STREAM, uri)
                                .putExtra(Intent.EXTRA_TEXT, "“$titulo”${if (autor.isNotBlank()) " por ${autor.trim()}" else ""} — abra com o app Novely para ler.")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            ctx.startActivity(Intent.createChooser(i, "Publicar “$titulo”"))
                            gerando = false; fechar()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { gerando = false; erro = "Não deu para gerar: ${e.message}" }
                    }
                }
            }) { Text("Gerar e enviar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = fechar, enabled = !gerando) { Text("Cancelar") } },
    )
}
