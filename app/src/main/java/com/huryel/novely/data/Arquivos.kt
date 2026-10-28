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

    suspend fun apagar(capId: Long) = withContext(Dispatchers.IO) {
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

    fun apagar(caminho: String?) {
        if (caminho != null && caminho.startsWith(raiz.absolutePath)) File(caminho).delete()
    }
}
