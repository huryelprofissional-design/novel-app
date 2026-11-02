package com.huryel.novely.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import com.huryel.novely.NovelyApp

/** Troca o ícone do app ligando um activity-alias e desligando os outros. */
object IconesApp {
    val opcoes = linkedMapOf(
        "PADRAO" to listOf(Color(0xFF000000), Color(0xFF1A1A1A)),
        "LARANJA" to listOf(Color(0xFFFF6A2B), Color(0xFFFF9A5A)),
        "ROXO" to listOf(Color(0xFF7A3B8F), Color(0xFFC13DFF)),
        "BRILHO" to listOf(Color(0xFFFF9A5A), Color(0xFFC13DFF)),
        "BRANCO" to listOf(Color(0xFFFFFFFF), Color(0xFFE1E1E1)),
    )
    private val nomes = mapOf("PADRAO" to "Padrao", "LARANJA" to "Laranja", "ROXO" to "Roxo", "BRILHO" to "Brilho", "BRANCO" to "Branco")

    fun trocar(ctx: Context, qual: String) {
        val pm = ctx.packageManager
        nomes.forEach { (k, n) ->
            pm.setComponentEnabledSetting(ComponentName(ctx, "com.huryel.novely.Icone$n"),
                if (k == qual) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
        NovelyApp.instancia.prefs.iconeApp.definir(qual)
    }
}
