package com.huryel.novely

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.huryel.novely.data.Preferencias
import com.huryel.novely.data.Repositorio
import com.huryel.novely.ui.AppNavegacao
import com.huryel.novely.ui.theme.NovelyTheme
import com.huryel.novely.ui.theme.TemaLeitura

class NovelyApp : Application() {
    lateinit var repo: Repositorio
    lateinit var prefs: Preferencias
    lateinit var importador: com.huryel.novely.data.Importador
    /** Escopo que sobrevive à troca de tela: usado para terminar salvamentos ao sair. */
    val escopo = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        instancia = this
        repo = Repositorio(this)
        prefs = Preferencias(this)
        importador = com.huryel.novely.data.Importador(this, repo)
    }
    companion object {
        lateinit var instancia: NovelyApp
            private set
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = NovelyApp.instancia.prefs
        setContent {
            val nomeTema by prefs.tema.collectAsState()
            val tema = TemaLeitura.entries.firstOrNull { it.name == nomeTema } ?: TemaLeitura.OLED
            NovelyTheme(tema) {
                AppNavegacao(temaAtual = tema, aoMudarTema = { prefs.definirTema(it.name) })
            }
        }
    }
}
