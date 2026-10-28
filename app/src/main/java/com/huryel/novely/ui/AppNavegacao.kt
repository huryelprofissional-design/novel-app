package com.huryel.novely.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.runtime.saveable.rememberSaveable
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

const val ROTA_LEITOR = "leitor/{id}?q={q}&n={n}"

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
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
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
        NavHost(nav, startDestination = "inicio", modifier = Modifier.padding(margem).consumeWindowInsets(margem),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) }) {
            composable("inicio") { TelaInicio(nav) }
            composable("escrever") { TelaEscrever(nav) }
            composable("buscar") { TelaBusca(nav) }
            composable("ajustes") { TelaAjustes(temaAtual, aoMudarTema, nav) }
            composable("obra/{id}", idArg) { e -> TelaObra(e.arguments!!.getLong("id"), nav) }
            composable(
                ROTA_LEITOR,
                listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("q") { type = NavType.StringType; defaultValue = "" },
                    navArgument("n") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { e -> TelaLeitor(e.arguments!!.getLong("id"), nav, e.arguments!!.getString("q") ?: "", e.arguments!!.getInt("n")) }
            composable("editor/{id}", idArg) { e -> TelaEditor(e.arguments!!.getLong("id"), nav) }
            composable(
                "imagem/{id}/{tipo}",
                listOf(navArgument("id") { type = NavType.LongType }, navArgument("tipo") { type = NavType.StringType }),
            ) { e -> TelaAjusteImagem(e.arguments!!.getLong("id"), e.arguments!!.getString("tipo") ?: "capa", nav) }
            composable("historico/{id}", idArg) { e -> TelaHistorico(e.arguments!!.getLong("id"), nav) }
            composable("importar") { TelaImportar(nav, null) }
            composable("importar/{id}", idArg) { e -> TelaImportar(nav, e.arguments!!.getLong("id")) }
            composable("backup") { TelaBackup(nav) }
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
    var menuNovo by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        val lista = obras
        if (lista == null) {
            Box(Modifier.fillMaxSize())
        } else if (lista.isEmpty()) {
            EstanteVazia({ criando = true }, { nav.navigate("importar") })
        } else {
            val continuar = lista.filter { it.lidaEm > 0 && it.ultimoCapituloId != null }.maxByOrNull { it.lidaEm }
            val escrevendo = lista.filter { it.status == StatusObra.ESCREVENDO }
            val filtradas = if (filtro == "TODAS") lista else lista.filter { it.status == filtro }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { CabecalhoInicio(lista) }
                if (continuar != null) item(span = { GridItemSpan(maxLineSpan) }) {
                    HeroContinuar(continuar) { nav.navigate("leitor/${continuar.ultimoCapituloId}") }
                }
                if (escrevendo.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TituloSecao("Escrevendo agora", "${escrevendo.size}")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(escrevendo, key = { it.id }) { o -> CartaoEscrita(o) { nav.navigate("obra/${o.id}") } }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TituloSecao("Sua estante", "${lista.size}")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip("Todas", filtro == "TODAS") { filtro = "TODAS" }
                            Chip("Escrevendo", filtro == StatusObra.ESCREVENDO) { filtro = StatusObra.ESCREVENDO }
                            Chip("Lendo", filtro == StatusObra.LENDO) { filtro = StatusObra.LENDO }
                            Chip("Concluídas", filtro == StatusObra.CONCLUIDA) { filtro = StatusObra.CONCLUIDA }
                        }
                    }
                }
                items(filtradas, key = { it.id }) { o ->
                    CapaEstante(o, Modifier.animateItem()) { nav.navigate("obra/${o.id}") }
                }
            }
        }
        if (!obras.isNullOrEmpty()) {
            Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { menuNovo = true }, containerColor = p.destaque, contentColor = p.sobreDestaque,
                    shape = RoundedCornerShape(20.dp), icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nova", fontWeight = FontWeight.Bold) },
                )
                DropdownMenu(menuNovo, { menuNovo = false }) {
                    DropdownMenuItem(text = { Text("Criar obra") }, onClick = { menuNovo = false; criando = true })
                    DropdownMenuItem(text = { Text("Importar TXT / EPUB") }, onClick = { menuNovo = false; nav.navigate("importar") })
                }
            }
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

@Composable
private fun EstanteVazia(criar: () -> Unit, importar: () -> Unit) {
    val p = LocalPaleta.current
    val pulso = rememberInfiniteTransition(label = "logo")
    val escala by pulso.animateFloat(1f, 1.06f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "escala")
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.logo_novely), contentDescription = null, modifier = Modifier.size(150.dp).graphicsLayer { scaleX = escala; scaleY = escala })
        Text("Novely", style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp))
        Spacer(Modifier.height(8.dp))
        Text("Sua estante está vazia. Escreva sua primeira história ou traga uma novel.", color = p.textoSuave, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Button(onClick = criar, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Criar obra", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = importar, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Importar TXT ou EPUB") }
    }
}

@Composable
private fun CabecalhoInicio(lista: List<ResumoObra>) {
    val p = LocalPaleta.current
    val hora = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val saudacao = when (hora) { in 5..11 -> "Bom dia"; in 12..17 -> "Boa tarde"; else -> "Boa noite" }
    val palavras = lista.sumOf { it.totalPalavras }
    val caps = lista.sumOf { it.totalCapitulos }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.logo_novely), contentDescription = null,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, p.borda, RoundedCornerShape(14.dp)))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(saudacao.uppercase(), style = MaterialTheme.typography.labelSmall, color = p.destaque)
                Text("Novely", style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Numero("${lista.size}", "obras", Modifier.weight(1f))
            Numero("$caps", "capítulos", Modifier.weight(1f))
            Numero(formatarPalavras(palavras), "palavras", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Numero(valor: String, rotulo: String, modifier: Modifier) {
    val p = LocalPaleta.current
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(p.superficie).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = p.destaque)
        Text(rotulo, fontSize = 12.sp, color = p.textoSuave)
    }
}

@Composable
private fun TituloSecao(titulo: String, extra: String) {
    val p = LocalPaleta.current
    Row(verticalAlignment = Alignment.Bottom) {
        Text(titulo, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp), modifier = Modifier.weight(1f))
        Text(extra, color = p.textoSuave, fontSize = 13.sp)
    }
}

/** Cartão grande de "continuar lendo": usa o banner (ou a arte da capa) como fundo. */
@Composable
private fun HeroContinuar(o: ResumoObra, aoAbrir: () -> Unit) {
    val p = LocalPaleta.current
    val repo = NovelyApp.instancia.repo
    val obra by remember(o.id) { repo.obra(o.id) }.collectAsState(initial = null)
    val cap by remember(o.ultimoCapituloId) { repo.capitulo(o.ultimoCapituloId ?: -1) }.collectAsState(initial = null)
    val alvo = cap?.progresso ?: 0f
    val prog by animateFloatAsState(alvo, tween(900), label = "prog")
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val escala by animateFloatAsState(if (pressionado) 0.97f else 1f, label = "escala")
    Box(
        Modifier.fillMaxWidth().height(210.dp).graphicsLayer { scaleX = escala; scaleY = escala }
            .clip(RoundedCornerShape(26.dp)).clickable(interacao, null, onClick = aoAbrir),
    ) {
        val ob = obra
        if (ob?.banner != null) ImagemEnquadrada(ob.banner, ob.bannerX, ob.bannerY, ob.bannerZoom, o.corBase, Modifier.fillMaxSize(), 1200)
        else Box(Modifier.fillMaxSize().background(Color(o.corBase))) { ArteCapa(o.titulo + "·hero", o.corBase) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x33000000), 1f to Color(0xEE000000))))
        Row(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.Bottom) {
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(78.dp).height(116.dp), 10.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CONTINUAR LENDO", style = MaterialTheme.typography.labelSmall, color = p.destaque)
                Text(o.titulo, fontFamily = com.huryel.novely.ui.theme.Serifa, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 24.sp,
                    color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(cap?.titulo ?: "", fontSize = 13.sp, color = Color(0xCCFFFFFF), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x44FFFFFF))) {
                    Box(Modifier.fillMaxWidth(prog).fillMaxHeight().background(p.destaque))
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).background(p.destaque), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, "Continuar", tint = p.sobreDestaque)
            }
        }
    }
}

@Composable
private fun CartaoEscrita(o: ResumoObra, aoAbrir: () -> Unit) {
    val p = LocalPaleta.current
    Row(
        Modifier.width(250.dp).clip(RoundedCornerShape(20.dp)).background(p.superficie)
            .border(1.dp, p.borda, RoundedCornerShape(20.dp)).clickable(onClick = aoAbrir).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(56.dp).height(84.dp), 9.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(o.titulo, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${o.totalCapitulos} caps · ${formatarPalavras(o.totalPalavras)} palavras", fontSize = 12.sp, color = p.textoSuave)
            Text("✎ continuar", fontSize = 12.sp, color = Color(0xFF6FC2B4), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CapaEstante(o: ResumoObra, modifier: Modifier, aoAbrir: () -> Unit) {
    val p = LocalPaleta.current
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val escala by animateFloatAsState(if (pressionado) 0.94f else 1f, label = "escala")
    Column(modifier.graphicsLayer { scaleX = escala; scaleY = escala }.clickable(interacao, null, onClick = aoAbrir)) {
        Box {
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
                Modifier.fillMaxWidth().aspectRatio(2f / 3f).shadow(10.dp, RoundedCornerShape(12.dp)))
            if (o.totalCapitulos > 0 && o.status != StatusObra.ESCREVENDO) {
                val f = o.lidos.toFloat() / o.totalCapitulos
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(6.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(p.destaque))
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(o.titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(linhaStatus(o), fontSize = 11.sp, color = p.textoSuave, maxLines = 1)
    }
}

private fun linhaStatus(o: ResumoObra): String = when (o.status) {
    StatusObra.LENDO -> if (o.totalCapitulos > 0) "Lendo · ${o.lidos}/${o.totalCapitulos}" else "Lendo"
    StatusObra.CONCLUIDA -> "Concluída · ${o.totalCapitulos} caps"
    else -> "Escrevendo · ${o.totalCapitulos} caps"
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 20.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(horizontal = 20.dp)) }
        item { AjustesLeitura() }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { nav.navigate("importar") }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Importar TXT / EPUB") }
                OutlinedButton(onClick = { nav.navigate("backup") }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Backup e restauração") }
                Text("Novely 0.5 · tudo salvo só neste aparelho, sem conta e sem internet.", fontSize = 12.sp, color = p.textoSuave)
            }
        }
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
