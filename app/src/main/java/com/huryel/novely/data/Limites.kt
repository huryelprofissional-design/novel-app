package com.huryel.novely.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Leitura com teto de tamanho para entradas de ZIP (backup .novely, EPUB, DOCX).
 * Um arquivo de poucos KB pode descompactar em GB ("zip bomb"); sem teto, o app morre sem memória ou enche o disco.
 */
object Limites {
    const val TEXTO_ZIP = 64L * 1024 * 1024   // manifest.json, document.xml, capítulo de EPUB
    const val IMAGEM_ZIP = 25L * 1024 * 1024  // capa, banner, foto de ficha

    fun lerAte(ins: InputStream, max: Long): ByteArray {
        val out = ByteArrayOutputStream()
        copiarAte(ins, out, max)
        return out.toByteArray()
    }

    fun copiarAte(ins: InputStream, out: OutputStream, max: Long): Long {
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val r = ins.read(buf); if (r <= 0) break
            total += r
            if (total > max) throw IllegalStateException("Arquivo grande demais (mais de ${max / (1024 * 1024)} MB descompactado). Pode estar corrompido.")
            out.write(buf, 0, r)
        }
        return total
    }
}
