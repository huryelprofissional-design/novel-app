package com.huryel.novely.ui

import com.huryel.novely.ui.theme.forma
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.EstadoImport
import com.huryel.novely.data.PreviaImport
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.launch

/** destino = id da obra para acrescentar capítulos, ou null para criar obra nova. */
@Composable
fun TelaImportar(nav: NavHostController, destino: Long?) {
    val app = NovelyApp.instancia
    val imp = app.importador
    val p = LocalPaleta.current
    val estado by imp.estado.collectAsState()

    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) app.escopo.launch { imp.analisar(uri) }
    }

    Column(Modifier.fillMaxSize()) {
        Topo(if (destino != null) "Importar capítulos" else "Importar e backup", nav)
        when (val e = estado) {
            EstadoImport.Ocioso -> Inicio(destino, seletor::launch, nav)
            is EstadoImport.Lendo -> Carregando(e.mensagem)
            is EstadoImport.Salvando -> Carregando("Salvando ${e.feito} de ${e.total} capítulos…", e.feito.toFloat() / e.total)
            is EstadoImport.Previa -> Previa(e.p, destino)
            is EstadoImport.Concluido -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Pronto!", style = MaterialTheme.typography.displaySmall)
                Text("${e.capitulos} capítulos importados.", color = p.textoSuave)
                Button(onClick = { imp.limpar(); nav.navigate("obra/${e.obraId}") { popUpTo("inicio") } },
                    colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Abrir obra", fontWeight = FontWeight.Bold)
                }
            }
            is EstadoImport.Erro -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Algo deu errado", style = MaterialTheme.typography.headlineSmall)
                Text(e.mensagem, color = p.textoSuave)
                Button(onClick = { imp.limpar() }, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque),
                    modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Tentar outro arquivo") }
            }
        }
    }
}

@Composable
private fun Inicio(destino: Long?, abrir: (Array<String>) -> Unit, nav: NavHostController) {
    val p = LocalPaleta.current
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Escolha um arquivo do celular. Você confere os capítulos antes de salvar.", color = p.textoSuave)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CartaoFormato("EPUB", "Livro digital", p.destaque, Modifier.weight(1f)) { abrir(arrayOf("application/epub+zip", "application/octet-stream")) }
            CartaoFormato("TXT", "Texto simples", Color(0xFF6FC2B4), Modifier.weight(1f)) { abrir(arrayOf("text/plain", "text/markdown", "application/octet-stream")) }
        }
        Text("Dica: em TXT, capítulos são detectados por linhas como “Capítulo 12”, “Chapter 12”, “Prólogo” ou “# Título”.",
            color = p.textoSuave, fontSize = 13.sp)
        if (destino == null) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = p.borda)
            Text("Backup", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { nav.navigate("backup") }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Backup e restauração") }
        }
    }
}

@Composable
private fun CartaoFormato(sigla: String, texto: String, cor: Color, modifier: Modifier, aoTocar: () -> Unit) {
    val p = LocalPaleta.current
    Column(
        modifier.height(100.dp).clip(forma(18.dp)).background(p.superficie)
            .border(1.dp, p.borda, forma(18.dp)).clickable(onClick = aoTocar).padding(14.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(sigla, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF1A1206),
            modifier = Modifier.clip(forma(6.dp)).background(cor).padding(horizontal = 6.dp, vertical = 2.dp))
        Spacer(Modifier.height(6.dp))
        Text("Importar", fontWeight = FontWeight.Bold)
        Text(texto, fontSize = 12.sp, color = p.textoSuave)
    }
}

@Composable
private fun Carregando(msg: String, progresso: Float? = null) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (progresso == null) LinearProgressIndicator(Modifier.fillMaxWidth(), color = p.destaque, trackColor = p.borda)
        else LinearProgressIndicator(progress = { progresso }, modifier = Modifier.fillMaxWidth(), color = p.destaque, trackColor = p.borda, drawStopIndicator = {})
        Text(msg, color = p.textoSuave)
    }
}

@Composable
private fun Previa(pr: PreviaImport, destino: Long?) {
    val app = NovelyApp.instancia
    val imp = app.importador
    val p = LocalPaleta.current
    var prefixo by remember { mutableStateOf(pr.prefixo) }
    val incluidos = pr.capitulos.count { it.incluir }
    val palavras = pr.capitulos.filter { it.incluir }.sumOf { it.palavras.toLong() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(pr.nomeArquivo, color = p.textoSuave, fontSize = 13.sp)
                if (destino == null) OutlinedTextField(pr.titulo, { imp.renomearObra(it) }, label = { Text("Título da obra") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Detectei $incluidos capítulos · ${formatarPalavras(palavras)} palavras", fontWeight = FontWeight.Bold)
                pr.avisos.forEach { a ->
                    Text("⚠ $a", fontSize = 13.sp, color = p.destaque,
                        modifier = Modifier.fillMaxWidth().clip(forma(12.dp)).background(p.superficie).padding(10.dp))
                }
                if (pr.tipo == "TXT") {
                    Text("DIVIDIR POR", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Automático", pr.modo == "AUTO") { app.escopo.launch { imp.analisar(pr.uri, "AUTO") } }
                        Chip("Não dividir", pr.modo == "UM") { app.escopo.launch { imp.analisar(pr.uri, "UM") } }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(prefixo, { prefixo = it }, label = { Text("Linhas que começam com…") }, singleLine = true, modifier = Modifier.weight(1f))
                        TextButton(onClick = { app.escopo.launch { imp.analisar(pr.uri, "PREFIXO", prefixo) } }, enabled = prefixo.isNotBlank()) { Text("Aplicar") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { imp.limpar() }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Cancelar") }
                    Button(onClick = { app.escopo.launch { imp.confirmar(destino) } }, enabled = incluidos > 0,
                        colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Importar $incluidos", fontWeight = FontWeight.Bold)
                    }
                }
                Text("Toque num capítulo para incluir ou deixar de fora.", fontSize = 12.sp, color = p.textoSuave)
            }
        }
        itemsIndexed(pr.capitulos) { i, c ->
            Row(
                Modifier.fillMaxWidth().clip(forma(12.dp)).background(if (c.incluir) p.superficie else Color.Transparent)
                    .clickable { imp.alternar(i) }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = c.incluir, onCheckedChange = { imp.alternar(i) },
                    colors = CheckboxDefaults.colors(checkedColor = p.destaque, checkmarkColor = p.sobreDestaque))
                Column(Modifier.weight(1f)) {
                    Text(c.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (c.incluir) p.texto else p.textoSuave)
                    Text("${formatarPalavras(c.palavras.toLong())} palavras", fontSize = 12.sp,
                        color = if (c.palavras > 80_000 || c.palavras < 30) p.destaque else p.textoSuave)
                }
            }
        }
    }
}
