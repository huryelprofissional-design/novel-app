package com.huryel.novely.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.ObraPublica
import com.huryel.novely.data.Progresso
import com.huryel.novely.data.ResumoObra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.Inter
import com.huryel.novely.ui.theme.InstrumentSerif
import com.huryel.novely.ui.theme.PlexMono
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Novely V2 · visual editorial: papel escuro, creme, vermelho-laranja; serifa grande, rótulos em mono, botões retos.
 * As quatro abas principais (Ler, Descobrir, Escrever, Eu) moram aqui.
 */
object Ed {
    val fundo = Color(0xFF12100E)
    val superficie = Color(0xFF1A1714)
    val linha = Color(0xFF2E2924)
    val creme = Color(0xFFEDE6DA)
    val suave = Color(0xFF968D81)
    val apagado = Color(0xFF5E574F)
    val laranja = Color(0xFFE5553B)
    val verde = Color(0xFF8DBB83)
}

private fun traco(nome: String, vararg caminhos: String): ImageVector =
    ImageVector.Builder(nome, 24.dp, 24.dp, 24f, 24f).apply {
        caminhos.forEach { d -> addPath(addPathNodes(d), stroke = SolidColor(Color.White), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
    }.build()

private object IconesEd {
    val sino = traco("sino", "M6 16V11a6 6 0 0 1 12 0v5l2 2H4z", "M10 21h4")
    val lupa = traco("lupa", "M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0", "M20 20l-4-4")
    val partilhar = traco("partilhar", "M12 3v12", "M8 7l4-4 4 4", "M5 13v7h14v-7")
    val marcador = traco("marcador", "M6 3h12v18l-6-4-6 4z")
    val marcadorCheio = ImageVector.Builder("marcadorCheio", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(addPathNodes("M6 3h12v18l-6-4-6 4z"), fill = SolidColor(Color.White))
    }.build()
}

// ───────── peças ─────────

@Composable
fun Mono(t: String, modifier: Modifier = Modifier, cor: Color = Ed.suave, tamanho: TextUnit = 11.sp, peso: FontWeight = FontWeight.Normal, maxLinhas: Int = 1) =
    Text(t.uppercase(), modifier, color = cor, fontFamily = PlexMono, fontSize = tamanho, fontWeight = peso, letterSpacing = 2.sp,
        maxLines = maxLinhas, overflow = TextOverflow.Ellipsis)

@Composable
fun Serif(t: String, tamanho: TextUnit, modifier: Modifier = Modifier, cor: Color = Ed.creme, italico: Boolean = false, maxLinhas: Int = Int.MAX_VALUE, centro: Boolean = false) =
    Text(t, modifier, color = cor, fontFamily = InstrumentSerif, fontSize = tamanho, lineHeight = tamanho * 1.05f,
        fontStyle = if (italico) FontStyle.Italic else FontStyle.Normal, maxLines = maxLinhas, overflow = TextOverflow.Ellipsis,
        textAlign = if (centro) TextAlign.Center else null)

@Composable
fun BotaoCreme(t: String, modifier: Modifier = Modifier, seta: Boolean = true, aoTocar: () -> Unit) {
    Row(modifier.height(54.dp).background(Ed.creme).clickable(onClick = aoTocar).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (seta) Arrangement.SpaceBetween else Arrangement.Center) {
        Text(t, color = Ed.fundo, fontFamily = Inter, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = if (seta) Modifier.weight(1f) else Modifier)
        if (seta) Text("→", color = Ed.fundo, fontSize = 20.sp)
    }
}

@Composable
fun BotaoLinha(t: String, modifier: Modifier = Modifier, aoTocar: () -> Unit) {
    Box(modifier.height(54.dp).border(1.dp, Ed.creme.copy(alpha = 0.75f)).clickable(onClick = aoTocar).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(t, color = Ed.creme, fontFamily = Inter, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
private fun Fio(modifier: Modifier = Modifier, cor: Color = Ed.linha) = Box(modifier.fillMaxWidth().height(1.dp).background(cor))

/** Capa com a faixa vermelha deslocada atrás (direita e embaixo). */
@Composable
fun CapaFaixa(titulo: String, capa: String?, x: Float, y: Float, zoom: Float, corBase: Long, largura: Dp, altura: Dp, desloc: Dp = 6.dp) {
    Box(Modifier.size(largura + desloc, altura + desloc)) {
        Box(Modifier.offset(desloc, desloc).size(largura, altura).background(Ed.laranja))
        CapaObra(titulo, capa, x, y, zoom, corBase, Modifier.size(largura, altura), (largura.value / 9).sp, semCantos = true)
    }
}

@Composable
private fun CapaFaixa(o: ResumoObra, largura: Dp, altura: Dp, desloc: Dp = 6.dp) = CapaFaixa(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, largura, altura, desloc)

/** Texto em pé (girado 90°), ocupando a largura certa. */
private fun Modifier.emPe() = layout { m, _ ->
    val p = m.measure(Constraints())
    layout(p.height, p.width) { p.placeWithLayer(-(p.width - p.height) / 2, (p.width - p.height) / 2) { rotationZ = 90f } }
}

fun romano(n: Int): String {
    if (n <= 0) return "0"
    var r = n; val sb = StringBuilder()
    listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
        .forEach { (v, s) -> while (r >= v) { sb.append(s); r -= v } }
    return sb.toString()
}

private fun mil(n: Long): String = when {
    n >= 1_000_000 -> "%.1f mi".format(n / 1e6).replace(".0", "").replace('.', ',')
    n >= 10_000 -> "${n / 1000} mil"
    n >= 1000 -> "%.1f mil".format(n / 1e3).replace(",0", "").replace(".0", "").replace('.', ',')
    else -> "$n"
}
private fun dois(n: Int) = n.toString().padStart(2, '0')

// ───────── barra de baixo ─────────

@Composable
fun BarraEditorial(abas: List<Pair<String, String>>, atual: String?, ir: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Ed.fundo)) {
        Fio(cor = Ed.creme.copy(alpha = 0.35f))
        Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
            abas.forEach { (rota, rotulo) ->
                val on = rota == atual
                Column(Modifier.weight(1f).fillMaxHeight().clickable { ir(rota) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (on) Ed.laranja else Color.Transparent))
                    Spacer(Modifier.height(8.dp))
                    Text(rotulo.uppercase(), fontFamily = PlexMono, fontSize = 12.sp, letterSpacing = 2.sp, color = if (on) Ed.creme else Ed.suave,
                        fontWeight = if (on) FontWeight.Medium else FontWeight.Normal, maxLines = 1)
                }
            }
        }
    }
}

// ───────────────────────── LER ─────────────────────────

@Composable
fun TelaLer(nav: NavHostController) {
    val app = NovelyApp.instancia
    val obras by remember { app.repo.resumos() }.collectAsState(initial = null)
    val estadoAt by app.atualizador.estado.collectAsState()
    val novidades by app.prefs.novidades.estado.collectAsState()
    var aba by rememberSaveable { mutableStateOf("LENDO") }
    var criando by remember { mutableStateOf(false) }
    val lista = obras ?: run { Box(Modifier.fillMaxSize().background(Ed.fundo)); return }
    val continuar = lista.filter { it.lidaEm > 0 && it.ultimoCapituloId != null }.maxByOrNull { it.lidaEm }
    val cap by remember(continuar?.ultimoCapituloId) { app.repo.capitulo(continuar?.ultimoCapituloId ?: -1L) }.collectAsState(initial = null)
    val deOutros = lista.filter { !it.minha }
    val porAba = linkedMapOf(
        "LENDO" to deOutros.filter { it.status == StatusObra.LENDO }.sortedByDescending { it.lidaEm },
        "QUERO" to deOutros.filter { it.status == StatusObra.QUERO },
        "LIDOS" to deOutros.filter { it.status == StatusObra.CONCLUIDA }.sortedByDescending { it.lidaEm },
    )
    val temAviso = estadoAt is com.huryel.novely.data.EstadoAtualizacao.Disponivel || novidades.isNotBlank()

    Column(Modifier.fillMaxSize().background(Ed.fundo).verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
        // ── capa grande ──
        Box(Modifier.fillMaxWidth().height(if (continuar != null) 420.dp else 200.dp)) {
            if (continuar != null) {
                CapaObra(continuar.titulo, continuar.capa, continuar.capaX, continuar.capaY, continuar.capaZoom, continuar.corBase,
                    Modifier.fillMaxSize(), semCantos = true, mostrarTitulo = false)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Ed.fundo.copy(alpha = 0.55f), 0.25f to Color.Transparent, 0.7f to Color.Transparent, 1f to Ed.fundo)))
                val c = cap
                Mono("${continuar.titulo} · ${continuar.autor.ifBlank { app.prefs.autorNome.valor }} · cap. ${dois(((c?.ordem ?: 0.0).toInt()).coerceAtLeast(1))}",
                    Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).emPe(), cor = Ed.creme.copy(alpha = 0.85f), tamanho = 10.sp)
            }
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Serif("Novely", 38.sp, Modifier.weight(1f), italico = true)
                Box(Modifier.clickable { nav.navigate("buscar") }.padding(10.dp)) { Icon(IconesEd.lupa, "Buscar", tint = Ed.creme, modifier = Modifier.size(24.dp)) }
                Box(Modifier.clickable { nav.navigate("avisos") }.padding(10.dp)) {
                    Icon(IconesEd.sino, "Avisos", tint = Ed.creme, modifier = Modifier.size(26.dp))
                    if (temAviso) Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(Ed.laranja))
                }
            }
        }
        Column(Modifier.padding(horizontal = 22.dp)) {
            if (continuar != null) {
                val c = cap
                Mono("Continuar · capítulo ${dois(((c?.ordem ?: 0.0).toInt()).coerceAtLeast(1))}", tamanho = 12.sp)
                Serif(continuar.titulo, if (continuar.titulo.length > 14) 54.sp else 72.sp, Modifier.padding(top = 6.dp), maxLinhas = 2)
                if (c != null) {
                    val min = ((1f - c.progresso) * c.palavras / 230f).toInt().coerceAtLeast(1)
                    Serif("${c.titulo.ifBlank { "Capítulo" }} — falta $min ${if (min == 1) "minuto" else "minutos"}", 24.sp, Modifier.padding(top = 4.dp), Ed.creme.copy(alpha = 0.85f), italico = true, maxLinhas = 2)
                    Row(Modifier.padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(3.dp).background(Ed.linha)) { Box(Modifier.fillMaxWidth(c.progresso.coerceIn(0f, 1f)).fillMaxHeight().background(Ed.creme)) }
                        Text("${(c.progresso * 100).toInt()}%", color = Ed.creme, fontFamily = Inter, fontSize = 14.sp, modifier = Modifier.padding(start = 14.dp))
                    }
                }
                BotaoCreme("Continuar leitura", Modifier.fillMaxWidth().padding(top = 26.dp)) { nav.navigate("leitor/${continuar.ultimoCapituloId}") }
            } else {
                Serif("Comece uma leitura", 44.sp)
                Text("Importe uma novel, abra uma obra recebida ou ache algo em Descobrir.", color = Ed.suave, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
                BotaoCreme("Descobrir histórias", Modifier.fillMaxWidth().padding(top = 22.dp)) { irAba(nav, "descobrir") }
            }

            // ── estante ──
            Row(Modifier.fillMaxWidth().padding(top = 40.dp), verticalAlignment = Alignment.Bottom) {
                Serif("Estante", 38.sp, Modifier.padding(end = 18.dp))
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("LENDO" to "Lendo", "QUERO" to "Quero", "LIDOS" to "Lidos").forEach { (k, n) ->
                        val on = aba == k
                        Column(Modifier.clickable { aba = k }) {
                            Mono("$n ${porAba[k]?.size ?: 0}", cor = if (on) Ed.creme else Ed.suave, tamanho = 12.sp)
                            Box(Modifier.padding(top = 3.dp).height(1.5.dp).width(if (on) 64.dp else 0.dp).background(Ed.creme))
                        }
                    }
                }
            }
            val itens = porAba[aba].orEmpty()
            Row(Modifier.padding(top = 18.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                itens.forEach { o ->
                    Column(Modifier.clickable { nav.navigate("obra/${o.id}") }) {
                        CapaFaixa(o, 110.dp, 160.dp)
                        val pct = if (o.totalCapitulos > 0) o.lidos * 100 / o.totalCapitulos else 0
                        if (aba == "LENDO") Mono("$pct%", Modifier.padding(top = 4.dp), Ed.laranja, 11.sp, maxLinhas = 1)
                        else Text(o.titulo, color = Ed.suave, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(110.dp).padding(top = 6.dp))
                    }
                }
                Box(Modifier.size(116.dp, 166.dp).drawBehind {
                    drawRect(Ed.suave.copy(alpha = 0.7f), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                }.clickable { nav.navigate("importar") }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("+", color = Ed.creme, fontSize = 28.sp, fontFamily = Inter)
                        Mono("Importar", cor = Ed.creme.copy(alpha = 0.85f), tamanho = 12.sp)
                    }
                }
            }
            if (itens.isEmpty()) Text(when (aba) {
                "QUERO" -> "Segure uma capa na estante completa e escolha “Quero ler”."
                "LIDOS" -> "As obras que você terminar aparecem aqui."
                else -> "Nada sendo lido agora."
            }, color = Ed.suave, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            Row(Modifier.fillMaxWidth().padding(top = 26.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotaoLinha("Estante completa", Modifier.weight(1f)) { nav.navigate("biblioteca") }
                BotaoLinha("Nova obra", Modifier.weight(1f)) { criando = true }
            }
        }
    }
    if (criando) DialogoCriarObra(nav) { criando = false }
}

// ───────────────────────── DESCOBRIR ─────────────────────────

@OptIn(ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TelaDescobrirEd(nav: NavHostController) {
    var aba by rememberSaveable { mutableStateOf("LINHAS") }
    var lista by remember(aba) { mutableStateOf<List<ObraPublica>?>(null) }
    var erro by remember(aba) { mutableStateOf(false) }
    LaunchedEffect(aba) {
        lista = try {
            when (aba) {
                "LINHAS" -> {
                    val a = CacheCatalogo.pegar("ALTA"); val n = try { CacheCatalogo.pegar("NOVIDADES") } catch (_: Exception) { emptyList() }
                    (a + n).distinctBy { it.id }.shuffled(java.util.Random(Calendar.getInstance().get(Calendar.DAY_OF_YEAR).toLong()))
                }
                "ALTA" -> CacheCatalogo.pegar("ALTA")
                "NOVAS" -> CacheCatalogo.pegar("NOVIDADES")
                else -> CacheCatalogo.pegar("CONCLUIDAS")
            }
        } catch (_: Exception) { erro = true; emptyList() }
    }
    // fundo quente (como a luz de um abajur)
    Box(Modifier.fillMaxSize().background(Ed.fundo).drawBehind {
        drawRect(Brush.radialGradient(listOf(Color(0xFF4A2618).copy(alpha = 0.85f), Color.Transparent), center = Offset(size.width * 0.5f, 0f), radius = size.width * 1.1f))
    }) {
        val l = lista
        when {
            l == null -> Mono("Carregando…", Modifier.align(Alignment.Center))
            l.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Serif(if (erro) "Sem conexão" else "Nada aqui ainda", 34.sp, centro = true)
                Text(if (erro) "Não deu para abrir o catálogo agora." else "Quando houver obras publicadas, elas aparecem aqui.", color = Ed.suave, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }
            aba == "LINHAS" -> {
                val pager = rememberPagerState { l.size }
                VerticalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { k -> PaginaLinha(l[k], nav) }
                // marcador de página à direita
                Column(Modifier.align(Alignment.CenterEnd).padding(end = 26.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val ini = (pager.currentPage - 1).coerceAtLeast(0)
                    (ini until minOf(l.size, ini + 4)).forEach { i ->
                        Box(Modifier.width(3.dp).height(if (i == pager.currentPage) 38.dp else 16.dp).background(if (i == pager.currentPage) Ed.creme else Ed.suave.copy(alpha = 0.6f)))
                    }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 84.dp, bottom = 120.dp)) {
                itemsIndexed(l, key = { _, o -> o.id }) { i, o -> LinhaRanking(i + 1, o, aba == "ALTA") { nav.navigate("pub/${o.id}") } }
                item {
                    Serif("Gêneros", 34.sp, Modifier.padding(top = 34.dp, bottom = 14.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Fantasia", "Romance", "Ação", "Mistério", "Terror", "Murim", "Sistema", "Reencarnação", "Sci-fi", "Drama").forEach { g ->
                            Box(Modifier.border(1.dp, Ed.linha.copy(alpha = 1f)).clickable { nav.navigate("genero/$g") }.padding(horizontal = 14.dp, vertical = 11.dp)) {
                                Mono(g, cor = Ed.creme.copy(alpha = 0.9f), tamanho = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        // abas no topo
        Row(Modifier.fillMaxWidth().background(if (aba == "LINHAS") Color.Transparent else Ed.fundo).padding(start = 22.dp, end = 10.dp, top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                listOf("LINHAS" to "Linhas", "ALTA" to "Em alta", "NOVAS" to "Novas", "COMPLETAS" to "Completas").forEach { (k, n) ->
                    val on = aba == k
                    Column(Modifier.clickable { aba = k }) {
                        Mono(n, cor = if (on) Ed.creme else Ed.suave, tamanho = 13.sp)
                        Box(Modifier.padding(top = 4.dp).height(1.5.dp).width(if (on) 60.dp else 0.dp).background(Ed.creme))
                    }
                }
            }
            Box(Modifier.clickable { nav.navigate("buscar") }.padding(10.dp)) { Icon(IconesEd.lupa, "Buscar", tint = Ed.creme, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
private fun PaginaLinha(o: ObraPublica, nav: NavHostController) {
    val app = NovelyApp.instancia
    val escopo = rememberCoroutineScope()
    val capa by produceState<ByteArray?>(null, o.id) { value = CachePoster.capa(o.id) }
    val img = imagemDeBytes(capa ?: o.capaMini)
    val frase by produceState<String?>(null, o.id) {
        val t = try { app.pub.inicioDaObra(o.id) } catch (_: Exception) { "" }
        // a primeira frase (ou as duas primeiras, se a primeira for curta)
        val frases = Regex("""[^.!?…]+[.!?…]+["”»]?""").findAll(t).map { it.value.trim() }.toList()
        var r = frases.firstOrNull() ?: t
        if (r.length < 50 && frases.size > 1) r += " " + frases[1]
        value = r.take(220).ifBlank { o.sinopse.take(220) }
    }
    var seguindoAutor by remember(o.id) { mutableStateOf<Boolean?>(null) }
    var seguindoObra by remember(o.id) { mutableStateOf(false) }
    LaunchedEffect(o.id) {
        seguindoObra = try { app.pub.seguindo(o.id) } catch (_: Exception) { false }
        seguindoAutor = try { app.prefs.autoresSeguidos.valor.split(',').contains(o.autorUid) } catch (_: Exception) { false }
    }
    Column(Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 110.dp, bottom = 96.dp)) {
        Mono("Primeiras linhas", tamanho = 12.sp, cor = Ed.creme.copy(alpha = 0.7f))
        Text("“", color = Ed.laranja, fontFamily = InstrumentSerif, fontSize = 92.sp, lineHeight = 70.sp, modifier = Modifier.height(64.dp))
        Box(Modifier.weight(1f).fillMaxWidth().padding(end = 26.dp)) {
            val f = frase
            if (f == null) Mono("…", tamanho = 18.sp)
            else Column {
                Serif(f, if (f.length > 140) 32.sp else 42.sp, italico = true, maxLinhas = 7)
                Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mono("— Capítulo 1", tamanho = 11.sp)
                    Box(Modifier.weight(1f).padding(horizontal = 14.dp).height(1.dp).background(Ed.suave.copy(alpha = 0.4f)))
                    Mono("Pág. 1", tamanho = 11.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp, 92.dp).background(Ed.superficie)) { if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Serif(o.titulo, 30.sp, maxLinhas = 2)
                Text("por ${o.autorNome}", color = Ed.creme.copy(alpha = 0.75f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { nav.navigate("autor/${o.autorUid}") })
            }
            val sa = seguindoAutor
            if (sa != null && o.autorUid != app.pub.meuUid) Row(Modifier.border(1.dp, Ed.suave).clickable {
                seguindoAutor = !sa; escopo.launch { try { app.pub.seguirAutor(o.autorUid, !sa) } catch (_: Exception) { seguindoAutor = sa } }
            }.padding(horizontal = 12.dp, vertical = 10.dp)) { Mono(if (sa) "✓ Seguindo" else "+ Seguir", cor = Ed.creme, tamanho = 11.sp) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BotaoCreme("Ler capítulo 1", Modifier.weight(1f)) { nav.navigate("pub/${o.id}") }
            Box(Modifier.size(54.dp).border(1.dp, Ed.creme).clickable {
                val s = seguindoObra; seguindoObra = !s
                escopo.launch { try { app.pub.seguir(o.id, !s) } catch (_: Exception) { seguindoObra = s } }
            }, contentAlignment = Alignment.Center) {
                Icon(if (seguindoObra) IconesEd.marcadorCheio else IconesEd.marcador, if (seguindoObra) "Seguindo a obra" else "Seguir a obra", tint = Ed.creme, modifier = Modifier.size(22.dp))
            }
        }
        Mono("↑ deslize para a próxima", Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp), tamanho = 11.sp)
    }
}

@Composable
private fun LinhaRanking(pos: Int, o: ObraPublica, numerar: Boolean, abrir: () -> Unit) {
    val capa by produceState<ByteArray?>(null, o.id) { value = CachePoster.capa(o.id) }
    val img = imagemDeBytes(capa ?: o.capaMini)
    Column(Modifier.fillMaxWidth().clickable(onClick = abrir)) {
        Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (numerar) Serif("$pos", 34.sp, Modifier.width(40.dp), if (pos == 1) Ed.laranja else Ed.creme)
            Box(Modifier.size(52.dp, 74.dp).background(Ed.superficie)) { if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Serif(o.titulo, 24.sp, maxLinhas = 2)
                Text("${o.autorNome} · ${o.numCaps} caps", color = Ed.suave, fontSize = 13.sp, maxLines = 1)
            }
            if (o.classificacao.contains("18")) Box(Modifier.border(1.dp, Ed.laranja).padding(horizontal = 6.dp, vertical = 2.dp)) { Mono("+18", cor = Ed.laranja, tamanho = 10.sp) }
        }
        Fio()
    }
}

// ───────────────────────── ESCREVER ─────────────────────────

@Composable
fun TelaMesa(nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val escopo = rememberCoroutineScope()
    val obras by remember { repo.resumos() }.collectAsState(initial = emptyList())
    val hoje by app.prefs.palavrasHoje.estado.collectAsState()
    val meta by app.prefs.metaDiaria.estado.collectAsState()
    val seq by app.prefs.sequencia.estado.collectAsState()
    val estSinc by app.sinc.estado.collectAsState()
    val ultSinc by app.prefs.ultimaSinc.estado.collectAsState()
    val minhas = obras.filter { it.minha && !it.trancada }.sortedByDescending { it.atualizadaEm }
    var criando by remember { mutableStateOf(false) }
    var escolherFichas by remember { mutableStateOf(false) }
    val atual = minhas.firstOrNull()
    val caps by produceState(emptyList<com.huryel.novely.data.Capitulo>(), atual?.id, atual?.atualizadaEm) { value = atual?.let { repo.dao.capitulosAgora(it.id) } ?: emptyList() }

    Column(Modifier.fillMaxSize().background(Ed.fundo).verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 120.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mono("Mesa do escritor", Modifier.weight(1f), tamanho = 12.sp)
            Mono("+ Nova obra", Modifier.clickable { criando = true }.padding(6.dp), Ed.laranja, 12.sp, FontWeight.Medium)
        }
        // ── palavras de hoje ──
        Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Serif(mil(hoje), if (mil(hoje).length > 6) 76.sp else 96.sp, maxLinhas = 1)
                Text("palavras hoje · meta de ${mil(meta.toLong())}", color = Ed.creme.copy(alpha = 0.85f), fontFamily = PlexMono, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
            val vezes = hoje / meta.coerceAtLeast(1f)
            Box(Modifier.size(112.dp).border(2.dp, Ed.laranja, CircleShape).clickable { nav.navigate("ajustes/escrita") }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Serif(if (vezes >= 1f) "${vezes.toInt()}×" else "${(vezes * 100).toInt()}%", 34.sp, cor = Ed.laranja, italico = true)
                    Mono("a meta", cor = Ed.laranja, tamanho = 9.sp)
                }
            }
        }
        // ── calendário do mês ──
        val cal = remember { Calendar.getInstance() }
        val dias = remember(hoje) { app.prefs.escritaDias() }
        val mesNome = remember { SimpleDateFormat("MMMM", Locale("pt", "BR")).format(cal.time) }
        Row(Modifier.padding(top = 34.dp, bottom = 12.dp)) {
            Mono(mesNome, Modifier.weight(1f), Ed.creme, 12.sp)
            if (seq > 0) Mono("$seq ${if (seq == 1L) "dia seguido" else "dias seguidos"}", cor = Ed.laranja, tamanho = 12.sp)
        }
        val hojeDia = cal.get(Calendar.DAY_OF_MONTH)
        val ultimo = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prefixo = SimpleDateFormat("yyyyMM", Locale.US).format(cal.time)
        (1..ultimo).chunked(7).forEach { semana ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                semana.forEach { d ->
                    val escreveu = (dias[prefixo + dois(d)] ?: 0) > 0
                    val (fundo, borda, cor) = when {
                        d == hojeDia -> Triple(Ed.laranja, Ed.laranja, Ed.fundo)
                        escreveu -> Triple(Ed.creme, Ed.creme, Ed.fundo)
                        d < hojeDia -> Triple(Color.Transparent, Ed.linha, Ed.suave)
                        else -> Triple(Color.Transparent, Ed.linha.copy(alpha = 0.5f), Ed.apagado)
                    }
                    Box(Modifier.weight(1f).height(36.dp).background(fundo).border(1.dp, borda), contentAlignment = Alignment.Center) {
                        Text("$d", color = cor, fontFamily = PlexMono, fontSize = 12.sp, fontWeight = if (d == hojeDia) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                repeat(7 - semana.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(Ed.creme to "Escreveu", Ed.laranja to "Hoje", Color.Transparent to "Por vir").forEach { (c, n) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(c).border(1.dp, if (c == Color.Transparent) Ed.suave else c))
                    Mono(n, Modifier.padding(start = 6.dp), tamanho = 10.sp)
                }
            }
        }
        // ── obra atual ──
        if (atual != null) {
            val rascunho = caps.filter { it.rascunho }.maxByOrNull { it.atualizadoEm }
            val publicados = caps.count { !it.rascunho }
            Row(Modifier.padding(top = 38.dp).clickable { nav.navigate("obra/${atual.id}") }, verticalAlignment = Alignment.CenterVertically) {
                CapaFaixa(atual, 92.dp, 130.dp)
                Column(Modifier.padding(start = 18.dp)) {
                    Serif(atual.titulo, 40.sp, maxLinhas = 2)
                    Mono(listOfNotNull(rascunho?.let { "cap. ${dois(it.ordem.toInt().coerceAtLeast(1))} rascunho" }, "$publicados publicados").joinToString(" · "),
                        Modifier.padding(top = 6.dp), tamanho = 11.sp, maxLinhas = 2)
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Ed.verde))
                        Mono(textoSinc(estSinc, ultSinc), Modifier.padding(start = 7.dp).clickable { nav.navigate("ajustes/conta") }, Ed.verde, 10.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotaoCreme(if (rascunho != null) "Abrir rascunho" else "Escrever", Modifier.weight(1.6f)) {
                    escopo.launch {
                        val alvo = (rascunho ?: caps.maxByOrNull { it.atualizadoEm })?.id ?: repo.novoCapitulo(atual.id, "", null, emBranco = true)
                        nav.navigate("editor/$alvo")
                    }
                }
                BotaoLinha("Novo capítulo", Modifier.weight(1f)) {
                    escopo.launch { nav.navigate("editor/${repo.novoCapitulo(atual.id, "Capítulo ${atual.totalCapitulos + 1}", null)}") }
                }
            }
            if (minhas.size > 1) {
                Mono("Outras obras", Modifier.padding(top = 30.dp, bottom = 4.dp), tamanho = 11.sp)
                minhas.drop(1).forEach { o ->
                    Row(Modifier.fillMaxWidth().clickable { nav.navigate("obra/${o.id}") }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.size(36.dp, 52.dp), 6.sp, semCantos = true)
                        Serif(o.titulo, 24.sp, Modifier.weight(1f).padding(start = 14.dp), maxLinhas = 1)
                        Mono("${o.totalCapitulos} caps", tamanho = 10.sp)
                    }
                    Fio()
                }
            }
        } else {
            Serif("Sua primeira história começa aqui.", 34.sp, Modifier.padding(top = 38.dp))
            BotaoCreme("Criar obra", Modifier.fillMaxWidth().padding(top = 18.dp)) { criando = true }
        }
        // ── sumário de ferramentas ──
        Mono("Sumário de ferramentas", Modifier.padding(top = 36.dp, bottom = 6.dp), tamanho = 12.sp)
        val ferramentas = listOfNotNull(
            atual?.let { Triple("Estúdio de IA", "prompts com o contexto da obra") { nav.navigate("ia/${it.id}") } },
            Triple("Personagens", "@ e & no texto") { if (minhas.size == 1) nav.navigate("fichas/${minhas[0].id}") else escolherFichas = true },
            Triple("Gerador de nomes", "coreano, japonês, fantasia") { nav.navigate("nomes") },
            Triple("Colaboração", "coautores por código") { nav.navigate("colaborar") },
            Triple("Importar TXT ou Word", "traga um livro pronto") { nav.navigate("importar") },
            Triple("Painel do escritor", "leituras e métricas") { nav.navigate("painel") },
        )
        ferramentas.forEachIndexed { i, (nome, _, acao) ->
            Row(Modifier.fillMaxWidth().clickable(onClick = acao).padding(vertical = 10.dp), verticalAlignment = Alignment.Bottom) {
                Text(dois(i + 1), color = Ed.suave, fontFamily = PlexMono, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp, bottom = 3.dp))
                Serif(nome, 28.sp, maxLinhas = 1)
                Canvas(Modifier.weight(1f).height(6.dp).padding(horizontal = 10.dp)) {
                    drawLine(Ed.suave.copy(alpha = 0.6f), Offset(0f, size.height), Offset(size.width, size.height), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f)))
                }
                Text("→", color = Ed.creme, fontSize = 16.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        Box(Modifier.padding(top = 26.dp).fillMaxWidth().drawBehind {
            drawRect(Ed.suave.copy(alpha = 0.5f), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
        }.padding(16.dp)) {
            Text(androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = Ed.laranja, fontFamily = PlexMono, letterSpacing = 2.sp)); append("DICA  "); pop()
                append("Escreva @Nome, descrição… para criar um personagem e &Lugar, … para um lugar. As fichas são montadas sozinhas.")
            }, color = Ed.creme.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
    if (criando) DialogoCriarObra(nav) { criando = false }
    if (escolherFichas) androidx.compose.material3.AlertDialog(onDismissRequest = { escolherFichas = false }, title = { Text("Personagens de qual obra?") },
        text = {
            Column {
                if (minhas.isEmpty()) Text("Crie uma obra primeiro.")
                minhas.forEach { o -> Text(o.titulo, modifier = Modifier.fillMaxWidth().clickable { escolherFichas = false; nav.navigate("fichas/${o.id}") }.padding(vertical = 12.dp)) }
            }
        }, confirmButton = { androidx.compose.material3.TextButton(onClick = { escolherFichas = false }) { Text("Fechar") } })
}

// ───────────────────────── EU ─────────────────────────

@Composable
fun TelaEu(nav: NavHostController) {
    val app = NovelyApp.instancia
    val prefs = app.prefs
    val obras by remember { app.repo.resumos() }.collectAsState(initial = emptyList())
    val nome by prefs.autorNome.estado.collectAsState()
    val bio by prefs.bio.estado.collectAsState()
    val seq by prefs.sequencia.estado.collectAsState()
    val xp by prefs.xp.estado.collectAsState()
    val foto by prefs.fotoPerfil.estado.collectAsState()
    val bannerArq by prefs.bannerPerfil.estado.collectAsState()
    val banner = rememberImagem(bannerArq.ifEmpty { null }, 1400)
    val img = rememberImagem(foto.ifEmpty { null }, 600)
    var editando by remember { mutableStateOf(false) }
    val minhas = obras.filter { it.minha && !it.trancada }.sortedByDescending { it.atualizadaEm }
    val horasLidas = remember { prefs.estatisticas().values.sumOf { it.first } / 3600f }
    val meuPerfil by produceState<com.huryel.novely.data.PerfilPublico?>(null) { value = CachePoster.perfil(app.pub.meuUid) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxSize().background(Ed.fundo).verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
        Box(Modifier.fillMaxWidth().height(340.dp)) {
            if (banner != null) Image(banner, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A1E14), Ed.fundo))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Ed.fundo.copy(alpha = 0.4f), 0.3f to Color.Transparent, 0.65f to Color.Transparent, 1f to Ed.fundo)))
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Mono("Página do autor", Modifier.weight(1f), Ed.creme, 12.sp)
                Box(Modifier.clickable {
                    val t = "Leia minhas histórias no Novely: ${nome.ifBlank { "eu" }}"
                    ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, t), null))
                }.padding(10.dp)) { Icon(IconesEd.partilhar, "Compartilhar", tint = Ed.creme, modifier = Modifier.size(24.dp)) }
                Box(Modifier.clickable { nav.navigate("ajustes") }.padding(10.dp)) { Icon(Icons.Filled.Settings, "Ajustes", tint = Ed.creme, modifier = Modifier.size(24.dp)) }
            }
            Box(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 4.dp).size(120.dp).border(2.dp, Ed.laranja, CircleShape).padding(5.dp).clip(CircleShape)
                .background(Ed.superficie).clickable { editando = true }, contentAlignment = Alignment.Center) {
                if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Serif(nome.trim().take(1).uppercase().ifEmpty { "?" }, 52.sp)
            }
        }
        Column(Modifier.padding(horizontal = 22.dp)) {
            Serif(nome.ifBlank { "Seu nome" }, if (nome.length > 12) 54.sp else 72.sp, Modifier.padding(top = 10.dp), maxLinhas = 2)
            Serif(if (minhas.isNotEmpty()) "autor de ${minhas.first().titulo}" else "leitor", 24.sp, cor = Ed.creme.copy(alpha = 0.8f), italico = true, maxLinhas = 1)
            Fio(Modifier.padding(top = 22.dp))
            Row(Modifier.fillMaxWidth().clickable { editando = true }.padding(vertical = 18.dp)) {
                Text("“", color = Ed.laranja, fontFamily = InstrumentSerif, fontSize = 40.sp, lineHeight = 34.sp, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Serif(bio.ifBlank { "Diga, em uma frase, o que move as suas histórias." }, 23.sp, cor = if (bio.isBlank()) Ed.suave else Ed.creme.copy(alpha = 0.9f), italico = true)
                    if (bio.isBlank()) Mono("Toque para escrever sua bio", Modifier.padding(top = 10.dp), Ed.laranja, 10.sp)
                }
            }
            Fio()
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotaoCreme("Editar página", Modifier.weight(1f), seta = false) { editando = true }
                BotaoLinha("Ver como leitor", Modifier.weight(1f)) { nav.navigate("autor/${app.pub.meuUid}") }
            }
            // números
            Column(Modifier.padding(top = 24.dp)) {
                Fio(cor = Ed.creme.copy(alpha = 0.7f))
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    listOf(mil(meuPerfil?.seguidores ?: 0) to "seguidores", mil(minhas.sumOf { it.totalPalavras }) to "palavras", "%.0f h".format(horasLidas) to "lendo")
                        .forEachIndexed { i, (v, r) ->
                            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(Ed.linha))
                            Column(Modifier.weight(1f).padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Serif(v, 40.sp, maxLinhas = 1)
                                Mono(r, Modifier.padding(top = 6.dp), tamanho = 10.sp)
                            }
                        }
                }
                Fio(cor = Ed.creme.copy(alpha = 0.7f))
            }
            // obras
            if (minhas.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 16.dp), verticalAlignment = Alignment.Bottom) {
                    Serif("Obras", 38.sp, Modifier.weight(1f))
                    Mono("${dois(minhas.size)} ${if (minhas.size == 1) "título" else "títulos"}", tamanho = 11.sp)
                }
                minhas.forEach { o -> ObraEu(o) { nav.navigate("obra/${o.id}") } }
            }
            JornadaEu(xp, seq.toInt())
            MissoesEu()
            val mes = remember { SimpleDateFormat("MMMM", Locale("pt", "BR")).format(java.util.Date()) }
            Row(Modifier.padding(top = 22.dp).fillMaxWidth().background(Ed.creme).clickable { nav.navigate("retro") }.padding(horizontal = 26.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Mono("Edição de $mes", cor = Ed.laranja, tamanho = 11.sp)
                    Serif("Seu mês no Novely", 32.sp, Modifier.padding(top = 4.dp), cor = Ed.fundo)
                }
                Text("→", color = Ed.fundo, fontSize = 24.sp)
            }
            // mais coisas da conta
            Mono("Mais", Modifier.padding(top = 36.dp, bottom = 4.dp), tamanho = 12.sp)
            listOf(
                "Painel do escritor" to "painel", "Conta e nuvem" to "ajustes/conta", "Lixeira" to "lixeira", "Ajustes" to "ajustes",
            ).forEach { (n, r) ->
                Row(Modifier.fillMaxWidth().clickable { nav.navigate(r) }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Serif(n, 24.sp, Modifier.weight(1f)); Text("→", color = Ed.creme)
                }
                Fio()
            }
        }
        Spacer(Modifier.height(20.dp))
        RankingSemana()
        CartaoMoedas()
        SecaoConquistas()
        SecaoCitacoes()
    }
    if (editando) DialogoPerfil { editando = false }
}

@Composable
private fun ObraEu(o: ResumoObra, abrir: () -> Unit) {
    val ultimo by produceState<com.huryel.novely.data.Capitulo?>(null, o.id, o.atualizadaEm) {
        value = NovelyApp.instancia.repo.dao.capitulosAgora(o.id).filter { !it.rascunho }.maxByOrNull { it.ordem }
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 26.dp).clickable(onClick = abrir)) {
        CapaFaixa(o, 150.dp, 216.dp, 8.dp)
        Column(Modifier.weight(1f).padding(start = 22.dp).height(224.dp)) {
            Mono(if (o.status == StatusObra.CONCLUIDA) "Concluída" else "Em andamento", cor = Ed.laranja, tamanho = 10.sp)
            Serif(o.titulo, 40.sp, Modifier.padding(top = 6.dp), maxLinhas = 2)
            Text("${o.totalCapitulos} capítulos\n${mil(o.totalPalavras)} palavras", color = Ed.creme.copy(alpha = 0.85f), fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.weight(1f))
            Fio()
            ultimo?.let { Mono("Último · cap. ${dois(it.ordem.toInt().coerceAtLeast(1))} ${it.titulo}", Modifier.padding(top = 10.dp), tamanho = 10.sp, maxLinhas = 2) }
        }
    }
}

@Composable
private fun JornadaEu(xp: Long, seq: Int) {
    val i = remember(xp) { Progresso.info(xp) }
    Row(Modifier.fillMaxWidth().padding(top = 26.dp)) {
        Mono("Sua jornada", Modifier.weight(1f), tamanho = 11.sp)
        if (seq > 0) Mono("$seq ${if (seq == 1) "dia seguido" else "dias seguidos"}", cor = Ed.laranja, tamanho = 11.sp)
    }
    Text(androidx.compose.ui.text.buildAnnotatedString {
        append("Capítulo ${romano(i.nivel)} — ")
        pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic)); append(Progresso.titulo(i.nivel)); pop()
    }, color = Ed.creme, fontFamily = InstrumentSerif, fontSize = 34.sp, lineHeight = 38.sp, modifier = Modifier.padding(top = 8.dp))
    // linha do tempo: dois capítulos antes, o atual e dois depois
    val ini = (i.nivel - 2).coerceAtLeast(1)
    val niveis = (ini until ini + 5).toList()
    Box(Modifier.fillMaxWidth().padding(top = 18.dp).height(30.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2; val passo = (size.width - 24.dp.toPx()) / 4; val x0 = 12.dp.toPx()
            val ate = niveis.indexOf(i.nivel).coerceAtLeast(0)
            drawLine(Ed.creme, Offset(x0, y), Offset(x0 + passo * ate + passo * 0.3f * i.fracao, y), 1.5f)
            drawLine(Ed.apagado, Offset(x0 + passo * ate, y), Offset(x0 + passo * 4, y), 1f)
            niveis.forEachIndexed { k, n ->
                val c = Offset(x0 + passo * k, y)
                when {
                    n < i.nivel -> drawCircle(Ed.creme, 7.dp.toPx(), c)
                    n == i.nivel -> { drawCircle(Ed.laranja, 12.dp.toPx(), c); drawCircle(Ed.laranja.copy(alpha = 0.25f), 16.dp.toPx(), c) }
                    else -> drawCircle(Ed.suave, 6.dp.toPx(), c, style = Stroke(1.2f))
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        niveis.forEachIndexed { k, n ->
            Text(romano(n), color = if (n == i.nivel) Ed.creme else Ed.suave, fontFamily = InstrumentSerif, fontSize = if (n == i.nivel) 20.sp else 17.sp,
                textAlign = when (k) { 0 -> TextAlign.Start; 4 -> TextAlign.End; else -> TextAlign.Center }, modifier = Modifier.weight(1f))
        }
    }
    Text("${i.noNivel} / ${i.precisa} XP · faltam ${i.precisa - i.noNivel} para o capítulo ${romano(i.nivel + 1)}", color = Ed.suave, fontFamily = PlexMono,
        fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun MissoesEu() {
    val prefs = NovelyApp.instancia.prefs
    val dia by prefs.missoesDia.estado.collectAsState()
    val bau by prefs.bauDia.estado.collectAsState()
    val ms = remember(dia, bau) { Progresso.missoes() }
    var aberto by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = 22.dp).border(1.dp, Ed.linha.copy(alpha = 1f))) {
        Row(Modifier.fillMaxWidth().clickable { aberto = !aberto }.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Missões de hoje", color = Ed.creme, fontFamily = Inter, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Mono("${ms.count { it.pronta }} / ${ms.size}", Modifier.padding(end = 12.dp), tamanho = 11.sp)
            Text(if (aberto) "↑" else "→", color = Ed.creme, fontSize = 18.sp)
        }
        if (aberto) {
            ms.forEach { m ->
                Fio()
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).border(1.dp, if (m.pronta) Ed.creme else Ed.suave).background(if (m.pronta) Ed.creme else Color.Transparent), contentAlignment = Alignment.Center) {
                        if (m.pronta) Text("✓", color = Ed.fundo, fontSize = 11.sp)
                    }
                    Text(m.titulo, color = if (m.pronta) Ed.suave else Ed.creme, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    Mono("${m.feito.coerceAtMost(m.alvo)}/${m.alvo}", tamanho = 10.sp)
                }
            }
            if (Progresso.bauPronto()) {
                Fio()
                Row(Modifier.fillMaxWidth().background(Ed.laranja).clickable { Progresso.abrirBau() }.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Mono("Abrir o baú de hoje · 10 moedas + 50 XP", cor = Ed.fundo, tamanho = 11.sp, peso = FontWeight.Medium)
                }
            }
        }
    }
}
