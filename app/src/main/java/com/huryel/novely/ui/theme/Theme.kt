package com.huryel.novely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Paleta da direção aprovada "Noite Âmbar". */
@Immutable
data class Paleta(
    val nome: String,
    val fundo: Color,
    val superficie: Color,
    val borda: Color,
    val texto: Color,
    val textoSuave: Color,
    val destaque: Color,
    val sobreDestaque: Color,
    val escuro: Boolean,
)

enum class TemaLeitura(val paleta: Paleta) {
    OLED(Paleta("OLED", Color(0xFF000000), Color(0xFF121110), Color(0xFF26231E),
        Color(0xFFE6DFD3), Color(0xFFA39A8B), Color(0xFFE9A65A), Color(0xFF1A1206), true)),
    AMBAR(Paleta("Âmbar", Color(0xFF0F0E0C), Color(0xFF1A1815), Color(0xFF2E2A24),
        Color(0xFFEDE6DA), Color(0xFFA39A8B), Color(0xFFE9A65A), Color(0xFF1A1206), true)),
    SEPIA(Paleta("Sépia", Color(0xFFEFE4CF), Color(0xFFE5D7BC), Color(0xFFCDBB9A),
        Color(0xFF3B2F22), Color(0xFF6E5F4B), Color(0xFF9A5B14), Color(0xFFFFF7EA), false)),
    PAPEL(Paleta("Papel", Color(0xFFFAF8F3), Color(0xFFF0ECE3), Color(0xFFDCD5C7),
        Color(0xFF1F1D1A), Color(0xFF615B51), Color(0xFF9A5B14), Color(0xFFFFFFFF), false)),
}

val LocalPaleta = staticCompositionLocalOf { TemaLeitura.OLED.paleta }

val Serifa = FontFamily.Serif

@Composable
fun NovelyTheme(tema: TemaLeitura = TemaLeitura.OLED, content: @Composable () -> Unit) {
    val p = tema.paleta
    val esquema = if (p.escuro) darkColorScheme(
        primary = p.destaque, onPrimary = p.sobreDestaque,
        background = p.fundo, onBackground = p.texto,
        surface = p.fundo, onSurface = p.texto,
        surfaceVariant = p.superficie, onSurfaceVariant = p.textoSuave,
        surfaceContainer = p.superficie, surfaceContainerHigh = p.superficie, outline = p.borda,
    ) else lightColorScheme(
        primary = p.destaque, onPrimary = p.sobreDestaque,
        background = p.fundo, onBackground = p.texto,
        surface = p.fundo, onSurface = p.texto,
        surfaceVariant = p.superficie, onSurfaceVariant = p.textoSuave,
        surfaceContainer = p.superficie, surfaceContainerHigh = p.superficie, outline = p.borda,
    )
    val tipografia = Typography(
        displaySmall = TextStyle(fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 32.sp),
        headlineSmall = TextStyle(fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 22.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
        bodyLarge = TextStyle(fontFamily = Serifa, fontSize = 18.sp, lineHeight = 31.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.8.sp),
    )
    CompositionLocalProvider(LocalPaleta provides p) {
        MaterialTheme(colorScheme = esquema, typography = tipografia, content = content)
    }
}
