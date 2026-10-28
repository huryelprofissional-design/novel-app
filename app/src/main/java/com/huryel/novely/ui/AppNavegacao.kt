package com.huryel.novely.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.huryel.novely.NovelyApp
import com.huryel.novely.R
import com.huryel.novely.data.ResumoObra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.TemaLeitura
import kotlinx.coroutines.launch

private data class Aba(val rota: String, val rotulo: String, val icone: ImageVector)

private val abas = listOf(
    Aba("inicio", "Início", Icons.Filled.Home),
    Aba("escrever", "Escrever", Icons.Filled.Edit),
    Aba("buscar", "Buscar", Icons.Filled.Search),
    Aba("ajustes", "Ajustes", Icons.Filled.Settings),
)

@Composable
fun AppNavegacao(temaAtual: TemaLeitura, aoMudarTema: (TemaLeitura) -> Unit) {
    val nav = rememberNavController()
    val atual by nav.currentBackStackEntryAsState()
    val rota = atual?.destination?.route
    val mostrarBarra = abas.any { it.rota == rota }
    val p = LocalPaleta.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar(containerColor = p.superficie) {
                    abas.forEach { aba ->
                        NavigationBarItem(
                            selected = rota == aba.rota,
                            onClick = {
                                nav.navigate(aba.rota) {
                                    popUpTo("inicio") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(aba.icone, contentDescription = null) },
                            label = { Text(aba.rotulo) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = p.destaque, selectedTextColor = p.destaque,
                                indicatorColor = p.borda,
                                unselectedIconColor = p.textoSuave, unselectedTextColor = p.textoSuave,
                            ),
                        )
                    }
                }
            }
        },
    ) { margem ->
        val idArg = listOf(navArgument("id") { type = NavType.LongType })
        NavHost(nav, startDestination = "inicio", modifier = Modifier.padding(margem)) {
            composable("inicio") { TelaInicio(nav) }
            composable("escrever") { TelaEscrever(nav) }
            composable("buscar") { TelaSimples("Buscar", "A busca em todas as obras chega na Fase F.") }
            composable("ajustes") { TelaAjustes(temaAtual, aoMudarTema, nav) }
            composable("obra/{id}", idArg) { e -> TelaObra(e.arguments!!.getLong("id"), nav) }
            composable("leitor/{id}", idArg) { e -> TelaLeitor(e.arguments!!.getLong("id"), nav) }
            composable("editor/{id}", idArg) { e -> TelaEditor(e.arguments!!.getLong("id"), nav) }
            composable(
                "imagem/{id}/{tipo}",
                listOf(navArgument("id") { type = NavType.LongType }, navArgument("tipo") { type = NavType.StringType }),
            ) { e -> TelaAjusteImagem(e.arguments!!.getLong("id"), e.arguments!!.getString("tipo") ?: "capa", nav) }
            composable("backup") { TelaSimples("Importar e backup", "Importação de TXT/EPUB (Fase C) e backup (Fase G).", nav) }
        }
    }
}

@Composable
fun Topo(titulo: String, nav: NavHostController?, acoes: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (nav != null) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 8.dp))
        acoes()
    }
}

@Composable
private fun TelaInicio(nav: NavHostController) {
    val repo = NovelyApp.instancia.repo
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val obras by remember { repo.resumos() }.collectAsState(initial = null)
    var filtro by rememberSaveable { mutableStateOf("TODAS") }
    var criando by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        val lista = obras
        if (lista == null) {
            Box(Modifier.fillMaxSize())
        } else if (lista.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Image(painterResource(R.drawable.logo_novely), contentDescription = null, modifier = Modifier.size(140.dp))
                Text("Novely", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(8.dp))
                Text("Sua estante está vazia. Crie sua primeira obra ou importe uma novel.", color = p.textoSuave)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { criando = true }, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque),
                    modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Criar obra", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { nav.navigate("backup") }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Importar novel") }
            }
        } else {
            val continuar = lista.filter { it.lidaEm > 0 && it.ultimoCapituloId != null }.maxByOrNull { it.lidaEm }
            val filtradas = when (filtro) {
                "TODAS" -> lista
                else -> lista.filter { it.status == filtro }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 110.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.logo_novely), contentDescription = null,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("NOVELY", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
                            Text("Sua estante", style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp))
                        }
                    }
                }
                if (continuar != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CartaoContinuar(continuar) { nav.navigate("leitor/${continuar.ultimoCapituloId}") }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Todas · ${lista.size}", filtro == "TODAS") { filtro = "TODAS" }
                        Chip("Escrevendo", filtro == StatusObra.ESCREVENDO) { filtro = StatusObra.ESCREVENDO }
                        Chip("Lendo", filtro == StatusObra.LENDO) { filtro = StatusObra.LENDO }
                        Chip("Concluídas", filtro == StatusObra.CONCLUIDA) { filtro = StatusObra.CONCLUIDA }
                    }
                }
                items(filtradas, key = { it.id }) { o ->
                    Column(Modifier.clickable { nav.navigate("obra/${o.id}") }) {
                        CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
                            Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                        Spacer(Modifier.height(6.dp))
                        Text(o.titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(linhaStatus(o), fontSize = 12.sp, color = p.textoSuave, maxLines = 1)
                    }
                }
            }
        }
        if (!obras.isNullOrEmpty()) {
            FloatingActionButton(
                onClick = { criando = true },
                containerColor = p.destaque, contentColor = p.sobreDestaque, shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) { Icon(Icons.Filled.Add, contentDescription = "Nova obra") }
        }
    }

    if (criando) {
        DialogoNovaObra(aoFechar = { criando = false }) { titulo, status ->
            criando = false
            escopo.launch {
                val id = repo.criarObra(titulo, "", status)
                nav.navigate("obra/$id")
            }
        }
    }
}

private fun linhaStatus(o: ResumoObra): String = when (o.status) {
    StatusObra.LENDO -> if (o.totalCapitulos > 0) "Lendo · ${o.lidos}/${o.totalCapitulos}" else "Lendo"
    StatusObra.CONCLUIDA -> "Concluída · ${o.totalCapitulos} caps"
    else -> "Escrevendo · ${o.totalCapitulos} caps"
}

@Composable
private fun CartaoContinuar(o: ResumoObra, aoAbrir: () -> Unit) {
    val p = LocalPaleta.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(p.superficie)
            .border(1.dp, p.borda, RoundedCornerShape(22.dp)).clickable(onClick = aoAbrir).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(76.dp).height(112.dp), 11.sp)
        Column(Modifier.weight(1f).height(112.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CONTINUAR LENDO", style = MaterialTheme.typography.labelSmall, color = p.destaque)
            Text(o.titulo, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            val prog = if (o.totalCapitulos > 0) o.lidos.toFloat() / o.totalCapitulos else 0f
            LinearProgressIndicator(
                progress = { prog }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = p.destaque, trackColor = p.borda, drawStopIndicator = {},
            )
            Text("${o.lidos} de ${o.totalCapitulos} capítulos lidos", fontSize = 12.sp, color = p.textoSuave)
        }
    }
}

@Composable
fun DialogoNovaObra(aoFechar: () -> Unit, aoCriar: (String, String) -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(StatusObra.ESCREVENDO) }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Nova obra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Vou escrever", status == StatusObra.ESCREVENDO) { status = StatusObra.ESCREVENDO }
                    Chip("Vou ler", status == StatusObra.LENDO) { status = StatusObra.LENDO }
                }
            }
        },
        confirmButton = { TextButton(onClick = { aoCriar(titulo, status) }) { Text("Criar") } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

@Composable
private fun TelaEscrever(nav: NavHostController) {
    val repo = NovelyApp.instancia.repo
    val p = LocalPaleta.current
    val obras by remember { repo.resumos() }.collectAsState(initial = emptyList())
    val minhas = obras.filter { it.status == StatusObra.ESCREVENDO }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Escrever", style = MaterialTheme.typography.displaySmall) }
        if (minhas.isEmpty()) item { Text("Nenhuma obra em escrita. Crie uma no Início com “Vou escrever”.", color = p.textoSuave) }
        items(minhas, key = { it.id }) { o ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.superficie)
                    .clickable { nav.navigate("obra/${o.id}") }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(54.dp).height(80.dp), 9.sp)
                Column(Modifier.weight(1f)) {
                    Text(o.titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${o.totalCapitulos} capítulos · ${formatarPalavras(o.totalPalavras)} palavras", color = p.textoSuave, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun TelaAjustes(temaAtual: TemaLeitura, aoMudarTema: (TemaLeitura) -> Unit, nav: NavHostController) {
    val p = LocalPaleta.current
    val prefs = NovelyApp.instancia.prefs
    val tamanho by prefs.tamanho.collectAsState()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.displaySmall)
        Text("TEMA", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TemaLeitura.entries.forEach { t ->
                val sel = t == temaAtual
                Box(
                    Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(16.dp)).background(t.paleta.fundo)
                        .border(if (sel) 2.dp else 1.dp, if (sel) p.destaque else p.borda, RoundedCornerShape(16.dp))
                        .clickable { aoMudarTema(t) },
                    contentAlignment = Alignment.Center,
                ) { Text(t.paleta.nome, color = t.paleta.texto, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Text("TAMANHO DO TEXTO NO LEITOR · ${tamanho.toInt()}", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
        Slider(value = tamanho, onValueChange = { prefs.definirTamanho(it) }, valueRange = 14f..28f, steps = 13,
            colors = SliderDefaults.colors(thumbColor = p.destaque, activeTrackColor = p.destaque, inactiveTrackColor = p.borda))
        OutlinedButton(onClick = { nav.navigate("backup") }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Importar e backup") }
        Text("Novely 0.2 · Fase B", fontSize = 12.sp, color = p.textoSuave)
    }
}

@Composable
fun TelaSimples(titulo: String, texto: String, nav: NavHostController? = null) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxSize()) {
        if (nav != null) Topo(titulo, nav)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (nav == null) Text(titulo, style = MaterialTheme.typography.displaySmall)
            Text(texto, color = p.textoSuave)
        }
    }
}
