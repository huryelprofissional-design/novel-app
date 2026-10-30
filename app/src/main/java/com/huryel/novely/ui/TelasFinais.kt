package com.huryel.novely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.ObraPublica
import com.huryel.novely.data.PerfilPublico
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.launch

// ───────────────────────── Bloqueio ─────────────────────────

@Composable
fun TelaBloqueio(desbloquear: () -> Unit) {
    val p = LocalPaleta.current
    LaunchedEffect(Unit) { desbloquear() }
    Column(Modifier.fillMaxSize().background(p.fundo).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🔒", fontSize = 48.sp)
        Text(buildAnnotatedString { append("Novely"); withStyle(SpanStyle(color = p.destaque)) { append(".") } }, fontWeight = FontWeight.Black, fontSize = 34.sp, modifier = Modifier.padding(top = 12.dp))
        Text("Protegido com a digital ou a senha do celular.", color = p.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = desbloquear, colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(top = 28.dp).fillMaxWidth().height(54.dp)) { Text("Desbloquear", fontWeight = FontWeight.ExtraBold) }
    }
}

// ───────────────────────── Tutorial da primeira vez ─────────────────────────

@Composable
fun Tutorial(fechar: () -> Unit) {
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    val paginas = listOf(
        Triple("◆", "Bem-vindo ao Novely", "Escreva, organize, leia e publique novels gigantes — com capítulos de 100 mil palavras sem travar."),
        Triple("▦", "Cinco abas", "Início: o que você está lendo.  Biblioteca: tudo que você lê.  Escrever: suas obras.  Descobrir: o catálogo.  Perfil: você e os ajustes (engrenagem)."),
        Triple("@", "Personagens no texto", "Escreva @Nome, descrição… e a ficha do personagem nasce sozinha. Para lugares use &Lugar, … No leitor as marcas somem e o nome fica destacado."),
        Triple("✦", "Publique", "Na sua obra: ⋮ › Publicar. Solte capítulo por capítulo ou agende. Leitores seguem, curtem, comentam e recebem aviso de capítulo novo."),
        Triple("☁", "Sempre salvo", "Funciona sem internet. Com a conta, tudo vai para a nuvem e aparece em qualquer celular (e no PC). Segure o dedo num parágrafo para grifar e anotar."),
    )
    val estado = rememberPagerState { paginas.size }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1C1424), p.fundo)))) {
        HorizontalPager(estado, Modifier.fillMaxSize()) { i ->
            val (simb, titulo, texto) = paginas[i]
            Column(Modifier.fillMaxSize().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(110.dp).clip(CircleShape).background(p.destaque.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Text(simb, fontSize = 48.sp, color = p.destaque, fontWeight = FontWeight.Black)
                }
                Text(titulo, fontWeight = FontWeight.Black, fontSize = 26.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 28.dp))
                Text(texto, fontSize = 16.sp, lineHeight = 24.sp, color = p.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(paginas.size) { i -> Box(Modifier.size(if (i == estado.currentPage) 22.dp else 8.dp, 8.dp).clip(CircleShape).background(if (i == estado.currentPage) p.destaque else p.borda)) }
            }
            Button(onClick = { if (estado.currentPage < paginas.lastIndex) escopo.launch { estado.animateScrollToPage(estado.currentPage + 1) } else fechar() },
                colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(54.dp)) {
                Text(if (estado.currentPage < paginas.lastIndex) "Próximo" else "Começar", fontWeight = FontWeight.ExtraBold)
            }
            TextButton(onClick = fechar) { Text("Pular", color = p.textoSuave) }
        }
    }
}

// ───────────────────────── Painel de dono ─────────────────────────

const val EMAIL_DONO = "huryelprofissional@gmail.com"

@Composable
fun TelaDono(nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val escopo = rememberCoroutineScope()
    var aba by remember { mutableStateOf("OBRAS") }
    var obras by remember { mutableStateOf<List<ObraPublica>?>(null) }
    var pessoas by remember { mutableStateOf<List<PerfilPublico>?>(null) }
    var remover by remember { mutableStateOf<ObraPublica?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    LaunchedEffect(recarga) {
        try { obras = app.pub.catalogo("NOVIDADES"); pessoas = app.pub.perfis() } catch (e: Exception) { msg = e.message }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item { Topo("Painel de dono", nav) }
        item {
            Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Obras · ${obras?.size ?: "…"}", aba == "OBRAS") { aba = "OBRAS" }
                Chip("Pessoas · ${pessoas?.size ?: "…"}", aba == "PESSOAS") { aba = "PESSOAS" }
            }
            msg?.let { Text(it, color = Color(0xFFE57373), fontSize = 13.sp, modifier = Modifier.padding(18.dp)) }
        }
        if (aba == "OBRAS") items(obras.orEmpty(), key = { it.id }) { o ->
            Row(Modifier.fillMaxWidth().clickable { nav.navigate("pub/${o.id}") }.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CapaPublica(o, Modifier.width(44.dp).height(62.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(o.titulo, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("por ${o.autorNome} · ${o.numCaps} caps · ${o.leituras} leituras" + if (o.classificacao != "LIVRE") " · +${o.classificacao}" else "", fontSize = 12.sp, color = p.textoSuave)
                }
                TextButton(onClick = { remover = o }) { Text("Remover", color = Color(0xFFE57373)) }
            }
        } else items(pessoas.orEmpty(), key = { it.uid }) { u ->
            Row(Modifier.fillMaxWidth().clickable { nav.navigate("autor/${u.uid}") }.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(p.superficie), contentAlignment = Alignment.Center) { Text(u.nome.take(1).uppercase(), fontWeight = FontWeight.Black) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(u.nome.ifBlank { "Sem nome" }, fontWeight = FontWeight.Bold)
                    Text("${u.seguidores} seguidores" + if (u.bio.isNotBlank()) " · ${u.bio}" else "", fontSize = 12.sp, color = p.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    remover?.let { o ->
        DialogoConfirmar("Remover “${o.titulo}” do catálogo?", "Some do Descobrir para todo mundo, com capítulos e comentários. A cópia do autor no aparelho dele não é apagada.", "Remover", { remover = null }) {
            remover = null
            escopo.launch { msg = app.pub.removerComoDono(o.id); if (msg == null) recarga++ }
        }
    }
}
