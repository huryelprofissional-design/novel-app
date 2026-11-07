package com.huryel.novely.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

/**
 * Vozes neurais do próprio Novely (sem internet depois de baixar), pela biblioteca sherpa-onnx.
 * O nome da voz começa com "novely:" e vive junto das vozes do Android (narrador e voz de cada personagem).
 * Os pacotes ficam no release "vozes-v1" do GitHub e só são baixados quando a pessoa escolhe a voz.
 */
object VozNeural {
    data class Voz(val id: String, val nome: String, val pacote: String, val sid: Int, val feminina: Boolean, val descricao: String)

    val VOZES = listOf(
        Voz("novely:faber", "Faber", "piper-faber", 0, false, "masculina, clara"),
        Voz("novely:cadu", "Cadu", "piper-cadu", 0, false, "masculina, grave"),
        Voz("novely:jeff", "Jeff", "piper-jeff", 0, false, "masculina, jovem"),
        Voz("novely:edresson", "Edresson", "piper-edresson", 0, false, "masculina, leve"),
        Voz("novely:dora-jovem", "Dora jovem", "kokoro", 1, true, "feminina, jovem"),
        Voz("novely:dora-expressiva", "Dora expressiva", "kokoro", 2, true, "feminina, expressiva"),
        Voz("novely:dora-sussurrada", "Dora sussurrada", "kokoro", 3, true, "feminina, sussurrada"),
    )
    private const val BASE = "https://github.com/huryelprofissional-design/novel-app/releases/download/vozes-v1/"
    private val TAMANHO_MB = mapOf("espeak" to 9, "piper-faber" to 56, "piper-cadu" to 56, "piper-jeff" to 56, "piper-edresson" to 56, "kokoro" to 117)

    fun ehNeural(nome: String?) = nome?.startsWith("novely:") == true
    fun voz(nome: String?) = VOZES.firstOrNull { it.id == nome }

    private lateinit var raiz: File
    fun iniciar(ctx: Context) { if (!::raiz.isInitialized) raiz = File(ctx.filesDir, "vozes").apply { mkdirs() } }
    private fun pasta(p: String) = File(raiz, p)
    fun instalada(v: Voz) = pronto("espeak") && pronto(v.pacote)
    private fun pronto(p: String) = File(pasta(p), ".ok").exists()
    /** MB que ainda faltam baixar para usar esta voz. */
    fun faltaMb(v: Voz) = (if (pronto("espeak")) 0 else TAMANHO_MB["espeak"]!!) + (if (pronto(v.pacote)) 0 else TAMANHO_MB[v.pacote] ?: 0)

    // ───────── baixar ─────────
    /** pacote → progresso 0..1 (ausente = parado); "erro:..." vai em [erroDownload]. */
    private val _baixando = MutableStateFlow<Map<String, Float>>(emptyMap())
    val baixando: StateFlow<Map<String, Float>> = _baixando
    var erroDownload: String? = null; private set
    private val travaDownload = Mutex()

    suspend fun baixar(v: Voz): Boolean = travaDownload.withLock {
        erroDownload = null
        try {
            for (p in listOf("espeak", v.pacote)) if (!pronto(p)) baixarPacote(p)
            true
        } catch (e: Exception) {
            erroDownload = "Não deu para baixar a voz: ${e.message ?: "sem internet"}"; false
        } finally { _baixando.value = emptyMap() }
    }

    private suspend fun baixarPacote(p: String) = withContext(Dispatchers.IO) {
        val zip = File(raiz, "$p.zip.parte")
        var tentativa = 0
        while (true) {
            try { baixarArquivo(BASE + "$p.zip", zip) { f -> _baixando.value = mapOf(p to f) }; break }
            catch (e: Exception) { if (++tentativa >= 6) throw e; delay(1500L * tentativa) }
        }
        val destino = pasta(p).apply { deleteRecursively(); mkdirs() }
        val raizCanon = destino.canonicalPath + File.separator
        ZipInputStream(zip.inputStream().buffered()).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val f = File(destino, e.name)
                if (!f.canonicalPath.startsWith(raizCanon)) throw IllegalStateException("pacote inválido") // zip slip
                if (e.isDirectory) { f.mkdirs(); continue }
                f.parentFile?.mkdirs()
                f.outputStream().use { Limites.copiarAte(z, it, 400L * 1024 * 1024) }
            }
        }
        zip.delete()
        File(destino, ".ok").writeText("1")
    }

    /** Download que continua de onde parou (a internet do celular cai). */
    private suspend fun baixarArquivo(url: String, destino: File, progresso: (Float) -> Unit) {
        val ja = if (destino.exists()) destino.length() else 0L
        var u = url; var con: HttpURLConnection; var saltos = 0
        while (true) {
            con = (URL(u).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false; connectTimeout = 15_000; readTimeout = 30_000
                setRequestProperty("User-Agent", "Novely-Android")
                if (ja > 0) setRequestProperty("Range", "bytes=$ja-")
            }
            val c = con.responseCode
            if (c in 300..399 && saltos++ < 5) { u = con.getHeaderField("Location"); con.disconnect(); continue }
            if (c == 416) { con.disconnect(); return }
            if (c != 200 && c != 206) throw IllegalStateException("resposta $c")
            break
        }
        val continua = con.responseCode == 206
        val inicio = if (continua) ja else 0L
        val total = con.contentLengthLong.takeIf { it > 0 }?.plus(inicio) ?: -1L
        con.inputStream.use { ins ->
            java.io.FileOutputStream(destino, continua).use { out ->
                val buf = ByteArray(128 * 1024); var feito = inicio; var ultimo = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val r = ins.read(buf); if (r <= 0) break
                    out.write(buf, 0, r); feito += r
                    if (feito - ultimo > 512_000) { ultimo = feito; if (total > 0) progresso(feito.toFloat() / total) }
                }
            }
        }
        if (total > 0 && destino.length() < total) throw IllegalStateException("download incompleto")
    }

    fun apagar(pacote: String) { pasta(pacote).deleteRecursively(); motores.remove(pacote)?.release() }

    // ───────── gerar ─────────
    private val motores = HashMap<String, OfflineTts>()
    private val travaMotor = Mutex()

    private fun motor(v: Voz): OfflineTts = motores.getOrPut(v.pacote) {
        val esp = File(pasta("espeak"), "espeak-ng-data").absolutePath
        val d = pasta(v.pacote)
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        val modelo = if (v.pacote == "kokoro") OfflineTtsModelConfig(
            kokoro = OfflineTtsKokoroModelConfig(model = File(d, "model.int8.onnx").absolutePath, voices = File(d, "vozes-novely.bin").absolutePath,
                tokens = File(d, "tokens.txt").absolutePath, dataDir = esp, lexicon = File(d, "lexicon-us-en.txt").absolutePath, lang = "pt-br"),
            numThreads = threads, debug = false, provider = "cpu")
        else OfflineTtsModelConfig(
            vits = OfflineTtsVitsModelConfig(model = File(d, "model.onnx").absolutePath, tokens = File(d, "tokens.txt").absolutePath, dataDir = esp),
            numThreads = threads, debug = false, provider = "cpu")
        OfflineTts(assetManager = null, config = OfflineTtsConfig(model = modelo))
    }

    class Audio(val amostras: FloatArray, val taxa: Int)

    /** Gera o áudio (pode levar alguns segundos no Kokoro: o Narrador pede o próximo trecho com antecedência). */
    suspend fun gerar(v: Voz, texto: String, velocidade: Float): Audio = withContext(Dispatchers.Default) {
        travaMotor.withLock {
            val a = motor(v).generate(texto, v.sid, velocidade.coerceIn(0.5f, 2.5f))
            Audio(a.samples, a.sampleRate)
        }
    }

    // ───────── tocar ─────────
    private var faixa: AudioTrack? = null

    /** Toca até o fim (ou até a coroutine ser cancelada: pausa/parar/pular). */
    suspend fun tocar(a: Audio) = withContext(Dispatchers.IO) {
        if (a.amostras.isEmpty()) return@withContext
        val min = AudioTrack.getMinBufferSize(a.taxa, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(a.taxa).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).build())
            .setBufferSizeInBytes(maxOf(min, a.taxa)).setTransferMode(AudioTrack.MODE_STREAM).build()
        faixa?.let { try { it.stop(); it.release() } catch (_: Exception) {} }
        faixa = t
        try {
            t.play()
            var i = 0
            val passo = a.taxa / 10
            while (i < a.amostras.size) {
                coroutineContext.ensureActive()
                val n = minOf(passo, a.amostras.size - i)
                t.write(a.amostras, i, n, AudioTrack.WRITE_BLOCKING); i += n
            }
            // espera o que ainda está no buffer acabar de tocar
            while (t.playbackHeadPosition < a.amostras.size) { coroutineContext.ensureActive(); delay(30) }
        } finally {
            try { t.pause(); t.flush(); t.stop(); t.release() } catch (_: Exception) {}
            if (faixa === t) faixa = null
        }
    }

    fun calar() { faixa?.let { try { it.pause(); it.flush() } catch (_: Exception) {} } }

    // ───────── fila com antecedência ─────────
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = LinkedHashMap<String, Deferred<Audio>>()

    /** Começa a gerar já (para não ter pausa entre um trecho e outro). Guarda só os últimos 6. */
    fun preparar(v: Voz, texto: String, velocidade: Float): Deferred<Audio> = synchronized(cache) {
        val k = "${v.id}|$velocidade|$texto"
        cache[k] ?: escopo.async { gerar(v, texto, velocidade) }.also {
            cache[k] = it
            while (cache.size > 6) cache.remove(cache.keys.first())
        }
    }
    fun esquecer() = synchronized(cache) { cache.values.forEach { it.cancel() }; cache.clear() }

    /** Para testar uma voz nas configurações. */
    suspend fun testar(v: Voz, velocidade: Float) { tocar(gerar(v, "Olá! É assim que eu leio a sua história.", velocidade)) }

}
