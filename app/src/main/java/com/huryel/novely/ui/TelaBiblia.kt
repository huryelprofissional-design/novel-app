package com.huryel.novely.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Biblia
import com.huryel.novely.data.TipoFicha
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.forma
import kotlinx.coroutines.launch

private val COR_FURO = Color(0xFFFF4D4D)
private val COR_ATENCAO = Color(0xFFFF9A3C)
private val COR_DICA = Color(0xFF4DA3FF)
private fun corNivel(n: Biblia.Nivel) = when (n) { Biblia.Nivel.FURO -> COR_FURO; Biblia.Nivel.ATENCAO -> COR_ATENCAO; Biblia.Nivel.DICA -> COR_DICA }
private fun nomeNivel(n: Biblia.Nivel) = when (n) { Biblia.Nivel.FURO -> "FURO"; Biblia.Nivel.ATENCAO -> "ATENÇÃO"; Biblia.Nivel.DICA -> "DICA" }
internal fun corRelacao(t: String) = when (t) { "família" -> Color(0xFF6FCF97); "romance" -> Color(0xFFFF6FA8); "rivais" -> COR_FURO; "aliados" -> COR_DICA; else -> Color(0xFF9A9A9A) }

/** Bíblia viva: furos, retrato de cada personagem e relações — tudo lido dos capítulos, no aparelho. */
@Composable
fun TelaBiblia(obraId: Long, nav: NavHostController, abaInicial: String = "ALERTAS") {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val ignorados by app.prefs.bibliaIgnorados.estado.collectAsState()
    val fichasFluxo by remember(obraId) { app.repo.fichas(obraId) }.collectAsState(initial = null)
    var r by remember { mutableStateOf<Biblia.Resultado?>(null) }
    val escopo = rememberCoroutineScope()
    LaunchedEffect(obraId, ignorados, fichasFluxo) { r = app.repo.biblia(obraId, ignorados) }

    Column(Modifier.fillMaxSize()) {
        Topo("Bíblia viva", nav)
        val res = r
        if (res == null) {
            Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CircularProgressIndicator(color = p.destaque)
                Text("Lendo seus capítulos…", color = p.textoSuave)
            }
            return@Column
        }
        ConteudoBiblia(res, abaInicial, abrirCap = { nav.navigate("editor/$it") }, ignorar = { app.prefs.ignorarAlerta(it) },
            porNaFicha = { f, fato -> escopo.launch { gravarNaFicha(f, fato) } })
    }
}

/** O conteúdo da Bíblia (sem carregar nada): separado para dar para testar e fotografar a tela. */
@Composable
fun ConteudoBiblia(res: Biblia.Resultado, abaInicial: String, abrirCap: (Long) -> Unit, ignorar: (String) -> Unit,
                   porNaFicha: (com.huryel.novely.data.Ficha, Biblia.Fato) -> Unit) {
    val p = LocalPaleta.current
    var aba by remember { mutableStateOf(abaInicial) }
    var aberto by remember { mutableStateOf<Long?>(null) }
    var relAberta by remember { mutableStateOf<String?>(null) }
    val estadoPessoas = androidx.compose.foundation.lazy.rememberLazyListState()
    // "Ver Fulano" nas relações: abre o retrato dele já rolado até lá
    LaunchedEffect(aba, aberto) {
        val id = aberto ?: return@LaunchedEffect
        if (aba != "PESSOAS") return@LaunchedEffect
        val i = res.retratos.filter { it.ficha.tipo != TipoFicha.LUGAR }.indexOfFirst { it.ficha.id == id }
        if (i >= 0) estadoPessoas.animateScrollToItem(i)
    }
    Column(Modifier.fillMaxSize()) {
        Text("Lida agora dos ${res.caps.size} capítulos, aqui no celular. São pistas, não regras: você decide.",
            fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(horizontal = 20.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Alertas · ${res.alertas.size}", aba == "ALERTAS") { aba = "ALERTAS" }
            Chip("Personagens", aba == "PESSOAS") { aba = "PESSOAS" }
            Chip("Mapa", aba == "MAPA") { aba = "MAPA" }
            Chip("Relações · ${res.relacoes.size}", aba == "RELACOES") { aba = "RELACOES" }
        }
        when (aba) {
            "ALERTAS" -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (res.alertas.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✓", fontSize = 40.sp, color = p.destaque)
                        Text("Nenhum furo encontrado", fontWeight = FontWeight.Bold)
                        Text("Idades, olhos, cabelo, mortes, ferimentos, níveis e nomes estão batendo.", fontSize = 13.sp, color = p.textoSuave)
                    }
                }
                items(res.alertas, key = { it.chave }) { a ->
                    val cor = corNivel(a.nivel)
                    Column(Modifier.fillMaxWidth().clip(forma(18.dp)).background(p.superficie).border(1.dp, cor.copy(alpha = 0.35f), forma(18.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(nomeNivel(a.nivel), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, letterSpacing = 1.sp,
                                modifier = Modifier.clip(forma(6.dp)).background(cor).padding(horizontal = 7.dp, vertical = 2.dp))
                            Text("  cap. ${a.cap + 1} · ${res.caps.getOrNull(a.cap)?.titulo ?: ""}", fontSize = 12.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(a.titulo, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(a.detalhe, fontSize = 13.5.sp, color = p.textoSuave, lineHeight = 19.sp)
                        if (a.frase.isNotBlank()) Text("“${a.frase}”", fontSize = 13.sp, fontStyle = FontStyle.Italic, lineHeight = 19.sp,
                            modifier = Modifier.fillMaxWidth().clip(forma(10.dp)).background(p.fundo.copy(alpha = 0.6f)).padding(10.dp))
                        Row {
                            res.caps.getOrNull(a.cap)?.let { c -> TextButton(onClick = { abrirCap(c.id) }) { Text("Abrir capítulo", color = p.destaque) } }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { ignorar(a.chave) }) { Text("Está certo, ignorar", color = p.textoSuave) }
                        }
                    }
                }
            }
            "PESSOAS" -> LazyColumn(state = estadoPessoas, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val lista = res.retratos.filter { it.ficha.tipo != TipoFicha.LUGAR }
                items(lista, key = { it.ficha.id }) { rt -> Retrato(rt, res, aberto == rt.ficha.id, { aberto = if (aberto == rt.ficha.id) null else rt.ficha.id }) { fato -> porNaFicha(rt.ficha, fato) } }
                val lugares = res.retratos.filter { it.ficha.tipo == TipoFicha.LUGAR && it.total > 0 }
                if (lugares.isNotEmpty()) item { Text("LUGARES", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = p.destaque, modifier = Modifier.padding(top = 10.dp)) }
                items(lugares, key = { "l" + it.ficha.id }) { rt ->
                    Row(Modifier.fillMaxWidth().clip(forma(14.dp)).background(p.superficie).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("⌂ ${rt.ficha.nome}", fontWeight = FontWeight.Bold)
                            Text("${rt.total} ${if (rt.total == 1) "menção" else "menções"} · ${rt.capsComEle} ${if (rt.capsComEle == 1) "capítulo" else "capítulos"}", fontSize = 12.sp, color = p.textoSuave)
                        }
                        Aparicoes(rt.porCap, Modifier.width(120.dp).height(26.dp))
                    }
                }
            }
            "MAPA" -> MapaRelacoes(res)
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (res.relacoes.isEmpty()) item { Text("Ainda não há personagens dividindo cenas (duas cenas juntos ou mais).", color = p.textoSuave, modifier = Modifier.padding(10.dp)) }
                item { Legenda() }
                val maior = res.relacoes.maxOfOrNull { it.cenas } ?: 1
                items(res.relacoes.take(80), key = { "${it.a}-${it.b}" }) { rl ->
                    val cor = corRelacao(rl.tipo)
                    val chave = "${rl.a}-${rl.b}"
                    val aberta = relAberta == chave
                    Column(Modifier.fillMaxWidth().clip(forma(14.dp)).background(p.superficie).clickable { relAberta = if (aberta) null else chave }.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            // um nome por linha: nome comprido não some mais
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(res.ficha(rl.a)?.nome ?: "?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("⇄ " + (res.ficha(rl.b)?.nome ?: "?"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(rl.tipo, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cor,
                                modifier = Modifier.padding(start = 8.dp).clip(forma(8.dp)).border(1.dp, cor.copy(alpha = 0.6f), forma(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(p.borda)) {
                            Box(Modifier.fillMaxWidth(rl.cenas / maior.toFloat()).fillMaxHeight().clip(CircleShape).background(cor))
                        }
                        Text("${rl.cenas} cenas juntos · cap. ${rl.primeiro + 1}" + (if (rl.ultimo != rl.primeiro) " a ${rl.ultimo + 1}" else "") + if (aberta) "" else "  ·  toque para ver mais",
                            fontSize = 12.sp, color = p.textoSuave)
                        if (aberta) {
                            Text(when (rl.tipo) {
                                "família" -> "As cenas dos dois falam de família (pai, irmão, clã…)."
                                "romance" -> "As cenas dos dois têm clima de romance (beijo, abraço, coração…)."
                                "rivais" -> "As cenas dos dois têm briga, ameaça ou rivalidade."
                                "aliados" -> "As cenas dos dois têm ajuda, treino ou amizade."
                                else -> "Dividem cenas, mas o texto não deixa claro o tipo de relação."
                            }, fontSize = 13.sp)
                            Text("CENAS JUNTOS POR CAPÍTULO", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = p.destaque, modifier = Modifier.padding(top = 4.dp))
                            Aparicoes(rl.porCap, Modifier.fillMaxWidth().height(28.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(rl.a, rl.b).forEach { id ->
                                    val nome = res.ficha(id)?.nome?.split(' ')?.let { if (it.size > 1) it.drop(1).joinToString(" ") else it.first() } ?: "?"
                                    TextButton(onClick = { aberto = id; aba = "PESSOAS" }) { Text("Ver $nome", color = p.destaque, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legenda() {
    val p = LocalPaleta.current
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("família", "romance", "rivais", "aliados", "convivem").forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(corRelacao(t)))
                Text(" $t", fontSize = 11.sp, color = p.textoSuave)
            }
        }
    }
}

/** Barrinhas: quanto o personagem aparece em cada capítulo. */
@Composable
private fun Aparicoes(porCap: IntArray, modifier: Modifier) {
    val p = LocalPaleta.current
    val maior = (porCap.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier) {
        if (porCap.isEmpty()) return@Canvas
        val w = size.width / porCap.size
        porCap.forEachIndexed { i, v ->
            val h = if (v == 0) 2f else (v / maior.toFloat()) * size.height
            drawRoundRect(if (v == 0) p.borda else p.destaque, Offset(i * w + w * 0.15f, size.height - h), Size((w * 0.7f).coerceAtLeast(1f), h), CornerRadius(2f, 2f))
        }
    }
}

@Composable
private fun Retrato(rt: Biblia.Retrato, res: Biblia.Resultado, aberto: Boolean, alternar: () -> Unit, porNaFicha: (Biblia.Fato) -> Unit) {
    val p = LocalPaleta.current
    val img = rememberImagem(rt.ficha.imagem, 200)
    Column(Modifier.fillMaxWidth().clip(forma(18.dp)).background(p.superficie).clickable(onClick = alternar).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(p.borda), contentAlignment = Alignment.Center) {
                if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text(rt.ficha.nome.split(' ').mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString(""), color = p.destaque, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(rt.ficha.nome, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (rt.total == 0) "Ainda não aparece no texto"
                else "${rt.capsComEle} de ${res.caps.size} caps · ${rt.falas} ${if (rt.falas == 1) "fala" else "falas"} · do cap. ${rt.primeiro + 1} ao ${rt.ultimo + 1}", fontSize = 12.sp, color = p.textoSuave)
            }
            if (rt.fatos.any { it.tipo == Biblia.Tipo.MORTE }) Text("✝", fontSize = 18.sp, color = p.textoSuave)
        }
        Aparicoes(rt.porCap, Modifier.fillMaxWidth().height(30.dp))
        if (aberto) {
            val parceiros = res.relacoes.filter { it.a == rt.ficha.id || it.b == rt.ficha.id }.take(4)
            if (parceiros.isNotEmpty()) {
                Text("MAIS JUNTO DE", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = p.destaque)
                parceiros.forEach { rl ->
                    val outro = res.ficha(if (rl.a == rt.ficha.id) rl.b else rl.a)?.nome ?: "?"
                    Text("• $outro — ${rl.tipo}, ${rl.cenas} cenas", fontSize = 13.sp)
                }
            }
            if (rt.lugares.isNotEmpty()) {
                Text("ONDE ESTEVE", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = p.destaque)
                Text(rt.lugares.joinToString(" · ") { (id, n) -> "${res.ficha(id)?.nome} ($n)" }, fontSize = 13.sp)
            }
            val fatos = rt.fatos.distinctBy { it.tipo to it.valor }
            if (fatos.isNotEmpty()) Text("O QUE O TEXTO DIZ", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = p.destaque)
            fatos.forEach { f ->
                Column(Modifier.fillMaxWidth().clip(forma(12.dp)).background(p.fundo.copy(alpha = 0.55f)).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${f.tipo.nome}: ", fontSize = 12.sp, color = p.textoSuave)
                        Text(f.valor, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                        Text("cap. ${f.cap + 1}", fontSize = 11.sp, color = p.textoSuave)
                    }
                    Text(f.frase, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = p.textoSuave, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (f.tipo in listOf(Biblia.Tipo.IDADE, Biblia.Tipo.OLHOS, Biblia.Tipo.CABELO, Biblia.Tipo.MORTE, Biblia.Tipo.JURAMENTO, Biblia.Tipo.PODER))
                        Text("+ Pôr na ficha", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.destaque,
                            modifier = Modifier.padding(top = 4.dp).clip(forma(8.dp)).clickable { porNaFicha(f) }.padding(4.dp))
                }
            }
            if (rt.total == 0) Text("Use o nome dele num capítulo (ou @${rt.ficha.nome}) para ele entrar na Bíblia.", fontSize = 12.sp, color = p.textoSuave)
        }
    }
}

/** Leva um fato do texto para a ficha (sem apagar o que já está lá). */
private suspend fun gravarNaFicha(f0: com.huryel.novely.data.Ficha, fato: Biblia.Fato) {
    val repo = NovelyApp.instancia.repo
    val f = repo.dao.fichasAgora(f0.obraId).firstOrNull { it.id == f0.id } ?: return
    fun junta(atual: String, novo: String) = if (atual.contains(novo, true)) atual else listOf(atual.trim().trimEnd('.'), novo).filter { it.isNotBlank() }.joinToString(". ")
    val valor = fato.valor.removeSuffix(" (mudou)")
    val nova = when (fato.tipo) {
        Biblia.Tipo.IDADE -> f.copy(idade = valor.substringBefore(' '))
        Biblia.Tipo.OLHOS -> f.copy(aparencia = junta(f.aparencia, "olhos $valor"))
        Biblia.Tipo.CABELO -> f.copy(aparencia = junta(f.aparencia, "cabelo $valor"))
        Biblia.Tipo.PODER -> f.copy(poder = junta(f.poder, "$valor (cap. ${fato.cap + 1})"))
        Biblia.Tipo.MORTE -> f.copy(descricao = junta(f.descricao, "Morreu no cap. ${fato.cap + 1}"))
        Biblia.Tipo.JURAMENTO -> f.copy(descricao = junta(f.descricao, "Jurou (cap. ${fato.cap + 1}): “${fato.frase.take(140)}”"))
        else -> return
    }
    repo.salvarFicha(nova)
}
