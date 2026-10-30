package com.huryel.novely.ui

import com.huryel.novely.ui.theme.forma
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

const val ROTA_LEITOR = "leitor/{id}?q={q}&n={n}&b={b}"

private data class Aba(val rota: String, val rotulo: String, val icone: ImageVector)

private val abas = listOf(
    Aba("inicio", "Início", Icones.inicio),
    Aba("biblioteca", "Biblioteca", Icones.biblioteca),
    Aba("escrever", "Escrever", Icones.escrever),
    Aba("descobrir", "Descobrir", Icones.descobrir),
    Aba("perfil", "Perfil", Icones.perfil),
)

@Composable
fun AppNavegacao(temaAtual: TemaLeitura, aoMudarTema: (TemaLeitura) -> Unit) {
    val app = NovelyApp.instancia
    val usuario by app.conta.usuario.collectAsState()
    if (usuario == null) { TelaLogin(); return }
    // sincroniza ao entrar, a cada 3 minutos com o app aberto e ao sair/minimizar
    LaunchedEffect(usuario?.uid) {
        while (true) {
            app.sinc.sincronizar()
            try { app.pub.atualizarSeguidas() } catch (_: Exception) {}
            try { app.pub.novasDeAutores(); app.pub.publicarPerfilSePreciso() } catch (_: Exception) {}
            kotlinx.coroutines.delay(180_000)
        }
    }
    val donoCiclo = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(donoCiclo) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP || e == androidx.lifecycle.Lifecycle.Event.ON_START) app.escopo.launch { app.sinc.sincronizar() }
            if (e == androidx.lifecycle.Lifecycle.Event.ON_START) app.escopo.launch { app.atualizador.verificarSePreciso() }
        }
        donoCiclo.lifecycle.addObserver(obs)
        onDispose { donoCiclo.lifecycle.removeObserver(obs) }
    }
    val nav = rememberNavController()
    val atual by nav.currentBackStackEntryAsState()
    val rota = atual?.destination?.route
    val mostrarBarra = abas.any { it.rota == rota }
    // a barra de abas some ao rolar para baixo e volta ao rolar para cima
    var barraVisivel by remember { mutableStateOf(true) }
    LaunchedEffect(rota) { barraVisivel = true }
    val rolagem = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < -6f) barraVisivel = false else if (available.y > 6f) barraVisivel = true
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    val p = LocalPaleta.current

    Scaffold(
        containerColor = p.fundo,
        contentColor = p.texto,
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(mostrarBarra && barraVisivel,
                enter = androidx.compose.animation.expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
                BarraTema(abas.map { Triple(it.rota, it.rotulo, it.icone) }, rota) { r -> irAba(nav, r) }
            }
        },
    ) { margem ->
        val idArg = listOf(navArgument("id") { type = NavType.LongType })
        Box(Modifier.fillMaxSize().padding(margem).consumeWindowInsets(margem).nestedScroll(rolagem)) {
        NavHost(nav, startDestination = "inicio", modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) }) {
            composable("inicio") { TelaInicioAba(nav) }
            composable("biblioteca") { TelaBibliotecaAba(nav) }
            composable("escrever") { TelaEscreverAba(nav) }
            composable("descobrir") { TelaDescobrirCatalogo(nav) }
            composable("perfil") { TelaPerfilAba(nav) }
            composable("buscar") { TelaBusca(nav) }
            composable("ajustes") { TelaAjustesNova(nav) }
            composable("ajustes/{s}") { e -> TelaAjusteSecao(e.arguments?.getString("s") ?: "", nav) }
            composable("avisos") { TelaAvisos(nav) }
            composable("autor/{u}") { e -> TelaAutor(e.arguments?.getString("u") ?: "", nav) }
            composable("pub/{p}") { e -> TelaObraPublica(e.arguments?.getString("p") ?: "", nav) }
            composable("publicar/{id}", idArg) { e -> TelaPublicar(e.arguments!!.getLong("id"), nav) }
            composable("lixeira/{id}", idArg) { e -> TelaLixeira(nav, e.arguments!!.getLong("id")) }
            composable("obra/{id}", idArg) { e -> TelaObra(e.arguments!!.getLong("id"), nav) }
            composable(
                ROTA_LEITOR,
                listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("q") { type = NavType.StringType; defaultValue = "" },
                    navArgument("n") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("b") { type = NavType.IntType; defaultValue = -1 },
                ),
            ) { e -> TelaLeitor(e.arguments!!.getLong("id"), nav, e.arguments!!.getString("q") ?: "", e.arguments!!.getInt("n"), e.arguments!!.getInt("b")) }
            composable("marcadores/{id}", idArg) { e -> TelaMarcadores(e.arguments!!.getLong("id"), nav) }
            composable("estatisticas") { TelaEstatisticas(nav) }
            composable("editor/{id}", idArg) { e -> TelaEditor(e.arguments!!.getLong("id"), nav) }
            composable(
                "imagem/{id}/{tipo}",
                listOf(navArgument("id") { type = NavType.LongType }, navArgument("tipo") { type = NavType.StringType }),
            ) { e -> TelaAjusteImagem(e.arguments!!.getLong("id"), e.arguments!!.getString("tipo") ?: "capa", nav) }
            composable("fichas/{id}", idArg) { e -> TelaFichas(e.arguments!!.getLong("id"), nav) }
            composable("lixeira") { TelaLixeira(nav) }
            composable("nomes") { TelaGeradorNomes(nav) }
            composable("tempo/{id}", idArg) { e -> TelaLinhaTempo(e.arguments!!.getLong("id"), nav) }
            composable("reordenar/{id}", idArg) { e -> TelaReordenar(e.arguments!!.getLong("id"), nav) }
            composable(
                "comparar/{id}/{quando}",
                listOf(navArgument("id") { type = NavType.LongType }, navArgument("quando") { type = NavType.LongType }),
            ) { e -> TelaComparar(e.arguments!!.getLong("id"), e.arguments!!.getLong("quando"), nav) }
            composable("historico/{id}", idArg) { e -> TelaHistorico(e.arguments!!.getLong("id"), nav) }
            composable("importar") { TelaImportar(nav, null) }
            composable("importar/{id}", idArg) { e -> TelaImportar(nav, e.arguments!!.getLong("id")) }
            composable("backup") { TelaBackup(nav) }
            composable("atualizar") { TelaAtualizar(nav) }
        }
        AvisosGlobais(nav, rota)
        // notificação tocada → avisos; e pedir permissão de notificação uma vez (Android 13+)
        val abrirAvisos by app.abrirAvisos.collectAsState()
        LaunchedEffect(abrirAvisos) { if (abrirAvisos) { app.abrirAvisos.value = false; nav.navigate("avisos") } }
        val pedirNotif = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) {
            if (android.os.Build.VERSION.SDK_INT >= 33 && !app.prefs.pediuNotificacao.valor) {
                app.prefs.pediuNotificacao.definir(true)
                pedirNotif.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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
fun DialogoNovaObra(aoFechar: () -> Unit, aoCriar: (String, String, Boolean) -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(StatusObra.ESCREVENDO) }
    var paginaEmBranco by remember { mutableStateOf(false) }
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
                if (status == StatusObra.ESCREVENDO) {
                    Text("COMEÇAR COM", style = MaterialTheme.typography.labelSmall, color = LocalPaleta.current.textoSuave)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Capítulos", !paginaEmBranco) { paginaEmBranco = false }
                        Chip("Página em branco", paginaEmBranco) { paginaEmBranco = true }
                    }
                    Text(
                        "Nos dois modos: escreva “Capítulo 2 — Título” ou “Prólogo” numa linha própria (depois de uma linha vazia) e aperte Enter. O texto abaixo vira um capítulo novo automaticamente.",
                        fontSize = 12.sp, color = LocalPaleta.current.textoSuave,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { aoCriar(titulo, status, paginaEmBranco && status == StatusObra.ESCREVENDO) }) { Text("Criar") } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
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
