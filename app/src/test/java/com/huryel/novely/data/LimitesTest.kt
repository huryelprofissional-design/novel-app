package com.huryel.novely.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LimitesTest {
    @Test fun zipBombaParaNoTeto() {
        // 80 MB de zeros comprimem para ~80 KB: o clássico zip bomb
        val bomba = java.io.ByteArrayOutputStream().also { b ->
            ZipOutputStream(b).use { z -> z.putNextEntry(ZipEntry("word/document.xml")); val zero = ByteArray(1024 * 1024); repeat(80) { z.write(zero) }; z.closeEntry() }
        }.toByteArray()
        assertTrue("zip pequeno", bomba.size < 1024 * 1024)
        val erro = runCatching { ZipInputStream(ByteArrayInputStream(bomba)).use { z -> z.nextEntry; Limites.lerAte(z, Limites.TEXTO_ZIP) } }.exceptionOrNull()
        assertTrue(erro is IllegalStateException)
    }
    @Test fun arquivoNormalPassa() {
        assertEquals(5, Limites.lerAte(ByteArrayInputStream("olá!".toByteArray()), 100).size)
    }
}
