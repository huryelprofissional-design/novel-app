package com.huryel.novely.data

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Guarda o que aconteceu quando o app fecha sozinho ou trava,
 * para o usuário poder mandar o relatório e o problema ser corrigido.
 *  - Fechou: captura a exceção que derrubou o app.
 *  - Travou: um vigia confere a cada segundo se a tela está respondendo; se ficar mais de 4 s parada,
 *    anota em que ponto do código ela estava presa.
 */
object Diagnostico {
    private lateinit var pasta: File
    private var versao = ""
    @Volatile private var batida = 0L

    val arquivoFechou get() = File(pasta, "fechou.txt")
    val arquivoTravou get() = File(pasta, "travou.txt")

    fun iniciar(ctx: Context) {
        pasta = File(ctx.filesDir, "diagnostico").apply { mkdirs() }
        versao = try {
            val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            "${pi.versionName} (${if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()})"
        } catch (_: Exception) { "?" }

        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { escrever(arquivoFechou, "O app fechou (thread ${t.name})\n\n" + e.stackTraceToString().take(12000)) } catch (_: Throwable) {}
            anterior?.uncaughtException(t, e)
        }

        val principal = Handler(Looper.getMainLooper())
        Thread({
            var anotado = false
            while (true) {
                val enviado = System.currentTimeMillis()
                principal.post { batida = System.currentTimeMillis() }
                try { Thread.sleep(4500) } catch (_: InterruptedException) { return@Thread }
                val travado = batida < enviado
                if (travado && !anotado) {
                    anotado = true
                    val pilha = Looper.getMainLooper().thread.stackTrace.take(40).joinToString("\n") { "  em $it" }
                    try { escrever(arquivoTravou, "A tela ficou parada por mais de 4 segundos.\n\n$pilha") } catch (_: Throwable) {}
                } else if (!travado) anotado = false
            }
        }, "novely-vigia").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }.start()
    }

    private fun escrever(f: File, texto: String) {
        val quando = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())
        f.writeText("Novely $versao · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n$quando\n\n$texto\n")
    }

    /** Relatório pendente (o mais recente), ou null. */
    fun relatorio(): String? {
        val fs = listOf(arquivoFechou, arquivoTravou).filter { it.exists() }.sortedByDescending { it.lastModified() }
        if (fs.isEmpty()) return null
        return fs.joinToString("\n\n────────\n\n") { it.readText() }
    }

    fun ultimoProblemaEm(): Long = listOf(arquivoFechou, arquivoTravou).filter { it.exists() }.maxOfOrNull { it.lastModified() } ?: 0L

    fun apagar() { arquivoFechou.delete(); arquivoTravou.delete() }
}
