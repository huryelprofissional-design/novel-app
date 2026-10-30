package com.huryel.novely.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Nova versão encontrada nos Releases do GitHub. */
data class Versao(val numero: Int, val nome: String, val novidades: String, val urlApk: String, val tamanho: Long)

sealed interface EstadoAtualizacao {
    data object Parado : EstadoAtualizacao
    data object Verificando : EstadoAtualizacao
    data object EmDia : EstadoAtualizacao
    data class Disponivel(val v: Versao) : EstadoAtualizacao
    data class Baixando(val v: Versao, val progresso: Float) : EstadoAtualizacao
    data class Pronta(val v: Versao, val arquivo: File) : EstadoAtualizacao
    data class Erro(val mensagem: String) : EstadoAtualizacao
}

/**
 * Atualização de dentro do app: consulta o último Release do GitHub (tag "build-N"),
 * compara com a versão instalada, baixa o APK e abre o instalador do Android.
 * A internet só é usada aqui, e só quando a pessoa pede (ou 1x por dia, se ativado).
 */
class Atualizador(private val ctx: Context, private val prefs: Preferencias) {
    companion object {
        const val REPOSITORIO = "huryelprofissional-design/novel-app"
    }

    private val _estado = MutableStateFlow<EstadoAtualizacao>(EstadoAtualizacao.Parado)
    val estado: StateFlow<EstadoAtualizacao> = _estado

    val versaoInstalada: Int = try {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        (if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()).toInt()
    } catch (_: Exception) { 0 }

    val nomeInstalado: String = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (_: Exception) { "" }

    /** Checagem automática discreta (no máximo a cada 12 h). */
    suspend fun verificarSePreciso() {
        if (!prefs.atualizarAuto.estado.value) return
        if (System.currentTimeMillis() - prefs.ultimaVerificacao.estado.value < 15 * 60_000L) return
        verificar(silencioso = true)
    }

    suspend fun verificar(silencioso: Boolean = false) = withContext(Dispatchers.IO) {
        if (_estado.value is EstadoAtualizacao.Baixando) return@withContext
        _estado.value = EstadoAtualizacao.Verificando
        try {
            val con = (URL("https://api.github.com/repos/$REPOSITORIO/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000; readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Novely-Android")
            }
            val codigo = con.responseCode
            if (codigo == 404 || codigo == 401 || codigo == 403) {
                _estado.value = if (silencioso) EstadoAtualizacao.Parado
                else EstadoAtualizacao.Erro("Não consegui ver as versões. O repositório de atualizações precisa estar público no GitHub.")
                return@withContext
            }
            if (codigo != 200) throw IllegalStateException("resposta $codigo")
            val j = JSONObject(con.inputStream.bufferedReader().use { it.readText() })
            prefs.ultimaVerificacao.definir(System.currentTimeMillis())
            val tag = j.optString("tag_name")
            val numero = tag.substringAfterLast('-').toIntOrNull() ?: 0
            val assets = j.optJSONArray("assets")
            var url = ""; var tam = 0L
            if (assets != null) for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) { url = a.optString("browser_download_url"); tam = a.optLong("size"); break }
            }
            val novidades = j.optString("body").lines()
                .filterNot { it.startsWith("Co-Authored-By") || it.startsWith("Claude-Session") || it.startsWith("APK gerado") }
                .joinToString("\n").trim()
            _estado.value = if (numero > versaoInstalada && url.isNotEmpty())
                EstadoAtualizacao.Disponivel(Versao(numero, j.optString("name").ifEmpty { tag }, novidades, url, tam))
            else EstadoAtualizacao.EmDia
        } catch (e: Exception) {
            _estado.value = if (silencioso) EstadoAtualizacao.Parado
            else EstadoAtualizacao.Erro("Sem conexão ou o GitHub não respondeu. Tente de novo daqui a pouco.")
        }
    }

    suspend fun baixar(v: Versao) = withContext(Dispatchers.IO) {
        _estado.value = EstadoAtualizacao.Baixando(v, 0f)
        try {
            val dir = File(ctx.cacheDir, "atualizacao").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val destino = File(dir, "novely-${v.numero}.apk")
            var url = v.urlApk
            var con: HttpURLConnection
            var saltos = 0
            while (true) {
                con = (URL(url).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false; connectTimeout = 15_000; readTimeout = 30_000
                    setRequestProperty("User-Agent", "Novely-Android")
                }
                val c = con.responseCode
                if (c in 300..399 && saltos < 5) { url = con.getHeaderField("Location"); con.disconnect(); saltos++; continue }
                if (c != 200) throw IllegalStateException("download respondeu $c")
                break
            }
            val total = con.contentLengthLong.takeIf { it > 0 } ?: v.tamanho
            con.inputStream.use { ins ->
                destino.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024); var feito = 0L; var ultimo = 0L
                    while (true) {
                        val r = ins.read(buf); if (r <= 0) break
                        out.write(buf, 0, r); feito += r
                        if (feito - ultimo > 200_000) { ultimo = feito; _estado.value = EstadoAtualizacao.Baixando(v, if (total > 0) feito.toFloat() / total else 0f) }
                    }
                }
            }
            _estado.value = EstadoAtualizacao.Pronta(v, destino)
        } catch (e: Exception) {
            _estado.value = EstadoAtualizacao.Erro("O download parou: ${e.message ?: "sem conexão"}. Tente de novo.")
        }
    }

    /** O Android exige permitir "instalar apps desconhecidos" para o Novely uma vez. */
    fun podeInstalar(): Boolean = Build.VERSION.SDK_INT < 26 || ctx.packageManager.canRequestPackageInstalls()

    fun abrirPermissao(ativ: Context) {
        if (Build.VERSION.SDK_INT >= 26) ativ.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun instalar(ativ: Context, arquivo: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".arquivos", arquivo)
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ativ.startActivity(i)
    }

    fun limpar() { _estado.value = EstadoAtualizacao.Parado }
}
