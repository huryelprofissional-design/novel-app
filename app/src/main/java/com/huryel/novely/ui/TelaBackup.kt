package com.huryel.novely.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Backup
import com.huryel.novely.data.EstadoBackup
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelaBackup(nav: NavHostController) {
    val app = NovelyApp.instancia
    val bk = app.backup
    val prefs = app.prefs
    val p = LocalPaleta.current
    val ctx = LocalContext.current
    val estado by bk.estado.collectAsState()
    val ultimo by prefs.ultimoBackup.estado.collectAsState()
    val auto by prefs.backupAuto.estado.collectAsState()
    val pasta by prefs.pastaBackup.estado.collectAsState()
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }

    val criar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) app.escopo.launch { bk.exportarBackup(uri) }
    }
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) app.escopo.launch { bk.analisarRestauracao(uri) }
    }
    val escolherPasta = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            prefs.pastaBackup.definir(uri.toString())
            prefs.backupAuto.definir(true)
            prefs.ultimoBackup.definir(0L)
            app.escopo.launch { bk.backupAutomaticoSePreciso() }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Topo("Backup e restauração", nav)
        when (val e = estado) {
            is EstadoBackup.Trabalhando -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (e.progresso == null) LinearProgressIndicator(Modifier.fillMaxWidth(), color = p.destaque, trackColor = p.borda)
                else LinearProgressIndicator(progress = { e.progresso }, modifier = Modifier.fillMaxWidth(), color = p.destaque, trackColor = p.borda, drawStopIndicator = {})
                Text(e.mensagem, color = p.textoSuave)
            }
            is EstadoBackup.PreviaRestauracao -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Backup de ${fmt.format(Date(e.criadoEm))}", fontWeight = FontWeight.Bold)
                        Text("Escolha o que restaurar. Nada que já está no aparelho é apagado ou sobrescrito: cada obra entra como uma obra nova. As que parecem repetidas vêm desmarcadas.",
                            fontSize = 13.sp, color = p.textoSuave)
                    }
                }
                items(e.obras, key = { it.indice }) { o ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.superficie).clickable { bk.alternar(o.indice) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(o.incluir, { bk.alternar(o.indice) }, colors = CheckboxDefaults.colors(checkedColor = p.destaque, checkmarkColor = p.sobreDestaque))
                        Column(Modifier.weight(1f)) {
                            Text(o.titulo, fontWeight = FontWeight.SemiBold)
                            Text("${o.capitulos} capítulos · ${formatarPalavras(o.palavras)} palavras" + if (o.jaExiste) " · já existe no aparelho" else "",
                                fontSize = 12.sp, color = if (o.jaExiste) p.destaque else p.textoSuave)
                        }
                    }
                }
                item {
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { bk.limpar() }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Cancelar") }
                        Button(onClick = { app.escopo.launch { bk.restaurar() } }, enabled = e.obras.any { it.incluir },
                            colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.weight(1f).height(52.dp)) {
                            Text("Restaurar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            else -> Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (e is EstadoBackup.Feito || e is EstadoBackup.Erro) {
                    val ok = e is EstadoBackup.Feito
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (ok) Color(0x221F8A70) else Color(0x22E57373)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text((if (ok) "✓ " else "⚠ ") + (if (e is EstadoBackup.Feito) e.mensagem else (e as EstadoBackup.Erro).mensagem),
                            modifier = Modifier.weight(1f), fontSize = 14.sp)
                        TextButton(onClick = { bk.limpar() }) { Text("OK") }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.superficie).border(1.dp, p.borda, RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(if (ultimo > 0) "Último backup: ${fmt.format(Date(ultimo))}" else "Você ainda não fez nenhum backup",
                        fontWeight = FontWeight.Bold, color = if (ultimo > 0) p.texto else p.destaque)
                    Text("O backup é um arquivo .novely com todas as obras, capítulos, capas e progresso. Guarde no Drive, no PC ou num pendrive.",
                        fontSize = 13.sp, color = p.textoSuave)
                    Button(onClick = { criar.launch(Backup.nomeArquivo()) }, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque),
                        modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Fazer backup agora", fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = { abrir.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Restaurar de um arquivo…") }
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.superficie).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LinhaInterruptor("Backup automático diário", auto && pasta.isNotEmpty()) { ligar ->
                        if (ligar && pasta.isEmpty()) escolherPasta.launch(null) else prefs.backupAuto.definir(ligar)
                    }
                    Text(
                        if (pasta.isEmpty()) "Escolha uma pasta (pode ser do Google Drive, se o app do Drive estiver instalado). Ao abrir o Novely, se passou um dia, um backup é salvo lá. Os 5 mais recentes são mantidos."
                        else "Pasta escolhida. Os 5 backups automáticos mais recentes ficam guardados lá.",
                        fontSize = 12.sp, color = p.textoSuave,
                    )
                    if (pasta.isNotEmpty()) TextButton(onClick = { escolherPasta.launch(null) }) { Text("Trocar pasta") }
                }
                Text("Para exportar uma obra só (EPUB, TXT ou backup), abra a obra e use o menu ⋮.", fontSize = 12.sp, color = p.textoSuave)
            }
        }
    }
}
