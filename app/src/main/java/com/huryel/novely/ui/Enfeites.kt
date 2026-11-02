package com.huryel.novely.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.data.Capitulo
import com.huryel.novely.ui.theme.Paleta

/** Enfeites de troca de cena que o autor escolhe para a obra. */
val ENFEITES = linkedMapOf("LOSANGO" to "Losango", "ESTRELAS" to "Estrelas", "LOTUS" to "Flor de lótus", "ESPADAS" to "Espadas", "ONDA" to "Onda")

@Composable
fun SeparadorCena(enfeite: String, cor: Color) {
    val linha = { inverter: Boolean, largura: Int ->
        @Composable { Box(Modifier.width(largura.dp).height(1.dp).background(Brush.horizontalGradient(if (inverter) listOf(cor.copy(alpha = 0.75f), Color.Transparent) else listOf(Color.Transparent, cor.copy(alpha = 0.75f))))) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        when (enfeite) {
            "ESTRELAS" -> Text("✦      ✦      ✦", color = cor, fontSize = 12.sp)
            "LOTUS" -> { linha(false, 56)(); Text("  ❀  ", color = cor, fontSize = 18.sp); linha(true, 56)() }
            "ESPADAS" -> {
                Text("⚔", color = cor, fontSize = 15.sp)
                Canvas(Modifier.padding(horizontal = 8.dp).width(92.dp).height(2.dp)) {
                    drawLine(cor.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                        strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                }
                Text("⚔", color = cor, fontSize = 15.sp)
            }
            "ONDA" -> Canvas(Modifier.width(150.dp).height(14.dp)) {
                val p = Path(); val passo = size.width / 8; p.moveTo(0f, size.height / 2)
                for (i in 0 until 8) p.quadraticTo(passo * i + passo / 2, if (i % 2 == 0) 0f else size.height, passo * (i + 1), size.height / 2)
                drawPath(p, cor, style = Stroke(2.dp.toPx()))
            }
            else -> { linha(false, 56)(); Box(Modifier.padding(horizontal = 10.dp).size(8.dp).rotate(45f).background(cor)); linha(true, 56)() }
        }
    }
}

/** "Anteriormente…": o resumo do capítulo anterior, para quem volta depois de dias. */
@Composable
fun CartaoAnteriormente(anterior: Capitulo, p: Paleta) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(22.dp))
        .background(Brush.linearGradient(listOf(p.destaque.copy(alpha = 0.13f), Color.White.copy(alpha = 0.02f))))
        .border(1.dp, p.destaque.copy(alpha = 0.28f), RoundedCornerShape(22.dp)).padding(16.dp)) {
        Text("ANTERIORMENTE…", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = p.destaque)
        Text(anterior.resumo, fontFamily = p.fonteLeitura, fontSize = 15.sp, lineHeight = 22.sp, color = p.texto.copy(alpha = 0.85f), modifier = Modifier.padding(top = 8.dp))
        Text(anterior.titulo, fontSize = 11.sp, color = p.textoSuave, modifier = Modifier.padding(top = 8.dp))
    }
}
