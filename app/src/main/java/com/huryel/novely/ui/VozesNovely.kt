package com.huryel.novely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.data.VozNeural
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.forma
import kotlinx.coroutines.launch

/**
 * Vozes do Novely: vozes neurais que vêm do próprio app (baixadas uma vez, funcionam sem internet).
 * Cada uma pode virar o narrador; no Elenco elas aparecem para os personagens.
 */
@Composable
fun DialogoVozesNovely(narrador: String, velocidade: Float, usarComoNarrador: (String) -> Unit, fechar: () -> Unit) {
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val baixando by VozNeural.baixando.collectAsState()
    var versao by remember { mutableIntStateOf(0) }
    var erro by remember { mutableStateOf<String?>(null) }
    var testando by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = fechar, confirmButton = { TextButton(onClick = fechar) { Text("Pronto") } },
        title = { Text("✦ Vozes do Novely") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Vozes que soam mais humanas e funcionam sem internet. Cada uma é baixada uma vez só (Wi-Fi recomendado).",
                    fontSize = 12.5.sp, color = p.textoSuave)
                erro?.let { Text(it, fontSize = 12.sp, color = Color(0xFFFF9D9D)) }
                key(versao) {
                    LazyColumn(Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(VozNeural.VOZES, key = { it.id }) { v ->
                            val pronta = VozNeural.instalada(v)
                            val progresso = baixando[v.pacote] ?: baixando["espeak"]?.takeIf { !pronta && baixando.containsKey("espeak") }
                            val usando = narrador == v.id
                            Column(Modifier.fillMaxWidth().clip(forma(14.dp)).background(if (usando) p.destaque.copy(alpha = 0.14f) else p.superficie).padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text((if (v.feminina) "♀ " else "♂ ") + v.nome, fontWeight = FontWeight.Bold)
                                        Text(v.descricao + if (usando) " · narrador agora" else "", fontSize = 12.sp, color = p.textoSuave)
                                    }
                                    if (pronta) TextButton(onClick = {
                                        testando = v.id
                                        escopo.launch { try { VozNeural.testar(v, velocidade) } catch (e: Exception) { erro = "Não consegui tocar: ${e.message}" }; testando = "" }
                                    }, enabled = testando.isEmpty()) { Text(if (testando == v.id) "gerando…" else "▶ Testar") }
                                }
                                when {
                                    progresso != null -> {
                                        LinearProgressIndicator(progress = { progresso }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = p.destaque, trackColor = p.borda)
                                        Text("Baixando… ${(progresso * 100).toInt()}%", fontSize = 11.sp, color = p.textoSuave)
                                    }
                                    !pronta -> TextButton(onClick = {
                                        erro = null
                                        escopo.launch { if (VozNeural.baixar(v)) versao++ else erro = VozNeural.erroDownload }
                                    }, enabled = baixando.isEmpty()) { Text("⬇ Baixar · ${VozNeural.faltaMb(v)} MB", color = p.destaque, fontWeight = FontWeight.Bold) }
                                    !usando -> TextButton(onClick = { usarComoNarrador(v.id) }) { Text("Usar como narrador", color = p.destaque, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
                Text("As vozes baixadas também aparecem no 🎭 Elenco, para os personagens.", fontSize = 11.5.sp, color = p.textoSuave)
            }
        })
}
