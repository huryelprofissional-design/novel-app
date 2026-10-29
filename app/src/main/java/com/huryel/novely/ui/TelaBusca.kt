package com.huryel.novely.ui

import com.huryel.novely.ui.theme.forma
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huryel.novely.NovelyApp
import com.huryel.novely.data.Buscador
import com.huryel.novely.data.ProgressoBusca
import com.huryel.novely.ui.theme.LocalPaleta
import com.huryel.novely.ui.theme.Serifa
import kotlinx.coroutines.delay

@Composable
fun TelaBusca(nav: NavHostController) {
    val app = NovelyApp.instancia
    val p = LocalPaleta.current
    val buscador = remember { Buscador(app.repo) }
    val obras by remember { app.repo.resumos() }.collectAsState(initial = emptyList())
    var consulta by rememberSaveable { mutableStateOf("") }
    var obraFiltro by rememberSaveable { mutableStateOf<Long?>(null) }
    var soTitulos by rememberSaveable { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<ProgressoBusca?>(null) }

    LaunchedEffect(consulta, obraFiltro, soTitulos) {
        resultado = null
        if (consulta.trim().length < 2) return@LaunchedEffect
        delay(300) // espera você parar de digitar
        buscador.buscar(consulta, obraFiltro, soTitulos).collect { resultado = it }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Buscar", style = MaterialTheme.typography.displaySmall)
            OutlinedTextField(
                value = consulta, onValueChange = { consulta = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = p.destaque) },
                trailingIcon = { if (consulta.isNotEmpty()) IconButton(onClick = { consulta = "" }) { Icon(Icons.Filled.Close, "Limpar") } },
                placeholder = { Text("Palavra, frase, nome de personagem…") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = forma(18.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.destaque, unfocusedBorderColor = p.borda),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Todas as obras", obraFiltro == null) { obraFiltro = null }
                obras.take(12).forEach { o -> Chip(o.titulo.take(22), obraFiltro == o.id) { obraFiltro = o.id } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("No texto", !soTitulos) { soTitulos = false }
                Chip("Só títulos", soTitulos) { soTitulos = true }
            }
            val r = resultado
            if (r != null) {
                val obrasDistintas = r.achados.map { it.item.obraId }.distinct().size
                Text(
                    if (r.terminou) "${r.achados.size}${if (r.achados.size >= 400) "+" else ""} resultados em $obrasDistintas obra(s)"
                    else "Procurando… ${r.lidos} de ${r.total} capítulos · ${r.achados.size} até agora",
                    fontSize = 13.sp, color = p.textoSuave,
                )
                if (!r.terminou && r.total > 0) LinearProgressIndicator(progress = { r.lidos.toFloat() / r.total }, modifier = Modifier.fillMaxWidth(),
                    color = p.destaque, trackColor = p.borda, drawStopIndicator = {})
            }
        }
        val r = resultado
        if (consulta.trim().length < 2) {
            Text("Digite ao menos 2 letras. A busca ignora acentos e maiúsculas: “coracao” encontra “Coração”.",
                color = p.textoSuave, fontSize = 14.sp, modifier = Modifier.padding(20.dp))
        } else if (r != null && r.terminou && r.achados.isEmpty()) {
            Text("Nada encontrado.", color = p.textoSuave, modifier = Modifier.padding(20.dp))
        } else if (r != null) {
            val agrupado = r.achados.groupBy { it.item.obraId }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                agrupado.forEach { (_, lista) ->
                    item(key = "o${lista.first().item.obraId}") {
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(lista.first().item.obraTitulo, fontFamily = Serifa, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${lista.size}", color = p.textoSuave, fontSize = 13.sp)
                        }
                    }
                    items(lista, key = { "a${it.item.capId}-${it.ocorrencia}-${it.noTitulo}" }) { a ->
                        val trecho = buildAnnotatedString {
                            append(a.trecho)
                            val ini = a.inicio.coerceIn(0, a.trecho.length)
                            val fim = (a.inicio + a.tamanho).coerceIn(ini, a.trecho.length)
                            addStyle(SpanStyle(background = p.destaque.copy(alpha = 0.4f), color = p.texto, fontWeight = FontWeight.Bold), ini, fim)
                        }
                        Column(
                            Modifier.fillMaxWidth().clip(forma(16.dp)).background(p.superficie)
                                .clickable {
                                    if (a.noTitulo) nav.navigate("leitor/${a.item.capId}")
                                    else nav.navigate("leitor/${a.item.capId}?q=${Uri.encode(consulta.trim())}&n=${a.ocorrencia}")
                                }.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(a.item.capTitulo + if (a.noTitulo) " · título" else "", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.destaque,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!a.noTitulo) Text(trecho, fontFamily = Serifa, fontSize = 15.sp, lineHeight = 22.sp, color = p.textoSuave)
                        }
                    }
                }
            }
        }
    }
}
