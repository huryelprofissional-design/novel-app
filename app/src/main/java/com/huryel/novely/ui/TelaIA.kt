package com.huryel.novely.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Capitulo
import com.huryel.novely.data.Ficha
import com.huryel.novely.data.Marcas
import com.huryel.novely.data.Prompts
import com.huryel.novely.data.TipoFicha
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estúdio de IA: escolhe o que pedir, monta o prompt com o contexto da obra, copia/abre no app de IA e traz a resposta de volta. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelaIA(obraId: Long, capInicial: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var tipo by remember { mutableStateOf(Prompts.TIPOS[0]) }
    var ideia by remember { mutableStateOf("") }
    var tom by remember { mutableStateOf(Prompts.TONS[0]) }
    var tamanho by remember { mutableIntStateOf(3000) }
    var ponto by remember { mutableStateOf(Prompts.PONTOS[0]) }
    var quantos by remember { mutableIntStateOf(3) }
    var caps by remember { mutableStateOf<List<Capitulo>>(emptyList()) }
    var capSel by remember { mutableLongStateOf(capInicial) }
    var prompt by remember { mutableStateOf("") }
    var montando by remember { mutableStateOf(true) }
    var verTudo by remember { mutableStateOf(false) }
    var resposta by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf("") }
    val obra by remember(obraId) { repo.obra(obraId) }.collectAsState(initial = null)

    LaunchedEffect(obraId) {
        caps = repo.dao.capitulosAgora(obraId)
        if (capSel <= 0) capSel = caps.lastOrNull { it.palavras > 0 }?.id ?: caps.lastOrNull()?.id ?: -1
    }
    // monta o prompt sempre que algo muda
    LaunchedEffect(obra, tipo, ideia, tom, tamanho, ponto, quantos, capSel, caps) {
        val o = obra ?: return@LaunchedEffect
        montando = true
        kotlinx.coroutines.delay(250)
        prompt = withContext(Dispatchers.IO) {
            val fichas = repo.dao.fichasAgora(obraId)
            val eventos = repo.eventos(obraId).first()
            val cap = caps.firstOrNull { it.id == capSel }
            val texto = cap?.let { repo.lerTexto(it.id) } ?: ""
            val semResumo = if (tipo.id == "RESUMOS") caps.filter { it.resumo.isBlank() && it.palavras > 0 }.take(40).map { it to repo.lerTexto(it.id) } else emptyList()
            Prompts.montar(Prompts.Contexto(o, caps, fichas, eventos, cap, texto, semResumo), Prompts.Opcoes(tipo.id, ideia, tom, tamanho, ponto, quantos))
        }
        montando = false
    }
    fun copiar(t: String) { (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("prompt", t)) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(bottom = 40.dp)) {
        Topo("", nav)
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Estúdio de IA", fontWeight = FontWeight.Black, fontSize = 30.sp)
            Text("Escolha o que pedir. O Novely monta um prompt com tudo da sua obra (estilo, fichas, linha do tempo, resumos e o capítulo) para colar no ChatGPT, Gemini ou Claude. Depois cole a resposta aqui embaixo.",
                fontSize = 13.sp, lineHeight = 19.sp, color = p.textoSuave)
            // tipos
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                Prompts.TIPOS.forEach { t ->
                    val sel = t.id == tipo.id
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                        .background(if (sel) Brush.linearGradient(listOf(p.destaque.copy(alpha = 0.28f), p.destaque.copy(alpha = 0.08f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f))))
                        .border(1.dp, if (sel) p.destaque else Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .clickable { tipo = t; resposta = ""; aviso = "" }.padding(14.dp)) {
                        Text(t.icone, fontSize = 20.sp)
                        Text(t.nome, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                        Text(t.desc, fontSize = 11.5.sp, lineHeight = 15.sp, color = p.textoSuave, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            // opções
            Column(Modifier.fillMaxWidth().vidro(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${tipo.icone}  ${tipo.nome}", fontWeight = FontWeight.Black, fontSize = 18.sp)
                if (tipo.usaCap && caps.isNotEmpty()) {
                    var abrir by remember { mutableStateOf(false) }
                    Box {
                        Text("Capítulo: ${caps.firstOrNull { it.id == capSel }?.titulo ?: "—"}  ▾", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.08f)).clickable { abrir = true }.padding(horizontal = 14.dp, vertical = 10.dp))
                        DropdownMenu(abrir, { abrir = false }, modifier = Modifier.heightIn(max = 380.dp)) {
                            caps.forEach { c -> DropdownMenuItem(text = { Text(c.titulo, maxLines = 1) }, onClick = { capSel = c.id; abrir = false }) }
                        }
                    }
                    if (tipo.id == "CONTINUAR") Text("Use o último capítulo escrito para a IA continuar de onde parou.", fontSize = 11.5.sp, color = p.textoSuave)
                }
                if (tipo.pedeIdeia) OutlinedTextField(ideia, { ideia = it }, placeholder = { Text(tipo.dicaIdeia) }, minLines = 2, modifier = Modifier.fillMaxWidth())
                if (tipo.id == "CONTINUAR" || tipo.id == "CENA") {
                    Rotulo("Tamanho")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Prompts.TAMANHOS.forEach { n -> Chip("${"%,d".format(n).replace(',', '.')} palavras", tamanho == n) { tamanho = n } } }
                    Rotulo("Tom")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Prompts.TONS.forEach { t -> Chip(t, tom == t) { tom = t } } }
                    Rotulo("Narrador")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Prompts.PONTOS.forEach { t -> Chip(t, ponto == t) { ponto = t } } }
                }
                if (tipo.id == "PERSONAGEM") {
                    Rotulo("Quantos")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(1, 3, 5).forEach { n -> Chip("$n", quantos == n) { quantos = n } } }
                }
            }
            // prompt
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF0E0E10)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PROMPT", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp, color = p.textoSuave, modifier = Modifier.weight(1f))
                    Text(if (montando) "montando…" else "${"%,d".format(prompt.length).replace(',', '.')} caracteres", fontSize = 11.sp, color = p.textoSuave)
                }
                Text(prompt, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp, color = Color(0xFFBDBDBD), maxLines = if (verTudo) Int.MAX_VALUE else 9,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).clickable { verTudo = !verTudo })
                Text(if (verTudo) "mostrar menos" else "ver o prompt inteiro", fontSize = 12.sp, color = p.destaque, modifier = Modifier.padding(top = 6.dp).clickable { verTudo = !verTudo })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotaoBranco("Copiar prompt", Modifier.weight(1f), enabled = !montando && prompt.isNotEmpty()) { copiar(prompt); aviso = "Copiado! Cole no app de IA." }
                Box(Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(29.dp)).background(p.destaque).clickable(enabled = !montando) {
                    copiar(prompt)
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, prompt), "Abrir no app de IA"))
                }, contentAlignment = Alignment.Center) { Text("Abrir no app de IA", fontWeight = FontWeight.Black, color = p.sobreDestaque) }
            }
            if (prompt.length > 60_000) Text("Prompt grande: alguns apps gratuitos cortam textos longos. Se der erro, escolha um capítulo menor.", fontSize = 11.5.sp, color = Color(0xFFE5A373))
            // resposta
            Text("Colar a resposta", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(top = 10.dp))
            OutlinedTextField(resposta, { resposta = it }, placeholder = { Text("Cole aqui o que a IA respondeu…") }, minLines = 6, maxLines = 14, modifier = Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString()?.let { resposta = it }
                }) { Text("📋 Colar") }
                val r = resposta.trim()
                when (tipo.id) {
                    "PERSONAGEM", "MUNDO" -> Button(onClick = {
                        escopo.launch {
                            val existentes = repo.dao.fichasAgora(obraId)
                            var n = 0
                            Marcas.encontrar(r, existentes.map { it.nome }).forEach { (m, paragrafo) ->
                                if (existentes.any { it.nome.equals(m.nome, true) }) return@forEach
                                val desc = (paragrafo ?: "").substringAfter(m.nome, paragrafo ?: "").trimStart(',', ' ', '—', '–', '-', ':', ';').let { d ->
                                    if (m.apelidos.isNotEmpty()) d.replace(Regex("^\\(.*?\\)\\s*[,:—–-]?\\s*"), "") else d }.trim()
                                repo.salvarFicha(Ficha(obraId = obraId, nome = m.nome, tipo = if (m.lugar) TipoFicha.LUGAR else TipoFicha.PERSONAGEM,
                                    apelidos = m.apelidos.joinToString(", "), descricao = desc))
                                n++
                            }
                            aviso = if (n == 0) "Não achei @Nome ou &Lugar novos na resposta." else "$n ficha(s) criada(s)!"
                        }
                    }, enabled = r.isNotEmpty(), colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque)) { Text("Criar fichas") }
                    "RESUMOS" -> Button(onClick = {
                        escopo.launch {
                            val mapa = Prompts.lerResumos(r, caps)
                            mapa.forEach { (id, res) -> repo.dao.capituloAgora(id)?.let { repo.salvarCapitulo(it.copy(resumo = res)) } }
                            caps = repo.dao.capitulosAgora(obraId)
                            aviso = if (mapa.isEmpty()) "Não reconheci os títulos. Peça para a IA manter os títulos iguais." else "${mapa.size} resumo(s) salvos — já aparecem no “Anteriormente…”."
                        }
                    }, enabled = r.isNotEmpty(), colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque)) { Text("Salvar resumos") }
                    "REVISAR", "DIALOGOS" -> {
                        var confirmar by remember { mutableStateOf(false) }
                        Button(onClick = { confirmar = true }, enabled = r.isNotEmpty() && capSel > 0, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque)) { Text("Trocar o texto do capítulo") }
                        if (confirmar) AlertDialog(onDismissRequest = { confirmar = false }, title = { Text("Trocar o texto?") },
                            text = { Text("O texto atual fica guardado no histórico de versões do capítulo e dá para voltar depois.") },
                            confirmButton = { TextButton(onClick = {
                                confirmar = false
                                escopo.launch {
                                    val limpo = r.substringBefore("\n===").trim()
                                    repo.dao.capituloAgora(capSel)?.let { repo.gravarTexto(it, limpo) }
                                    aviso = "Capítulo atualizado (a versão antiga está no histórico)."
                                }
                            }) { Text("Trocar") } }, dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } })
                    }
                    else -> {}
                }
                if (tipo.id in setOf("CONTINUAR", "CENA", "GANCHO")) Button(onClick = {
                    escopo.launch {
                        val linhas = r.lines()
                        val primeira = linhas.firstOrNull { it.isNotBlank() }?.trim()?.trim('#', '*', ' ') ?: ""
                        val ehTitulo = primeira.length < 90 && Regex("^(cap[ií]tulo|cena|pr[oó]logo|ep[ií]logo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(primeira)
                        val titulo = if (ehTitulo) primeira else if (tipo.id == "CENA") "Cena nova" else "Capítulo ${caps.size + 1}"
                        val corpo = if (ehTitulo) r.substringAfter(linhas.first { it.isNotBlank() }).trim() else r
                        val id = repo.novoCapitulo(obraId, titulo, null)
                        repo.dao.capituloAgora(id)?.let { repo.gravarTexto(it, corpo) }
                        nav.navigate("editor/$id")
                    }
                }, enabled = r.isNotEmpty(), colors = ButtonDefaults.buttonColors(if (tipo.id in setOf("CONTINUAR", "CENA")) p.destaque else p.superficie, p.sobreDestaque)) { Text("Criar capítulo novo") }
            }
            if (aviso.isNotEmpty()) Text(aviso, fontWeight = FontWeight.Bold, color = p.destaque)
            Text("Dica: personagens novos que a IA escrever com @Nome viram fichas sozinhos quando você cria o capítulo.", fontSize = 11.5.sp, color = p.textoSuave)
        }
    }
}

@Composable
private fun Rotulo(t: String) = Text(t.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp, color = LocalPaleta.current.textoSuave)
