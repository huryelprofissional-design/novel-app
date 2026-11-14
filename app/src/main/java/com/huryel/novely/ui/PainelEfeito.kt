package com.huryel.novely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huryel.novely.data.Marcas
import com.huryel.novely.ui.theme.Paleta

/** Efeitos que o painel oferece: em um trecho (dentro da frase) e no parágrafo inteiro. */
val EFEITOS_TRECHO = listOf(
    "\$corte" to "Corte 2.0", "\$corar" to "Corando 2.0", "\$aura" to "Aura 2.0", "\$cristal" to "Cristal 2.0", "\$assinatura" to "Assinatura 2.0",
    "fogo" to "Fogo", "gelo" to "Gelo", "neon" to "Neon", "raio" to "Raio", "sombra" to "Sombra", "divino" to "Divino",
    "feitico" to "Feitiço", "runa" to "Runas", "medo" to "Medo", "eco" to "Eco", "sangue" to "Sangue", "veneno" to "Veneno",
    "ouro" to "Ouro", "coracao" to "Coração", "quebra" to "Quebrando", "vento" to "Vento", "cor" to "Só a cor", "idioma" to "Outro idioma",
    "estrela" to "Brilho estelar", "fantasma" to "Fantasma", "sagrado" to "Luz sagrada", "cinzas" to "Cinzas", "mao" to "Letra à mão",
    "contagem" to "Contagem", "mente" to "Telepatia", "corrompido" to "Corrompido", "dano" to "Dano",
)
/** Efeitos de parágrafo: todos os do Mega Pack, por gênero (chave, nome, gênero, modelo de campos). */
data class FxParagrafo(val chave: String, val nome: String, val cat: String, val modelo: String)
val EFEITOS_PARAGRAFO: List<FxParagrafo> = MEGA_PACK.filter { it.third != "inline" }.mapNotNull { (nome, cod, cat) ->
    Regex("""^\[([#$])([a-z0-9-]+)(?::[a-z]+)?\s*([\s\S]*)]$""").find(cod)?.let { FxParagrafo((if (it.groupValues[1] == "$") "$" else "") + it.groupValues[2], nome.substringBefore(" ("), cat, it.groupValues[3]) }
}
val CAT_FX = linkedMapOf("rpg" to "RPG / SISTEMA", "murim" to "MURIM", "terror" to "TERROR", "misterio" to "MISTÉRIO", "fantasia" to "FANTASIA",
    "scifi" to "SCI-FI", "celular" to "CELULAR", "romance" to "ROMANCE", "acao" to "AÇÃO", "epoca" to "ÉPOCA", "estrutura" to "ESTRUTURA")
// estes levam o trecho inteiro; os outros vêm com o modelo de campos e o trecho no lugar do texto
private val FX_SIMPLES = setOf("\$juramento", "\$vitral", "\$recortes", "\$dobra", "\$repete", "sistema", "perigo", "conquista", "missao-ok", "missao-falhou", "aviso", "titulo", "alerta-vermelho", "terminal", "profecia", "diario", "telegrama", "ia", "proximo", "radio", "enquanto", "lembranca", "grimorio", "epigrafe")

/** O que vai dentro do [#efeito …]. */
fun conteudoBloco(efeito: String, trecho: String): String {
    val t = trecho.trim(); val modelo = EFEITOS_PARAGRAFO.firstOrNull { it.chave == efeito }?.modelo.orEmpty()
    if (efeito in FX_SIMPLES || modelo.isEmpty()) return t.ifEmpty { "Você subiu de nível" }
    if (t.isEmpty()) return modelo
    val cs = modelo.split(" | ").toMutableList()
    val i = cs.indexOfFirst { it.trim().lowercase() in setOf("texto", "corpo", "transcrição", "mensagem", "legenda") }.coerceAtLeast(0)
    cs[i] = t.replace(Regex("\n+"), " "); return cs.joinToString(" | ")
}
private val PISCAM = setOf("raio", "neon", "medo", "quebra", "vento", "coracao", "fantasma", "corrompido", "dano", "cinzas", "perigo", "alerta-vermelho", "terminal")

/** Aviso de leitura para a combinação escolhida (null = tudo certo). */
fun alertaLeitura(efeito: String, cor: String, forca: Int, bloco: Boolean, tamanho: Int): String? = when {
    forca > 0 && efeito in PISCAM -> "Forte e mexendo pode cansar a leitura. Quem liga “Reduzir animações” vê a versão parada."
    cor == "sombrio" && !bloco -> "Sombrio fica com pouco contraste no fundo escuro. Prefira roxo ou branco."
    !bloco && tamanho > 280 -> "Efeito num trecho longo fica pesado de ler. Para um parágrafo inteiro, use a aba Parágrafo."
    else -> null
}

/**
 * Painel de efeitos (Fase 3): escolhe o efeito, a cor e a intensidade com prévia ao vivo, desenhada pelo mesmo motor do leitor.
 * Aplicar devolve o texto com os símbolos (o efeito continua sendo texto: {fogo+:azul: …} ou [#sistema …]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PainelEfeito(trecho: String, p: Paleta, aoAplicar: (texto: String, bloco: Boolean) -> Unit, aoCancelar: () -> Unit, verSimbolos: () -> Unit) {
    val texto = trecho.trim().ifEmpty { "Você subiu de nível" }
    var bloco by remember { mutableStateOf(trecho.trim().length > 60 || trecho.isBlank()) }
    var efeito by remember { mutableStateOf(if (bloco) "sistema" else "fogo") }
    var cor by remember { mutableStateOf("roxo") }
    var forca by remember { mutableIntStateOf(0) }
    val conteudo = if (bloco) conteudoBloco(efeito, trecho) else texto
    val sintaxe = Marcas.escreverEfeito(efeito, cor, forca, conteudo, bloco)
    val alerta = alertaLeitura(efeito, cor, forca, bloco, texto.length)
    val suave = p.textoSuave

    Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 18.dp).padding(bottom = 18.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Efeito no trecho", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Text("“${texto.take(26)}${if (texto.length > 26) "…" else ""}”", fontSize = 12.sp, color = suave, maxLines = 1)
        }
        // prévia ao vivo (o mesmo desenho do leitor)
        Box(Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 120.dp, max = 230.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF050507))
            .border(1.dp, Color(0xFF26282F), RoundedCornerShape(18.dp)).padding(14.dp), contentAlignment = Alignment.Center) {
            Text("PRÉVIA AO VIVO", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Color(0xFF8B8D97), modifier = Modifier.align(Alignment.TopStart))
            val linha = if (bloco) sintaxe else "Ele sentiu: $sintaxe"
            val previa = remember(linha) { montarBlocos(linha).firstOrNull() }
            key(linha) {
                previa?.let { BlocoPrevia(it, TextStyle(fontSize = 17.sp, lineHeight = 27.sp, color = Color(0xFFD9DAE0), fontFamily = FontFamily.Serif), p, Modifier.padding(top = 16.dp)) }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Segmentado(listOf("No trecho", "Parágrafo"), if (bloco) 1 else 0) { i ->
                bloco = i == 1; efeito = if (bloco) "sistema" else EFEITOS_TRECHO.first().first
            }
            Rotulo("EFEITO", suave)
            @Composable fun Chip(k: String, nome: String) {
                val on = k == efeito
                Text(nome, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6E6EA),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (on) Color(0xFF23202F) else Color(0xFF18191E))
                        .border(if (on) 1.5.dp else 1.dp, if (on) corFx("roxo") else Color(0xFF26282F), RoundedCornerShape(14.dp))
                        .clickable(role = Role.RadioButton) { efeito = k }.semantics { selected = on }
                        .padding(horizontal = 14.dp, vertical = 12.dp))
            }
            if (bloco) CAT_FX.forEach { (cat, rotulo) ->
                val l = EFEITOS_PARAGRAFO.filter { it.cat == cat }
                if (l.isNotEmpty()) {
                    Text(rotulo, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = suave)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { l.forEach { Chip(it.chave, it.nome) } }
                }
            } else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EFEITOS_TRECHO.forEach { (k, nome) -> Chip(k, nome) }
            }
            Rotulo("COR", suave)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CORES_FX.forEach { (nome, c) ->
                    val on = nome == cor
                    Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.RadioButton) { cor = nome }.semantics { contentDescription = nome; selected = on },
                        contentAlignment = Alignment.Center) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(c).border(if (on) 3.dp else 0.dp, if (on) Color.White else Color.Transparent, CircleShape))
                    }
                }
            }
            Rotulo("INTENSIDADE", suave)
            Segmentado(listOf("Suave", "Normal", "Forte"), forca + 1) { forca = it - 1 }
            alerta?.let {
                Text("⚠  $it", fontSize = 12.sp, lineHeight = 17.sp, color = Color(0xFFFDE68A),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x1AFBBF24))
                        .border(1.dp, Color(0x59FBBF24), RoundedCornerShape(14.dp)).padding(12.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("No texto fica:  ", fontSize = 11.sp, color = suave)
                Text(Marcas.escreverEfeito(efeito, cor, forca, if (conteudo.length > 40) conteudo.take(40) + "…" else conteudo, bloco), fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    color = Color(0xFFC4B5FD), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1B21)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
            TextButton(onClick = verSimbolos, modifier = Modifier.align(Alignment.Start)) { Text("Ver todos os símbolos (cenas, cartas, chat…)", fontSize = 13.sp, color = p.destaque) }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).border(1.dp, Color(0xFF2F3139), RoundedCornerShape(25.dp))
                .clickable(role = Role.Button, onClick = aoCancelar), contentAlignment = Alignment.Center) { Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            Box(Modifier.weight(1.4f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Color.White)
                .clickable(role = Role.Button) { aoAplicar(Marcas.escreverEfeito(efeito, cor, forca, if (bloco) conteudoBloco(efeito, trecho) else if (trecho.isBlank()) "texto" else trecho, bloco), bloco) },
                contentAlignment = Alignment.Center) { Text("Aplicar", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF0B0B0E)) }
        }
    }
}

@Composable
private fun Rotulo(t: String, cor: Color) = Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = cor)

@Composable
private fun Segmentado(opcoes: List<String>, sel: Int, escolher: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFF1A1B21)).border(1.dp, Color(0xFF26282F), RoundedCornerShape(22.dp)).padding(3.dp)) {
        opcoes.forEachIndexed { i, o ->
            val on = i == sel
            Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (on) Color.White else Color.Transparent)
                .clickable(role = Role.Tab) { escolher(i) }.semantics { selected = on }, contentAlignment = Alignment.Center) {
                Text(o, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) Color(0xFF0B0B0E) else Color(0xFF9A9CA6))
            }
        }
    }
}
