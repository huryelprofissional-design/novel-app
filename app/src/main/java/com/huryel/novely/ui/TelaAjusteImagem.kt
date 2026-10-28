package com.huryel.novely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Capitulo
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Serifa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ───────────────────────── Ajuste de capa / banner ─────────────────────────

@Composable
fun TelaAjusteImagem(obraId: Long, tipo: String, nav: NavHostController) {
    val repo = NovelyApp.instancia.repo
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val obra by remember(obraId) { repo.obra(obraId) }.collectAsState(initial = null)
    val o = obra ?: return
    val banner = tipo == "banner"
    var x by remember(o.id) { mutableFloatStateOf(if (banner) o.bannerX else o.capaX) }
    var y by remember(o.id) { mutableFloatStateOf(if (banner) o.bannerY else o.capaY) }
    var zoom by remember(o.id) { mutableFloatStateOf(if (banner) o.bannerZoom else o.capaZoom) }
    var tamanho by remember { mutableStateOf(IntSize(1, 1)) }

    Column(Modifier.fillMaxSize()) {
        Topo(if (banner) "Ajustar banner" else "Ajustar capa", nav)
        Text("Arraste para mover · faça pinça (ou use a barra) para aproximar.",
            color = p.textoSuave, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val moldura = if (banner) Modifier.fillMaxWidth().aspectRatio(1.86f) else Modifier.fillMaxHeight(0.9f).aspectRatio(2f / 3f)
            ImagemEnquadrada(
                if (banner) o.banner else o.capa, x, y, zoom, o.corBase,
                moldura.clip(RoundedCornerShape(if (banner) 0.dp else 14.dp))
                    .onSizeChanged { tamanho = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestoZoom, _ ->
                            zoom = (zoom * gestoZoom).coerceIn(1f, 4f)
                            x = (x - pan.x / tamanho.width * 1.4f / zoom).coerceIn(0f, 1f)
                            y = (y - pan.y / tamanho.height * 1.4f / zoom).coerceIn(0f, 1f)
                        }
                    },
                ladoMax = 1600,
            )
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ZOOM", style = MaterialTheme.typography.labelSmall, color = p.textoSuave)
            Slider(zoom, { zoom = it }, valueRange = 1f..4f,
                colors = SliderDefaults.colors(thumbColor = p.destaque, activeTrackColor = p.destaque, inactiveTrackColor = p.borda))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { x = 0.5f; y = 0.5f; zoom = 1f }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Centralizar") }
                Button(
                    onClick = { escopo.launch { repo.ajustarImagem(obraId, tipo, x, y, zoom); nav.popBackStack() } },
                    colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Salvar", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

