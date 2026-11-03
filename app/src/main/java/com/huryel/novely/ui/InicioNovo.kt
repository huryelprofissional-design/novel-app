package com.huryel.novely.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.GENEROS
import com.huryel.novely.data.ObraPublica
import com.huryel.novely.data.ResumoObra
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ───────────────────────── catálogo em cache (não baixa de novo a cada troca de aba) ─────────────────────────

object CacheCatalogo {
    private val dados = HashMap<String, Pair<Long, List<ObraPublica>>>()
    suspend fun pegar(modo: String, forcar: Boolean = false): List<ObraPublica> {
        val c = dados[modo]
        if (!forcar && c != null && System.currentTimeMillis() - c.first < 5 * 60_000) return c.second
        val l = NovelyApp.instancia.pub.catalogo(modo)
        dados[modo] = System.currentTimeMillis() to l
        return l
    }
    fun ja(modo: String): List<ObraPublica>? = dados[modo]?.second
}

/** Capas grandes (900 px) e perfis de autores, guardados enquanto o app está aberto. */
object CachePoster {
    private val capas = HashMap<String, ByteArray?>()
    private val perfis = HashMap<String, com.huryel.novely.data.PerfilPublico?>()
    suspend fun capa(pid: String): ByteArray? {
        if (capas.containsKey(pid)) return capas[pid]
        val b = try { NovelyApp.instancia.pub.imagem(pid, "capa") } catch (_: Exception) { return null }
        capas[pid] = b; return b
    }
    suspend fun perfil(uid: String): com.huryel.novely.data.PerfilPublico? {
        if (perfis.containsKey(uid)) return perfis[uid]
        val r = try { NovelyApp.instancia.pub.perfil(uid) } catch (_: Exception) { return null }
        perfis[uid] = r; return r
    }
    fun perfilJa(uid: String) = perfis[uid]
}

/** "agora", "12 min", "3 h", "2 d", "5 sem". */
fun haQuanto(ms: Long): String {
    val d = (System.currentTimeMillis() - ms).coerceAtLeast(0) / 60_000
    return when { d < 1 -> "agora"; d < 60 -> "$d min"; d < 1440 -> "${d / 60} h"; d < 10080 -> "${d / 1440} d"; else -> "${d / 10080} sem" }
}

/** Foto do autor (baixa o perfil uma vez); sem foto, a inicial num degradê. */
@Composable
fun AvatarAutor(uid: String, nome: String, tamanho: Dp, quadrado: Boolean = false) {
    val pf by produceState(CachePoster.perfilJa(uid), uid) { value = CachePoster.perfil(uid) }
    val img = imagemDeBytes(pf?.foto)
    val (a, b) = coresGenero(uid)
    Box(Modifier.size(tamanho).clip(if (quadrado) RoundedCornerShape(tamanho * 0.3f) else CircleShape)
        .background(Brush.linearGradient(listOf(a.copy(red = (a.red + 0.25f).coerceAtMost(1f)), b))), contentAlignment = Alignment.Center) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text((pf?.nome ?: nome).trim().take(1).uppercase().ifEmpty { "?" }, fontWeight = FontWeight.Black, fontSize = (tamanho.value * 0.4f).sp, color = Color.White)
    }
}

// ───────────────────────── gêneros com cor própria ─────────────────────────

private val CORES_GENERO = listOf(
    0xFF5B2A86 to 0xFF1A0E2B, 0xFF8A2C2C to 0xFF2A0D0D, 0xFF1F5E6B to 0xFF0A2126, 0xFF8A5A1C to 0xFF2B1A07,
    0xFF2D4F8A to 0xFF0C1729, 0xFF6B2A5E to 0xFF240C1F, 0xFF2F6B3A to 0xFF0C2410, 0xFF7A3B1F to 0xFF2A130A,
    0xFF4A3D8F to 0xFF14102E, 0xFF8A2C5A to 0xFF2B0C1C, 0xFF3D6B6B to 0xFF0F2222, 0xFF6B5A1F to 0xFF241D08,
)
fun coresGenero(g: String): Pair<Color, Color> { val (a, b) = CORES_GENERO[(g.hashCode() and 0x7fffffff) % CORES_GENERO.size]; return Color(a) to Color(b) }
private val SIMBOLO_GENERO = mapOf("Fantasia" to "✧", "Murim" to "⚔", "Romance" to "♥", "Sistema" to "▣", "Terror" to "☾", "Mistério" to "?", "Ação" to "✶",
    "Aventura" to "➶", "Ficção científica" to "◎", "Comédia" to "☺", "Cultivo" to "☯", "Regressão" to "↺", "Reencarnação" to "∞", "Isekai" to "⇄",
    "Histórico" to "♜", "Drama" to "◐", "Slice of life" to "☕", "Wuxia" to "⚔", "Xianxia" to "☁", "Sobrenatural" to "✦", "Vilã" to "♛", "Esportes" to "◉")

@Composable
fun TileGenero(g: String, modifier: Modifier = Modifier, aoTocar: () -> Unit) {
    val (a, b) = coresGenero(g)
    Box(modifier.height(78.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(a, b))).clickable(onClick = aoTocar)) {
        Text(SIMBOLO_GENERO[g] ?: "◆", fontSize = 46.sp, color = Color.White.copy(alpha = 0.13f), fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp).graphicsLayer { rotationZ = -12f })
        Text(g, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White, maxLines = 2, lineHeight = 17.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).fillMaxWidth(0.8f))
    }
}

// ───────────────────────── peças ─────────────────────────

@Composable
fun Esqueleto(largura: Dp, altura: Dp, raio: Dp = 12.dp) {
    val t = rememberInfiniteTransition(label = "esq")
    val a by t.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    val cor = LocalPaleta.current.superficie
    // a animação só redesenha (não recompõe): esqueletos não pesam enquanto carrega
    Box(Modifier.width(largura).height(altura).clip(RoundedCornerShape(raio)).drawBehind { drawRect(cor.copy(alpha = a)) })
}

@Composable
fun CabSecao(titulo: String, sub: String? = null, link: String? = null, aoTocar: () -> Unit = {}) {
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 26.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = (-0.2).sp)
            if (sub != null) Text(sub, fontSize = 12.5.sp, color = p.textoSuave)
        }
        if (link != null) Text(link, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.textoSuave, modifier = Modifier.clickable(onClick = aoTocar).padding(4.dp))
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Apertavel(modifier: Modifier = Modifier, aoTocar: () -> Unit, segurar: (() -> Unit)? = null, conteudo: @Composable () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val apertado by i.collectIsPressedAsState()
    val e by animateFloatAsState(if (apertado) 0.95f else 1f, label = "e")
    val vibrar = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(modifier.graphicsLayer { scaleX = e; scaleY = e }.combinedClickable(i, null, onClick = aoTocar,
        onLongClick = segurar?.let { s -> { vibrar.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); s() } })) { conteudo() }
}

/** Prévia ao segurar um pôster/capa: sinopse e primeira frase sem abrir a obra. */
@Composable
fun PreviaObra(o: ObraPublica, nav: NavHostController, fechar: () -> Unit) {
    val bytes by produceState(o.capaMini, o.id) { value = CachePoster.capa(o.id) ?: o.capaMini }
    val inicio by produceState("", o.id) { value = NovelyApp.instancia.pub.inicioDaObra(o.id) }
    val img = imagemDeBytes(bytes)
    val cor = rememberCorBytes(o.capaMini) ?: com.huryel.novely.ui.theme.Laranja
    androidx.compose.ui.window.Dialog(onDismissRequest = fechar, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(horizontal = 26.dp).fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color(0xF01A1A1C))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(30.dp))) {
            Box(Modifier.fillMaxWidth().height(210.dp)) {
                if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color(0xF01A1A1C))))
                Text(o.titulo.uppercase(), fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 28.sp, color = Color.White, maxLines = 3,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeloPoster(if (System.currentTimeMillis() - o.atualizadaEm < 3 * 86_400_000L) "NOVO CAP" else if (o.status == StatusObra.CONCLUIDA) "CONCLUÍDA" else "EM ANDAMENTO", cor)
                    Text("  ${o.generos.take(2).joinToString(" · ")} · ${o.numCaps} caps", fontSize = 11.5.sp, color = Color(0xFFBBBBBB), maxLines = 1)
                }
                if (o.sinopse.isNotBlank()) Text(o.sinopse, fontSize = 13.5.sp, lineHeight = 19.sp, color = Color(0xFFDDDDDD), maxLines = 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                if (inicio.isNotBlank()) Text("“$inicio…”", fontFamily = com.huryel.novely.ui.theme.Newsreader, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF9A9A9A),
                    maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoBranco("Ver obra", Modifier.weight(1f)) { fechar(); nav.navigate("pub/${o.id}") }
                    Box(Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(29.dp)).background(Color.White.copy(alpha = 0.12f)).clickable(onClick = fechar), contentAlignment = Alignment.Center) {
                        Text("Fechar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun notaTxt(o: ObraPublica) = if (o.numEstrelas > 0) "★ %.1f".format(o.nota).replace('.', ',') else "novo"
private fun mil(n: Long) = when { n >= 1_000_000 -> "%.1f mi".format(n / 1e6).replace('.', ','); n >= 1000 -> "%.1f mil".format(n / 1e3).replace('.', ','); else -> "$n" }

@Composable
fun CartaoPublico(o: ObraPublica, largura: Dp?, posicao: Int? = null, segurar: (() -> Unit)? = null, aoTocar: () -> Unit) {
    val p = LocalPaleta.current
    Apertavel(if (largura != null) Modifier.width(largura) else Modifier.fillMaxWidth(), aoTocar, segurar) {
        Column {
            Box {
                CapaPublica(o, Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(20.dp)))
                if (posicao == null) Text(if (o.numEstrelas > 0) notaTxt(o) else "NOVO", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(9.dp)).background(com.huryel.novely.ui.theme.AzulNota).padding(horizontal = 8.dp, vertical = 2.dp))
                if (o.classificacao == "18") Text("+18", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xCCB3261E)).padding(horizontal = 5.dp, vertical = 1.dp))
                if (posicao != null) Text("$posicao", modifier = Modifier.align(Alignment.BottomStart).offset(x = (-4).dp, y = 16.dp),
                    style = androidx.compose.ui.text.TextStyle(drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(5f), color = p.destaque, fontSize = 46.sp, fontWeight = FontWeight.Black))
            }
            Text(o.titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = if (posicao != null) 12.dp else 8.dp))
            Text(if (posicao != null) "${notaTxt(o)} · ${o.generos.firstOrNull() ?: "${o.numCaps} caps"}" else o.autorNome.ifBlank { o.generos.firstOrNull() ?: "" },
                fontSize = 11.5.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FileiraPublica(lista: List<ObraPublica>?, nav: NavHostController, largura: Dp = 116.dp, ranking: Boolean = false) {
    LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(if (ranking) 16.dp else 12.dp)) {
        if (lista == null) items(4) { Esqueleto(largura, largura / 0.7f) }
        else itemsIndexed(lista, key = { _, o -> o.id }) { i, o ->
            var previa by remember { mutableStateOf(false) }
            if (previa) PreviaObra(o, nav) { previa = false }
            CartaoPublico(o, largura, if (ranking) i + 1 else null, segurar = { previa = true }) { nav.navigate("pub/${o.id}") }
        }
    }
}

// ───────────────────────── pôsteres (estilo C) ─────────────────────────

@Composable
fun PosterObra(o: ObraPublica, nav: NavHostController, selo: String?, largura: Dp = 258.dp) {
    val escopo = rememberCoroutineScope()
    val pub = NovelyApp.instancia.pub
    val bytes by produceState(o.capaMini, o.id) { value = CachePoster.capa(o.id) ?: o.capaMini }
    val img = imagemDeBytes(bytes)
    var segue by remember(o.autorUid) { mutableStateOf(pub.segueAutor(o.autorUid)) }
    var previa by remember { mutableStateOf(false) }
    if (previa) PreviaObra(o, nav) { previa = false }
    Apertavel(Modifier.width(largura).height(largura * 1.3f), { nav.navigate("pub/${o.id}") }, { previa = true }) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))) {
            if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else CapaObra(o.titulo, null, .5f, .5f, 1f, 0xFF3B2A4A, Modifier.fillMaxSize(), 12.sp, semCantos = true, mostrarTitulo = false)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x66000000), 0.25f to Color.Transparent, 0.45f to Color.Transparent, 1f to Color(0xE6000000))))
            if (o.autorUid != pub.meuUid) Row(Modifier.align(Alignment.TopStart).padding(12.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp))
                .background(Color(0x66000000)).clickable { nav.navigate("autor/${o.autorUid}") }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarAutor(o.autorUid, o.autorNome, 34.dp)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(o.autorNome, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${mil(o.seguidores)} SEGUIDORES DA OBRA", fontSize = 8.5.sp, color = Color(0xFFBBBBBB), maxLines = 1, letterSpacing = 0.3.sp)
                }
                Text(if (segue) "Seguindo" else "Seguir", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (segue) Color(0xFFBBBBBB) else Color(0xFFFFD54A),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.12f)).clickable {
                        val novo = !segue; segue = novo
                        escopo.launch { try { pub.seguirAutor(o.autorUid, novo) } catch (_: Exception) { segue = !novo } }
                    }.padding(horizontal = 11.dp, vertical = 5.dp))
            }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(o.titulo.uppercase(), fontWeight = FontWeight.Black, fontSize = 29.sp, lineHeight = 29.sp, letterSpacing = (-0.3).sp, color = Color.White,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selo != null) SeloPoster(selo)
                    Text("●  ${haQuanto(o.atualizadaEm)}", fontSize = 12.sp, color = Color(0xFFDDDDDD), modifier = Modifier.padding(start = if (selo != null) 10.dp else 0.dp))
                    Text("   ▶ ${mil(o.leituras)}", fontSize = 12.sp, color = Color(0xFFDDDDDD), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FileiraPosteres(lista: List<ObraPublica>, nav: NavHostController, selo: (Int, ObraPublica) -> String?) {
    LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(lista, key = { _, o -> o.id }) { i, o -> PosterObra(o, nav, selo(i, o)) }
    }
}

/** Fileira de autores: quadradinhos com foto e bolinha verde quando atualizaram nos últimos 3 dias. */
@Composable
private fun FileiraAutores(autores: List<Pair<String, String>>, ativos: Set<String>, nav: NavHostController) {
    val p = LocalPaleta.current
    LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(autores, key = { it.first }) { (uid, nome) ->
            Column(Modifier.width(60.dp).clickable { nav.navigate("autor/$uid") }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    AvatarAutor(uid, nome, 58.dp, quadrado = true)
                    if (uid in ativos) Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(15.dp).clip(CircleShape).background(p.fundo).padding(2.5.dp)
                        .clip(CircleShape).background(com.huryel.novely.ui.theme.VerdeOnline))
                }
                Text(nome.substringBefore(' ').ifBlank { "Autor" }, fontSize = 11.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

// ───────────────────────── carrossel de destaques ─────────────────────────

@Composable
private fun Carrossel(lista: List<ObraPublica>, nav: NavHostController) {
    val p = LocalPaleta.current
    val estado = rememberPagerState { lista.size }
    LaunchedEffect(lista.size) { while (lista.size > 1) { delay(5500); estado.animateScrollToPage((estado.currentPage + 1) % lista.size) } }
    Column {
        HorizontalPager(estado, contentPadding = PaddingValues(horizontal = 18.dp), pageSpacing = 12.dp) { i ->
            val o = lista[i]
            val img = imagemDeBytes(o.capaMini)
            Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(22.dp)).clickable { nav.navigate("pub/${o.id}") }) {
                if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(18.dp))
                else Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(coresGenero(o.generos.firstOrNull() ?: "").first, Color.Black))))
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF2000000), Color(0x55000000)))))
                CapaPublica(o, Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).width(118.dp).height(170.dp).shadow(18.dp, RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)))
                Column(Modifier.align(Alignment.CenterStart).padding(start = 18.dp).fillMaxWidth(0.52f)) {
                    Text(if (i == 0) "EM ALTA" else "DESTAQUE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = p.sobreDestaque,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(p.destaque).padding(horizontal = 8.dp, vertical = 3.dp))
                    Text(o.titulo, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 25.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                    Text(o.generos.take(2).joinToString(" · "), fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 4.dp))
                    Text("${notaTxt(o)} · ${mil(o.leituras)} leituras", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 2.dp))
                    Text("Ler agora ›", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = p.destaque, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        if (lista.size > 1) Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
            repeat(lista.size) { i ->
                Box(Modifier.padding(horizontal = 3.dp).size(if (i == estado.currentPage) 18.dp else 6.dp, 6.dp).clip(CircleShape)
                    .background(if (i == estado.currentPage) p.destaque else p.borda))
            }
        }
    }
}

// ───────────────────────── ranking em lista (top 5) ─────────────────────────

@Composable
private fun RankingLista(lista: List<ObraPublica>?, nav: NavHostController) {
    val p = LocalPaleta.current
    Column(Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.superficie).border(1.dp, p.borda, RoundedCornerShape(18.dp))) {
        if (lista == null) repeat(3) { Row(Modifier.padding(12.dp)) { Esqueleto(48.dp, 68.dp, 8.dp); Spacer(Modifier.width(12.dp)); Esqueleto(160.dp, 16.dp) } }
        else if (lista.isEmpty()) Text("Ninguém leu nada esta semana ainda. Que tal ser o primeiro?", color = p.textoSuave, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        else lista.take(5).forEachIndexed { i, o ->
            Row(Modifier.fillMaxWidth().clickable { nav.navigate("pub/${o.id}") }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = if (i < 3) p.destaque else p.textoSuave, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                CapaPublica(o, Modifier.width(46.dp).height(66.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(o.titulo, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("por ${o.autorNome} · ${o.generos.firstOrNull() ?: ""}", fontSize = 12.sp, color = p.textoSuave, maxLines = 1)
                    Text("▲ ${mil(o.leiturasSemana)} leituras na semana", fontSize = 11.sp, color = p.destaque2, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (i < minOf(4, lista.lastIndex)) Box(Modifier.padding(start = 92.dp).fillMaxWidth().height(1.dp).background(p.borda))
        }
    }
}

// ───────────────────────── cartão "sua semana" ─────────────────────────

@Composable
private fun SuaSemana(nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val hoje by app.prefs.palavrasHoje.estado.collectAsState()
    val meta by app.prefs.metaDiaria.estado.collectAsState()
    val seq by app.prefs.sequencia.estado.collectAsState()
    val dias = remember {
        val dados = app.prefs.estatisticas(); val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        (6 downTo 0).map { d -> val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -d) }; dados[fmt.format(c.time)]?.first ?: 0 }
    }
    val maior = (dias.maxOrNull() ?: 0).coerceAtLeast(60)
    Row(Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.superficie).border(1.dp, p.borda, RoundedCornerShape(18.dp))
        .clickable { nav.navigate("estatisticas") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("SUA SEMANA", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = p.destaque)
            Text("${dias.sum() / 60} min lendo", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
            Text("Hoje: $hoje/${meta.toInt()} palavras escritas" + if (seq > 0) " · 🔥 $seq" else "", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 2.dp))
        }
        Row(Modifier.height(54.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
            dias.forEachIndexed { i, s ->
                Box(Modifier.width(9.dp).fillMaxHeight((s.toFloat() / maior).coerceIn(0.08f, 1f)).clip(RoundedCornerShape(3.dp))
                    .background(if (i == 6) p.destaque else p.destaque.copy(alpha = 0.35f)))
            }
        }
    }
}

// ───────────────────────── INÍCIO ─────────────────────────

private val SECOES_INICIO = linkedMapOf(
    "AUTORES" to "Seus autores", "DESTAQUES" to "Saindo agora (pôsteres)", "CONT" to "Continuar lendo", "NOVOSCAPS" to "Capítulos novos", "ALTA" to "Populares",
    "GENEROS" to "Explorar gêneros", "SEMANA" to "Ranking da semana", "LENDO" to "Na sua estante", "NOVIDADES" to "Mais novidades",
    "QUERO" to "Quero ler", "MINHAS" to "Suas obras", "SUASEMANA" to "Sua semana", "CONCLUIDAS" to "Concluídas para maratonar",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaInicioNova(nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val obras by remember { app.repo.resumos() }.collectAsState(initial = emptyList())
    val novidadesTxt by app.prefs.novidades.estado.collectAsState()
    val secoesTxt by app.prefs.secoesInicio.estado.collectAsState()
    // migração: lista antiga de seções → todas ligadas
    val secoes = secoesTxt.split(',').toSet().let { s -> if (s.none { it in SECOES_INICIO }) SECOES_INICIO.keys else s }
        .let { s -> if ("v2" in s || s.isEmpty()) s else s + "AUTORES" } // seção nova entra ligada para quem já tinha personalizado
    var alta by remember { mutableStateOf(CacheCatalogo.ja("ALTA")) }
    var semana by remember { mutableStateOf(CacheCatalogo.ja("SEMANA")) }
    var novas by remember { mutableStateOf(CacheCatalogo.ja("NOVIDADES")) }
    var semRede by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf(false) }
    var criando by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try { alta = CacheCatalogo.pegar("ALTA"); semana = CacheCatalogo.pegar("SEMANA"); novas = CacheCatalogo.pegar("NOVIDADES") }
        catch (_: Exception) { semRede = true; alta = alta ?: emptyList(); semana = semana ?: emptyList(); novas = novas ?: emptyList() }
    }
    val hora = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val saudacao = when (hora) { in 5..11 -> "Bom dia"; in 12..17 -> "Boa tarde"; else -> "Boa noite" }
    val nome by app.prefs.autorNome.estado.collectAsState()
    val outras = obras.filter { !it.minha }
    val continuar = obras.filter { it.lidaEm > 0 && it.ultimoCapituloId != null }.maxByOrNull { it.lidaEm }
    val comNovos = novidadesTxt.split('\n').mapNotNull { it.split('|').getOrNull(0)?.toLongOrNull() }.toSet()
    val novosCaps = outras.filter { it.publicaId.isNotEmpty() && it.id in comNovos }
    val lendo = outras.filter { it.status == StatusObra.LENDO }.sortedByDescending { it.lidaEm }
    val minhas = obras.filter { it.minha && !it.trancada }.sortedByDescending { it.atualizadaEm }

    var atualizando by remember { mutableStateOf(false) }
    PullToRefreshBox(atualizando, onRefresh = {
        atualizando = true
        escopo.launch {
            try {
                alta = CacheCatalogo.pegar("ALTA", true); semana = CacheCatalogo.pegar("SEMANA", true); novas = CacheCatalogo.pegar("NOVIDADES", true); semRede = false
                app.sinc.sincronizar(); app.pub.atualizarSeguidas()
            } catch (_: Exception) { semRede = true }
            atualizando = false
        }
    }, modifier = Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        item { TopoAba(null, nav) }
        item {
            Text("$saudacao${if (nome.isNotBlank()) ", ${nome.substringBefore(' ')}" else ""}", fontSize = 13.sp, color = p.textoSuave,
                modifier = Modifier.padding(start = 20.dp, bottom = 2.dp))
        }
        val seguidosTxt = app.prefs.autoresSeguidos.valor
        val catalogo = (novas.orEmpty() + alta.orEmpty()).distinctBy { it.id }
        val ativos = catalogo.filter { System.currentTimeMillis() - it.atualizadaEm < 3 * 86_400_000L }.map { it.autorUid }.toSet()
        if ("AUTORES" in secoes) {
            val seguidos = seguidosTxt.split(',').filter { it.isNotBlank() }
            val nomes = catalogo.associate { it.autorUid to it.autorNome }
            val autores = if (seguidos.isNotEmpty()) seguidos.map { it to (nomes[it] ?: CachePoster.perfilJa(it)?.nome ?: "") }.sortedByDescending { it.first in ativos }
                else catalogo.filter { it.autorUid != app.pub.meuUid }.distinctBy { it.autorUid }.take(10).map { it.autorUid to it.autorNome }
            if (autores.isNotEmpty()) item {
                CabSecao(if (seguidos.isNotEmpty()) "Seus autores" else "Autores no Novely", if (seguidos.isEmpty()) "toque para conhecer e seguir" else null)
                FileiraAutores(autores, ativos, nav)
            }
        }
        if ("DESTAQUES" in secoes) item {
            val d = novas
            when {
                d == null -> { CabSecao("Saindo agora"); Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) { repeat(2) { Esqueleto(258.dp, 335.dp, 26.dp) } } }
                d.isNotEmpty() -> {
                    CabSecao("Saindo agora", "capítulos que acabaram de sair")
                    FileiraPosteres(d.take(10), nav) { _, o -> if (System.currentTimeMillis() - o.atualizadaEm < 3 * 86_400_000L) "NOVO CAP" else null }
                }
                else -> Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF3A1E12), Color(0xFF1A0B24)))).padding(22.dp)) {
                    Text(if (semRede) "SEM CONEXÃO" else "O CATÁLOGO ESTÁ COMEÇANDO", fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 25.sp)
                    Text(if (semRede) "As obras do catálogo aparecem quando a internet voltar. Suas obras continuam aqui embaixo."
                        else "Seja um dos primeiros: publique sua obra e ela aparece aqui para todo mundo.", fontSize = 13.sp, color = p.textoSuave, modifier = Modifier.padding(top = 6.dp))
                    if (!semRede && minhas.isNotEmpty()) BotaoBranco("Publicar “${minhas[0].titulo}”", Modifier.padding(top = 16.dp).fillMaxWidth()) { nav.navigate("publicar/${minhas[0].id}") }
                }
            }
        }
        if ("CONT" in secoes && continuar != null) item {
            CabSecao("Continuar lendo")
            FaixaContinuarNova(continuar) { nav.navigate("leitor/${continuar.ultimoCapituloId}") }
        }
        if ("NOVOSCAPS" in secoes && novosCaps.isNotEmpty()) item {
            CabSecao("Capítulos novos", "das obras que você segue", "Avisos ›") { nav.navigate("avisos") }
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(novosCaps, key = { it.id }) { o -> CapaLocal(o, 116.dp, "NOVO") { nav.navigate("obra/${o.id}") } }
            }
        }
        if ("ALTA" in secoes && alta?.isNotEmpty() != false) item {
            CabSecao("Populares no Novely", "as mais lidas", "Ver tudo ›") { irAba(nav, "descobrir") }
            FileiraPublica(alta, nav, largura = 124.dp)
        }
        if ("GENEROS" in secoes) item {
            CabSecao("Explorar gêneros", "${GENEROS.size} gêneros", "Todos ›") { nav.navigate("generos") }
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(GENEROS.take(14)) { g -> TileGenero(g, Modifier.width(140.dp)) { nav.navigate("genero/$g") } }
            }
        }
        if ("SEMANA" in secoes && alta?.isNotEmpty() != false) item {
            CabSecao("Ranking da semana", "o que mais subiu nos últimos dias")
            RankingLista(semana, nav)
        }
        if ("LENDO" in secoes && lendo.isNotEmpty()) item {
            CabSecao("Na sua estante", "${lendo.size} obras", "Biblioteca ›") { irAba(nav, "biblioteca") }
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(lendo, key = { it.id }) { o -> CapaLocal(o, 112.dp, null) { nav.navigate("obra/${o.id}") } }
            }
        }
        if ("NOVIDADES" in secoes && (novas?.size ?: 0) > 10) item {
            CabSecao("Mais novidades", "atualizadas nos últimos dias", "Ver tudo ›") { irAba(nav, "descobrir") }
            FileiraPublica(novas?.drop(10), nav, largura = 104.dp)
        }
        if ("SUASEMANA" in secoes) item { CabSecao("Você"); SuaSemana(nav) }
        if ("MINHAS" in secoes && minhas.isNotEmpty()) item {
            CabSecao("Suas obras", null, "Escrever ›") { irAba(nav, "escrever") }
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(minhas, key = { it.id }) { o -> CapaLocal(o, 100.dp, if (o.publicada) "●" else null) { nav.navigate("obra/${o.id}") } }
                item {
                    Box(Modifier.width(100.dp).aspectRatio(0.7f).clip(RoundedCornerShape(10.dp)).border(1.dp, p.borda, RoundedCornerShape(10.dp)).clickable { criando = true },
                        contentAlignment = Alignment.Center) { Text("+\nNova obra", textAlign = TextAlign.Center, color = p.textoSuave, fontWeight = FontWeight.Bold) }
                }
            }
        }
        if ("QUERO" in secoes) outras.filter { it.status == StatusObra.QUERO }.takeIf { it.isNotEmpty() }?.let { l ->
            item {
                CabSecao("Quero ler")
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(l, key = { it.id }) { o -> CapaLocal(o, 100.dp, null) { nav.navigate("obra/${o.id}") } }
                }
            }
        }
        if ("CONCLUIDAS" in secoes) {
            val conc = (alta.orEmpty() + novas.orEmpty()).distinctBy { it.id }.filter { it.status == StatusObra.CONCLUIDA }
            if (conc.isNotEmpty()) item { CabSecao("Concluídas para maratonar", "do começo ao fim, sem esperar"); FileiraPublica(conc, nav, largura = 104.dp) }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalArrangement = Arrangement.Center) {
                Text("✎  Personalizar o Início", fontSize = 13.sp, color = p.textoSuave, modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    .border(1.dp, p.borda, RoundedCornerShape(16.dp)).clickable { editando = true }.padding(horizontal = 16.dp, vertical = 9.dp))
            }
        }
    }

    }

    if (editando) AlertDialog(onDismissRequest = { editando = false }, title = { Text("O que aparece no Início") },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp)) {
                items(SECOES_INICIO.entries.toList()) { (k, n) ->
                    Row(Modifier.fillMaxWidth().clickable {
                        val novo = if (k in secoes) secoes - k else secoes + k
                        app.prefs.secoesInicio.definir((SECOES_INICIO.keys.filter { it in novo } + "v2").joinToString(","))
                    }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(k in secoes, null, colors = CheckboxDefaults.colors(checkedColor = p.destaque, checkmarkColor = p.sobreDestaque))
                        Text(n, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { editando = false }) { Text("Pronto") } })
    if (criando) DialogoCriarObra(nav) { criando = false }
}

@Composable
private fun CapaLocal(o: ResumoObra, largura: Dp, selo: String?, aoTocar: () -> Unit) {
    val p = LocalPaleta.current
    Apertavel(Modifier.width(largura), aoTocar) {
        Column {
            Box {
                CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.fillMaxWidth().aspectRatio(0.7f).shadow(8.dp, RoundedCornerShape(10.dp)), (largura.value / 9).sp)
                if (selo != null) Text(selo, fontSize = 10.sp, fontWeight = FontWeight.Black, color = p.sobreDestaque,
                    modifier = Modifier.padding(7.dp).clip(RoundedCornerShape(6.dp)).background(p.destaque).padding(horizontal = 6.dp, vertical = 2.dp))
                if (o.totalCapitulos > 0 && !o.minha) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(6.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth(o.lidos.toFloat() / o.totalCapitulos).fillMaxHeight().background(p.destaque))
                }
            }
            Text(o.titulo, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
            Text(if (o.minha) "${o.totalCapitulos} caps" else if (o.totalCapitulos > 0) "${o.lidos}/${o.totalCapitulos} lidos" else "", fontSize = 11.sp, color = p.textoSuave)
        }
    }
}

@Composable
private fun FaixaContinuarNova(o: ResumoObra, abrir: () -> Unit) {
    val p = LocalPaleta.current
    val cap by remember(o.ultimoCapituloId) { NovelyApp.instancia.repo.capitulo(o.ultimoCapituloId ?: -1) }.collectAsState(initial = null)
    val c = cap
    Apertavel(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), abrir) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(listOf(Color(o.corBase).copy(alpha = 0.55f), p.superficie)))
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(54.dp).height(78.dp).shadow(10.dp, RoundedCornerShape(8.dp)), 7.sp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(o.titulo, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (c != null) {
                    val min = ((1f - c.progresso) * c.palavras / 230f).toInt().coerceAtLeast(1)
                    Text(c.titulo.ifEmpty { "Capítulo" }, fontSize = 12.5.sp, color = p.texto.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x33FFFFFF))) {
                            Box(Modifier.fillMaxWidth(c.progresso.coerceIn(0f, 1f)).fillMaxHeight().background(p.destaque))
                        }
                        Text("  ${(c.progresso * 100).toInt()}% · $min min", fontSize = 11.sp, color = p.textoSuave)
                    }
                }
            }
            Box(Modifier.padding(start = 12.dp).size(48.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Text("▶", color = Color.Black, fontSize = 16.sp)
            }
        }
    }
}

// ───────────────────────── TODOS OS GÊNEROS e GÊNERO ─────────────────────────

@Composable
fun TelaGeneros(nav: NavHostController) {
    val p = LocalPaleta.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item { Topo("Gêneros", nav) }
        com.huryel.novely.data.GENEROS_GRUPOS.forEach { (grupo, lista) ->
            item(key = grupo) {
                Text(grupo.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp, color = p.textoSuave,
                    modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 10.dp))
                Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    lista.chunked(2).forEach { par ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            par.forEach { g -> TileGenero(g, Modifier.weight(1f)) { nav.navigate("genero/$g") } }
                            if (par.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelaGenero(g: String, nav: NavHostController) {
    val p = LocalPaleta.current
    var lista by remember { mutableStateOf<List<ObraPublica>?>(null) }
    LaunchedEffect(g) { lista = try { CacheCatalogo.pegar("genero:$g") } catch (_: Exception) { emptyList() } }
    val (a, b) = coresGenero(g)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(a, b, p.fundo)))) {
                IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.statusBarsPadding().padding(6.dp)) { Text("←", fontSize = 22.sp, color = Color.White) }
                Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                    Text("GÊNERO", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(g, fontWeight = FontWeight.Black, fontSize = 30.sp, color = Color.White)
                }
            }
        }
        val l = lista
        if (l == null) item { Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { repeat(3) { Esqueleto(104.dp, 148.dp) } } }
        else if (l.isEmpty()) item { Text("Ainda não tem obra de $g no catálogo. Escreva a primeira!", color = p.textoSuave, modifier = Modifier.padding(18.dp)) }
        else l.chunked(3).forEachIndexed { i, linha ->
            item(key = "g$i") {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    linha.forEach { o -> Box(Modifier.weight(1f)) { CartaoPublico(o, null) { nav.navigate("pub/${o.id}") } } }
                    repeat(3 - linha.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
