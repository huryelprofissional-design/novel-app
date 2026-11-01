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
    topo: @Composable RowScope.() -> Unit,
    nome: String,
    papel: String,
    botoes: @Composable RowScope.() -> Unit,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFB4B4B4), Color(0xFF5A5A5A), Color(0xFF1E1E1E))))) {
        if (foto != null) Image(
            foto, null, contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 60.dp).fillMaxWidth(1.05f).fillMaxHeight(0.75f).blur(3.dp),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x22000000), 1f to Color(0x99000000))))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, content = topo)
            Column(
                Modifier.padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 16.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp)).background(Color(0xD9141414)).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(30.dp))
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF555555)))
                Row(Modifier.padding(top = 14.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(nome.ifBlank { "Sem nome" } + "\n" + papel, fontWeight = FontWeight.Black, fontSize = 42.sp, lineHeight = 41.sp, letterSpacing = (-1).sp,
                        color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Row(Modifier.padding(start = 8.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = botoes)
                }
                conteudo()
            }
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

/** Números do perfil (seguidores, obras…). */
@Composable
fun NumerosPerfil(itens: List<Pair<String, String>>) {
    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
        itens.forEach { (n, r) ->
            Column {
                Text(n, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = Color.White)
                Text(r, fontSize = 12.sp, color = Color(0xFFAAAAAA))
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
