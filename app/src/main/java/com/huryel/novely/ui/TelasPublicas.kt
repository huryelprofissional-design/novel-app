package com.huryel.novely.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.CLASSIFICACOES
import com.huryel.novely.data.CapPublico
import com.huryel.novely.data.Capitulo
import com.huryel.novely.data.Comentario
import com.huryel.novely.data.GENEROS
import com.huryel.novely.data.ObraPublica
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ───────────────────────── peças ─────────────────────────

@Composable
fun imagemDeBytes(b: ByteArray?): ImageBitmap? = remember(b) { b?.let { try { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } catch (_: Exception) { null } } }

@Composable
fun CapaPublica(o: ObraPublica, modifier: Modifier, tamanhoTitulo: Int = 11) {
    val img = imagemDeBytes(o.capaMini)
    Box(modifier.clip(RoundedCornerShape(10.dp))) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else CapaObra(o.titulo, null, .5f, .5f, 1f, 0xFF3B2A4A, Modifier.fillMaxSize(), tamanhoTitulo.sp)
    }
}

private fun nota(o: ObraPublica) = if (o.numEstrelas > 0) "★ %.1f".format(o.nota).replace('.', ',') else "★ —"
private fun milhar(n: Long) = when { n >= 1_000_000 -> "%.1f mi".format(n / 1e6).replace('.', ','); n >= 1000 -> "%.1f mil".format(n / 1e3).replace('.', ','); else -> "$n" }
private fun quando(ms: Long): String {
    val d = (System.currentTimeMillis() - ms) / 86_400_000
    return when { d < 1 -> "hoje"; d < 2 -> "ontem"; d < 7 -> "há $d dias"; d < 30 -> "há ${d / 7} sem"; else -> SimpleDateFormat("dd/MM/yy", Locale("pt", "BR")).format(Date(ms)) }
}

// ───────────────────────── DESCOBRIR (catálogo) ─────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelaDescobrirCatalogo(nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    var modo by rememberSaveableTexto("ALTA")
    var busca by rememberSaveableTexto("")
    var lista by remember { mutableStateOf<List<ObraPublica>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var recarregar by remember { mutableIntStateOf(0) }
    val obrasLocais by remember { app.repo.resumos() }.collectAsState(initial = emptyList())
    val recebidas = obrasLocais.filter { it.trancada && it.autor.isNotBlank() && !obrasLocais.isEmpty() }

    LaunchedEffect(modo, recarregar) {
        lista = null; erro = null
        try { lista = app.pub.catalogo(modo) } catch (e: Exception) {
            erro = if (e.message?.contains("PERMISSION_DENIED") == true) "O catálogo ainda não está liberado (faltam as Regras novas do Firestore)." else "Sem conexão com o catálogo. Toque para tentar de novo."
        }
    }
    val q = busca.trim().lowercase()
    val filtrada = lista?.filter { q.isEmpty() || it.titulo.lowercase().contains(q) || it.autorNome.lowercase().contains(q) || it.tags.any { t -> t.contains(q) } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item { TopoAba("Descobrir", nav) }
        item {
            OutlinedTextField(busca, { busca = it }, placeholder = { Text("Obras, autores, tags…") }, singleLine = true, shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icones.lupa, null, tint = p.textoSuave, modifier = Modifier.size(18.dp)) },
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = p.superficie, focusedContainerColor = p.superficie, unfocusedBorderColor = p.borda, focusedBorderColor = p.destaque),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp))
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf("ALTA" to "Em alta", "SEMANA" to "Ranking da semana", "NOVIDADES" to "Novidades", "CONCLUIDAS" to "Concluídas").forEach { (k, n) ->
                    val on = modo == k
                    Column(Modifier.clickable { modo = k }) {
                        Text(n, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (on) p.texto else p.textoSuave.copy(alpha = 0.7f))
                        Box(Modifier.padding(top = 10.dp).height(2.dp).width(if (on) 36.dp else 0.dp).background(p.destaque))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.borda))
        }
        if (modo.startsWith("genero:")) item {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Gênero: ${modo.removePrefix("genero:")}", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { modo = "ALTA" }) { Text("Limpar", color = p.destaque) }
            }
        }
        val l = filtrada
        when {
            erro != null -> item { Text(erro!!, color = p.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { recarregar++ }.padding(30.dp)) }
            l == null -> item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.destaque) } }
            l.isEmpty() -> item {
                Text(if (q.isNotEmpty()) "Nada encontrado para “$busca”." else if (modo == "SEMANA") "Ninguém leu nada esta semana ainda." else "O catálogo ainda está vazio. Publique a primeira obra! (Escrever › sua obra › ⋮ › Publicar)",
                    color = p.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(30.dp))
            }
            else -> {
                val destaque = l.first()
                if (q.isEmpty()) item {
                    Box(Modifier.padding(18.dp).fillMaxWidth().height(200.dp).clip(RoundedCornerShape(18.dp)).clickable { nav.navigate("pub/${destaque.id}") }) {
                        CapaPublica(destaque, Modifier.fillMaxSize().blur(16.dp))
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF0000000), Color(0x66000000)))))
                        CapaPublica(destaque, Modifier.align(Alignment.CenterEnd).padding(16.dp).width(110.dp).height(160.dp).shadow(12.dp, RoundedCornerShape(10.dp)))
                        Column(Modifier.align(Alignment.CenterStart).padding(18.dp).fillMaxWidth(0.55f)) {
                            Text(if (modo == "SEMANA") "#1 DA SEMANA" else "DESTAQUE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = p.sobreDestaque,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(p.destaque).padding(horizontal = 8.dp, vertical = 3.dp))
                            Text(destaque.titulo, fontWeight = FontWeight.Black, fontSize = 21.sp, lineHeight = 24.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                            Text("por ${destaque.autorNome}", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 4.dp))
                            Text("${nota(destaque)} · ${milhar(destaque.leituras)} leituras", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                val resto = if (q.isEmpty()) l.drop(1) else l
                resto.chunked(3).forEachIndexed { li, linha ->
                    item(key = "l$li") {
                        Row(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            linha.forEachIndexed { ci, o ->
                                Column(Modifier.weight(1f).clickable { nav.navigate("pub/${o.id}") }) {
                                    Box {
                                        CapaPublica(o, Modifier.fillMaxWidth().aspectRatio(0.7f))
                                        if (modo == "SEMANA" || modo == "ALTA") Text("${li * 3 + ci + (if (q.isEmpty()) 2 else 1)}", fontWeight = FontWeight.Black, fontSize = 13.sp,
                                            color = p.sobreDestaque, modifier = Modifier.padding(6.dp).clip(RoundedCornerShape(6.dp)).background(p.destaque).padding(horizontal = 6.dp))
                                    }
                                    Text(o.titulo, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                                    Text("${nota(o)} · ${o.numCaps} caps", fontSize = 11.sp, color = p.textoSuave, maxLines = 1)
                                }
                            }
                            repeat(3 - linha.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        item {
            Text("Gêneros", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.padding(start = 18.dp, top = 24.dp, bottom = 12.dp))
            FlowRow(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GENEROS.forEach { g -> Chip(g, modo == "genero:$g") { modo = "genero:$g" } }
            }
        }
        if (recebidas.isNotEmpty()) item {
            Text("Recebidas por arquivo", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.padding(start = 18.dp, top = 24.dp, bottom = 12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recebidas, key = { it.id }) { o ->
                    Column(Modifier.width(100.dp).clickable { nav.navigate("obra/${o.id}") }) {
                        CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.fillMaxWidth().aspectRatio(0.7f), 10.sp)
                        Text(o.titulo, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSaveableTexto(inicial: String): MutableState<String> = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(inicial) }

// ───────────────────────── PÁGINA PÚBLICA DA OBRA ─────────────────────────

@Composable
fun TelaObraPublica(pid: String, nav: NavHostController) {
    val app = NovelyApp.instancia
    val pub = app.pub
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    var obra by remember { mutableStateOf<ObraPublica?>(null) }
    var caps by remember { mutableStateOf<List<CapPublico>>(emptyList()) }
    var capa by remember { mutableStateOf<ByteArray?>(null) }
    var seguindo by remember { mutableStateOf(false) }
    var minhaNota by remember { mutableIntStateOf(0) }
    var erro by remember { mutableStateOf<String?>(null) }
    var trabalhando by remember { mutableStateOf<String?>(null) }
    var sinopseAberta by remember { mutableStateOf(false) }
    var maisRecentes by remember { mutableStateOf(false) }

    LaunchedEffect(pid) {
        try {
            obra = pub.obra(pid) ?: run { erro = "Esta obra não está mais publicada."; return@LaunchedEffect }
            caps = pub.capitulos(pid, autor = obra?.autorUid == pub.meuUid)
            seguindo = pub.seguindo(pid); minhaNota = pub.minhaNota(pid)
            capa = pub.imagem(pid, "capa")
        } catch (e: Exception) { erro = "Sem conexão. Volte e tente de novo." }
    }
    val o = obra
    if (o == null) {
        Column(Modifier.fillMaxSize()) {
            Topo("", nav)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (erro != null) Text(erro!!, color = p.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.padding(30.dp))
                else CircularProgressIndicator(color = p.destaque)
            }
        }
        return
    }
    val souAutor = o.autorUid == pub.meuUid

    /** Garante a obra na biblioteca (baixando tudo) e abre o capítulo. */
    fun ler(c: CapPublico?) {
        escopo.launch {
            try {
                trabalhando = "Preparando…"
                if (souAutor) {
                    val local = app.repo.dao.obraNuvem(pid)
                    val cap = c?.let { app.repo.dao.capituloNuvem(it.id) } ?: local?.let { app.repo.dao.capitulosAgora(it.id).firstOrNull() }
                    trabalhando = null
                    cap?.let { nav.navigate("leitor/${it.id}") }
                    return@launch
                }
                val obraId = pub.baixar(pid) { trabalhando = it }
                val local = app.repo.dao.obraAgora(obraId)
                val alvo = c?.let { app.repo.dao.capituloPublico(obraId, it.id) }
                    ?: local?.ultimoCapituloId?.let { app.repo.dao.capituloAgora(it) }
                    ?: app.repo.dao.capitulosAgora(obraId).firstOrNull()
                trabalhando = null
                alvo?.let { nav.navigate("leitor/${it.id}") }
            } catch (e: Exception) { trabalhando = null; erro = e.message }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(330.dp)) {
                    val img = imagemDeBytes(capa ?: o.capaMini)
                    if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(20.dp))
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x88000000), 1f to p.fundo)))
                    IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.statusBarsPadding().padding(8.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).width(150.dp).height(214.dp).shadow(24.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
                        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else CapaPublica(o, Modifier.fillMaxSize())
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(o.titulo, fontWeight = FontWeight.Black, fontSize = 25.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                    Text("por ${o.autorNome} · ${o.generos.take(3).joinToString(" · ")}" + if (o.classificacao != "LIVRE") " · +${o.classificacao}" else "",
                        fontSize = 13.sp, color = p.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
                    Text("${nota(o)} (${o.numEstrelas}) · ${milhar(o.leituras)} leituras · ${milhar(o.seguidores)} seguidores · ${o.numCaps} caps · " +
                        if (o.status == StatusObra.CONCLUIDA) "concluída" else "em andamento", fontSize = 12.sp, color = p.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { ler(null) }, enabled = caps.isNotEmpty() && trabalhando == null, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque),
                            shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(50.dp)) {
                            Text(if (caps.isEmpty()) "Sem capítulos" else "▶  Ler", fontWeight = FontWeight.ExtraBold)
                        }
                        if (!souAutor) OutlinedButton(onClick = {
                            escopo.launch {
                                try {
                                    val novo = !seguindo
                                    pub.seguir(pid, novo); seguindo = novo
                                    if (novo) { trabalhando = "Adicionando à biblioteca…"; pub.baixar(pid) { trabalhando = it }; trabalhando = null }
                                    obra = pub.obra(pid)
                                } catch (e: Exception) { trabalhando = null; erro = e.message }
                            }
                        }, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(50.dp)) {
                            Text(if (seguindo) "✓ Seguindo" else "+ Seguir", color = if (seguindo) p.destaque else p.texto, fontWeight = FontWeight.Bold)
                        } else OutlinedButton(onClick = { app.escopo.launch { app.repo.dao.obraNuvem(pid)?.let { withContext(Dispatchers.Main) { nav.navigate("obra/${it.id}") } } } },
                            shape = RoundedCornerShape(14.dp), modifier = Modifier.height(50.dp)) { Text("Minha obra", color = p.texto) }
                    }
                    trabalhando?.let { Text(it, fontSize = 12.sp, color = p.destaque, modifier = Modifier.padding(top = 8.dp)) }
                    erro?.let { Text(it, fontSize = 12.sp, color = Color(0xFFE57373), modifier = Modifier.padding(top = 8.dp)) }
                    if (!souAutor) Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sua nota: ", fontSize = 13.sp, color = p.textoSuave)
                        (1..5).forEach { n ->
                            Text(if (n <= minhaNota) "★" else "☆", fontSize = 26.sp, color = if (n <= minhaNota) p.destaque else p.textoSuave,
                                modifier = Modifier.clickable { escopo.launch { try { pub.avaliar(pid, n); minhaNota = n; obra = pub.obra(pid) } catch (_: Exception) {} } }.padding(horizontal = 3.dp))
                        }
                    }
                    if (o.sinopse.isNotBlank()) Text(o.sinopse, fontSize = 14.sp, lineHeight = 21.sp, color = p.textoSuave, maxLines = if (sinopseAberta) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 14.dp).fillMaxWidth().clickable { sinopseAberta = !sinopseAberta })
                    if (o.tags.isNotEmpty()) Row(Modifier.padding(top = 10.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        o.tags.forEach { t -> Text("#$t", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(p.superficie).padding(horizontal = 10.dp, vertical = 5.dp)) }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Capítulos", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(if (maisRecentes) "⇅ mais recentes" else "⇅ do início", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.clickable { maisRecentes = !maisRecentes })
                    }
                }
            }
            val ordenados = if (maisRecentes) caps.reversed() else caps
            items(ordenados, key = { it.id }) { c ->
                val n = caps.indexOf(c) + 1
                Row(Modifier.fillMaxWidth().clickable { ler(c) }.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$n", fontWeight = FontWeight.Bold, color = p.textoSuave.copy(alpha = 0.6f), modifier = Modifier.width(38.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text((if (c.publicadoEm > System.currentTimeMillis()) "agendado · " else "") + quando(c.publicadoEm) +
                            if (c.atualizadoEm - c.publicadoEm > 3_600_000) " · atualizado ${quando(c.atualizadoEm)}" else "", fontSize = 11.5.sp, color = p.textoSuave)
                    }
                    Text("♥ ${c.curtidas}  💬 ${c.comentarios}", fontSize = 12.sp, color = p.textoSuave)
                }
            }
        }
    }
}

// ───────────────────────── PUBLICAR (autor) ─────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TelaPublicar(obraId: Long, nav: NavHostController) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val pub = app.pub
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val obraF by remember(obraId) { repo.obra(obraId) }.collectAsState(initial = null)
    val caps by remember(obraId) { repo.capitulos(obraId) }.collectAsState(initial = emptyList())
    val o = obraF ?: run { Box(Modifier.fillMaxSize()); return }
    var sinopse by remember(o.id) { mutableStateOf(o.descricao) }
    var tags by remember(o.id) { mutableStateOf(o.tagsPub) }
    val generos = o.generos.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val selecionados = remember { mutableStateListOf<Long>() }
    var trabalhando by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var agendar by remember { mutableStateOf(false) }
    var despublicar by remember { mutableStateOf(false) }
    var arquivo by remember { mutableStateOf(false) }
    var alterados by remember { mutableStateOf<Set<Long>>(emptySet()) }
    LaunchedEffect(caps) {
        alterados = withContext(Dispatchers.IO) { caps.filter { it.publicadoEm > 0 && app.sinc.hash(repo.textos.arquivo(it.id)) != it.publicadoHash }.map { it.id }.toSet() }
        if (selecionados.isEmpty()) selecionados.addAll(caps.filter { it.publicadoEm == 0L && !it.rascunho }.map { it.id })
    }
    fun salvarDados(mod: (com.huryel.novely.data.Obra) -> com.huryel.novely.data.Obra) { escopo.launch { repo.salvarObra(mod(o)) } }
    val pend = pub.pendencias(o.copy(descricao = sinopse))

    fun rodar(texto: String, acao: suspend () -> String?) {
        trabalhando = texto; msg = null
        escopo.launch {
            repo.salvarObra(o.copy(descricao = sinopse.trim(), tagsPub = tags))
            val r = acao(); trabalhando = null; msg = r ?: "✓ Pronto!"
            if (r == null) selecionados.clear()
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item { Topo(if (o.publicada) "Publicação" else "Publicar", nav) }
        item {
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.superficie).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase, Modifier.width(54.dp).height(78.dp), 8.sp)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(o.titulo, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                        Text(if (o.publicada) "● No catálogo · ${caps.count { it.publicadoEm in 1..System.currentTimeMillis() }} caps publicados" else "○ Ainda não publicada",
                            fontSize = 12.sp, color = if (o.publicada) p.destaque2 else p.textoSuave)
                        if (o.publicada) Text("Ver como leitor ›", fontSize = 12.sp, color = p.destaque, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp).clickable { nav.navigate("pub/${o.nuvemId}") })
                    }
                }
                if (pend.isNotEmpty()) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x22E57373)).padding(12.dp)) {
                    Text("Para publicar falta:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    pend.forEach { Text("• $it", fontSize = 13.sp, color = p.textoSuave) }
                    if (o.capa == null || o.banner == null) Text("Capa e banner: na página da obra, toque na capa ou no banner.", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 4.dp))
                }
                Text("SINOPSE", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
                OutlinedTextField(sinopse, { sinopse = it }, minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Do que se trata a história…") })
                Text("GÊNEROS", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GENEROS.forEach { g ->
                        Chip(g, g in generos) {
                            val novo = if (g in generos) generos - g else (generos + g).take(4)
                            salvarDados { it.copy(generos = novo.joinToString(",")) }
                        }
                    }
                }
                Text("TAGS (separe com vírgula)", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
                OutlinedTextField(tags, { tags = it }, singleLine = true, placeholder = { Text("vingança, regressão, slow burn") }, modifier = Modifier.fillMaxWidth())
                Text("CLASSIFICAÇÃO", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CLASSIFICACOES.forEach { c -> Chip(if (c == "LIVRE") "Livre" else "+$c", o.classificacao == c) { salvarDados { it.copy(classificacao = c) } } }
                }
                Text("IDIOMA E SITUAÇÃO", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("pt" to "Português", "en" to "Inglês", "es" to "Espanhol").forEach { (k, n) -> Chip(n, o.idioma == k) { salvarDados { it.copy(idioma = k) } } }
                    Chip("Em andamento", o.status != StatusObra.CONCLUIDA) { salvarDados { it.copy(status = StatusObra.ESCREVENDO) } }
                    Chip("Concluída", o.status == StatusObra.CONCLUIDA) { salvarDados { it.copy(status = StatusObra.CONCLUIDA) } }
                }
                if (o.publicada) OutlinedButton(onClick = { rodar("Atualizando dados…") { pub.publicarObra(repo.dao.obraAgora(o.id)!!) } }, enabled = trabalhando == null && pend.isEmpty(),
                    modifier = Modifier.fillMaxWidth()) { Text("Atualizar dados no catálogo") }
                Text("CAPÍTULOS", style = MaterialTheme.typography.labelSmall, color = p.textoSuave, modifier = Modifier.padding(top = 8.dp))
                Text("Marque os que quer publicar. Rascunhos só vão se você marcar. Mudou um capítulo já publicado? Ele aparece com “republicar”.",
                    fontSize = 12.sp, color = p.textoSuave)
            }
        }
        items(caps, key = { it.id }) { c ->
            val agora = System.currentTimeMillis()
            val estado = when {
                c.publicadoEm == 0L -> if (c.rascunho) "rascunho" else "não publicado"
                c.publicadoEm > agora -> "agendado para ${SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(c.publicadoEm))}"
                c.id in alterados -> "publicado · mudou desde então"
                else -> "publicado ${quando(c.publicadoEm)}"
            }
            var menu by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().clickable { if (c.id in selecionados) selecionados.remove(c.id) else selecionados.add(c.id) }.padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Checkbox(c.id in selecionados, { if (it) selecionados.add(c.id) else selecionados.remove(c.id) },
                    colors = CheckboxDefaults.colors(checkedColor = p.destaque, checkmarkColor = p.sobreDestaque))
                Column(Modifier.weight(1f)) {
                    Text(c.titulo.ifEmpty { "Sem título" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(estado, fontSize = 11.5.sp, color = when { c.id in alterados -> p.destaque; c.publicadoEm > 0 -> p.destaque2; else -> p.textoSuave })
                }
                if (c.publicadoEm > 0) Box {
                    TextButton(onClick = { menu = true }) { Text("⋮", color = p.textoSuave) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Republicar agora") }, onClick = { menu = false; rodar("Republicando…") { pub.publicarCapitulos(o, listOf(c)) } })
                        DropdownMenuItem(text = { Text("Despublicar", color = Color(0xFFE57373)) }, onClick = { menu = false; rodar("Tirando do catálogo…") { pub.despublicarCapitulo(o, c) } })
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val escolhidos = caps.filter { it.id in selecionados }
                trabalhando?.let { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(16.dp), color = p.destaque, strokeWidth = 2.dp); Text("  $it", fontSize = 13.sp) } }
                msg?.let { Text(it, fontSize = 13.sp, color = if (it.startsWith("✓")) p.destaque2 else Color(0xFFE57373)) }
                Button(onClick = { rodar("Publicando ${escolhidos.size} capítulo(s)…") { pub.publicarCapitulos(repo.dao.obraAgora(o.id)!!, escolhidos) } },
                    enabled = trabalhando == null && escolhidos.isNotEmpty() && pend.isEmpty(), colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(if (escolhidos.isEmpty()) "Marque capítulos para publicar" else "Publicar ${escolhidos.size} agora", fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(onClick = { agendar = true }, enabled = trabalhando == null && escolhidos.isNotEmpty() && pend.isEmpty(),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("◷ Agendar…") }
                OutlinedButton(onClick = { arquivo = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Enviar como arquivo (.novely)") }
                if (o.publicada) TextButton(onClick = { despublicar = true }, modifier = Modifier.fillMaxWidth()) { Text("Tirar a obra do catálogo", color = Color(0xFFE57373)) }
            }
        }
    }

    if (agendar) {
        val dia = rememberDatePickerState(System.currentTimeMillis() + 86_400_000)
        val hora = rememberTimePickerState(18, 0, true)
        var etapa by remember { mutableIntStateOf(0) }
        if (etapa == 0) DatePickerDialog(onDismissRequest = { agendar = false },
            confirmButton = { TextButton(onClick = { etapa = 1 }) { Text("Próximo") } },
            dismissButton = { TextButton(onClick = { agendar = false }) { Text("Cancelar") } }) { DatePicker(dia) }
        else AlertDialog(onDismissRequest = { agendar = false }, title = { Text("Horário") }, text = { TimePicker(hora) },
            confirmButton = {
                TextButton(onClick = {
                    agendar = false
                    val utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = dia.selectedDateMillis ?: System.currentTimeMillis() }
                    val local = java.util.Calendar.getInstance().apply {
                        set(utc.get(java.util.Calendar.YEAR), utc.get(java.util.Calendar.MONTH), utc.get(java.util.Calendar.DAY_OF_MONTH), hora.hour, hora.minute, 0)
                    }
                    val escolhidos = caps.filter { it.id in selecionados }
                    rodar("Agendando…") { pub.publicarCapitulos(repo.dao.obraAgora(o.id)!!, escolhidos, local.timeInMillis) }
                }) { Text("Agendar") }
            }, dismissButton = { TextButton(onClick = { agendar = false }) { Text("Cancelar") } })
    }
    if (despublicar) DialogoConfirmar("Tirar do catálogo?", "A obra some do Descobrir. Quem já baixou continua com o que tem. Suas obras e textos não são apagados.", "Tirar", { despublicar = false }) {
        despublicar = false; rodar("Tirando do catálogo…") { pub.despublicarObra(o) }
    }
    if (arquivo) DialogoPublicar(o.id, o.titulo) { arquivo = false }
}

// ───────────────────────── LEITOR: curtir e comentar ─────────────────────────

/** Barra de curtir/comentar no fim do capítulo de uma obra publicada. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialCapitulo(pid: String, cid: String) {
    val app = NovelyApp.instancia
    val pub = app.pub
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    var curtiu by remember { mutableStateOf(false) }
    var curtidas by remember { mutableLongStateOf(0) }
    var comentariosN by remember { mutableLongStateOf(0) }
    var abrir by remember { mutableStateOf(false) }
    LaunchedEffect(pid, cid) {
        try { curtiu = pub.curtiu(pid, cid); val (c, m) = pub.contadores(pid, cid); curtidas = c; comentariosN = m } catch (_: Exception) {}
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(if (curtiu) p.destaque.copy(alpha = 0.16f) else p.superficie)
            .border(1.dp, if (curtiu) p.destaque else p.borda, RoundedCornerShape(14.dp)).clickable {
                val novo = !curtiu; curtiu = novo; curtidas += if (novo) 1 else -1
                escopo.launch { try { pub.curtir(pid, cid, novo) } catch (_: Exception) {} }
            }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(if (curtiu) "♥" else "♡", fontSize = 20.sp, color = if (curtiu) p.destaque else p.texto)
            Text("  $curtidas", fontWeight = FontWeight.Bold)
        }
        Row(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(p.superficie).border(1.dp, p.borda, RoundedCornerShape(14.dp))
            .clickable { abrir = true }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("💬  $comentariosN comentários", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
    if (abrir) ModalBottomSheet(onDismissRequest = { abrir = false }, containerColor = p.superficie) {
        FolhaComentarios(pid, cid) { comentariosN = it.toLong() }
    }
}

@Composable
private fun FolhaComentarios(pid: String, cid: String, aoMudar: (Int) -> Unit) {
    val app = NovelyApp.instancia
    val pub = app.pub
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<Comentario>?>(null) }
    var texto by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var autorObra by remember { mutableStateOf("") }
    suspend fun carregar() { lista = try { pub.comentarios(pid, cid) } catch (_: Exception) { emptyList() }; aoMudar(lista?.size ?: 0) }
    LaunchedEffect(Unit) { autorObra = pub.obra(pid)?.autorUid ?: ""; carregar() }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.8f).imePadding()) {
        Text("Comentários", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 20.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val l = lista
            if (l == null) item { CircularProgressIndicator(color = p.destaque) }
            else if (l.isEmpty()) item { Text("Ninguém comentou ainda. Seja o primeiro!", color = p.textoSuave) }
            else items(l, key = { it.id }) { c ->
                Row {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Brush.linearGradient(listOf(p.destaque, Color(0xFF7A3B8F)))), contentAlignment = Alignment.Center) {
                        Text(c.nome.take(1).uppercase(), fontWeight = FontWeight.Black, color = p.sobreDestaque)
                    }
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.nome, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (c.uid == autorObra) Text("  AUTOR", fontSize = 9.sp, fontWeight = FontWeight.Black, color = p.destaque)
                            Text("  ${quando(c.em)}", fontSize = 11.sp, color = p.textoSuave)
                        }
                        Text(c.texto, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 2.dp))
                        if (c.uid == pub.meuUid || autorObra == pub.meuUid) Text("apagar", fontSize = 11.sp, color = p.textoSuave,
                            modifier = Modifier.padding(top = 4.dp).clickable { escopo.launch { try { pub.apagarComentario(pid, cid, c); carregar() } catch (_: Exception) {} } })
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(texto, { texto = it.take(2000) }, placeholder = { Text("Escreva um comentário…") }, maxLines = 4, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                enviando = true
                escopo.launch { try { pub.comentar(pid, cid, texto); texto = ""; carregar() } catch (_: Exception) {}; enviando = false }
            }, enabled = texto.isNotBlank() && !enviando) { Text("Enviar", color = p.destaque, fontWeight = FontWeight.Bold) }
        }
    }
}
