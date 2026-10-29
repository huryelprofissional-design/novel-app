package com.huryel.novely.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.R

// ───────── Fontes embutidas (funcionam offline) ─────────
val Newsreader = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.newsreader_semibold, FontWeight.SemiBold),
    Font(R.font.newsreader_semibold, FontWeight.Bold),
    Font(R.font.newsreader_semibold, FontWeight.Black),
)
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold), Font(R.font.inter_bold, FontWeight.ExtraBold), Font(R.font.inter_bold, FontWeight.Black),
)
/** Como as falas aparecem no leitor. */
enum class EstiloFala { BARRA, FIO, BALAO, ETIQUETA }

/** Identidade visual completa de um tema. */
@Immutable
data class Paleta(
    val nome: String,
    val descricao: String,
    val fundo: Color,
    val superficie: Color,
    val borda: Color,
    val texto: Color,
    val textoSuave: Color,
    val destaque: Color,
    val sobreDestaque: Color,
    val destaque2: Color,
    val destaque3: Color,
    val escuro: Boolean,
    val fonteTitulo: FontFamily,
    val fonteUi: FontFamily,
    val fonteLeitura: FontFamily,
    /** multiplica os cantos arredondados (0 = quadrado) */
    val escalaRaio: Float,
    /** bordas grossas e retas (Terminal) */
    val bordaForte: Boolean,
    /** títulos em MAIÚSCULAS */
    val maiusculas: Boolean,
    /** brilhos coloridos no fundo (Neon) */
    val brilho: Boolean,
    val fala: EstiloFala,
    val separador: String,
)

enum class TemaLeitura(val paleta: Paleta) {
    OLED(Paleta(
        "Noite", "Preto puro e âmbar",
        Color(0xFF000000), Color(0xFF121110), Color(0xFF26231E), Color(0xFFE6DFD3), Color(0xFFA39A8B),
        Color(0xFFE9A65A), Color(0xFF1A1206), Color(0xFF6FC2B4), Color(0xFFE88AA8), true,
        Inter, Inter, Newsreader, 1f, false, false, false, EstiloFala.BARRA, "◆   ◆   ◆",
    )),
}

val LocalPaleta = staticCompositionLocalOf { TemaLeitura.OLED.paleta }

/** Fonte de títulos do tema atual (antes era sempre serifa). */
val Serifa: FontFamily
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.fonteTitulo

/** Cantos que respeitam o tema (Terminal = quadrado, Neon = bem redondo). */
@Composable @ReadOnlyComposable
fun forma(r: Dp): RoundedCornerShape = RoundedCornerShape(r * LocalPaleta.current.escalaRaio)

@Composable @ReadOnlyComposable
fun forma(topStart: Dp = 0.dp, topEnd: Dp = 0.dp, bottomEnd: Dp = 0.dp, bottomStart: Dp = 0.dp): RoundedCornerShape {
    val e = LocalPaleta.current.escalaRaio
    return RoundedCornerShape(topStart * e, topEnd * e, bottomEnd * e, bottomStart * e)
}

/** Aplica MAIÚSCULAS quando o tema pede. */
@Composable @ReadOnlyComposable
fun titulo(t: String): String = if (LocalPaleta.current.maiusculas) t.uppercase() else t

@Composable
fun NovelyTheme(tema: TemaLeitura = TemaLeitura.OLED, content: @Composable () -> Unit) {
    val p = tema.paleta
    val esquema = if (p.escuro) darkColorScheme(
        primary = p.destaque, onPrimary = p.sobreDestaque, secondary = p.destaque2,
        background = p.fundo, onBackground = p.texto, surface = p.fundo, onSurface = p.texto,
        surfaceVariant = p.superficie, onSurfaceVariant = p.textoSuave,
        surfaceContainer = p.superficie, surfaceContainerHigh = p.superficie, surfaceContainerLow = p.superficie, outline = p.borda,
    ) else lightColorScheme(
        primary = p.destaque, onPrimary = p.sobreDestaque, secondary = p.destaque2,
        background = p.fundo, onBackground = p.texto, surface = p.fundo, onSurface = p.texto,
        surfaceVariant = p.superficie, onSurfaceVariant = p.textoSuave,
        surfaceContainer = p.superficie, surfaceContainerHigh = p.superficie, surfaceContainerLow = p.superficie, outline = p.borda,
    )
    val ui = p.fonteUi
    val tit = p.fonteTitulo
    val pesoTitulo = FontWeight.ExtraBold
    val tipografia = Typography(
        displaySmall = TextStyle(fontFamily = tit, fontWeight = pesoTitulo, fontSize = 30.sp),
        headlineSmall = TextStyle(fontFamily = tit, fontWeight = pesoTitulo, fontSize = 21.sp),
        titleMedium = TextStyle(fontFamily = ui, fontWeight = FontWeight.Bold, fontSize = 16.sp),
        bodyLarge = TextStyle(fontFamily = ui, fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontFamily = ui, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = ui, fontSize = 12.sp),
        labelLarge = TextStyle(fontFamily = ui, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = ui, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
        labelSmall = TextStyle(fontFamily = ui, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp),
        titleLarge = TextStyle(fontFamily = tit, fontWeight = pesoTitulo, fontSize = 20.sp),
    )
    val e = p.escalaRaio
    val formas = Shapes(
        extraSmall = RoundedCornerShape(4.dp * e), small = RoundedCornerShape(8.dp * e), medium = RoundedCornerShape(14.dp * e),
        large = RoundedCornerShape(20.dp * e), extraLarge = RoundedCornerShape(28.dp * e),
    )
    CompositionLocalProvider(LocalPaleta provides p) {
        MaterialTheme(colorScheme = esquema, typography = tipografia, shapes = formas, content = content)
    }
}
