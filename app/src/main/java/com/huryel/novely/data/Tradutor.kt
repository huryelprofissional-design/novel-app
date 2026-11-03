package com.huryel.novely.data

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Tradução no próprio celular (ML Kit do Google): grátis, sem conta, funciona offline depois de baixar
 * o pacote do idioma (~30 MB, uma vez). Traduz linha por linha, mantendo falas, cenas e títulos no lugar.
 */
object Tradutor {
    val IDIOMAS = listOf(
        "pt" to "Português", "en" to "Inglês", "es" to "Espanhol", "fr" to "Francês", "it" to "Italiano", "de" to "Alemão",
        "ja" to "Japonês", "ko" to "Coreano", "zh" to "Chinês", "ru" to "Russo",
    )
    fun nome(cod: String) = IDIOMAS.firstOrNull { it.first == cod }?.second ?: cod

    /** Idioma do texto (pt, en, ko…) pelos primeiros parágrafos; "pt" se não der para saber. */
    suspend fun detectar(texto: String): String = try {
        val amostra = Marcas.limpar(texto).lines().filter { it.length > 40 }.take(6).joinToString(" ").take(1200)
        LanguageIdentification.getClient().identifyLanguage(amostra).await().takeIf { it != "und" }?.substringBefore('-') ?: "pt"
    } catch (_: Exception) { "pt" }

    private val SEPARADOR = Regex("^(\\*\\s*){3,}$|^[◆◇~\\-—=_]{3,}$")

    /** Traduz o capítulo. [progresso] recebe (feitas, total). Usa o que já foi traduzido antes (guardado no aparelho). */
    suspend fun traduzir(ctx: android.content.Context, chave: String, texto: String, para: String, progresso: (Int, Int) -> Unit): String = withContext(Dispatchers.IO) {
        val pasta = File(ctx.filesDir, "traducoes").apply { mkdirs() }
        val arq = File(pasta, "$chave-$para-${texto.hashCode().toUInt()}.txt")
        if (arq.exists()) return@withContext arq.readText()
        val de = detectar(texto)
        if (de == para) return@withContext texto
        val origem = TranslateLanguage.fromLanguageTag(de) ?: TranslateLanguage.PORTUGUESE
        val destino = TranslateLanguage.fromLanguageTag(para) ?: error("Idioma não suportado")
        val t = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(origem).setTargetLanguage(destino).build())
        try {
            progresso(0, -1) // baixando o pacote do idioma
            t.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            val linhas = Marcas.limpar(texto).split('\n')
            val total = linhas.count { it.isNotBlank() }
            var feitas = 0
            val saida = linhas.map { linha ->
                val l = linha.trim()
                if (l.isEmpty() || SEPARADOR.matches(l)) return@map linha
                feitas++; if (feitas % 4 == 0) progresso(feitas, total)
                val prefixo = Regex("^(#+\\s*|[—–-]\\s*)").find(l)?.value ?: ""
                val corpo = l.removePrefix(prefixo).replace("**", "").replace(Regex("(?<!\\*)\\*(?!\\s)([^*\\n]+?)\\*"), "$1")
                if (corpo.isBlank()) return@map linha
                val trad = try { t.translate(corpo).await() } catch (_: Exception) { corpo }
                // falas: mantém o travessão e os travessões de narração intercalada
                prefixo + trad.replace(Regex("\\s+[-–]\\s+"), " — ")
            }.joinToString("\n")
            arq.writeText(saida)
            saida
        } finally { t.close() }
    }

    fun apagarCache(ctx: android.content.Context) { File(ctx.filesDir, "traducoes").deleteRecursively() }
}
