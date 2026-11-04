package com.huryel.novely.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Som ambiente gerado no próprio app (sem arquivo e sem internet): chuva, lareira, vento, noite e café.
 * Um único fio escreve num AudioTrack em streaming; trocar de clima faz um fade curto.
 */
object Ambiente {
    private const val TAXA = 22050
    @Volatile private var alvo: String? = null
    @Volatile var atual: String? = null; private set
    private var fio: Thread? = null

    val TIPOS = listOf("chuva" to "🌧 Chuva", "lareira" to "🔥 Lareira", "vento" to "🌬 Vento", "noite" to "🌙 Noite", "cafe" to "☕ Café")

    @Synchronized fun tocar(tipo: String?) {
        alvo = tipo
        if (tipo != null && fio == null) {
            fio = Thread({ laco() }, "ambiente").apply { isDaemon = true; start() }
        }
    }
    fun parar() = tocar(null)

    private fun laco() {
        val min = AudioTrack.getMinBufferSize(TAXA, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val faixa = try {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(TAXA).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(min, 8192)).build()
        } catch (e: Exception) { synchronized(this) { fio = null }; return }
        faixa.play()
        val r = Random(System.nanoTime())
        val bloco = ShortArray(1024)
        var vol = 0.0
        var tipo: String? = null
        // estados dos filtros
        var marrom = 0.0; var rosa1 = 0.0; var rosa2 = 0.0; var passa = 0.0; var t = 0L
        var estalo = 0.0; var estaloF = 0.0; var gota = 0.0; var grilo = 0; var tilinta = 0; var tilF = 0.0
        try {
            while (true) {
                val quer = alvo
                if (quer != tipo) {
                    // fade-out, troca, fade-in
                    if (vol < 0.01) { tipo = quer; atual = quer; if (quer == null) break } else vol -= 0.08
                } else if (vol < 1.0) vol = minOf(1.0, vol + 0.04)
                for (i in bloco.indices) {
                    t++
                    val branco = r.nextDouble() * 2 - 1
                    marrom = (marrom + branco * 0.02) * 0.998
                    rosa1 = 0.99 * rosa1 + branco * 0.1; rosa2 = 0.6 * rosa2 + branco * 0.4
                    val s: Double = when (tipo) {
                        "chuva" -> {
                            // chiado constante + gotas aleatórias batendo
                            if (r.nextInt(900) == 0) gota = 0.6 + r.nextDouble() * 0.4
                            gota *= 0.995
                            (rosa2 * 0.18 + rosa1 * 0.25) + branco * gota * 0.25
                        }
                        "lareira" -> {
                            // ronco grave + estalos de lenha
                            if (r.nextInt(2600) == 0) { estalo = 1.0; estaloF = 0.3 + r.nextDouble() * 0.5 }
                            estalo *= 0.985
                            passa += (branco - passa) * estaloF
                            marrom * 2.2 + passa * estalo * 0.7
                        }
                        "vento" -> {
                            // ruído passando por um filtro que abre e fecha devagar
                            val abre = 0.02 + 0.05 * (1 + sin(2 * PI * t / (TAXA * 7.0))) / 2
                            passa += (branco - passa) * abre
                            passa * (0.9 + 0.6 * sin(2 * PI * t / (TAXA * 3.3)))
                        }
                        "noite" -> {
                            // fundo bem baixo + grilos
                            if (grilo <= 0 && r.nextInt(TAXA * 2) == 0) grilo = (TAXA * 0.35).toInt()
                            val g = if (grilo > 0) { grilo--; val on = (grilo / (TAXA / 28)) % 2 == 0; if (on) sin(2 * PI * 4400 * t / TAXA) * 0.07 else 0.0 } else 0.0
                            marrom * 0.9 + g
                        }
                        "coracao" -> {
                            // tum-tum grave, 70 batidas por minuto
                            val c = (t % (TAXA * 60 / 70)).toDouble() / TAXA
                            val bat = { ini: Double, v: Double -> if (c >= ini && c < ini + 0.12) sin(2 * PI * 48 * (c - ini)) * kotlin.math.exp(-(c - ini) * 30) * v else 0.0 }
                            (bat(0.0, 1.0) + bat(0.22, 0.7)) * 2.6 + marrom * 0.3
                        }
                        "cafe" -> {
                            // burburinho (ruído rosa modulado) + xícaras tilintando
                            if (tilinta <= 0 && r.nextInt(TAXA * 4) == 0) { tilinta = (TAXA * 0.4).toInt(); tilF = 2600.0 + r.nextDouble() * 1800 }
                            val x = if (tilinta > 0) { tilinta--; sin(2 * PI * tilF * t / TAXA) * 0.06 * tilinta / (TAXA * 0.4) } else 0.0
                            rosa1 * 0.32 * (0.8 + 0.3 * sin(2 * PI * t / (TAXA * 1.7))) + marrom * 0.8 + x
                        }
                        else -> 0.0
                    }
                    bloco[i] = (s * vol * 9000).coerceIn(-30000.0, 30000.0).toInt().toShort()
                }
                faixa.write(bloco, 0, bloco.size)
            }
        } catch (_: Exception) {
        } finally {
            atual = null
            runCatching { faixa.stop() }; faixa.release()
            synchronized(this) { fio = null }
            alvo?.let { tocar(it) } // pediram outro som enquanto este desligava
        }
    }
}
