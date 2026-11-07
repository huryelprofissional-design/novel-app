package com.huryel.novely.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Texto de cada capítulo = um arquivo próprio.
 * Gravação ATÔMICA: escreve num arquivo temporário, força no disco (fsync) e só então
 * troca pelo original. Se o celular desligar no meio, o original continua intacto.
 * A versão anterior fica como .bak para recuperação.
 */
class ArquivosTexto(ctx: Context) {
    private val raiz = File(ctx.filesDir, "textos").apply { mkdirs() }

    fun arquivo(capId: Long) = File(raiz, "$capId.md")

    suspend fun ler(capId: Long): String = withContext(Dispatchers.IO) {
        val f = arquivo(capId)
        when {
            f.exists() -> f.readText(Charsets.UTF_8)
            File(raiz, "$capId.md.bak").exists() -> File(raiz, "$capId.md.bak").readText(Charsets.UTF_8)
            else -> ""
        }
    }

    suspend fun gravar(capId: Long, texto: String) = withContext(Dispatchers.IO) {
        val destino = arquivo(capId)
        val tmp = File(raiz, "$capId.md.tmp")
        tmp.outputStream().use { out ->
            out.write(texto.toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        if (destino.exists()) {
            val bak = File(raiz, "$capId.md.bak")
            bak.delete()
            destino.renameTo(bak)
        }
        if (!tmp.renameTo(destino)) {
            tmp.copyTo(destino, overwrite = true)
            tmp.delete()
        }
    }

    // ── Histórico de versões: guarda uma cópia antes de sobrescrever, no máximo a cada 10 min ──
    private val pastaVersoes = File(raiz.parentFile, "versoes").apply { mkdirs() }
    private fun dirVersoes(capId: Long) = File(pastaVersoes, "$capId")

    /** nome vazio = cópia automática; com nome = versão que o autor (ou um conflito) guardou e que nunca é apagada sozinha. */
    data class Versao(val arquivo: File, val quando: Long, val palavras: Int, val inicio: String, val nome: String = "")
    private fun nomeDe(dir: File, quando: String) = File(dir, "$quando.nome").takeIf { it.exists() }?.readText(Charsets.UTF_8)?.trim().orEmpty()

    suspend fun guardarVersaoSePreciso(capId: Long, forcar: Boolean = false) = withContext(Dispatchers.IO) {
        val atual = arquivo(capId)
        if (!atual.exists() || atual.length() == 0L) return@withContext
        val dir = dirVersoes(capId).apply { mkdirs() }
        val ultima = dir.listFiles()?.maxOfOrNull { it.name.substringBefore('.').toLongOrNull() ?: 0L } ?: 0L
        val agora = System.currentTimeMillis()
        if (!forcar && agora - ultima < 10 * 60_000) return@withContext
        atual.copyTo(File(dir, "$agora.md"), overwrite = true)
        // só as automáticas entram no limite de 30; as com nome ficam
        dir.listFiles()?.filter { it.name.endsWith(".md") && !File(dir, it.name.removeSuffix(".md") + ".nome").exists() }
            ?.sortedByDescending { it.name }?.drop(30)?.forEach { it.delete() }
    }

    /** Guarda um texto qualquer como versão (ex.: a versão do outro aparelho num conflito, ou "Salvar versão com nome"). Devolve quando. */
    suspend fun gravarVersao(capId: Long, texto: String, nome: String = ""): Long = withContext(Dispatchers.IO) {
        val dir = dirVersoes(capId).apply { mkdirs() }
        val agora = System.currentTimeMillis()
        File(dir, "$agora.md").writeText(texto, Charsets.UTF_8)
        if (nome.isNotBlank()) File(dir, "$agora.nome").writeText(nome.trim().take(60), Charsets.UTF_8)
        agora
    }

    suspend fun versoes(capId: Long): List<Versao> = withContext(Dispatchers.IO) {
        val dir = dirVersoes(capId)
        (dir.listFiles() ?: emptyArray()).filter { it.name.endsWith(".md") }.sortedByDescending { it.name }.map { f ->
            val t = f.readText(Charsets.UTF_8)
            val q = f.name.substringBefore('.')
            Versao(f, q.toLongOrNull() ?: 0L, contarPalavras(t), t.take(220), nomeDe(dir, q))
        }
    }

    // ── Rascunho de emergência: gravado logo depois de digitar, apagado quando o salvamento normal termina ──
    private val pastaRascunhos = File(raiz.parentFile, "rascunhos").apply { mkdirs() }
    suspend fun gravarRascunho(capId: Long, texto: String) = withContext(Dispatchers.IO) { File(pastaRascunhos, "$capId.md").writeText(texto, Charsets.UTF_8) }
    suspend fun lerRascunho(capId: Long): Pair<Long, String>? = withContext(Dispatchers.IO) {
        File(pastaRascunhos, "$capId.md").takeIf { it.exists() }?.let { it.lastModified() to it.readText(Charsets.UTF_8) }
    }
    suspend fun apagarRascunho(capId: Long) = withContext(Dispatchers.IO) { File(pastaRascunhos, "$capId.md").delete(); Unit }

    suspend fun apagar(capId: Long) = withContext(Dispatchers.IO) {
        dirVersoes(capId).deleteRecursively()
        File(pastaRascunhos, "$capId.md").delete()
        arquivo(capId).delete()
        File(raiz, "$capId.md.bak").delete()
        File(raiz, "$capId.md.tmp").delete()
    }

    companion object {
        fun contarPalavras(texto: String): Int {
            var n = 0
            var dentro = false
            for (ch in texto) {
                val letra = ch.isLetterOrDigit()
                if (letra && !dentro) n++
                dentro = letra
            }
            return n
        }
    }
}

/** Imagens de capa/banner: copiadas para dentro do app e reduzidas (o original nunca é alterado). */
class ArquivosImagem(private val ctx: Context) {
    private val raiz = File(ctx.filesDir, "imagens").apply { mkdirs() }

    suspend fun importar(uri: Uri, ladoMax: Int = 1600): String? = withContext(Dispatchers.IO) {
        try {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
            if (limites.outWidth <= 0) return@withContext null
            var amostra = 1
            while (limites.outWidth / (amostra * 2) >= ladoMax || limites.outHeight / (amostra * 2) >= ladoMax) amostra *= 2
            val bmp = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = amostra })
            } ?: return@withContext null
            val f = File(raiz, UUID.randomUUID().toString() + ".jpg")
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            bmp.recycle()
            f.absolutePath
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /** Salva bytes de imagem vindos da nuvem e devolve o caminho. */
    fun salvarBytes(bytes: ByteArray): String {
        val f = File(raiz, UUID.randomUUID().toString() + ".jpg")
        f.writeBytes(bytes)
        return f.absolutePath
    }

    /** JPEG reduzido para enviar à nuvem (cabe folgado num documento). */
    fun comprimir(caminho: String?, ladoMax: Int, qualidade: Int = 72): ByteArray? {
        val f = caminho?.let { File(it) }?.takeIf { it.exists() } ?: return null
        return try {
            val lim = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(f.absolutePath, lim)
            var amostra = 1
            while (lim.outWidth / (amostra * 2) >= ladoMax || lim.outHeight / (amostra * 2) >= ladoMax) amostra *= 2
            var bmp = android.graphics.BitmapFactory.decodeFile(f.absolutePath, android.graphics.BitmapFactory.Options().apply { inSampleSize = amostra }) ?: return null
            val maior = maxOf(bmp.width, bmp.height)
            if (maior > ladoMax) bmp = android.graphics.Bitmap.createScaledBitmap(bmp, bmp.width * ladoMax / maior, bmp.height * ladoMax / maior, true)
            val out = java.io.ByteArrayOutputStream()
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, qualidade, out)
            out.toByteArray()
        } catch (e: Throwable) { null }
    }

    fun apagar(caminho: String?) {
        if (caminho != null && caminho.startsWith(raiz.absolutePath)) File(caminho).delete()
    }
}
