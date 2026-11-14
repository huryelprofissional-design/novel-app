package com.huryel.novely.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Capitulo
import com.huryel.novely.data.ObraPublica
import com.huryel.novely.data.Texto
import com.huryel.novely.ui.theme.Inter
import com.huryel.novely.ui.theme.InstrumentSerif
import com.huryel.novely.ui.theme.PlexMono
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ───────── Telas novas (U3–U18) no visual V2 editorial: peças comuns ─────────

fun romanoV2(n0: Int): String {
    var n = n0; val sb = StringBuilder()
    listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
        .forEach { (v, s) -> while (n >= v) { sb.append(s); n -= v } }
    return sb.toString()
}

/** Topo editorial: seta de voltar e título em serifa. */
@Composable
fun TopoV2(titulo: String, nav: NavHostController, extra: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clickable { nav.popBackStack() }, contentAlignment = Alignment.Center) { Text("←", color = Ed.creme, fontSize = 22.sp) }
        Serif(titulo, 34.sp, Modifier.weight(1f).padding(start = 4.dp), maxLinhas = 1)
        extra()
    }
}

/** Cabeçalho de seção do sumário: "I  Leitura" com fio embaixo. */
@Composable
fun SecaoSumario(rom: String, titulo: String) {
    Row(Modifier.fillMaxWidth().padding(top = 22.dp).drawBehind { drawLine(Ed.linha, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(bottom = 6.dp),
        verticalAlignment = Alignment.Bottom) {
        Mono(rom, cor = Ed.laranja, tamanho = 11.sp); Spacer(Modifier.width(12.dp)); Serif(titulo, 24.sp)
    }
}

/** Linha com pontilhado até o valor: "Meta diária ........ 1.500". */
@Composable
fun LinhaPontilhada(rotulo: String, valor: String, corValor: Color = Ed.suave, direita: (@Composable () -> Unit)? = null, aoTocar: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier).padding(vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
        Text(rotulo, color = Ed.creme, fontFamily = Inter, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Canvas(Modifier.weight(1f).height(10.dp).padding(horizontal = 8.dp)) {
            drawLine(Ed.apagado, Offset(0f, size.height - 2f), Offset(size.width, size.height - 2f), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f)))
        }
        if (direita != null) direita() else Mono(valor, cor = corValor, tamanho = 11.sp)
    }
}

// ───────── U5: Ajustes como sumário ─────────
@Composable
fun TelaAjustesSumario(nav: NavHostController) {
    val app = NovelyApp.instancia; val prefs = app.prefs
    val usuario by app.conta.usuario.collectAsState()
    val estado by app.sinc.estado.collectAsState()
    val ultima by prefs.ultimaSinc.estado.collectAsState()
    val meta by prefs.metaDiaria.estado.collectAsState()
    val vidro by prefs.efeitoVidro.estado.collectAsState()
    val reduzir by prefs.reduzirAnimacoes.estado.collectAsState()
    val contraste by prefs.altoContraste.estado.collectAsState()
    val bloq by prefs.bloqueio.estado.collectAsState()
    Column(Modifier.fillMaxSize().background(Ed.fundo).verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
        TopoV2("Ajustes", nav)
        Column(Modifier.padding(horizontal = 20.dp)) {
            SecaoSumario("I", "Leitura")
            LinhaPontilhada("Fonte, tamanho e páginas", "no leitor · Aa")
            SecaoSumario("II", "Aparência")
            LinhaPontilhada("Efeito de vidro", if (vidro == "LIQUIDO") "líquido" else "leve") { nav.navigate("ajustes/aparencia") }
            LinhaPontilhada("Reduzir animações", "", aoTocar = { prefs.reduzirAnimacoes.definir(!reduzir) },
                direita = { Switch(reduzir, { prefs.reduzirAnimacoes.definir(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Ed.laranja, checkedThumbColor = Ed.creme)) })
            LinhaPontilhada("Alto contraste", if (contraste) "sim" else "não", if (contraste) Ed.creme else Ed.suave) { prefs.altoContraste.definir(!contraste) }
            LinhaPontilhada("Ícone do app", "trocar →") { nav.navigate("ajustes/aparencia") }
            SecaoSumario("III", "Escrita")
            LinhaPontilhada("Meta diária", "${meta.toInt()} palavras") { nav.navigate("ajustes/escrita") }
            SecaoSumario("IV", "Conta")
            LinhaPontilhada("E-mail", usuario?.email ?: "—")
            LinhaPontilhada("Nuvem", textoSinc(estado, ultima).lowercase(), if (estado is com.huryel.novely.data.EstadoSinc.Erro) Ed.laranja else Ed.verde) { nav.navigate("ajustes/conta") }
            LinhaPontilhada("Aparelhos conectados", "ver →") { nav.navigate("ajustes/conta") }
            LinhaPontilhada("Pedir digital ou senha", if (bloq) "sim" else "não", if (bloq) Ed.creme else Ed.suave) { nav.navigate("ajustes/privacidade") }
            SecaoSumario("V", "Dados")
            LinhaPontilhada("Backup e restauração", "→") { nav.navigate("backup") }
            LinhaPontilhada("Importar TXT / EPUB", "→") { nav.navigate("importar") }
            LinhaPontilhada("Lixeira", "30 dias →") { nav.navigate("lixeira") }
            SecaoSumario("VI", "Sobre")
            LinhaPontilhada("Versão", app.atualizador.nomeInstalado) { nav.navigate("atualizar") }
            LinhaPontilhada("O que é o Novely", "tutorial →") { nav.navigate("ajustes/sobre") }
            if (usuario?.email.equals(EMAIL_DONO, true)) { SecaoSumario("VII", "Dono"); LinhaPontilhada("Painel de dono", "→") { nav.navigate("dono") } }
        }
    }
}

// ───────── U18: avisos como telegramas ─────────
@Composable
fun Telegrama(tipo: String, quando: String, texto: String, novo: Boolean, alerta: Boolean, botao: String? = null, acao: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().border(1.dp, if (alerta) Ed.laranja else Ed.linha)
        .background(if (alerta) Ed.laranja.copy(alpha = 0.06f) else Color.Transparent).clickable(onClick = acao).padding(14.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Mono((if (novo) "● " else "") + tipo, cor = if (alerta || novo) Ed.laranja else Ed.suave, tamanho = 10.sp, modifier = Modifier.weight(1f))
            Mono(quando, cor = if (alerta) Ed.laranja else Ed.suave, tamanho = 10.sp)
        }
        Text(texto, color = Color(0xFFE0D9CE), fontFamily = com.huryel.novely.ui.theme.Newsreader, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp))
        if (botao != null) Box(Modifier.padding(top = 10.dp)) { BotaoCreme(botao, Modifier.height(42.dp).fillMaxWidth(), aoTocar = acao) }
    }
}

// ───────── U7: selos (conquistas) e extrato (moedas) ─────────
@Composable
fun SelosConquistas() {
    var l by remember { mutableStateOf<List<Conquista>>(emptyList()) }
    LaunchedEffect(Unit) { l = calcularConquistas() }
    if (l.isEmpty()) return
    Column(Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth()) { Mono("Conquistas", Modifier.weight(1f)); Mono("${l.count { it.ganhou }} de ${l.size}") }
        val linhas = l.sortedByDescending { it.ganhou }.chunked(4)
        linhas.forEach { fila ->
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fila.forEach { c ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(54.dp).clip(CircleShape).then(
                            if (c.ganhou) Modifier.background(Brush.radialGradient(listOf(Color(0xFFEF7A5F), Color(0xFFA8321E)), Offset(40f, 36f), 120f))
                            else Modifier.drawBehind { drawCircle(Ed.apagado, size.minDimension / 2 - 1f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))) }
                        ), contentAlignment = Alignment.Center) {
                            Text(if (c.ganhou) c.icone else "?", color = if (c.ganhou) Color(0xFFFBE8DC) else Ed.apagado, fontFamily = InstrumentSerif, fontSize = 20.sp)
                        }
                        Text(c.titulo.replace('\n', ' '), color = if (c.ganhou) Ed.creme else Ed.suave, fontSize = 10.5.sp, lineHeight = 13.sp, maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                repeat(4 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun ExtratoMoedasV2() {
    val saldo by com.huryel.novely.data.Moedas.saldo.collectAsState()
    val linhas = remember(saldo) { com.huryel.novely.data.Moedas.extrato() }
    val fmt = remember { SimpleDateFormat("dd MMM", Locale("pt", "BR")) }
    var comoGanhar by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().drawBehind { drawLine(Ed.linha, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(bottom = 8.dp)) {
            Mono("Moedas", Modifier.weight(1f)); Mono("$saldo", cor = Ed.creme)
        }
        if (linhas.isEmpty()) Text("Grátis, para ler capítulos antes de todo mundo. Ganhe abrindo o app e terminando capítulos. O extrato aparece aqui.",
            color = Ed.suave, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        linhas.take(8).forEach { (em, qtd, motivo) ->
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Ed.linha, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Mono(fmt.format(Date(em)).replace(".", ""), Modifier.width(62.dp), tamanho = 10.sp)
                Text(motivo, color = Ed.creme, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Mono((if (qtd >= 0) "+" else "−") + kotlin.math.abs(qtd), cor = if (qtd >= 0) Ed.verde else Ed.laranja, tamanho = 11.sp)
            }
        }
        Mono(if (comoGanhar) "▲ como ganhar" else "▼ como ganhar", Modifier.padding(top = 10.dp).clickable { comoGanhar = !comoGanhar }, tamanho = 10.sp)
        if (comoGanhar) listOf("Abrir o app: +3 por dia", "7 dias seguidos: +10 de bônus", "Terminar capítulo de outro autor: +1 (até 10 por dia)", "Comentar: +1 (até 5 por dia)")
            .forEach { Text("· $it", color = Ed.suave, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp)) }
    }
}

// ───────── U8: pódio tipográfico ─────────
/** Ranking da semana entre quem você segue (perfil), com o 1º gigante. */
@Composable
fun PodioSemana() {
    var l by remember { mutableStateOf<List<com.huryel.novely.data.Progresso.NoRanking>?>(null) }
    LaunchedEffect(Unit) { l = try { com.huryel.novely.data.Progresso.ranking() } catch (_: Exception) { emptyList() } }
    val lista = l ?: return
    if (lista.size < 2) return
    Column(Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
        Mono("Esta semana entre quem você segue", tamanho = 10.sp)
        val a = lista.first()
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).drawBehind { drawLine(Ed.linha, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(bottom = 10.dp),
            verticalAlignment = Alignment.Bottom) {
            Text("1", color = Ed.laranja, fontFamily = InstrumentSerif, fontSize = 88.sp, lineHeight = 80.sp)
            Column(Modifier.padding(start = 14.dp, bottom = 10.dp)) { Serif(a.nome, 26.sp, cor = if (a.eu) Ed.laranja else Ed.creme, maxLinhas = 1); Mono("${a.xp} XP") }
        }
        lista.drop(1).take(5).forEachIndexed { i, r ->
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Ed.linha, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Serif("${i + 2}", 22.sp, Modifier.width(34.dp), cor = if (r.eu) Ed.laranja else Ed.creme)
                Text(r.nome, color = if (r.eu) Ed.laranja else Ed.creme, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                Mono("${r.xp} XP", cor = if (r.eu) Ed.laranja else Ed.suave, tamanho = 10.sp)
            }
        }
    }
}

/** 1º lugar do ranking do Descobrir: número gigante, capa e título. */
@Composable
fun PodioPrimeiro(o: ObraPublica, minha: Boolean, abrir: () -> Unit) {
    val capa by produceState<ByteArray?>(null, o.id) { value = CachePoster.capa(o.id) }
    val img = imagemDeBytes(capa ?: o.capaMini)
    Row(Modifier.fillMaxWidth().clickable(onClick = abrir).padding(top = 6.dp, bottom = 14.dp).drawBehind { drawLine(Ed.linha, Offset(0f, size.height), Offset(size.width, size.height), 1f) },
        verticalAlignment = Alignment.Bottom) {
        Text("1", color = Ed.laranja, fontFamily = InstrumentSerif, fontSize = 120.sp, lineHeight = 100.sp)
        Box(Modifier.padding(start = 8.dp, bottom = 10.dp).size(60.dp, 86.dp).background(Ed.superficie)) { if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f).padding(start = 14.dp, bottom = 12.dp)) {
            Serif(o.titulo, 28.sp, cor = if (minha) Ed.laranja else Ed.creme, maxLinhas = 2)
            Mono("${o.autorNome} · ${o.numCaps} caps", tamanho = 10.sp)
        }
    }
}

// ───────── U14: capa da semana ─────────
@Composable
fun PaginaCapaSemana(o: ObraPublica, nav: NavHostController) {
    val app = NovelyApp.instancia
    val capa by produceState<ByteArray?>(null, o.id) { value = CachePoster.capa(o.id) }
    val img = imagemDeBytes(capa ?: o.capaMini)
    val frase by produceState<String?>(null, o.id) {
        val t = try { app.pub.inicioDaObra(o.id) } catch (_: Exception) { "" }
        val frases = Regex("""[^.!?…]+[.!?…]+["”»]?""").findAll(t).map { it.value.trim() }.toList()
        var r = frases.firstOrNull() ?: t
        if (r.length < 50 && frases.size > 1) r += " " + frases[1]
        value = r.take(220).ifBlank { o.sinopse.take(220) }
    }
    val semana = remember { Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFFC4472F), Color(0xFF3A1610), Ed.fundo))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Ed.fundo.copy(alpha = 0.55f), 0.3f to Color.Transparent, 1f to Ed.fundo)))
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 110.dp)) { Mono("Capa da semana", Modifier.weight(1f), cor = Ed.creme); Mono("Edição $semana", cor = Ed.creme) }
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp, vertical = 10.dp)) {
                Serif(o.titulo, 54.sp, maxLinhas = 3)
                Mono("por ${o.autorNome}" + (o.generos.firstOrNull()?.let { " · $it" } ?: ""), cor = Color(0xFFF1D6CB), modifier = Modifier.padding(top = 6.dp))
            }
        }
        Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 96.dp)) {
            val f = frase
            Serif(if (f.isNullOrBlank()) "…" else "“$f”", 22.sp, italico = true, cor = Color(0xFFD8D2C8), maxLinhas = 5)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotaoCreme("Ler o capítulo 1", Modifier.weight(1f)) { nav.navigate("pub/${o.id}") }
            }
            Mono("↑ deslize para as primeiras linhas", Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp), tamanho = 10.sp)
        }
    }
}

// ───────── U6: relatório do mês (Estatísticas) ─────────
@Composable
fun TelaRelatorioMes(nav: NavHostController) {
    val prefs = NovelyApp.instancia.prefs
    val seq by prefs.sequencia.estado.collectAsState()
    val dadosLeitura = remember { prefs.estatisticas() }
    val escrita = remember { prefs.escritaDias() }
    val hoje = remember { Calendar.getInstance() }
    val fmt = remember { SimpleDateFormat("yyyyMMdd", Locale.US) }
    val diasMes = hoje.getActualMaximum(Calendar.DAY_OF_MONTH); val diaHoje = hoje.get(Calendar.DAY_OF_MONTH)
    val chave = { d: Int -> fmt.format(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, d) }.time) }
    val vals = remember { (1..diasMes).map { escrita[chave(it)] ?: 0 } }
    val total = vals.sum(); val max = (vals.maxOrNull() ?: 0).coerceAtLeast(1)
    val totalAnt = remember { val c = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }; val pre = SimpleDateFormat("yyyyMM", Locale.US).format(c.time); escrita.filterKeys { it.startsWith(pre) }.values.sum() }
    val comp = if (totalAnt > 0) ((total.toFloat() / totalAnt - 1) * 100).toInt() else null
    val melhor = vals.withIndex().maxByOrNull { it.value }
    val escritos = vals.count { it > 0 }
    val nomeMes = remember { SimpleDateFormat("MMMM", Locale("pt", "BR")).format(Date()).replaceFirstChar { it.uppercase() } }
    val pre = remember { SimpleDateFormat("yyyyMM", Locale.US).format(Date()) }
    val segMes = dadosLeitura.filterKeys { it.startsWith(pre) }.values.sumOf { it.first }
    val frase = when {
        total == 0 -> "Ainda não há palavras neste mês. A primeira linha é a mais difícil."
        comp != null && comp > 0 -> "$comp% a mais que o mês passado. Continue assim."
        escritos >= 15 -> "Mais da metade dos dias com escrita. Isso é constância."
        else -> "Cada dia escrito conta. O mês ainda não acabou."
    }
    Column(Modifier.fillMaxSize().background(Ed.fundo).verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
        TopoV2("Relatório", nav)
        Column(Modifier.padding(horizontal = 22.dp)) {
            Mono("$nomeMes · relatório", Modifier.padding(top = 10.dp))
            Text("%,d".format(total).replace(',', '.'), color = Ed.creme, fontFamily = InstrumentSerif, fontSize = 76.sp, lineHeight = 80.sp)
            Mono("palavras escritas" + (comp?.let { " · ${if (it >= 0) "+" else ""}$it% que o mês passado" } ?: ""), tamanho = 10.sp)
            Row(Modifier.fillMaxWidth().height(130.dp).padding(top = 22.dp).drawBehind { drawLine(Ed.linha, Offset(0f, size.height), Offset(size.width, size.height), 1f) },
                horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                vals.forEachIndexed { k, v ->
                    Box(Modifier.weight(1f).fillMaxHeight(if (v > 0) (v.toFloat() / max).coerceAtLeast(0.04f) else 0.02f)
                        .background(when { k + 1 == diaHoje -> Ed.laranja; v > 0 -> Ed.creme; else -> Ed.linha }))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp)) { Mono("1", tamanho = 10.sp); Spacer(Modifier.weight(1f)); Mono("hoje · $diaHoje", tamanho = 10.sp); Spacer(Modifier.weight(1f)); Mono("$diasMes", tamanho = 10.sp) }
            LinhaPontilhada("Maior dia", melhor?.takeIf { it.value > 0 }?.let { "${"%,d".format(it.value).replace(',', '.')} pal. · dia ${it.index + 1}" } ?: "—")
            LinhaPontilhada("Dias com escrita", "$escritos de $diaHoje")
            LinhaPontilhada("Sequência", "$seq ${if (seq == 1L) "dia" else "dias"}")
            LinhaPontilhada("Média por dia escrito", if (escritos > 0) "%,d".format(total / escritos).replace(',', '.') else "—")
            LinhaPontilhada("Lendo neste mês", "${segMes / 3600} h ${segMes / 60 % 60} min")
            Text("“$frase”", color = Ed.suave, fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic, fontSize = 20.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 18.dp))
            Mono("Tudo é calculado e guardado só neste aparelho.", Modifier.padding(top = 18.dp), tamanho = 9.sp, maxLinhas = 2)
        }
    }
}

// ───────── U9: abertura do capítulo ─────────
@Composable
fun AberturaV2(c: Capitulo, obra: com.huryel.novely.data.Obra?, numero: Int, posicao: Int, total: Int, abrirSobre: () -> Unit) {
    val especial = Texto.tipoEspecial(c.titulo)
    val titulo = if (especial != null) c.titulo.substringAfter('—', c.titulo.substringAfter(':', "")).trim().ifEmpty { especial.lowercase().replaceFirstChar { it.uppercase() } }
        else c.titulo.replace(Regex("^cap[ií]tulo\\s*\\d+\\s*[—–:-]\\s*", RegexOption.IGNORE_CASE), "").ifEmpty { "Sem título" }
    Column(Modifier.fillMaxWidth().padding(top = 70.dp, bottom = 12.dp)) {
        Text(if (especial != null) especial.uppercase() else romanoV2(numero.coerceAtLeast(1)), color = Color(0xFF3A332C), fontFamily = InstrumentSerif,
            fontSize = if (especial != null) 40.sp else 74.sp, lineHeight = 74.sp)
        Text(titulo, color = Ed.creme, fontFamily = InstrumentSerif, fontSize = 36.sp, lineHeight = 38.sp, modifier = Modifier.padding(top = 6.dp))
        Box(Modifier.padding(top = 16.dp, bottom = 14.dp).width(34.dp).height(2.dp).background(Ed.laranja))
        if (obra != null) Mono(obra.titulo, Modifier.clickable(onClick = abrirSobre), tamanho = 10.sp)
        Mono((if (especial == null && total > 0) "Cap. $numero · $posicao de $total · " else "") + "${formatarPalavras(c.palavras.toLong())} palavras · ~${(c.palavras / 230).coerceAtLeast(1)} min",
            Modifier.padding(top = 4.dp), tamanho = 10.sp, maxLinhas = 2)
        if (c.progresso in 0.02f..0.97f) Mono("${(c.progresso * 100).toInt()}% lido", Modifier.padding(top = 4.dp), cor = Ed.laranja, tamanho = 10.sp)
    }
}

// ───────── U10: marcas do mapa do capítulo na barra de progresso ─────────
data class MarcaMapa(val fracao: Float, val tipo: Int, val bloco: Int) // 0 cena, 1 efeito, 2 parou

@Composable
fun BarraComMapa(progresso: Float, marcas: List<MarcaMapa>, cor: Color, modifier: Modifier = Modifier, ir: (Int) -> Unit) {
    BoxWithConstraints(modifier.height(16.dp)) {
        val w = maxWidth
        Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(2.dp).background(Color(0xFF1C1C1C))) {
            Box(Modifier.fillMaxWidth(progresso.coerceIn(0f, 1f)).fillMaxHeight().background(cor))
        }
        marcas.forEach { m ->
            val x = w * m.fracao.coerceIn(0f, 1f)
            Box(Modifier.offset(x = x - 8.dp).size(16.dp).clickable { ir(m.bloco) }, contentAlignment = Alignment.Center) {
                when (m.tipo) {
                    0 -> Box(Modifier.width(1.dp).height(10.dp).background(Ed.suave))
                    1 -> Box(Modifier.size(6.dp).clip(CircleShape).background(Ed.laranja))
                    else -> Canvas(Modifier.size(10.dp)) {
                        drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height * 0.8f); close() }, Ed.creme)
                    }
                }
            }
        }
    }
}

// ───────── U11: fim de capítulo ─────────
@Composable
fun FimV2(vazio: Boolean, atual: Capitulo, numero: Int, proximo: Capitulo?, numeroProx: Int, social: Pair<String, String>?, deOutroOuPublicada: Boolean,
          ir: (Capitulo) -> Unit, voltarObra: () -> Unit, concluir: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp).drawBehind { drawLine(Ed.linha, Offset(0f, 0f), Offset(size.width, 0f), 1f) }.padding(top = 24.dp)) {
        if (vazio) Text("Este capítulo ainda está vazio.", color = Ed.suave)
        Mono(if (numero > 0) "Fim do capítulo $numero" else "Fim", tamanho = 10.sp)
        Serif(atual.titulo.ifEmpty { "Sem título" }, 32.sp, Modifier.padding(top = 6.dp, bottom = 16.dp))
        if (social != null && deOutroOuPublicada) { Reacoes(social.first, social.second); SocialCapitulo(social.first, social.second) }
        if (proximo != null) {
            Mono("Próximo", Modifier.padding(top = 26.dp), tamanho = 10.sp)
            Serif((if (numeroProx > 0) "$numeroProx · " else "") + proximo.titulo.ifEmpty { "Sem título" }, 26.sp, Modifier.padding(top = 2.dp, bottom = 4.dp), maxLinhas = 2)
            Mono("~${(proximo.palavras / 230).coerceAtLeast(1)} min · ${formatarPalavras(proximo.palavras.toLong())} palavras", tamanho = 10.sp)
            BotaoCreme("Ler agora", Modifier.fillMaxWidth().padding(top = 16.dp)) { ir(proximo) }
            BotaoLinha("Voltar à obra", Modifier.fillMaxWidth().padding(top = 8.dp), aoTocar = voltarObra)
        } else {
            Serif("Você chegou ao último capítulo.", 22.sp, Modifier.padding(top = 18.dp), italico = true, cor = Ed.suave)
            if (concluir != null) BotaoCreme("Marcar obra como lida", Modifier.fillMaxWidth().padding(top = 14.dp), aoTocar = concluir)
            BotaoLinha("Voltar à obra", Modifier.fillMaxWidth().padding(top = 8.dp), aoTocar = voltarObra)
        }
    }
}

// ───────── U12: quem está falando agora (topo do painel do Ouvir) ─────────
@Composable
fun FalandoAgora(nome: String, tocando: Boolean) {
    val t = rememberInfiniteTransition(label = "onda")
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "o")
    val parado = reduzirAnimacoes()
    Column(Modifier.fillMaxWidth()) {
        Mono("Falando agora", cor = Ed.laranja, tamanho = 10.sp)
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Serif(nome, 28.sp, Modifier.weight(1f), maxLinhas = 1)
            Row(Modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(14) { k ->
                    val f = if (!tocando || parado) 0.35f else (0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin((v * 2 * Math.PI + k * 0.7).toFloat())))
                    Box(Modifier.width(2.dp).fillMaxHeight(f).background(Ed.creme))
                }
            }
        }
    }
}

// ───────── U17: foco de um parágrafo (editor) ─────────
/** Apaga tudo menos a linha do cursor; [cursor] < 0 apaga o pedaço inteiro. */
class FocoParagrafo(private val base: VisualTransformation, private val cursor: Int) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val b = base.filter(text)
        val t = b.text.text
        val ini = if (cursor < 0) t.length else t.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (cursor == 0) 0 else it + 1 }.coerceIn(0, t.length)
        val fim = if (cursor < 0) t.length else t.indexOf('\n', cursor.coerceAtMost(t.length)).let { if (it < 0) t.length else it }
        val apagado = SpanStyle(color = Color(0xFF3A352F))
        val r = buildAnnotatedString {
            append(b.text)
            if (cursor < 0) addStyle(apagado, 0, t.length)
            else { if (ini > 0) addStyle(apagado, 0, ini); if (fim < t.length) addStyle(apagado, fim, t.length) }
        }
        return TransformedText(r, b.offsetMapping)
    }
    override fun equals(other: Any?) = other is FocoParagrafo && other.base == base && other.cursor == cursor
    override fun hashCode() = base.hashCode() * 31 + cursor
}
