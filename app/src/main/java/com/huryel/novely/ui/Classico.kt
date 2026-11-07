package com.huryel.novely.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Capitulo
import com.huryel.novely.data.Obra
import com.huryel.novely.data.ResumoObra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.data.Texto
import com.huryel.novely.data.Volume
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Paleta

// ───────────────────────── Peças comuns ─────────────────────────

/** Banner da obra (ou arte gerada) com degradê que se funde ao fundo preto. */
@Composable
fun BannerFundo(o: Obra?, corBase: Long, titulo: String, modifier: Modifier) {
    val p = LocalPaleta.current
    Box(modifier) {
        if (o?.banner != null) ImagemEnquadrada(o.banner, o.bannerX, o.bannerY, o.bannerZoom, corBase, Modifier.fillMaxSize(), 1600)
        else Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(corBase), Color(0xFF120D16))))) { ArteCapa("$titulo·banner", corBase) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.3f to Color.Transparent, 0.55f to Color.Black.copy(alpha = 0.2f), 1f to p.fundo)))
    }
}

@Composable
fun BotaoVidro(icone: ImageVector, desc: String, acao: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape).clickable(onClick = acao),
        contentAlignment = Alignment.Center,
    ) { Icon(icone, desc, tint = Color(0xFFE6DFD3), modifier = Modifier.size(21.dp)) }
}

@Composable
private fun Etiqueta(t: String) {
    Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6DFD3),
        modifier = Modifier.padding(7.dp).clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(alpha = 0.72f)).padding(horizontal = 7.dp, vertical = 3.dp))
}

@Composable
private fun Barra(f: Float, modifier: Modifier, altura: Int = 4) {
    val p = LocalPaleta.current
    val anim by animateFloatAsState(f.coerceIn(0f, 1f), tween(900), label = "barra")
    Box(modifier.height(altura.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.12f))) {
        Box(Modifier.fillMaxWidth(anim).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(p.destaque))
    }
}

/** Barra de navegação inferior no estilo "E-Paper Ink": barra escura sólida, pílula branca que desliza e ícone preenchido na aba ativa. */
@Composable
fun BarraTema(abas: List<Triple<String, String, ImageVector>>, atual: String?, vidro: EstadoVidro? = null, ir: (String) -> Unit) {
    val idx = abas.indexOfFirst { it.first == atual }.coerceAtLeast(0)
    val reduzir = reduzirAnimacoes()
    BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp), contentAlignment = Alignment.Center) {
        val total = minOf(maxWidth, 440.dp)
        val larg = (total - 12.dp) / abas.size
        val x by androidx.compose.animation.core.animateDpAsState(larg * idx,
            if (reduzir) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)), label = "pilula")
        Box(Modifier.width(total).height(72.dp).shadow(20.dp, RoundedCornerShape(36.dp)).clip(RoundedCornerShape(36.dp)).background(INK_BARRA)
            .border(1.dp, INK_BORDA, RoundedCornerShape(36.dp)).padding(6.dp)) {
            Box(Modifier.offset(x = x).width(larg).fillMaxHeight().clip(RoundedCornerShape(30.dp)).background(Color.White))
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                abas.forEachIndexed { i, (r, rot, _) ->
                    val sel = i == idx
                    val cor by androidx.compose.animation.animateColorAsState(if (sel) INK_ATIVO else INK_INATIVO, androidx.compose.animation.core.tween(if (reduzir) 0 else 180), label = "cor")
                    val toque = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val apertado by toque.collectIsPressedAsState()
                    val escala by androidx.compose.animation.core.animateFloatAsState(if (apertado && !reduzir) 0.94f else 1f, label = "aperto")
                    Column(Modifier.weight(1f).fillMaxHeight().graphicsLayer { scaleX = escala; scaleY = escala }.clip(RoundedCornerShape(30.dp))
                        .clickable(toque, null, role = androidx.compose.ui.semantics.Role.Tab) { ir(r) }
                        .semantics { selected = sel },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Image(iconeInk(r, sel), null, Modifier.size(22.dp).offset(y = if (sel) (-1).dp else 0.dp))
                        Text(rot, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp, color = cor, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    }
}

private val INK_BARRA = Color(0xFF18191D)
private val INK_BORDA = Color(0xFF2A2C33)
private val INK_ATIVO = Color(0xFF090A0C)
private val INK_INATIVO = Color(0xFF80838E)

/** "Reduzir animações": a opção do app ou a do sistema (escala de animação 0). */
@Composable
fun reduzirAnimacoes(): Boolean {
    val pref by com.huryel.novely.NovelyApp.instancia.prefs.reduzirAnimacoes.estado.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val sistema = remember { try { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (_: Exception) { false } }
    return pref || sistema
}

// Ícones da barra: traço quando inativo; preenchido (com recortes brancos) quando ativo — iguais ao protótipo
private val ICONES_INK: Map<String, List<Pair<String, Char>>> = mapOf( // 'f' = preenche, 'c' = recorte, 'l' = só traço
    "inicio" to listOf("M3 19a9 9 0 0 1 9 0 9 9 0 0 1 9 0V5a9 9 0 0 0-9 0 9 9 0 0 0-9 0v14z" to 'f', "M12 5v14" to 'l', "M12 7l2 2-2 2" to 'c'),
    "biblioteca" to listOf(
        "M4.5 4h2a1.5 1.5 0 0 1 1.5 1.5v13a1.5 1.5 0 0 1-1.5 1.5h-2a1.5 1.5 0 0 1-1.5-1.5v-13a1.5 1.5 0 0 1 1.5-1.5z" to 'f',
        "M11 4h2a1.5 1.5 0 0 1 1.5 1.5v13a1.5 1.5 0 0 1-1.5 1.5h-2a1.5 1.5 0 0 1-1.5-1.5v-13a1.5 1.5 0 0 1 1.5-1.5z" to 'f',
        "M16 4h3.5a1.5 1.5 0 0 1 1.5 1.5v13a1.5 1.5 0 0 1-1.5 1.5H16V4z" to 'f', "M5.5 8v3" to 'c', "M12 8v3" to 'c'),
    "escrever" to listOf("M20.24 3.76a6 6 0 0 0-8.49 0L3.5 12.02a3 3 0 0 0-.82 1.57l-1.1 5.48a1 1 0 0 0 1.18 1.18l5.48-1.1a3 3 0 0 0 1.57-.82l8.26-8.25a6 6 0 0 0 0-8.52z" to 'f', "M16 8L8 16" to 'c'),
    "descobrir" to listOf("M2.5 12a9.5 9.5 0 1 0 19 0a9.5 9.5 0 1 0-19 0" to 'l', "M16.24 7.76L14.12 14.12L7.76 16.24L9.88 9.88z" to 'f', "M10.5 12a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0" to 'c'),
    "perfil" to listOf("M8 7.5a4 4 0 1 0 8 0a4 4 0 1 0-8 0" to 'f', "M4.5 20.25a7.5 7.5 0 0 1 15 0" to 'f'),
)
/** Ícones de traço do HUD do leitor (protótipo "Reader HUD"). */
object IconesHud {
    private fun t(nome: String, vararg d: String) = ImageVector.Builder(nome, 24.dp, 24.dp, 24f, 24f).apply {
        d.forEach { addPath(androidx.compose.ui.graphics.vector.addPathNodes(it), stroke = androidx.compose.ui.graphics.SolidColor(Color.White), strokeLineWidth = 2f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) }
    }.build()
    val voltar = t("voltar", "M19 12H5", "M12 19l-7-7 7-7")
    val web = t("web", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0", "M2 12h20", "M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z")
    val info = t("info", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0", "M12 16v-4", "M12 8h0.01")
    val editar = t("editar", "M12 20h9", "M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z")
    val ouvir = t("ouvir", "M11 5L6 9H2v6h4l5 4V5z", "M19.07 4.93a10 10 0 0 1 0 14.14", "M15.54 8.46a5 5 0 0 1 0 7.07")
    val buscar = t("buscar", "M3 11a8 8 0 1 0 16 0a8 8 0 1 0-16 0", "M21 21l-4.35-4.35")
    val capitulos = t("capitulos", "M8 6h13", "M8 12h13", "M8 18h13", "M3 6h0.01", "M3 12h0.01", "M3 18h0.01")
    val rolar = t("rolar", "M12 5v14", "M19 12l-7 7-7-7")
    val leitura = t("leitura", "M4 19L10 5h4l6 14", "M6.5 14h11")
}

private val cacheInk = HashMap<String, ImageVector>()
fun iconeInk(rota: String, ativo: Boolean): ImageVector = cacheInk.getOrPut("$rota$ativo") {
    val traco = if (ativo) INK_ATIVO else INK_INATIVO
    ImageVector.Builder("ink$rota$ativo", 24.dp, 24.dp, 24f, 24f).apply {
        (ICONES_INK[rota] ?: emptyList()).forEach { (d, t) ->
            addPath(androidx.compose.ui.graphics.vector.addPathNodes(d),
                fill = if (ativo && t == 'f') androidx.compose.ui.graphics.SolidColor(INK_ATIVO) else null,
                stroke = androidx.compose.ui.graphics.SolidColor(if (ativo && t == 'c') Color.White else traco),
                strokeLineWidth = 1.8f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round)
        }
    }.build()
}

// ───────────────────────── Visualizador de capa (zoom) ─────────────────────────

@Composable
fun VisorCapa(o: Obra, fechar: () -> Unit) {
    val img = rememberImagem(o.capa, 2400)
    var escala by remember { mutableFloatStateOf(1f) }
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    Dialog(onDismissRequest = fechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        escala = (escala * zoom).coerceIn(1f, 5f)
                        if (escala > 1f) { dx += pan.x; dy += pan.y } else { dx = 0f; dy = 0f }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { if (escala > 1f) { escala = 1f; dx = 0f; dy = 0f } else escala = 2.5f })
                },
            contentAlignment = Alignment.Center,
        ) {
            val mod = Modifier.fillMaxWidth().padding(24.dp).graphicsLayer { scaleX = escala; scaleY = escala; translationX = dx; translationY = dy }
            if (img != null) Image(img, "Capa de ${o.titulo}", contentScale = ContentScale.Fit, modifier = mod)
            else CapaObra(o.titulo, null, .5f, .5f, 1f, o.corBase, mod.aspectRatio(2f / 3f), 28.sp)
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(o.titulo, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFE6DFD3), textAlign = TextAlign.Center)
                Text("pinça ou toque duplo para ampliar", fontSize = 12.sp, color = Color(0xFFA39A8B))
            }
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)) { BotaoVidro(Icons.Filled.Close, "Fechar", fechar) }
        }
    }
}

// ───────────────────────── INÍCIO (modelo B) ─────────────────────────

private fun destaque(lista: List<ResumoObra>): ResumoObra =
    lista.filter { it.lidaEm > 0 && it.ultimoCapituloId != null }.maxByOrNull { it.lidaEm } ?: lista.first()

@Composable
fun InicioClassico(lista: List<ResumoObra>, nav: NavHostController) {
    val p = LocalPaleta.current
    val repo = NovelyApp.instancia.repo
    val d = destaque(lista)
    val obra by remember(d.id) { repo.obra(d.id) }.collectAsState(initial = null)
    val cap by remember(d.ultimoCapituloId) { repo.capitulo(d.ultimoCapituloId ?: -1) }.collectAsState(initial = null)
    val outras = lista.filter { it.id != d.id }
    val lendo = outras.filter { it.status != StatusObra.ESCREVENDO }
    val escrevendo = lista.filter { it.status == StatusObra.ESCREVENDO }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
        item(key = "hero") {
            Box(Modifier.fillMaxWidth().height(344.dp)) {
                BannerFundo(obra, d.corBase, d.titulo, Modifier.fillMaxWidth().height(310.dp))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Novely", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFFE6DFD3))
                    Text(".", fontWeight = FontWeight.Black, fontSize = 24.sp, color = p.destaque)
                    Spacer(Modifier.weight(1f))
                    BotaoVidro(Icons.Filled.Search, "Buscar") { nav.navigate("buscar") }
                    Spacer(Modifier.width(10.dp))
                    BotaoVidro(Icons.Filled.Add, "Importar") { nav.navigate("importar") }
                }
                CapaObra(d.titulo, d.capa, d.capaX, d.capaY, d.capaZoom, d.corBase,
                    Modifier.align(Alignment.BottomCenter).width(150.dp).height(222.dp)
                        .shadow(18.dp, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { nav.navigate("obra/${d.id}") }, 16.sp)
            }
        }
        item(key = "info") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (d.ultimoCapituloId != null) "CONTINUAR LENDO" else "OBRA RECENTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, color = p.destaque)
                Text(d.titulo, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                cap?.let { c ->
                    val faltam = ((1 - c.progresso) * c.palavras / 230).toInt().coerceAtLeast(1)
                    Text("${c.titulo.ifEmpty { "Sem título" }} · faltam $faltam min", fontSize = 13.sp, color = p.textoSuave,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    Barra(c.progresso, Modifier.padding(top = 12.dp).width(240.dp))
                }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { if (d.ultimoCapituloId != null) nav.navigate("leitor/${d.ultimoCapituloId}") else nav.navigate("obra/${d.id}") },
                        shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("▶  Continuar", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
                    Button(
                        onClick = { nav.navigate("obra/${d.id}") }, shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF1A1815), p.texto), modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("Ver obra", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
            }
        }
        if (lendo.isNotEmpty()) {
            item(key = "s1") { Secao("Sua estante", "Ver todas (${lista.size}) ›") { nav.navigate("biblioteca") } }
            item(key = "r1") { Fileira(lendo, nav) }
        }
        if (escrevendo.isNotEmpty()) {
            item(key = "s2") { Secao("Escrevendo", "${escrevendo.size}") { nav.navigate("escrever") } }
            item(key = "r2") { Fileira(escrevendo, nav) }
        }
    }
}

@Composable
private fun Secao(titulo: String, extra: String, acao: () -> Unit) {
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(extra, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.destaque, modifier = Modifier.clickable(onClick = acao))
    }
}

@Composable
private fun Fileira(lista: List<ResumoObra>, nav: NavHostController) {
    val p = LocalPaleta.current
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(lista, key = { it.id }) { o ->
            Column(Modifier.width(112.dp).clickable { nav.navigate("obra/${o.id}") }) {
                Box {
                    CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.fillMaxWidth().height(158.dp), 12.sp)
                    val etiqueta = when {
                        o.status == StatusObra.ESCREVENDO -> "${o.totalCapitulos} caps"
                        o.totalCapitulos == 0 -> "Novo"
                        o.lidos == 0 -> "Novo"
                        else -> "Cap. ${o.lidos}"
                    }
                    Etiqueta(etiqueta)
                }
                Text(o.titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                Text(
                    when {
                        o.status == StatusObra.ESCREVENDO -> "escrevendo"
                        o.status == StatusObra.CONCLUIDA -> "lida"
                        o.totalCapitulos > 0 -> "${o.lidos * 100 / o.totalCapitulos}% lido"
                        else -> "sem capítulos"
                    }, fontSize = 12.sp, color = p.textoSuave,
                )
            }
        }
    }
}

// ───────────────────────── OBRA (clássico) ─────────────────────────

class AcoesObra(
    val voltar: () -> Unit, val menu: () -> Unit, val banner: () -> Unit, val capa: () -> Unit,
    val ler: (() -> Unit)?, val novoCap: (() -> Unit)?, val novoVol: () -> Unit,
)

@Composable
fun CabecalhoObra(estilo: String, o: Obra, caps: List<Capitulo>, volumes: List<Volume>, alvo: Capitulo?, desloc: () -> Float, a: AcoesObra) {
    val p = LocalPaleta.current
    val palavras = caps.sumOf { it.palavras.toLong() }
    val lidos = caps.count { it.progresso >= 0.98f }
    var sinopseAberta by remember { mutableStateOf(false) }
    var verCapa by remember { mutableStateOf(false) }
    var menuCapa by remember { mutableStateOf(false) }
    if (verCapa) VisorCapa(o) { verCapa = false }
    Column(Modifier.fillMaxWidth()) {
        // estilo Pôster: a capa ocupa o topo inteiro, escurece embaixo e o título aparece grande, em maiúsculas
        Box(Modifier.fillMaxWidth().height(540.dp).clipToBounds()) {
            // fundo: a própria capa desfocada; no meio, a capa inteira como um livro
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.fillMaxSize().blur(28.dp), 30.sp, semCantos = true, mostrarTitulo = false)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x99000000), 0.45f to Color(0x66000000), 0.75f to Color(0xDD000000), 1f to p.fundo)))
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
                Modifier.align(Alignment.TopCenter).padding(top = 64.dp).width(160.dp).height(232.dp).shadow(26.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).clickable { menuCapa = true }, 15.sp)
            DropdownMenu(menuCapa, { menuCapa = false }) {
                DropdownMenuItem(text = { Text("Ver capa") }, onClick = { menuCapa = false; verCapa = true })
                if (!o.trancada) DropdownMenuItem(text = { Text("Trocar ou ajustar capa") }, onClick = { menuCapa = false; a.capa() })
                if (!o.trancada) DropdownMenuItem(text = { Text("Trocar banner") }, onClick = { menuCapa = false; a.banner() })
            }
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                BotaoVidro(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", a.voltar)
                Spacer(Modifier.weight(1f))
                BotaoVidro(Icons.Filled.MoreVert, "Opções da obra", a.menu)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeloPoster(nomeStatus(o.status).uppercase(), p.destaque)
                    val extra = if (o.trancada) "Publicada" else if (!o.minha) "Só leitura" else if (o.publicada) "No catálogo" else null
                    if (extra != null) Text(extra, fontSize = 12.sp, color = Color(0xFFDDDDDD), modifier = Modifier.padding(start = 10.dp))
                }
                Text(o.titulo.uppercase(), fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, color = Color.White, modifier = Modifier.padding(top = 12.dp))
                if (o.autor.isNotBlank()) Text("por ${o.autor}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PilulaInfo("${caps.size} caps")
                PilulaInfo("${formatarPalavras(palavras)} palavras")
                if (caps.isNotEmpty() && !o.minha) PilulaInfo("${lidos * 100 / caps.size}% lido")
                if (caps.isNotEmpty() && !o.minha) {
                    val min = (caps.sumOf { (it.palavras * (1f - it.progresso.coerceIn(0f, 1f))).toDouble() } / 230).toInt()
                    if (min > 0) PilulaInfo(if (min >= 60) "faltam ${min / 60} h ${min % 60} min" else "faltam $min min")
                }
            }
            if (o.descricao.isNotBlank()) Text(o.descricao, fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFFCCCCCC),
                maxLines = if (sinopseAberta) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 16.dp).animateContentSize().clickable { sinopseAberta = !sinopseAberta })
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (a.novoCap != null) Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable(onClick = a.novoCap),
                    contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "Novo capítulo", tint = Color.White) }
                BotaoBranco(
                    "▶  " + when {
                        alvo == null -> "Sem capítulos"
                        alvo.progresso in 0.01f..0.97f -> "Continuar"
                        caps.indexOf(alvo) > 0 -> "Ler cap. ${caps.indexOf(alvo) + 1}"
                        else -> "Ler agora"
                    }, Modifier.weight(1f), enabled = a.ler != null,
                ) { a.ler?.invoke() }
            }
            if (alvo != null && alvo.progresso in 0.01f..0.97f) Text("Parou em: ${alvo.titulo.ifEmpty { "sem título" }} · ${(alvo.progresso * 100).toInt()}%",
                fontSize = 12.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Capítulos", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = a.novoVol) { Text("+ Volume", color = p.textoSuave) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.borda))
        }
    }
}

// ───────────────────────── peças do estilo Pôster ─────────────────────────

/** Selo laranja tipo "NOVO CAP", "EM ALTA #1". */
@Composable
fun SeloPoster(t: String, cor: Color = com.huryel.novely.ui.theme.Laranja) {
    Text(t, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (cor.luminance() > 0.5f) Color(0xFF0B0B0B) else Color.White, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(cor).padding(horizontal = 9.dp, vertical = 3.dp))
}

/** Pílula cinza translúcida com uma informação (▶ 512 mil, ★ 4,8…). */
@Composable
fun PilulaInfo(t: String, modifier: Modifier = Modifier, aoTocar: (() -> Unit)? = null) {
    Text(t, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.1f))
            .then(if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier).padding(horizontal = 13.dp, vertical = 8.dp))
}

/** Botão principal: pílula branca com texto preto. */
@Composable
fun BotaoBranco(t: String, modifier: Modifier = Modifier, enabled: Boolean = true, aoTocar: () -> Unit) {
    Box(modifier.height(58.dp).clip(RoundedCornerShape(29.dp)).background(if (enabled) Color.White else Color(0xFF3A3A3A))
        .clickable(enabled = enabled, onClick = aoTocar).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
        Text(t, fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (enabled) Color.Black else Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LinhaCapEstilo(
    estilo: String, c: Capitulo, o: Obra, atual: Boolean, lido: Boolean, numero: Int,
    abrir: () -> Unit, selecionado: Boolean = false, emSelecao: Boolean = false, segurar: () -> Unit = {},
    acoes: @Composable RowScope.() -> Unit,
) {
    val p = LocalPaleta.current
    val especial = Texto.tipoEspecial(c.titulo)
    val info = when {
        c.rascunho && o.status == StatusObra.ESCREVENDO -> "rascunho · ${formatarPalavras(c.palavras.toLong())} palavras"
        c.progresso in 0.01f..0.979f -> "${(c.progresso * 100).toInt()}% · faltam ${((1 - c.progresso) * c.palavras / 230).toInt().coerceAtLeast(1)} min"
        lido -> "lido"
        else -> "${formatarPalavras(c.palavras.toLong())} palavras"
    }.let { base ->
        if (!o.publicada || !o.minha) base else when {
            c.publicadoEm == 0L -> "○ não publicado · $base"
            c.publicadoEm > System.currentTimeMillis() -> "◷ agendado · $base"
            else -> "● publicado · $base"
        }
    }
    Row(
        Modifier.fillMaxWidth().background(if (selecionado) p.destaque.copy(alpha = 0.12f) else Color.Transparent)
            .combinedClickable(onClick = abrir, onLongClick = segurar).padding(start = 20.dp, top = 6.dp, bottom = 6.dp, end = if (emSelecao) 20.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emSelecao) {
            Box(Modifier.padding(end = 12.dp).size(22.dp).clip(CircleShape).background(if (selecionado) p.destaque else Color.Transparent)
                .border(2.dp, if (selecionado) p.destaque else p.borda, CircleShape), contentAlignment = Alignment.Center) {
                if (selecionado) Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Black, color = p.sobreDestaque)
            }
        }
        Text(if (especial != null) "◆" else "$numero", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
            color = if (atual) p.destaque else Color(0xFF6F675B), modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(c.titulo.ifEmpty { "Sem título" }, fontSize = 15.sp, fontWeight = if (atual) FontWeight.Bold else FontWeight.Medium,
                color = if (lido && !atual) Color(0xFF6F675B) else p.texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(info + c.tags.split(',').filter { it.isNotBlank() }.joinToString("") { "  #${it.trim()}" },
                fontSize = 12.sp, color = if (atual) p.destaque else if (info.startsWith("rascunho")) p.destaque2 else Color(0xFF6F675B),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        acoes()
    }
    Box(Modifier.padding(start = 20.dp, end = 20.dp).fillMaxWidth().height(1.dp).background(Color(0xFF15130F)))
}
