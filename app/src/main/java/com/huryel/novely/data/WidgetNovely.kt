package com.huryel.novely.data

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.huryel.novely.MainActivity
import com.huryel.novely.NovelyApp
import com.huryel.novely.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Widget da tela inicial: continuar lendo (capa, capítulo, progresso) e meta do dia. Toque abre o capítulo. */
class WidgetNovely : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pendente = goAsync()
        NovelyApp.instancia.escopo.launch(Dispatchers.IO) { try { desenhar(ctx, mgr, ids) } finally { pendente.finish() } }
    }

    companion object {
        fun atualizar(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, WidgetNovely::class.java))
            if (ids.isEmpty()) return
            NovelyApp.instancia.escopo.launch(Dispatchers.IO) { try { desenhar(ctx, mgr, ids) } catch (_: Exception) {} }
        }

        private suspend fun desenhar(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            val app = NovelyApp.instancia
            val o = app.repo.dao.obraContinuar()
            val c = o?.ultimoCapituloId?.let { app.repo.dao.capituloAgora(it) }
            val v = RemoteViews(ctx.packageName, R.layout.widget_novely)
            if (o != null && c != null) {
                v.setTextViewText(R.id.w_rotulo, "CONTINUAR LENDO")
                v.setTextViewText(R.id.w_titulo, o.titulo)
                val min = ((1f - c.progresso) * c.palavras / 230f).toInt().coerceAtLeast(1)
                v.setTextViewText(R.id.w_cap, "${c.titulo.ifEmpty { "Capítulo" }} · faltam $min min")
                v.setProgressBar(R.id.w_prog, 100, (c.progresso * 100).toInt(), false)
                val capa = o.capa?.let {
                    try {
                        val op = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeFile(it, op)
                    } catch (_: Exception) { null }
                }
                if (capa != null) v.setImageViewBitmap(R.id.w_capa, capa) else v.setImageViewResource(R.id.w_capa, R.mipmap.ic_launcher)
            } else {
                v.setTextViewText(R.id.w_rotulo, "NOVELY")
                v.setTextViewText(R.id.w_titulo, "Nada sendo lido")
                v.setTextViewText(R.id.w_cap, "Toque para abrir o app")
                v.setProgressBar(R.id.w_prog, 100, 0, false)
            }
            val hoje = app.prefs.palavrasHoje.valor; val meta = app.prefs.metaDiaria.valor.toInt(); val seq = app.prefs.sequencia.valor
            v.setTextViewText(R.id.w_meta, "Meta de hoje: $hoje/$meta palavras" + if (seq > 0) " · 🔥 $seq" else "")
            val abrir = PendingIntent.getActivity(ctx, 77, Intent(ctx, MainActivity::class.java).putExtra("abrir", "continuar")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            v.setOnClickPendingIntent(R.id.w_raiz, abrir)
            mgr.updateAppWidget(ids, v)
        }
    }
}
