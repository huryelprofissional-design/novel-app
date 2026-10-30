package com.huryel.novely

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.huryel.novely.data.Preferencias
import kotlinx.coroutines.launch
import com.huryel.novely.data.Repositorio
import com.huryel.novely.ui.AppNavegacao
import com.huryel.novely.ui.theme.NovelyTheme
import com.huryel.novely.ui.theme.TemaLeitura

class NovelyApp : Application() {
    lateinit var repo: Repositorio
    lateinit var prefs: Preferencias
    lateinit var importador: com.huryel.novely.data.Importador
    lateinit var backup: com.huryel.novely.data.Backup
    lateinit var atualizador: com.huryel.novely.data.Atualizador
    lateinit var conta: com.huryel.novely.data.Conta
    lateinit var sinc: com.huryel.novely.data.Sincronizador
    /** Arquivo aberto com o Novely (WhatsApp, gerenciador de arquivos…), esperando ser tratado. */
    val arquivoRecebido = kotlinx.coroutines.flow.MutableStateFlow<android.net.Uri?>(null)
    /** Escopo que sobrevive à troca de tela: usado para terminar salvamentos ao sair. */
    val escopo = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        instancia = this
        com.huryel.novely.data.Diagnostico.iniciar(this)
        repo = Repositorio(this)
        prefs = Preferencias(this)
        importador = com.huryel.novely.data.Importador(this, repo)
        backup = com.huryel.novely.data.Backup(this, repo, prefs)
        atualizador = com.huryel.novely.data.Atualizador(this, prefs)
        com.huryel.novely.data.Sincronizador.configurar()
        conta = com.huryel.novely.data.Conta(this)
        sinc = com.huryel.novely.data.Sincronizador(this, repo, prefs, conta)
        repo.aoEscreverPalavras = { prefs.registrarPalavras(it) }
        prefs.conferirDia()
        escopo.launch { backup.backupAutomaticoSePreciso() }
        escopo.launch { repo.limparLixeiraAntiga() }
        escopo.launch { kotlinx.coroutines.delay(4000); atualizador.verificarSePreciso() }
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
        if (savedInstanceState == null) receber(intent)
        setContent {
            val nomeTema by prefs.tema.collectAsState()
            val tema = TemaLeitura.entries.firstOrNull { it.name == nomeTema } ?: TemaLeitura.OLED
            androidx.compose.runtime.LaunchedEffect(tema) {
                val transp = android.graphics.Color.TRANSPARENT
                val estilo = if (tema.paleta.escuro) androidx.activity.SystemBarStyle.dark(transp) else androidx.activity.SystemBarStyle.light(transp, transp)
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }
            NovelyTheme(tema) {
                AppNavegacao(temaAtual = tema, aoMudarTema = { prefs.definirTema(it.name) })
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        receber(intent)
    }

    /** "Abrir com Novely" / "Compartilhar com Novely": guarda o arquivo para a navegação tratar. */
    private fun receber(i: android.content.Intent?) {
        val uri: android.net.Uri? = when (i?.action) {
            android.content.Intent.ACTION_VIEW -> i.data
            android.content.Intent.ACTION_SEND ->
                if (android.os.Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
                else @Suppress("DEPRECATION") i.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
            else -> null
        }
        if (uri != null) NovelyApp.instancia.arquivoRecebido.value = uri
    }
}
