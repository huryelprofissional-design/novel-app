package com.huryel.novely.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.CapPublico
import com.huryel.novely.data.ObraPublica
import com.huryel.novely.ui.theme.Serifa
import kotlinx.coroutines.launch

/** Stories de capítulo novo: um por obra atualizada nos últimos dias, começando pelo autor tocado. */
@Composable
fun TelaStories(uidInicio: String, nav: NavHostController) {
    val app = NovelyApp.instancia
    val lista = remember(uidInicio) {
        val cat = ((CacheCatalogo.ja("NOVIDADES") ?: emptyList()) + (CacheCatalogo.ja("ALTA") ?: emptyList())).distinctBy { it.id }
        cat.filter { System.currentTimeMillis() - it.atualizadaEm < 3 * 86_400_000L }.sortedWith(compareByDescending<ObraPublica> { it.autorUid == uidInicio }.thenByDescending { it.atualizadaEm })
    }
    if (lista.isEmpty()) { LaunchedEffect(Unit) { nav.popBackStack() }; return }
    var i by remember { mutableIntStateOf(0) }
    val o = lista[i.coerceIn(0, lista.lastIndex)]
    val tempo = remember(i) { Animatable(0f) }
    var pausado by remember { mutableStateOf(false) }
    var abrindo by remember { mutableStateOf<String?>(null) }
    val escopo = rememberCoroutineScope()
    fun proximo() { if (i < lista.lastIndex) i++ else nav.popBackStack() }
    LaunchedEffect(i, pausado) { if (!pausado) { tempo.animateTo(1f, tween(((1f - tempo.value) * 7000).toInt(), easing = LinearEasing)); proximo() } }
    val capa by produceState<ByteArray?>(null, o.id) { value = CachePoster.capa(o.id) }
    val dados by produceState<Pair<CapPublico, String>?>(null, o.id) { value = app.pub.ultimoComTrecho(o.id) }
    val img = imagemDeBytes(capa ?: o.capaMini)
    val cor = rememberCorBytes(capa ?: o.capaMini) ?: Color(0xFFFF6A2B)

    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(i) {
        detectTapGestures(
            onPress = { pausado = true; tryAwaitRelease(); pausado = false },
            onTap = { pos -> if (pos.x < size.width * 0.3f) { if (i > 0) i-- } else proximo() },
        )
    }) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.35f; scaleY = 1.35f; alpha = 0.85f }.blur(30.dp))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.92f)))))
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 12.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                lista.indices.forEach { k ->
                    Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxWidth(if (k < i) 1f else if (k == i) tempo.value else 0f).fillMaxHeight().background(Color.White))
                    }
                }
            }
            Row(Modifier.padding(top = 12.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarAutor(o.autorUid, o.autorNome, 36.dp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(o.autorNome, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("publicou há ${haQuanto(dados?.first?.publicadoEm ?: o.atualizadaEm)}", fontSize = 11.sp, color = Color(0xFFDDDDDD))
                }
                Text("✕", fontSize = 22.sp, color = Color.White, modifier = Modifier.clickable { nav.popBackStack() }.padding(10.dp))
            }
            Spacer(Modifier.weight(0.3f))
            Box(Modifier.align(Alignment.CenterHorizontally).rotate(-3f).shadow(30.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).size(190.dp, 274.dp).background(cor.copy(alpha = 0.4f))) {
                if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.weight(0.25f))
            Column(Modifier.padding(horizontal = 14.dp)) {
                val c = dados?.first
                Text(if (c != null) "NOVO CAPÍTULO" else o.titulo.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color.Black,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(cor).padding(horizontal = 9.dp, vertical = 4.dp))
                Text(c?.titulo ?: o.titulo, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 30.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp))
                if (c != null) Text(o.titulo, fontSize = 13.sp, color = Color(0xFFCCCCCC))
                val trecho = dados?.second.orEmpty()
                if (trecho.isNotEmpty()) Row(Modifier.padding(top = 14.dp)) {
                    Box(Modifier.width(3.dp).heightIn(min = 40.dp).background(cor))
                    Text("“$trecho”", fontFamily = Serifa, fontStyle = FontStyle.Italic, fontSize = 18.sp, lineHeight = 27.sp, color = Color(0xFFF2F2F2),
                        maxLines = 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp))
                }
                abrindo?.let { Text(it, fontSize = 12.sp, color = cor, modifier = Modifier.padding(top = 8.dp)) }
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).navigationBarsPadding().padding(bottom = 18.dp).height(58.dp).clip(RoundedCornerShape(29.dp)).background(Color.White)
                .clickable {
                    val c = dados?.first ?: run { nav.navigate("pub/${o.id}"); return@clickable }
                    pausado = true
                    escopo.launch {
                        try {
                            val obraId = app.pub.baixar(o.id) { abrindo = it }
                            val local = app.repo.dao.capituloPublico(obraId, c.id)
                            abrindo = null
                            if (local != null) nav.navigate("leitor/${local.id}") else nav.navigate("pub/${o.id}")
                        } catch (e: Exception) { abrindo = "Sem conexão"; pausado = false }
                    }
                }, contentAlignment = Alignment.Center) {
                Text("Ler agora ↑", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}
