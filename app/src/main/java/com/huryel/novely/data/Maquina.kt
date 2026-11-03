package com.huryel.novely.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sons da máquina de escrever, gerados no próprio app (sem arquivo de áudio):
 * tecla (estalo metálico curto), espaço (mais grave), apagar e o "plim" + carro voltando no Enter.
 */
object Maquina {
    private const val TAXA = 44100
    private val faixas = HashMap<String, Array<AudioTrack>>()
    private var vez = 0

    private fun ruido(seg: Double, vol: Double, freq: Double, decaimento: Double = 5.0, semente: Int = 1): ShortArray {
        val n = (TAXA * seg).toInt(); val r = Random(semente); val s = ShortArray(n)
        // passa-banda simples (duas ressonâncias) sobre ruído que some depressa
        var y1 = 0.0; var y2 = 0.0
        val w = 2 * PI * freq / TAXA; val rr = 0.97; val a1 = 2 * rr * kotlin.math.cos(w); val a2 = -rr * rr
        for (i in 0 until n) {
            val env = (1 - i.toDouble() / n).pow(decaimento)
            val x = (r.nextDouble() * 2 - 1) * env
            val y = x + a1 * y1 + a2 * y2; y2 = y1; y1 = y
            s[i] = (y * vol * 2500).coerceIn(-32000.0, 32000.0).toInt().toShort()
        }
        return s
    }
    private fun tom(seg: Double, freq: Double, vol: Double, inicio: Double = 0.0, total: Double = seg + inicio): ShortArray {
        val n = (TAXA * total).toInt(); val s = ShortArray(n); val i0 = (TAXA * inicio).toInt()
        for (i in i0 until n) { val t = (i - i0).toDouble() / TAXA; s[i] = (sin(2 * PI * freq * t) * exp(-t * 5) * vol * 32000).toInt().toShort() }
        return s
    }
    private fun mistura(vararg ps: ShortArray): ShortArray {
        val n = ps.maxOf { it.size }; val s = ShortArray(n)
        for (i in 0 until n) s[i] = ps.sumOf { (it.getOrNull(i) ?: 0).toInt() }.coerceIn(-32000, 32000).toShort()
        return s
    }
    private fun faixa(pcm: ShortArray): AudioTrack {
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(TAXA).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.size * 2).build()
        t.write(pcm, 0, pcm.size)
        return t
    }
    private fun preparar() {
        if (faixas.isNotEmpty()) return
        // 3 cópias de cada som para tocar rápido sem cortar o anterior
        faixas["tecla"] = Array(3) { k -> faixa(mistura(ruido(0.035, 1.0, 2400.0 + k * 300, 6.0, k + 1), ruido(0.05, 0.8, 260.0, 4.0, k + 7))) }
        faixas["espaco"] = Array(2) { k -> faixa(mistura(ruido(0.05, 0.9, 900.0, 5.0, k + 3), ruido(0.07, 0.7, 180.0, 3.0, k + 9))) }
        faixas["apagar"] = Array(2) { k -> faixa(ruido(0.03, 0.7, 1500.0, 6.0, k + 5)) }
        faixas["enter"] = arrayOf(faixa(mistura(tom(0.9, 2093.0, 0.22), ruido(0.35, 0.7, 700.0, 1.5, 11), ruido(0.07, 1.0, 300.0, 4.0, 13).let { atraso(it, 0.27) })))
    }
    private fun atraso(p: ShortArray, seg: Double): ShortArray { val d = (TAXA * seg).toInt(); return ShortArray(p.size + d).also { p.copyInto(it, d) } }

    fun tocar(tipo: String) {
        try {
            preparar()
            val l = faixas[tipo] ?: return
            val t = l[vez++ % l.size]
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.reloadStaticData(); t.play()
        } catch (_: Exception) {}
    }

    /** Qual som tocar para uma mudança de texto (null = nenhum). */
    fun somDa(antes: String, depois: String): String? = when {
        depois.length < antes.length -> "apagar"
        depois.length == antes.length -> null
        depois.length - antes.length > 3 -> null // colou texto ou o teclado completou a palavra inteira
        else -> {
            val novos = depois.length - antes.length
            val i = (0 until minOf(antes.length, depois.length)).firstOrNull { antes[it] != depois[it] } ?: antes.length
            val c = depois.substring(i, i + novos)
            when { '\n' in c -> "enter"; c.isNotEmpty() && c.last() == ' ' -> "espaco"; else -> "tecla" }
        }
    }
}
