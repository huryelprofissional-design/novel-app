package com.huryel.novely.ui

import com.huryel.novely.ui.theme.forma
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Ficha
import com.huryel.novely.data.ItemLixeira
import com.huryel.novely.data.TipoFicha
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun nomeTipo(t: String) = when (t) { TipoFicha.LUGAR -> "Lugar"; TipoFicha.OUTRO -> "Outro"; else -> "Personagem" }
private fun simboloTipo(t: String) = when (t) { TipoFicha.LUGAR -> "⌂"; TipoFicha.OUTRO -> "✦"; else -> "☺" }

/** Fichas de personagens, lugares e outros elementos de uma obra. */
@Composable
fun TelaFichas(obraId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    val fichas by remember(obraId) { repo.fichas(obraId) }.collectAsState(initial = emptyList())
    var filtro by remember { mutableStateOf("") }
    var editando by remember { mutableStateOf<Ficha?>(null) }
    var aberta by remember { mutableStateOf<Long?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Topo("Personagens e lugares", nav)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Todos · ${fichas.size}", filtro == "") { filtro = "" }
                Chip("Personagens", filtro == TipoFicha.PERSONAGEM) { filtro = TipoFicha.PERSONAGEM }
                Chip("Lugares", filtro == TipoFicha.LUGAR) { filtro = TipoFicha.LUGAR }
                Chip("Outros", filtro == TipoFicha.OUTRO) { filtro = TipoFicha.OUTRO }
            }
            val lista = fichas.filter { filtro == "" || it.tipo == filtro }
            if (lista.isEmpty()) Text("Nenhuma ficha ainda. Toque em + para criar a primeira (nome, descrição e foto).",
                color = p.textoSuave, modifier = Modifier.padding(20.dp))
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(lista, key = { it.id }) { f ->
                    val img = rememberImagem(f.imagem, 300)
                    Row(
                        Modifier.fillMaxWidth().clip(forma(18.dp)).background(p.superficie)
                            .clickable { aberta = if (aberta == f.id) null else f.id }.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(p.borda), contentAlignment = Alignment.Center) {
                            if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else Text(simboloTipo(f.tipo), fontSize = 22.sp, color = p.destaque)
                        }
                        Column(Modifier.weight(1f).animateContentSizeSeguro(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(nomeTipo(f.tipo).uppercase(), style = MaterialTheme.typography.labelSmall, color = p.destaque)
                            Text(f.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (f.descricao.isNotBlank()) Text(f.descricao, fontSize = 14.sp, color = p.textoSuave, lineHeight = 20.sp,
                                maxLines = if (aberta == f.id) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                            if (aberta == f.id) TextButton(onClick = { editando = f }) { Text("Editar") }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { editando = Ficha(obraId = obraId, nome = "") }, containerColor = p.destaque, contentColor = p.sobreDestaque,
            shape = forma(20.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) { Icon(Icons.Filled.Add, "Nova ficha") }
    }
    editando?.let { f -> DialogoFicha(f, { editando = null }) }
}

private fun Modifier.animateContentSizeSeguro() = this.then(Modifier)

@Composable
private fun DialogoFicha(inicial: Ficha, fechar: () -> Unit) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    var f by remember { mutableStateOf(inicial) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) app.escopo.launch { repo.imagemFicha(f, uri)?.let { f = it } }
    }
    val img = rememberImagem(f.imagem, 300)
    AlertDialog(
        onDismissRequest = fechar,
        title = { Text(if (inicial.id == 0L) "Nova ficha" else "Editar ficha") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(p.borda).clickable {
                        seletor.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, contentAlignment = Alignment.Center) {
                        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Text("foto", fontSize = 12.sp, color = p.textoSuave)
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(TipoFicha.PERSONAGEM, TipoFicha.LUGAR, TipoFicha.OUTRO).forEach { t ->
                            Chip(nomeTipo(t), f.tipo == t) { f = f.copy(tipo = t) }
                        }
                    }
                }
                OutlinedTextField(f.nome, { f = f.copy(nome = it) }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(f.apelidos, { f = f.copy(apelidos = it) }, label = { Text("Apelidos (separe com vírgula)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (f.tipo == TipoFicha.PERSONAGEM) {
                    OutlinedTextField(f.idade, { f = f.copy(idade = it) }, label = { Text("Idade") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.aparencia, { f = f.copy(aparencia = it) }, label = { Text("Aparência") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.poder, { f = f.copy(poder = it) }, label = { Text("Poder / habilidade") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.faccao, { f = f.copy(faccao = it) }, label = { Text("Facção / clã / seita") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.relacoes, { f = f.copy(relacoes = it) }, label = { Text("Relações (ex.: irmão de Li Mei, rival de Kael)") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(f.descricao, { f = f.copy(descricao = it) }, label = { Text("Anotações") },
                    minLines = 3, maxLines = 10, modifier = Modifier.fillMaxWidth())
                Text("Dica: no texto, escreva @Nome, descrição… (ou &Lugar, …) e a ficha é criada e completada sozinha.", fontSize = 12.sp, color = p.textoSuave)
                if (inicial.id != 0L) TextButton(onClick = { confirmarExclusao = true }) { Text("Excluir ficha", color = Color(0xFFE57373)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (f.nome.isNotBlank()) { app.escopo.launch { repo.salvarFicha(f.copy(nome = f.nome.trim())) }; fechar() }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = fechar) { Text("Cancelar") } },
    )
    if (confirmarExclusao) DialogoConfirmar("Excluir “${f.nome}”?", "A ficha e a foto serão apagadas.", "Excluir", { confirmarExclusao = false }) {
        confirmarExclusao = false; app.escopo.launch { repo.excluirFicha(f) }; fechar()
    }
}

/** Lixeira: capítulos excluídos ficam 30 dias. */
@Composable
fun TelaLixeira(nav: NavHostController, obraId: Long? = null) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    val todos by remember { repo.lixeira() }.collectAsState(initial = emptyList())
    val itens = if (obraId == null) todos else todos.filter { it.obraId == obraId }
    val fmt = remember { SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR")) }
    var apagar by remember { mutableStateOf<ItemLixeira?>(null) }
    val snack = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Topo("Lixeira", nav)
            Text("Capítulos excluídos ficam aqui por 30 dias e depois somem sozinhos.", fontSize = 13.sp, color = p.textoSuave,
                modifier = Modifier.padding(horizontal = 20.dp))
            if (itens.isEmpty()) Text("A lixeira está vazia.", color = p.textoSuave, modifier = Modifier.padding(20.dp))
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(itens, key = { it.id }) { i ->
                    val dias = (30 - (System.currentTimeMillis() - i.excluidoEm) / 86_400_000).coerceAtLeast(0)
                    Column(Modifier.fillMaxWidth().clip(forma(16.dp)).background(p.superficie).padding(14.dp)) {
                        Text(i.titulo.ifEmpty { "(sem título)" }, fontWeight = FontWeight.Bold)
                        Text("${i.obraTitulo} · ${formatarPalavras(i.palavras.toLong())} palavras · excluído em ${fmt.format(Date(i.excluidoEm))} · some em $dias dias",
                            fontSize = 12.sp, color = p.textoSuave)
                        Row {
                            TextButton(onClick = {
                                app.escopo.launch {
                                    repo.restaurarDaLixeira(i)
                                    escopo.launch { snack.showSnackbar("“${i.titulo}” restaurado em ${i.obraTitulo}.") }
                                }
                            }) { Text("Restaurar") }
                            TextButton(onClick = { apagar = i }) { Text("Apagar de vez", color = Color(0xFFE57373)) }
                        }
                    }
                }
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter))
    }
    apagar?.let { i ->
        DialogoConfirmar("Apagar de vez?", "“${i.titulo}” não poderá mais ser recuperado.", "Apagar", { apagar = null }) {
            apagar = null; app.escopo.launch { repo.apagarDeVez(i) }
        }
    }
}
