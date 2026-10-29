package com.huryel.novely.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.data.Capitulo
import com.huryel.novely.data.Obra
import com.huryel.novely.data.Volume
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Paleta
import com.huryel.novely.ui.theme.Serifa

/** Puxa o conteúdo para cima sem deixar buraco embaixo (a capa "sai" do painel). */
private fun Modifier.subir(d: Dp) = this.layout { m, c ->
    val pl = m.measure(c)
    val px = d.roundToPx()
    layout(pl.width, (pl.height - px).coerceAtLeast(0)) { pl.place(0, -px) }
}

class AcoesObra(
    val voltar: () -> Unit, val menu: () -> Unit, val banner: () -> Unit, val capa: () -> Unit,
    val ler: (() -> Unit)?, val novoCap: () -> Unit, val novoVol: () -> Unit,
)

@Composable
fun CabecalhoObra(estilo: String, o: Obra, caps: List<Capitulo>, volumes: List<Volume>, alvo: Capitulo?, desloc: () -> Float, a: AcoesObra) {
    when (estilo) {
        "REVISTA" -> CabecalhoRevista(o, caps, alvo, a)
        "ESTANTE" -> CabecalhoEstante(o, caps, volumes, alvo, a)
        else -> CabecalhoCinema(o, caps, alvo, desloc, a)
    }
}

@Composable
private fun BotaoCirculo(icone: @Composable () -> Unit, acao: () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(Color(0x88000000)).clickable(onClick = acao), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { icone() }
    }
}

@Composable
private fun Sinopse(texto: String, p: Paleta, capitular: Boolean, centro: Boolean = false) {
    var aberta by remember { mutableStateOf(false) }
    if (texto.isBlank()) {
        Text("Sem sinopse. Toque em ⋮ › Editar título e descrição.", fontSize = 13.sp, color = p.textoSuave, textAlign = if (centro) TextAlign.Center else TextAlign.Start)
        return
    }
    val t = if (capitular) buildAnnotatedString {
        withStyle(SpanStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, color = p.destaque)) { append(texto.take(1)) }
        append(texto.drop(1))
    } else buildAnnotatedString { append(texto) }
    Text(t, fontFamily = Serifa, fontSize = 15.sp, lineHeight = 23.sp, color = p.textoSuave, fontStyle = if (centro) FontStyle.Italic else FontStyle.Normal,
        textAlign = if (centro) TextAlign.Center else TextAlign.Start,
        maxLines = if (aberta) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { aberta = !aberta })
}

private fun textoAlvo(alvo: Capitulo?): String = when {
    alvo == null -> "Sem capítulos"
    alvo.progresso in 0.01f..0.97f -> "Continuar · ${alvo.titulo.ifEmpty { "sem título" }} · ${(alvo.progresso * 100).toInt()}%"
    else -> "Ler · ${alvo.titulo.ifEmpty { "sem título" }}"
}

// ───────── CINEMA ─────────

@Composable
private fun CabecalhoCinema(o: Obra, caps: List<Capitulo>, alvo: Capitulo?, desloc: () -> Float, a: AcoesObra) {
    val p = LocalPaleta.current
    val palavras = caps.sumOf { it.palavras.toLong() }
    val lidos = caps.count { it.progresso >= 0.98f }
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(340.dp).graphicsLayer { translationY = desloc() * 0.5f }.clickable(onClick = a.banner)) {
            if (o.banner != null) ImagemEnquadrada(o.banner, o.bannerX, o.bannerY, o.bannerZoom, o.corBase, Modifier.fillMaxSize(), 1400)
            else Box(Modifier.fillMaxSize().background(Color(o.corBase))) {
                ArteCapa(o.titulo + "·banner", o.corBase)
                Text("toque para colocar um banner", fontSize = 12.sp, color = Color(0x99FFFFFF), modifier = Modifier.align(Alignment.Center))
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BotaoCirculo({ Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }, a.voltar)
            BotaoCirculo({ Icon(Icons.Filled.MoreVert, "Opções da obra") }, a.menu)
        }
        Column(
            Modifier.padding(top = 260.dp).fillMaxWidth().shadow(24.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(p.fundo).padding(horizontal = 20.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(p.borda))
            Row(Modifier.padding(top = 12.dp).subir(70.dp), verticalAlignment = Alignment.Bottom) {
                CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
                    Modifier.width(104.dp).height(156.dp).shadow(16.dp, RoundedCornerShape(12.dp)).clickable(onClick = a.capa), 13.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.padding(bottom = 4.dp)) {
                    Text(nomeStatus(o.status).uppercase() + if (caps.isNotEmpty()) " · ${lidos * 100 / caps.size}%" else "",
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, color = p.destaque)
                    Text(o.titulo, fontFamily = Serifa, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 27.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { a.ler?.invoke() }, enabled = a.ler != null, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = p.texto, contentColor = p.fundo), modifier = Modifier.weight(1f).height(50.dp)) {
                    Text("▶  " + textoAlvo(alvo), fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(onClick = a.novoCap, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = p.superficie, contentColor = p.texto), modifier = Modifier.height(50.dp)) {
                    Text("+ Cap.", fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Sinopse(o.descricao, p, capitular = false)
            Text("${caps.size} capítulos · ${formatarPalavras(palavras)} palavras · ~${(palavras / 230 / 60).coerceAtLeast(0)} h de leitura",
                fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Capítulos", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier
                    .drawBehind { drawRect(p.destaque, topLeft = Offset(0f, size.height + 6f), size = androidx.compose.ui.geometry.Size(size.width, 5f)) })
                Spacer(Modifier.weight(1f))
                TextButton(onClick = a.novoVol) { Text("+ Volume") }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.borda))
        }
    }
}

// ───────── REVISTA ─────────

@Composable
private fun CabecalhoRevista(o: Obra, caps: List<Capitulo>, alvo: Capitulo?, a: AcoesObra) {
    val p = LocalPaleta.current
    val palavras = caps.sumOf { it.palavras.toLong() }
    val lidos = caps.count { it.progresso >= 0.98f }
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ ESTANTE", fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = p.textoSuave,
                modifier = Modifier.clickable(onClick = a.voltar).padding(vertical = 12.dp))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = a.menu) { Icon(Icons.Filled.MoreVert, "Opções da obra", tint = p.textoSuave) }
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
            CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
                Modifier.width(118.dp).height(176.dp).clickable(onClick = a.capa), 14.sp)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(nomeStatus(o.status).uppercase(), fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold, color = p.destaque)
                Text(o.titulo, fontFamily = Serifa, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 30.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.texto))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Coluna("${caps.size}", "CAPÍTULOS", p, Modifier.weight(1f), p.texto)
            Box(Modifier.width(1.dp).fillMaxHeight().background(p.borda))
            Coluna(formatarPalavras(palavras), "PALAVRAS", p, Modifier.weight(1f), p.texto)
            Box(Modifier.width(1.dp).fillMaxHeight().background(p.borda))
            Coluna(if (caps.isNotEmpty()) "${lidos * 100 / caps.size}%" else "—", "LIDO", p, Modifier.weight(1f), p.destaque)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.borda))
        Spacer(Modifier.height(14.dp))
        Sinopse(o.descricao, p, capitular = true)
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().background(if (a.ler != null) p.destaque else p.borda).clickable(enabled = a.ler != null) { a.ler?.invoke() }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(textoAlvo(alvo).uppercase(), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontSize = 13.sp, color = p.sobreDestaque,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("→", fontWeight = FontWeight.ExtraBold, color = p.sobreDestaque)
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("SUMÁRIO", fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold, color = p.textoSuave, modifier = Modifier.weight(1f))
            TextButton(onClick = a.novoCap) { Text("+ CAPÍTULO", fontSize = 12.sp, letterSpacing = 1.sp) }
            TextButton(onClick = a.novoVol) { Text("+ VOLUME", fontSize = 12.sp, letterSpacing = 1.sp) }
        }
    }
}

@Composable
private fun Coluna(valor: String, rotulo: String, p: Paleta, modifier: Modifier, cor: Color) {
    Column(modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
        Text(valor, fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = cor)
        Text(rotulo, fontSize = 10.sp, letterSpacing = 1.5.sp, color = p.textoSuave)
    }
}

// ───────── ESTANTE ─────────

@Composable
private fun CabecalhoEstante(o: Obra, caps: List<Capitulo>, volumes: List<Volume>, alvo: Capitulo?, a: AcoesObra) {
    val p = LocalPaleta.current
    val palavras = caps.sumOf { it.palavras.toLong() }
    val lidos = caps.count { it.progresso >= 0.98f }
    Column(
        Modifier.fillMaxWidth().drawBehind {
            drawCircle(Brush.radialGradient(listOf(p.destaque.copy(alpha = 0.17f), Color.Transparent), center = Offset(size.width / 2, 120f * density), radius = size.width * 0.65f),
                radius = size.width * 0.65f, center = Offset(size.width / 2, 120f * density))
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BotaoCirculo({ Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }, a.voltar)
            BotaoCirculo({ Icon(Icons.Filled.MoreVert, "Opções da obra") }, a.menu)
        }
        CapaObra(o.titulo, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
            Modifier.width(152.dp).height(228.dp)
                .graphicsLayer { rotationY = -14f; cameraDistance = 14f * density }
                .shadow(22.dp, RoundedCornerShape(12.dp)).clickable(onClick = a.capa), 18.sp)
        Spacer(Modifier.height(18.dp))
        Text(o.titulo, fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 30.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp))
        Text("${caps.size} capítulos · ${formatarPalavras(palavras)} palavras · ${nomeStatus(o.status)}", fontSize = 13.sp, color = p.textoSuave,
            modifier = Modifier.padding(top = 4.dp))
        Box(Modifier.padding(horizontal = 28.dp, vertical = 10.dp)) { Sinopse(o.descricao, p, capitular = false, centro = true) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { a.ler?.invoke() }, enabled = a.ler != null, shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.height(48.dp).widthIn(max = 230.dp)) {
                Text("▶  " + (alvo?.titulo?.ifEmpty { "sem título" } ?: "Sem capítulos"), fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = a.novoCap, shape = RoundedCornerShape(26.dp), modifier = Modifier.height(48.dp)) { Text("+ Cap.") }
        }
        // progresso dividido por volume
        if (caps.isNotEmpty()) {
            val grupos = buildList {
                val sem = caps.filter { c -> c.volumeId == null || volumes.none { it.id == c.volumeId } }
                if (sem.isNotEmpty()) add((if (volumes.isEmpty()) "Capítulos" else "Sem vol.") to sem)
                volumes.forEach { v -> val cs = caps.filter { it.volumeId == v.id }; if (cs.isNotEmpty()) add(v.titulo to cs) }
            }
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp)) {
                Row {
                    Text("Progresso da obra", fontSize = 12.sp, color = p.textoSuave, modifier = Modifier.weight(1f))
                    Text("${lidos * 100 / caps.size}%", fontSize = 12.sp, color = p.destaque, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    grupos.forEach { (_, cs) ->
                        val f = cs.count { it.progresso >= 0.98f }.toFloat() / cs.size
                        Box(Modifier.weight(cs.size.toFloat()).height(8.dp).clip(RoundedCornerShape(3.dp)).background(p.borda)) {
                            Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(p.destaque))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    grupos.forEach { (nome, cs) ->
                        Text(nome, fontSize = 10.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(cs.size.toFloat()))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Capítulos", fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = a.novoVol) { Text("+ Volume") }
        }
    }
}

// ───────── Linha de capítulo em cada estilo ─────────

/** Visual da linha (o menu ⋮ e o botão de editar são os mesmos nos três estilos). */
@Composable
fun LinhaCapEstilo(
    estilo: String, c: Capitulo, o: Obra, atual: Boolean, lido: Boolean, numero: Int,
    abrir: () -> Unit, acoes: @Composable RowScope.() -> Unit,
) {
    val p = LocalPaleta.current
    val especial = com.huryel.novely.data.Texto.tipoEspecial(c.titulo)
    val titulo = c.titulo.ifEmpty { "(sem título — escreva o título na 1ª linha)" }
    val corTitulo = if (atual) p.destaque else if (lido) p.textoSuave.copy(alpha = 0.7f) else p.texto
    val direita = when {
        c.rascunho && o.status == com.huryel.novely.data.StatusObra.ESCREVENDO -> "RASCUNHO"
        c.progresso in 0.01f..0.979f -> "${(c.progresso * 100).toInt()}%"
        lido -> "✓"
        else -> formatarPalavras(c.palavras.toLong())
    }
    val corDireita = if (direita == "RASCUNHO") Color(0xFF6FC2B4) else if (atual) p.destaque else p.textoSuave
    when (estilo) {
        "REVISTA" -> Row(Modifier.fillMaxWidth().clickable(onClick = abrir).padding(start = 22.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).padding(vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
                Text((if (especial != null) "" else "$numero · ") + titulo, fontFamily = Serifa, fontSize = 16.sp, color = corTitulo,
                    fontWeight = if (atual) FontWeight.Bold else FontWeight.Normal, fontStyle = if (especial != null || c.titulo.isEmpty()) FontStyle.Italic else FontStyle.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Box(Modifier.weight(1f).padding(horizontal = 6.dp).height(14.dp).drawBehind {
                    var x = 0f
                    while (x < size.width) { drawCircle(if (atual) p.destaque else p.borda, radius = 1.4f, center = Offset(x, size.height - 3f)); x += 7f }
                })
                Text(direita, fontSize = if (direita == "RASCUNHO") 10.sp else 12.sp, fontWeight = FontWeight.Bold, color = corDireita)
            }
            acoes()
        }
        "ESTANTE" -> Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(p.superficie)
                .clickable(onClick = abrir).drawBehind { if (atual) drawRect(p.destaque, size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)) }
                .padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (especial != null) "◆" else "$numero", fontWeight = FontWeight.ExtraBold, color = if (atual) p.destaque else p.textoSuave, modifier = Modifier.width(40.dp))
            Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(titulo, fontWeight = if (atual) FontWeight.Bold else FontWeight.Normal, color = if (atual) p.texto else corTitulo, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontStyle = if (especial != null) FontStyle.Italic else FontStyle.Normal)
                Text(direita, fontSize = 11.sp, color = corDireita, fontWeight = FontWeight.Bold)
            }
            acoes()
        }
        else -> Row(Modifier.fillMaxWidth().clickable(onClick = abrir).padding(start = 20.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(86.dp).height(50.dp).clip(RoundedCornerShape(8.dp)).background(Color(o.corBase).copy(alpha = if (lido) 0.35f else 0.8f))) {
                Text(if (especial != null) especial.take(3) else "$numero", fontFamily = Serifa, fontWeight = FontWeight.Black, fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.85f), modifier = Modifier.align(Alignment.Center))
                if (c.progresso > 0.01f) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(c.progresso.coerceAtMost(1f)).height(3.dp).background(p.destaque))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = corTitulo, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontStyle = if (especial != null) FontStyle.Italic else FontStyle.Normal)
                Text("${formatarPalavras(c.palavras.toLong())} palavras · ~${(c.palavras / 230).coerceAtLeast(1)} min" + if (direita == "RASCUNHO") " · rascunho" else "",
                    fontSize = 12.sp, color = p.textoSuave, maxLines = 1)
            }
            acoes()
        }
    }
}
