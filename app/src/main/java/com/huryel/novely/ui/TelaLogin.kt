package com.huryel.novely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.NovelyApp
import com.huryel.novely.ui.theme.LocalPaleta
import kotlinx.coroutines.launch

/** Entrada obrigatória: Google ou e-mail. Fundo com capas inclinadas, como no mockup. */
@Composable
fun TelaLogin() {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var modo by remember { mutableStateOf("INICIO") } // INICIO | ENTRAR | CRIAR | ESQUECI
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val capas by remember { app.repo.resumos() }.collectAsState(initial = emptyList())

    fun rodar(acao: suspend () -> String?) {
        ocupado = true; erro = null; info = null
        escopo.launch { erro = acao(); ocupado = false }
    }

    Box(Modifier.fillMaxSize().background(p.fundo)) {
        // mosaico de capas (as suas, ou artes geradas)
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.62f).graphicsLayer { rotationZ = -8f; scaleX = 1.25f; scaleY = 1.25f; alpha = 0.55f }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { linha ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(4) { col ->
                            val i = linha * 4 + col
                            val o = capas.getOrNull(i % capas.size.coerceAtLeast(1))
                            val titulo = o?.titulo ?: listOf("Torre", "Lâmina", "Vazio", "Céu", "Sal", "Rosa", "Lobo", "Mar")[i % 8]
                            CapaObra(titulo, o?.capa, o?.capaX ?: .5f, o?.capaY ?: .5f, o?.capaZoom ?: 1f,
                                o?.corBase ?: listOf(0xFF3B2A4A, 0xFF4A2420, 0xFF14302E, 0xFF1C2440)[i % 4], Modifier.width(118.dp).height(170.dp), 0.sp)
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.35f to p.fundo.copy(alpha = 0.4f), 0.62f to p.fundo)))

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Spacer(Modifier.height(260.dp))
            Text(buildAnnotatedString { append("Novely"); withStyle(SpanStyle(color = p.destaque)) { append(".") } },
                fontWeight = FontWeight.Black, fontSize = 42.sp, letterSpacing = (-1).sp)
            Text("Escreva, publique e leia novels gigantes — no celular e no PC.", fontSize = 16.sp, color = p.textoSuave, lineHeight = 22.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 26.dp))

            val campos = TextFieldDefaults.colors(focusedContainerColor = p.superficie, unfocusedContainerColor = p.superficie,
                focusedIndicatorColor = p.destaque, cursorColor = p.destaque, focusedLabelColor = p.destaque)
            when (modo) {
                "INICIO" -> {
                    Button(onClick = { rodar { app.conta.entrarGoogle(ctx) } }, enabled = !ocupado, shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(Color.White, Color(0xFF111111)), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("Continuar com Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    OutlinedButton(onClick = { modo = "ENTRAR"; erro = null }, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(top = 12.dp).fillMaxWidth().height(54.dp)) {
                        Text("Entrar com e-mail", color = p.texto, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { modo = "CRIAR"; erro = null }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(buildAnnotatedString { append("Não tem conta? "); withStyle(SpanStyle(color = p.destaque, fontWeight = FontWeight.Bold)) { append("Criar conta") } },
                            color = p.textoSuave)
                    }
                }
                else -> {
                    Text(when (modo) { "CRIAR" -> "Criar conta"; "ESQUECI" -> "Recuperar senha"; else -> "Entrar com e-mail" },
                        fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp))
                    if (modo == "CRIAR") TextField(nome, { nome = it }, label = { Text("Nome de autor") }, singleLine = true, colors = campos,
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                    TextField(email, { email = it }, label = { Text("E-mail") }, singleLine = true, colors = campos, shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                    if (modo != "ESQUECI") TextField(senha, { senha = it }, label = { Text("Senha (mínimo 6)") }, singleLine = true, colors = campos,
                        shape = RoundedCornerShape(14.dp), visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    Button(
                        onClick = {
                            when (modo) {
                                "CRIAR" -> rodar {
                                    val r = app.conta.criarConta(nome, email, senha)
                                    if (r == null && nome.isNotBlank()) { app.prefs.autorNome.definir(nome.trim()); app.prefs.perfilAlteradoEm.definir(System.currentTimeMillis()) }
                                    r
                                }
                                "ESQUECI" -> rodar { app.conta.recuperarSenha(email).also { if (it == null) info = "Mandamos um link para ${email.trim()}. Abra o e-mail, crie a senha nova e volte aqui." } }
                                else -> rodar { app.conta.entrarEmail(email, senha) }
                            }
                        },
                        enabled = !ocupado && email.contains('@') && (modo == "ESQUECI" || senha.length >= 6),
                        colors = ButtonDefaults.buttonColors(p.destaque, p.sobreDestaque), shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.padding(top = 14.dp).fillMaxWidth().height(54.dp),
                    ) { Text(when (modo) { "CRIAR" -> "Criar conta"; "ESQUECI" -> "Enviar link"; else -> "Entrar" }, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { modo = "INICIO"; erro = null; info = null }) { Text("‹ Voltar", color = p.textoSuave) }
                        if (modo == "ENTRAR") TextButton(onClick = { modo = "ESQUECI"; erro = null }) { Text("Esqueci a senha", color = p.textoSuave) }
                        if (modo == "CRIAR") TextButton(onClick = { modo = "ENTRAR"; erro = null }) { Text("Já tenho conta", color = p.destaque) }
                    }
                }
            }
            if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(2.dp)), color = p.destaque, trackColor = p.borda)
            erro?.let { Text(it, color = Color(0xFFE57373), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) }
            info?.let { Text(it, color = p.destaque2, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) }
            Text("Suas obras ficam guardadas na sua conta. As que já estão neste aparelho sobem para a nuvem depois de entrar.",
                fontSize = 12.sp, color = p.textoSuave.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 28.dp))
        }
    }
}
