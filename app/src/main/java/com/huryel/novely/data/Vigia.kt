package com.huryel.novely.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.huryel.novely.MainActivity
import com.huryel.novely.NovelyApp
import com.huryel.novely.R
import java.util.concurrent.TimeUnit

/**
 * Roda de hora em hora mesmo com o app fechado (só com internet):
 * baixa capítulos novos das obras seguidas, procura obras novas de autores seguidos
 * e versão nova do app — e avisa com notificação.
 */
class Vigia(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as NovelyApp
        if (app.conta.usuario.value == null) return Result.success()
        try {
            app.sinc.sincronizar()
            app.pub.atualizarSeguidas().forEach { (o, n) ->
                Vigia.notificar(applicationContext, o.id.toInt(), "Capítulo novo", if (n == 1) "Saiu 1 capítulo de “${o.titulo}”" else "Saíram $n capítulos de “${o.titulo}”")
            }
            app.pub.novasDeAutores().forEach { p ->
                Vigia.notificar(applicationContext, p.id.hashCode(), "Obra nova de ${p.autorNome}", "“${p.titulo}” acabou de ser publicada")
            }
            app.atualizador.verificar(silencioso = true)
            (app.atualizador.estado.value as? EstadoAtualizacao.Disponivel)?.v?.let { v ->
                if (app.prefs.notificadaVersao.valor < v.numero) {
                    Vigia.notificar(applicationContext, 999_001, "Versão nova do Novely", "Toque para atualizar (build ${v.numero})")
                    app.prefs.notificadaVersao.definir(v.numero.toLong())
                }
            }
        } catch (_: Exception) { return Result.retry() }
        return Result.success()
    }

    companion object {
        private const val CANAL = "novidades"

        fun agendar(ctx: Context) {
            val pedido = PeriodicWorkRequestBuilder<Vigia>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("vigia-novidades", ExistingPeriodicWorkPolicy.KEEP, pedido)
        }

        fun notificar(ctx: Context, id: Int, titulo: String, texto: String) {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CANAL) == null)
                nm.createNotificationChannel(NotificationChannel(CANAL, "Capítulos e novidades", NotificationManager.IMPORTANCE_DEFAULT))
            val abrir = PendingIntent.getActivity(ctx, id, Intent(ctx, MainActivity::class.java).putExtra("abrir", "avisos")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val n = NotificationCompat.Builder(ctx, CANAL).setSmallIcon(R.drawable.ic_notificacao).setColor(0xFFE9A65A.toInt())
                .setContentTitle(titulo).setContentText(texto).setAutoCancel(true).setContentIntent(abrir).build()
            try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) {}
        }
    }
}
