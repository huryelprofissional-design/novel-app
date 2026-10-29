package com.huryel.novely.ui

import com.huryel.novely.ui.theme.forma
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.data.StatusObra
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Serifa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Imagens já abertas ficam na memória (até ~1/8 da RAM do app): trocar de tela não decodifica de novo. */
private object CacheImagens {
    val lru = object : android.util.LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap) = (value.width * value.height * 4 / 1024).coerceAtLeast(1)
    }
}

/** Carrega uma imagem do armazenamento do app fora da thread principal, já reduzida (com cache). */
@Composable
fun rememberImagem(caminho: String?, ladoMax: Int = 900): ImageBitmap? {
    val chave = caminho?.let { "$it|$ladoMax" }
    val estado by produceState<ImageBitmap?>(chave?.let { CacheImagens.lru.get(it) }, chave) {
        if (caminho == null || chave == null) { value = null; return@produceState }
        CacheImagens.lru.get(chave)?.let { value = it; return@produceState }
        value = withContext(Dispatchers.IO) {
            try {
                val lim = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(caminho, lim)
                var amostra = 1
                while (lim.outWidth / (amostra * 2) >= ladoMax || lim.outHeight / (amostra * 2) >= ladoMax) amostra *= 2
                BitmapFactory.decodeFile(caminho, BitmapFactory.Options().apply { inSampleSize = amostra })?.asImageBitmap()
                    ?.also { it.prepareToDraw(); CacheImagens.lru.put(chave, it) }
            } catch (e: Throwable) { null }
        }
    }
    return estado
}

/**
 * Mostra a imagem recortada no enquadramento escolhido:
 * x/y (0..1) = ponto de foco; zoom >= 1 aproxima em volta desse ponto.
 */
@Composable
fun ImagemEnquadrada(
    caminho: String?, x: Float, y: Float, zoom: Float, corBase: Long,
    modifier: Modifier, ladoMax: Int = 900, semImagem: @Composable BoxScope.() -> Unit = {},
) {
    val img = rememberImagem(caminho, ladoMax)
    Box(modifier.background(Color(corBase))) {
        if (img != null) {
            Image(
                bitmap = img, contentDescription = null, contentScale = ContentScale.Crop,
                alignment = BiasAlignment(x * 2 - 1, y * 2 - 1),
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom; scaleY = zoom
                    transformOrigin = TransformOrigin(x, y)
                },
            )
        } else semImagem()
    }
}

@Composable
fun CapaObra(
    titulo: String, caminho: String?, x: Float, y: Float, zoom: Float, corBase: Long,
    modifier: Modifier, tamanhoTitulo: TextUnit = 14.sp,
) {
    ImagemEnquadrada(caminho, x, y, zoom, corBase, modifier.clip(forma(12.dp)), 600) {
        ArteCapa(titulo, corBase)
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomStart) {
            Text(titulo, fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = tamanhoTitulo,
                lineHeight = tamanhoTitulo * 1.15f, color = Color(0xFFF5EEE2), maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Capa gerada (quando não há imagem): formas geométricas únicas para cada título, nas cores da obra. */
@Composable
fun ArteCapa(titulo: String, corBase: Long) {
    val semente = remember(titulo) { titulo.hashCode() }
    val base = Color(corBase)
    Canvas(Modifier.fillMaxSize()) {
        val r = java.util.Random(semente.toLong())
        val w = size.width; val h = size.height
        val claro = lerp(base, Color.White, 0.18f)
        val ambar = Color(0xFFE9A65A)
        drawCircle(claro, radius = w * (0.55f + r.nextFloat() * 0.3f), center = Offset(w * r.nextFloat(), h * 0.15f * r.nextFloat()))
        drawCircle(lerp(base, Color.Black, 0.25f), radius = w * 0.45f, center = Offset(w * (0.2f + r.nextFloat() * 0.8f), h * (0.45f + r.nextFloat() * 0.3f)))
        when (r.nextInt(3)) {
            0 -> drawCircle(ambar, radius = w * 0.13f, center = Offset(w * (0.25f + r.nextFloat() * 0.5f), h * (0.18f + r.nextFloat() * 0.2f)))
            1 -> drawLine(ambar, Offset(w * 0.12f, h * 0.3f), Offset(w * 0.88f, h * 0.3f), strokeWidth = 3f)
            else -> drawCircle(ambar, radius = w * 0.2f, center = Offset(w * 0.7f, h * 0.28f), style = Stroke(width = 3f))
        }
        drawRect(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xAA000000)))
    }
}

fun nomeStatus(s: String) = when (s) {
    StatusObra.LENDO -> "Lendo"
    StatusObra.CONCLUIDA -> "Concluída"
    StatusObra.QUERO -> "Quero ler"
    else -> "Escrevendo"
}

fun formatarPalavras(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1f mi", n / 1_000_000.0).replace('.', ',')
    n >= 1_000 -> "${n / 1000} mil"
    else -> "$n"
}

@Composable
fun DialogoTexto(
    titulo: String, rotulo: String, inicial: String = "", confirmar: String = "Salvar",
    multilinha: Boolean = false, aoFechar: () -> Unit, aoConfirmar: (String) -> Unit,
) {
    var texto by remember { mutableStateOf(inicial) }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = texto, onValueChange = { texto = it }, label = { Text(rotulo) },
                singleLine = !multilinha, minLines = if (multilinha) 3 else 1,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { aoConfirmar(texto) }) { Text(confirmar) } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

@Composable
fun DialogoConfirmar(titulo: String, texto: String, confirmar: String, aoFechar: () -> Unit, aoConfirmar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = aoConfirmar) { Text(confirmar, color = Color(0xFFE57373)) } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

@Composable
fun Chip(texto: String, selecionado: Boolean, aoTocar: () -> Unit) {
    val p = LocalPaleta.current
    FilterChip(
        selected = selecionado, onClick = aoTocar, label = { Text(texto, fontWeight = FontWeight.SemiBold) },
        shape = forma(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = p.destaque, selectedLabelColor = p.sobreDestaque,
            containerColor = Color.Transparent, labelColor = p.texto,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selecionado, borderColor = p.borda),
        modifier = Modifier.height(40.dp),
    )
}
