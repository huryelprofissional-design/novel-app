package com.huryel.novely.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.ui.theme.Brilho

/**
 * Perfil no estilo Pôster: fundo cinza com a foto em preto e branco e um cartão escuro "fosco" por cima,
 * com o nome bem grande. Usado no Perfil (aba) e na página pública do autor.
 */
@Composable
fun PerfilPoster(
    foto: ImageBitmap?,
    banner: ImageBitmap?,
    topo: @Composable RowScope.() -> Unit,
    nome: String,
    papel: String,
    botoes: @Composable RowScope.() -> Unit,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())) {
        // topo: o banner (ou a foto) ocupa a largura toda e some no preto; sem nada, um degradê
        Box(Modifier.fillMaxWidth().height(320.dp)) {
            val fundo = banner ?: foto
            if (fundo != null) Image(fundo, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF5A2A12), Color(0xFF3A1458), Color(0xFF0A0A0A)))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x88000000), 0.35f to Color(0x22000000), 1f to Color.Black)))
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, content = topo)
        }
        Column(
            Modifier.padding(start = 12.dp, end = 12.dp, top = 210.dp, bottom = 16.dp).fillMaxWidth()
                .clip(RoundedCornerShape(30.dp)).background(Color(0xFF141414)).border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(30.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFF6A2B), Color(0xFF7A3B8F)))), contentAlignment = Alignment.Center) {
                    if (foto != null) Image(foto, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Text(nome.trim().take(1).uppercase().ifEmpty { "?" }, fontWeight = FontWeight.Black, fontSize = 30.sp, color = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = botoes)
            }
            Text(papel.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp, color = Color(0xFFFF6A2B), modifier = Modifier.padding(top = 16.dp))
            Text(nome.ifBlank { "Sem nome" }, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp,
                color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            conteudo()
        }
    }
}

/** Botão redondo do perfil: com brilho laranja→roxo ou branco. */
@Composable
fun BotaoRedondo(icone: ImageVector, desc: String, brilho: Boolean, aoTocar: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape)
            .background(if (brilho) Brush.radialGradient(Brilho) else Brush.linearGradient(listOf(Color.White, Color.White)))
            .clickable(onClick = aoTocar),
        contentAlignment = Alignment.Center,
    ) { Icon(icone, desc, tint = if (brilho) Color.White else Color.Black, modifier = Modifier.size(20.dp)) }
}

/** Círculo escuro translúcido para o topo (voltar, ajustes…). */
@Composable
fun CirculoTopo(icone: ImageVector, desc: String, aoTocar: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0x55000000)).clickable(onClick = aoTocar), contentAlignment = Alignment.Center) {
        Icon(icone, desc, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Pílula preta "Editar perfil". */
@Composable
fun PilulaPreta(t: String, aoTocar: () -> Unit) {
    Text(t, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF111111)).clickable(onClick = aoTocar).padding(horizontal = 14.dp, vertical = 10.dp))
}

/** Números do perfil (seguidores, obras…) em caixinhas lado a lado. */
@Composable
fun NumerosPerfil(itens: List<Pair<String, String>>) {
    Row(Modifier.padding(top = 18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itens.forEach { (n, r) ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1F1F1F)).padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(n, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White, maxLines = 1)
                Text(r, fontSize = 11.sp, color = Color(0xFFAAAAAA), maxLines = 1)
            }
        }
    }
}

/** Pílula com contorno (atalhos e tags). */
@Composable
fun PilulaContorno(t: String, aoTocar: (() -> Unit)? = null) {
    Text(t, fontSize = 12.5.sp, color = Color(0xFFCCCCCC), maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, Color(0xFF444444), RoundedCornerShape(14.dp))
            .then(if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier).padding(horizontal = 12.dp, vertical = 6.dp))
}

/** Paleta das telas de texto (leitor e editor): falas e enfeites em azul suave, nomes em verde-água. */
@Composable
fun CoresDeLeitura(conteudo: @Composable () -> Unit) {
    val p = com.huryel.novely.ui.theme.LocalPaleta.current
    val calma = remember(p) { p.copy(destaque = Color(0xFF8FB0E0), sobreDestaque = Color(0xFF0B1220), destaque2 = Color(0xFF7FD1C2), destaque3 = Color(0xFF8FB0E0)) }
    androidx.compose.runtime.CompositionLocalProvider(com.huryel.novely.ui.theme.LocalPaleta provides calma) { conteudo() }
}
