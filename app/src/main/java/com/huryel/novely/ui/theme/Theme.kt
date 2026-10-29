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
val Unbounded = FontFamily(
    Font(R.font.unbounded_medium, FontWeight.Normal), Font(R.font.unbounded_medium, FontWeight.Medium),
    Font(R.font.unbounded_bold, FontWeight.SemiBold), Font(R.font.unbounded_bold, FontWeight.Bold),
    Font(R.font.unbounded_black, FontWeight.ExtraBold), Font(R.font.unbounded_black, FontWeight.Black),
)
val Sora = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal), Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold), Font(R.font.sora_bold, FontWeight.ExtraBold), Font(R.font.sora_bold, FontWeight.Black),
)
val SpaceMono = FontFamily(
    Font(R.font.spacemono_regular, FontWeight.Normal), Font(R.font.spacemono_bold, FontWeight.SemiBold),
    Font(R.font.spacemono_bold, FontWeight.Bold), Font(R.font.spacemono_bold, FontWeight.ExtraBold), Font(R.font.spacemono_bold, FontWeight.Black),
)
val ArchivoBlack = FontFamily(Font(R.font.archivoblack_regular, FontWeight.Normal), Font(R.font.archivoblack_regular, FontWeight.Bold),
    Font(R.font.archivoblack_regular, FontWeight.ExtraBold), Font(R.font.archivoblack_regular, FontWeight.Black))
val PlexSerif = FontFamily(Font(R.font.plexserif_regular, FontWeight.Normal), Font(R.font.plexserif_italic, FontWeight.Normal, FontStyle.Italic))

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
        "Noite", "Preto puro e âmbar, clássico e econômico",
        Color(0xFF000000), Color(0xFF121110), Color(0xFF26231E), Color(0xFFE6DFD3), Color(0xFFA39A8B),
        Color(0xFFE9A65A), Color(0xFF1A1206), Color(0xFF6FC2B4), Color(0xFFE88AA8), true,
        FontFamily.Serif, FontFamily.Default, FontFamily.Serif, 1f, false, false, false, EstiloFala.BARRA, "◆   ◆   ◆",
    )),
    PAPEL(Paleta(
        "Papel", "Claro como um livro: creme, tinta e vermelho",
        Color(0xFFF3EDE2), Color(0xFFEAE2D4), Color(0xFFDCD3C4), Color(0xFF1B1714), Color(0xFF6B6157),
        Color(0xFFB5372A), Color(0xFFFFF8EE), Color(0xFF1B1714), Color(0xFF2F6A5E), false,
        Newsreader, Inter, Newsreader, 0.7f, false, false, false, EstiloFala.FIO, "⁂",
    )),
    NEON(Paleta(
        "Neon", "Noite elétrica: violeta, ciano e rosa",
        Color(0xFF0B0820), Color(0xFF151034), Color(0xFF2A2350), Color(0xFFEDEBFF), Color(0xFFA9A3D9),
        Color(0xFF2EE6D6), Color(0xFF0B0820), Color(0xFF7C5CFF), Color(0xFFFF4FA3), true,
        Unbounded, Sora, Sora, 1.35f, false, false, true, EstiloFala.BALAO, "✦   ✦   ✦",
    )),
    TERMINAL(Paleta(
        "Terminal", "Cru e direto: grafite, limão e monoespaçado",
        Color(0xFF0E0F0C), Color(0xFF16180F), Color(0xFF2A2C24), Color(0xFFEDEFE6), Color(0xFF8A8E7E),
        Color(0xFFC8FF2E), Color(0xFF0E0F0C), Color(0xFFEDEFE6), Color(0xFFFF5C39), true,
        ArchivoBlack, SpaceMono, PlexSerif, 0f, true, true, false, EstiloFala.ETIQUETA, "// ─── nova cena ─── //",
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
    val pesoTitulo = if (tit == ArchivoBlack) FontWeight.Normal else FontWeight.Bold
    val tipografia = Typography(
        displaySmall = TextStyle(fontFamily = tit, fontWeight = pesoTitulo, fontSize = if (tit == Unbounded) 28.sp else 32.sp),
        headlineSmall = TextStyle(fontFamily = tit, fontWeight = pesoTitulo, fontSize = if (tit == Unbounded) 19.sp else 22.sp),
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
