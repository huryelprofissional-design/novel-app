package com.huryel.novely.ui

import com.huryel.novely.ui.theme.forma
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Ficha
import com.huryel.novely.data.Marcador
import com.huryel.novely.data.TipoFicha
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Paleta
import com.huryel.novely.ui.theme.Serifa
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val coresGrifo = linkedMapOf(
    "AMBAR" to Color(0xFFE9A65A), "VERDE" to Color(0xFF6FC2B4), "AZUL" to Color(0xFF7FA7E6), "ROSA" to Color(0xFFE88AA8),
)

/** Folha que abre ao segurar um parágrafo no leitor. */
@Composable
fun FolhaParagrafo(
    trecho: String, marcador: Marcador?, obraId: Long, capId: Long, bloco: Int, obraTitulo: String, capTitulo: String,
    fichas: List<Ficha>, fechar: () -> Unit, abrirFicha: (Ficha) -> Unit,
) {
    val app = NovelyApp.instancia
    val repo = app.repo
    val p = LocalPaleta.current
    val ctx = LocalContext.current
    var nota by remember { mutableStateOf(marcador?.nota ?: "") }
    fun salvar(cor: String?, marcar: Boolean? = null) {
        val base = marcador ?: Marcador(obraId = obraId, capId = capId, bloco = bloco, trecho = trecho.take(400))
        val novo = base.copy(cor = cor ?: base.cor, nota = nota.trim())
        app.escopo.launch { repo.salvarMarcador(novo) }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(trecho, fontFamily = Serifa, fontStyle = FontStyle.Italic, fontSize = 15.sp, lineHeight = 22.sp, color = p.textoSuave, maxLines = 4, overflow = TextOverflow.Ellipsis)
        Text("GRIFAR", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            coresGrifo.forEach { (nome, cor) ->
                val sel = marcador?.cor == nome
                Box(Modifier.size(40.dp).clip(CircleShape).background(cor.copy(alpha = 0.85f)).border(if (sel) 3.dp else 0.dp, p.texto, CircleShape)
                    .clickable { salvar(nome); fechar() })
            }
            if (marcador != null) TextButton(onClick = { app.escopo.launch { repo.excluirMarcador(marcador) }; fechar() }) { Text("Remover", color = Color(0xFFE57373)) }
        }
        OutlinedTextField(nota, { nota = it }, label = { Text("Nota (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { salvar(if (marcador == null) "" else null); fechar() }, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque)) {
                Text(if (marcador == null) "🔖 Marcar" else "Salvar nota")
            }
            OutlinedButton(onClick = {
                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("trecho", trecho)); fechar()
            }) { Text("Copiar") }
            OutlinedButton(onClick = { compartilharTrecho(ctx, trecho, obraTitulo, capTitulo, p); fechar() }) { Text("Compartilhar imagem") }
        }
        val citados = fichas.filter { it.nome.length >= 2 && trecho.contains(it.nome, ignoreCase = true) }
        if (citados.isNotEmpty()) {
            Text("NESTE PARÁGRAFO", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                citados.forEach { f -> Chip((if (f.tipo == TipoFicha.LUGAR) "⌂ " else "☺ ") + f.nome, false) { abrirFicha(f) } }
            }
        }
    }
}

/** Mostra uma ficha (somente leitura) em folha. */
@Composable
fun VerFicha(f: Ficha, ateOrdem: Double? = null) {
    val p = LocalPaleta.current
    val img = rememberImagem(f.imagem, 500)
    val trechos by remember(f.id) { com.huryel.novely.NovelyApp.instancia.repo.trechosFicha(f.id) }.collectAsState(initial = emptyList())
    // sem spoiler: no leitor, só o que já apareceu até o capítulo atual
    val visiveis = if (ateOrdem == null) trechos else trechos.filter { it.capOrdem <= ateOrdem }
    Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(p.borda), contentAlignment = Alignment.Center) {
                if (img != null) androidx.compose.foundation.Image(img, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text(if (f.tipo == TipoFicha.LUGAR) "⌂" else f.nome.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = p.destaque)
            }
            Column {
                Text(when (f.tipo) { TipoFicha.LUGAR -> "LUGAR"; TipoFicha.OUTRO -> "OUTRO"; else -> "PERSONAGEM" }, style = MaterialTheme.typography.labelSmall, color = p.destaque)
                Text(f.nome, style = MaterialTheme.typography.headlineSmall)
                if (f.apelidos.isNotBlank()) Text("também: ${f.apelidos}", fontSize = 12.sp, color = p.textoSuave)
            }
        }
        listOf("Idade" to f.idade, "Aparência" to f.aparencia, "Poder" to f.poder, "Facção" to f.faccao, "Relações" to f.relacoes)
            .filter { it.second.isNotBlank() }.forEach { (r, v) ->
                Row { Text("$r  ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.textoSuave); Text(v, fontSize = 14.sp) }
            }
        if (f.descricao.isNotBlank()) Text(f.descricao, fontSize = 15.sp, lineHeight = 22.sp)
        if (visiveis.isNotEmpty()) {
            Text("NA HISTÓRIA", style = MaterialTheme.typography.labelSmall, color = p.textoSuave, modifier = Modifier.padding(top = 6.dp))
            visiveis.forEach { t ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.fundo).padding(12.dp)) {
                    Text(t.capTitulo, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.destaque)
                    Text(t.texto, fontSize = 14.sp, lineHeight = 20.sp, color = p.textoSuave, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (f.descricao.isBlank() && visiveis.isEmpty()) Text("Sem descrição ainda.", fontSize = 14.sp, color = p.textoSuave)
    }
}

/** Gera um cartão (imagem) com o trecho e abre o compartilhar do Android. */
fun compartilharTrecho(ctx: Context, trecho: String, obra: String, cap: String, p: Paleta) {
    try {
        val w = 1080; val h = 1350; val m = 110
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(p.fundo.toArgb())
        val ambar = p.destaque.toArgb()
        val pinta = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ambar }
        c.drawRect(m.toFloat(), 150f, (m + 90).toFloat(), 158f, pinta)
        val aspas = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = ambar; textSize = 220f; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD) }
        c.drawText("“", m.toFloat() - 10f, 380f, aspas)
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = p.texto.toArgb(); textSize = if (trecho.length > 400) 42f else 54f; typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL) }
        val texto = if (trecho.length > 700) trecho.take(700) + "…" else trecho
        val layout = StaticLayout.Builder.obtain(texto, 0, texto.length, tp, w - m * 2).setLineSpacing(0f, 1.35f).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        c.save(); c.translate(m.toFloat(), 420f); layout.draw(c); c.restore()
        val rodape = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = p.textoSuave.toArgb(); textSize = 36f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        c.drawText("— $obra".take(50), m.toFloat(), (h - 210).toFloat(), rodape)
        rodape.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC); rodape.textSize = 32f
        c.drawText(cap.take(60), m.toFloat(), (h - 160).toFloat(), rodape)
        val marca = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = ambar; textSize = 34f; letterSpacing = 0.3f; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD) }
        c.drawText("NOVELY", (w - m - marca.measureText("NOVELY")), (h - 160).toFloat(), marca)
        val dir = File(ctx.cacheDir, "compartilhar").apply { mkdirs() }
        val f = File(dir, "trecho.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".arquivos", f)
        val envio = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        ctx.startActivity(Intent.createChooser(envio, "Compartilhar trecho").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { }
}

// ───────── Lista de marcadores da obra ─────────

@Composable
fun TelaMarcadores(obraId: Long, nav: NavHostController) {
    val repo = NovelyApp.instancia.repo
    val p = LocalPaleta.current
    val marcas by remember(obraId) { repo.marcadoresObra(obraId) }.collectAsState(initial = emptyList())
    val caps by remember(obraId) { repo.capitulos(obraId) }.collectAsState(initial = emptyList())
    var filtro by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Topo("Marcadores e grifos", nav)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Todos · ${marcas.size}", filtro == "") { filtro = "" }
            Chip("🔖 Marcadores", filtro == "M") { filtro = "M" }
            Chip("Grifos", filtro == "G") { filtro = "G" }
            Chip("Com nota", filtro == "N") { filtro = "N" }
        }
        val lista = marcas.filter { when (filtro) { "M" -> it.cor.isEmpty(); "G" -> it.cor.isNotEmpty(); "N" -> it.nota.isNotBlank(); else -> true } }
        if (lista.isEmpty()) Text("Nada aqui ainda. No leitor, segure um parágrafo para marcar ou grifar.", color = p.textoSuave, modifier = Modifier.padding(20.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(lista, key = { it.id }) { m ->
                val cap = caps.firstOrNull { it.id == m.capId }
                Row(Modifier.fillMaxWidth().clip(forma(16.dp)).background(p.superficie)
                    .clickable { nav.navigate("leitor/${m.capId}?b=${m.bloco}") }.padding(14.dp)) {
                    Box(Modifier.width(4.dp).height(56.dp).clip(forma(2.dp)).background(coresGrifo[m.cor] ?: p.destaque))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(cap?.titulo?.ifEmpty { "sem título" } ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.destaque)
                        Text(m.trecho, fontFamily = Serifa, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        if (m.nota.isNotBlank()) Text("✎ ${m.nota}", fontSize = 13.sp, color = p.textoSuave)
                    }
                }
            }
        }
    }
}

// ───────── Estatísticas ─────────

@Composable
fun TelaEstatisticas(nav: NavHostController) {
    val prefs = NovelyApp.instancia.prefs
    val p = LocalPaleta.current
    val dados = remember { prefs.estatisticas() }
    val hoje by prefs.palavrasHoje.estado.collectAsState()
    val seq by prefs.sequencia.estado.collectAsState()
    val fmt = remember { SimpleDateFormat("yyyyMMdd", Locale.US) }
    val dias = remember {
        (6 downTo 0).map { d ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -d) }
            val chave = fmt.format(cal.time)
            Triple(SimpleDateFormat("EEE", Locale("pt", "BR")).format(cal.time).take(3), dados[chave]?.first ?: 0, dados[chave]?.second ?: 0)
        }
    }
    val maxMin = (dias.maxOfOrNull { it.second } ?: 0).coerceAtLeast(60)
    val totalSeg = dados.values.sumOf { it.first }
    val totalPal = dados.values.sumOf { it.second.toLong() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item { Topo("Estatísticas", nav) }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("LEITURA NOS ÚLTIMOS 7 DIAS (MINUTOS)", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
                Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    dias.forEach { (nome, seg, _) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${seg / 60}", fontSize = 11.sp, color = p.textoSuave)
                            Box(Modifier.fillMaxWidth().height((130f * seg / maxMin).dp.coerceAtLeast(3.dp)).clip(forma(6.dp))
                                .background(if (seg > 0) p.destaque else p.borda))
                            Text(nome, fontSize = 11.sp, color = p.textoSuave)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CartaoNumero("${totalSeg / 3600}h ${totalSeg / 60 % 60}min", "lendo (120 dias)", Modifier.weight(1f))
                    CartaoNumero(formatarPalavras(totalPal), "palavras lidas", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CartaoNumero("$hoje", "escritas hoje", Modifier.weight(1f))
                    CartaoNumero("🔥 $seq", "dias seguidos na meta", Modifier.weight(1f))
                }
                Text("Tudo é calculado e guardado só neste aparelho.", fontSize = 12.sp, color = p.textoSuave)
            }
        }
    }
}

@Composable
private fun CartaoNumero(valor: String, rotulo: String, modifier: Modifier) {
    val p = LocalPaleta.current
    Column(modifier.clip(forma(16.dp)).background(p.superficie).padding(14.dp)) {
        Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = p.destaque)
        Text(rotulo, fontSize = 12.sp, color = p.textoSuave)
    }
}
