package com.huryel.novely.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Elenco: divide cada parágrafo em narração e falas e descobre quem fala
 * ("— Vem — disse Kang." / "Kang gritou: — Vem!"), para cada personagem ter a própria voz.
 */
object Elenco {
    data class Trecho(val texto: String, val quem: Long?) // null = narrador

    private const val VERBOS = "disse|diz|perguntou|pergunta|respondeu|responde|gritou|grita|murmurou|sussurrou|falou|fala|retrucou|rosnou|riu|completou|" +
        "continuou|explicou|insistiu|berrou|resmungou|exclamou|declarou|rebateu|admitiu|repetiu|chamou|cortou|provocou|zombou|suspirou|ordenou|avisou|sorriu|bufou"
    private val RX_VERBO = Regex("\\b($VERBOS)\\b")
    private val RX_TRAVESSAO = Regex("\\s+[—–]\\s+")

    class Nomes(fichas: List<Ficha>) {
        val pessoas = fichas.filter { it.tipo == TipoFicha.PERSONAGEM }
        private val mapa: Map<String, Long> = HashMap<String, Long>().apply {
            pessoas.forEach { f -> put(f.nome.trim(), f.id); f.apelidos.split(',').map { it.trim() }.filter { it.length >= 2 && it[0].isUpperCase() }.forEach { putIfAbsent(it, f.id) } }
            Marcas.partesUnicas(pessoas.associate { it.nome.trim() to it.id }).forEach { (k, v) -> putIfAbsent(k, v) }
        }.filterKeys { it.length >= 2 }
        private val rx = if (mapa.isEmpty()) null else Regex("(?<![\\p{L}\\p{N}'’-])(?:" + mapa.keys.sortedByDescending { it.length }.joinToString("|") { Marcas.alternativa(it) } + ")(?![\\p{L}\\p{N}'’-])")
        fun achar(s: String): List<Pair<Long, IntRange>> = rx?.findAll(s)?.mapNotNull { m -> mapa[m.value]?.let { it to m.range } }?.toList() ?: emptyList()
    }

    /** Quem fala, pela narração em volta da fala: verbo de fala + nome (ou nome + verbo), senão o único nome citado. */
    private fun falante(narracao: String, nomes: Nomes): Long? {
        val ns = nomes.achar(narracao)
        if (ns.isEmpty()) return null
        RX_VERBO.findAll(narracao).forEach { v ->
            ns.firstOrNull { (_, r) -> r.first >= v.range.last && r.first - v.range.last <= 25 }?.let { return it.first }
            ns.lastOrNull { (_, r) -> r.last <= v.range.first && v.range.first - r.last <= 14 }?.let { return it.first }
        }
        return ns.map { it.first }.distinct().singleOrNull()
    }

    /** Divide os parágrafos do capítulo. [falas] diz quais começam com travessão. */
    fun dividir(paragrafos: List<String>, falas: List<Boolean>, nomes: Nomes): List<List<Trecho>> {
        var ultimo: Long? = null; var penultimo: Long? = null
        return paragrafos.mapIndexed { i, t ->
            if (!falas.getOrElse(i) { false } || t.isBlank()) {
                // narração: "Kang gritou:" antes da próxima fala ajuda a saber quem fala
                return@mapIndexed listOf(Trecho(t, null))
            }
            val corpo = t.trim().trimStart('—', '–', '-').trim()
            val pedacos = corpo.split(RX_TRAVESSAO)
            val narr = pedacos.filterIndexed { k, _ -> k % 2 == 1 }.joinToString(" ")
            val anterior = if (i > 0 && !falas.getOrElse(i - 1) { false }) paragrafos[i - 1].takeLast(160) else ""
            var quem = falante(narr, nomes) ?: anterior.takeIf { it.trimEnd().endsWith(":") }?.let { falante(it, nomes) }
            // conversa a dois sem "disse": alterna entre os dois últimos que falaram
            if (quem == null && ultimo != null && penultimo != null && ultimo != penultimo) quem = penultimo
            if (quem != null && quem != ultimo) { penultimo = ultimo; ultimo = quem }
            pedacos.mapIndexedNotNull { k, s -> s.trim().takeIf { it.isNotEmpty() }?.let { Trecho(it, if (k % 2 == 0) quem else null) } }
        }
    }

    private val FEM = Regex("(?iu)\\b(garota|mulher|menina|moça|irmã|mãe|filha|rainha|princesa|senhora|avó|tia|ela|dela|esposa|namorada|guerreira|aluna)\\b")
    private val MASC = Regex("(?iu)\\b(garoto|homem|menino|rapaz|irmão|pai|filho|rei|príncipe|senhor|avô|tio|ele|dele|marido|namorado|guerreiro|aluno)\\b")

    /** 'F', 'M' ou null — pela ficha e pelo texto ("Hana… ela", "o garoto Seo-jun"). */
    fun genero(f: Ficha, texto: String, nomes: Nomes): Char? {
        val ficha = "${f.descricao} ${f.aparencia} ${f.relacoes}"
        var fem = FEM.findAll(ficha).count() * 3; var masc = MASC.findAll(ficha).count() * 3
        nomes.achar(texto).filter { it.first == f.id }.take(80).forEach { (_, r) ->
            val perto = texto.substring(r.last + 1, minOf(texto.length, r.last + 70))
            fem += FEM.findAll(perto).count(); masc += MASC.findAll(perto).count()
        }
        return when { fem > masc * 1.3 && fem >= 2 -> 'F'; masc > fem * 1.3 && masc >= 2 -> 'M'; else -> null }
    }
}

/** Voz e tom de cada personagem: escolha do autor (guardada) ou automática, sempre a mesma para o mesmo personagem. */
data class VozPersonagem(val voz: String, val tom: Float)

/**
 * Narrador: lê o capítulo em voz alta com o motor de voz do Android (offline), com voz diferente
 * para cada personagem, e continua com a tela apagada (serviço com notificação de mídia).
 */
object Narrador {
    data class Estado(val chave: String = "", val bloco: Int = -1, val tocando: Boolean = false, val titulo: String = "", val timerFim: Long = 0L)

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado
    private val _pronta = MutableStateFlow(false)
    val pronta: StateFlow<Boolean> = _pronta
    var erro: String? = null; private set

    private var tts: TextToSpeech? = null
    private lateinit var ctxApp: Context
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var blocos: List<List<Elenco.Trecho>> = emptyList()
    private var vozes: Map<Long, VozPersonagem> = emptyMap()
    var nomesElenco: Map<Long, String> = emptyMap(); private set
    var narrador: String = ""; private set
    var velocidade = 1f
    var vozesPersonagens = true
    private var timer: Job? = null
    /** Leitura com as vozes do Novely (neurais): um trecho de cada vez, gerando o próximo com antecedência. */
    private var jobMisto: Job? = null
    private val esperas = HashMap<String, kotlinx.coroutines.CompletableDeferred<Unit>>()
    private var nEspera = 0

    fun iniciar(ctx: Context) {
        if (tts != null) return
        ctxApp = ctx.applicationContext
        VozNeural.iniciar(ctxApp)
        tts = TextToSpeech(ctxApp) { st ->
            if (st == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("pt", "BR"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts?.language = Locale.getDefault()
                _pronta.value = true
            } else erro = "Nenhum motor de voz disponível. Instale “Serviços de voz do Google” na Play Store."
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { escopo.launch { if (id?.startsWith("m:") == true) esperas.remove(id)?.complete(Unit) else terminou(id) } }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { escopo.launch { if (id?.startsWith("m:") == true) esperas.remove(id)?.complete(Unit) else terminou(id) } }
        })
    }

    /** Vozes do Brasil que funcionam sem internet. */
    fun vozesDisponiveis(): List<Voice> = try {
        (tts?.voices?.toList() ?: emptyList())
            .filter { it.locale.language == "pt" && it.locale.country.equals("BR", true) && !it.isNetworkConnectionRequired && !it.features.contains("notInstalled") }
            .sortedWith(compareBy({ generoDaVoz(it.name) ?: 'z' }, { it.name }))
    } catch (_: Exception) { emptyList() }

    fun generoDaVoz(nome: String): Char? {
        val n = nome.lowercase()
        return when {
            "female" in n || "feminin" in n -> 'F'
            "male" in n || "masculin" in n -> 'M'
            Regex("smtf\\d").containsMatchIn(n) -> 'F'
            Regex("smtm\\d").containsMatchIn(n) -> 'M'
            "-afs" in n || "-pte" in n || "pt-br-language" in n -> 'F'
            "-ptd" in n || "-ptz" in n -> 'M'
            else -> null
        }
    }

    fun definirNarrador(nome: String) {
        narrador = nome
        if (_estado.value.tocando) falarAtual()
    }

    /** Prepara o capítulo: divide em narração/falas e dá voz a cada personagem. */
    suspend fun carregar(chave: String, titulo: String, paragrafos: List<String>, falas: List<Boolean>, obraId: Long) {
        if (_estado.value.chave == chave && blocos.size == paragrafos.size) { _estado.value = _estado.value.copy(titulo = titulo); return }
        parar()
        val app = com.huryel.novely.NovelyApp.instancia
        val fichas = try { app.repo.dao.fichasAgora(obraId) } catch (_: Exception) { emptyList() }
        val nomes = Elenco.Nomes(fichas)
        blocos = kotlinx.coroutines.withContext(Dispatchers.Default) { Elenco.dividir(paragrafos, falas, nomes) }
        val falam = blocos.flatten().mapNotNull { it.quem }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
        val texto = paragrafos.joinToString("\n")
        val escolhidas = app.prefs.vozesElenco(obraId)
        // vozes do Novely já baixadas vêm primeiro (soam melhor); depois as do Android
        val neurais = VozNeural.VOZES.filter { VozNeural.instalada(it) && it.id != narrador }
        val android = vozesDisponiveis()
        val disponiveis = neurais.map { it.id } + android.map { it.name }
        val fem = (neurais.filter { it.feminina }.map { it.id } + android.filter { generoDaVoz(it.name) == 'F' && it.name != narrador }.map { it.name }).ifEmpty { disponiveis }
        val masc = (neurais.filter { !it.feminina }.map { it.id } + android.filter { generoDaVoz(it.name) == 'M' && it.name != narrador }.map { it.name }).ifEmpty { disponiveis }
        val tons = listOf(1.0f, 0.86f, 1.14f, 0.93f, 1.07f)
        var nf = 0; var nm = 0; var nx = 0
        vozes = falam.associateWith { id ->
            escolhidas[id] ?: run {
                val f = fichas.firstOrNull { it.id == id }
                val g = f?.let { Elenco.genero(it, texto, nomes) }
                val lista = when (g) { 'F' -> fem; 'M' -> masc; else -> disponiveis }
                val k = when (g) { 'F' -> nf++; 'M' -> nm++; else -> nx++ }
                val v = lista.getOrNull(k % lista.size.coerceAtLeast(1)) ?: ""
                VozPersonagem(v, tons[(k / lista.size.coerceAtLeast(1) + (if (g == null) 2 else 0)) % tons.size])
            }
        }
        nomesElenco = falam.associateWith { id -> fichas.firstOrNull { it.id == id }?.nome ?: "?" }
        _estado.value = Estado(chave, -1, false, titulo)
    }

    fun vozDe(id: Long) = vozes[id]
    fun mudarVoz(obraId: Long, id: Long, v: VozPersonagem) {
        vozes = vozes + (id to v)
        com.huryel.novely.NovelyApp.instancia.prefs.definirVozElenco(obraId, id, v)
        if (_estado.value.tocando) falarAtual()
    }

    /** Ouve uma frase com a voz escolhida (para testar no Elenco). */
    fun amostra(v: VozPersonagem, frase: String) {
        VozNeural.voz(v.voz)?.takeIf { VozNeural.instalada(it) }?.let { n -> tts?.stop(); escopo.launch { try { VozNeural.tocar(VozNeural.gerar(n, frase, velocidade)) } catch (_: Exception) {} }; return }
        val t = tts ?: return
        aplicar(v.voz, v.tom); t.setSpeechRate(velocidade)
        t.speak(frase, TextToSpeech.QUEUE_FLUSH, null, "amostra")
    }

    fun tocar(a: Int) {
        if (blocos.isEmpty()) return
        _estado.value = _estado.value.copy(bloco = a.coerceIn(0, blocos.size - 1), tocando = true)
        ServicoVoz.atualizar(ctxApp)
        falarAtual()
    }

    /**
     * Guarda no capítulo o parágrafo que está sendo falado (a cada parágrafo).
     * Se o Android fechar o app (tela apagada, pouca memória) ou a voz cortar, ao voltar o leitor abre
     * exatamente aí e o Ouvir continua dali — antes voltava para onde a tela estava quando deu play.
     */
    private var ultimoGuardado = -1
    private fun guardarPosicao(bloco: Int) {
        val capId = _estado.value.chave.split(':').getOrNull(1)?.toLongOrNull() ?: return
        if (bloco < 0 || bloco == ultimoGuardado || blocos.isEmpty()) return
        ultimoGuardado = bloco
        val total = blocos.size
        escopo.launch(Dispatchers.IO) {
            try {
                val dao = com.huryel.novely.NovelyApp.instancia.repo.dao
                val c = dao.capituloAgora(capId) ?: return@launch
                // item 0 da lista do leitor é o cabeçalho: o parágrafo i é o item i + 1
                dao.salvarPosicao(capId, maxOf(c.progresso, (bloco + 1f) / total), bloco + 1, 0)
            } catch (_: Exception) {}
        }
    }
    fun pausar() { _estado.value = _estado.value.copy(tocando = false); calarTudo(); ServicoVoz.atualizar(ctxApp) }
    fun parar() { calarTudo(); timer?.cancel(); _estado.value = Estado(); if (::ctxApp.isInitialized) ServicoVoz.atualizar(ctxApp) }
    private fun calarTudo() { jobMisto?.cancel(); jobMisto = null; VozNeural.calar(); tts?.stop(); esperas.values.forEach { it.cancel() }; esperas.clear() }
    fun irPara(bloco: Int) { _estado.value = _estado.value.copy(bloco = bloco); if (_estado.value.tocando) falarAtual() }
    fun proximo() = proximoFalavel(_estado.value.bloco + 1)?.let { irPara(it) }
    fun anterior() = (_estado.value.bloco - 1 downTo 0).firstOrNull { falavel(it) }?.let { irPara(it) }

    fun timer(minutos: Int) {
        timer?.cancel()
        if (minutos <= 0) { _estado.value = _estado.value.copy(timerFim = 0); return }
        val fim = System.currentTimeMillis() + minutos * 60_000L
        _estado.value = _estado.value.copy(timerFim = fim)
        timer = escopo.launch { delay(minutos * 60_000L); pausar(); _estado.value = _estado.value.copy(timerFim = 0) }
    }

    private fun falavel(i: Int) = blocos.getOrNull(i)?.any { it.texto.isNotBlank() } == true
    private fun proximoFalavel(de: Int) = (de until blocos.size).firstOrNull { falavel(it) }

    private fun aplicar(voz: String, tom: Float) {
        val t = tts ?: return
        try {
            val nome = voz.ifEmpty { narrador }
            if (nome.isNotEmpty()) t.voices?.firstOrNull { it.name == nome }?.let { if (t.voice?.name != it.name) t.voice = it }
            else if (narrador.isEmpty()) t.language = Locale("pt", "BR")
        } catch (_: Exception) {}
        t.setPitch(tom)
    }

    /** Voz (nome) e tom de um trecho: a do personagem ou a do narrador. */
    private fun vozDoTrecho(tr: Elenco.Trecho): Pair<String, Float> {
        val v = if (vozesPersonagens && tr.quem != null) vozes[tr.quem] else null
        return (v?.voz?.takeIf { it.isNotEmpty() } ?: narrador) to (v?.tom ?: 1f)
    }
    private fun neuralPronta(nome: String) = VozNeural.voz(nome)?.takeIf { VozNeural.instalada(it) }

    private fun falarAtual() {
        val t = tts ?: return
        val i = _estado.value.bloco
        if (!falavel(i)) { val p = proximoFalavel(i); if (p == null) { fim(); return }; _estado.value = _estado.value.copy(bloco = p); return falarAtual() }
        guardarPosicao(i)
        calarTudo()
        if (blocos[i].any { neuralPronta(vozDoTrecho(it).first) != null }) { falarMisto(i); return }
        val trechos = blocos[i]
        t.setSpeechRate(velocidade)
        trechos.forEachIndexed { k, tr ->
            val v = if (vozesPersonagens && tr.quem != null) vozes[tr.quem] else null
            aplicar(v?.voz ?: narrador, v?.tom ?: 1f)
            t.speak(tr.texto.take(3900), if (k == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, Bundle(), "$i:$k:${trechos.size}")
        }
        aplicar(narrador, 1f)
    }

    /** Gera com antecedência os trechos com voz do Novely deste parágrafo e do próximo. */
    private fun prepararBloco(b: Int) {
        blocos.getOrNull(b)?.forEach { tr -> neuralPronta(vozDoTrecho(tr).first)?.let { VozNeural.preparar(it, tr.texto.take(3900), velocidade) } }
    }

    /** Fala o parágrafo trecho a trecho, misturando vozes do Novely e do Android. */
    private fun falarMisto(i: Int) {
        val t = tts ?: return
        jobMisto = escopo.launch {
            try {
                prepararBloco(i); proximoFalavel(i + 1)?.let { prepararBloco(it) }
                for (tr in blocos[i]) {
                    val (nome, tom) = vozDoTrecho(tr)
                    val n = neuralPronta(nome)
                    if (n != null) VozNeural.tocar(VozNeural.preparar(n, tr.texto.take(3900), velocidade).await())
                    else {
                        val id = "m:${nEspera++}"
                        val fim = kotlinx.coroutines.CompletableDeferred<Unit>(); esperas[id] = fim
                        aplicar(nome, tom); t.setSpeechRate(velocidade)
                        t.speak(tr.texto.take(3900), TextToSpeech.QUEUE_FLUSH, Bundle(), id)
                        fim.await()
                    }
                }
                val e = _estado.value
                if (!e.tocando || e.bloco != i) return@launch
                val prox = proximoFalavel(i + 1)
                if (prox == null) { Progresso.registrar("ouvir"); fim() } else { _estado.value = e.copy(bloco = prox); falarAtual() }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) { erro = "A voz do Novely falhou: ${e.message}"; pausar() }
        }
    }

    private fun terminou(id: String?) {
        val partes = id?.split(':') ?: return
        if (partes.size != 3) return
        val (b, k, n) = partes.map { it.toIntOrNull() ?: return }
        val e = _estado.value
        if (!e.tocando || b != e.bloco || k != n - 1) return
        val prox = proximoFalavel(b + 1)
        if (prox == null) { Progresso.registrar("ouvir"); fim() } else { _estado.value = e.copy(bloco = prox); falarAtual() }
    }

    private fun fim() { _estado.value = _estado.value.copy(tocando = false, bloco = -1); ServicoVoz.atualizar(ctxApp) }
}

/** Mantém a leitura em voz alta com a tela apagada e mostra os controles na notificação. */
class ServicoVoz : Service() {
    companion object {
        private const val CANAL = "leitura_voz"
        private const val ID = 4242
        fun atualizar(ctx: Context) {
            val e = Narrador.estado.value
            val i = Intent(ctx, ServicoVoz::class.java)
            try {
                if (e.tocando) androidx.core.content.ContextCompat.startForegroundService(ctx, i)
                else if (e.chave.isNotEmpty() && e.bloco >= 0) ctx.startService(i.setAction("PAUSADO"))
                else ctx.stopService(i)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "TOCAR" -> { val b = Narrador.estado.value.bloco; Narrador.tocar(if (b < 0) 0 else b) }
            "PAUSAR" -> Narrador.pausar()
            "PROXIMO" -> Narrador.proximo()
            "PARAR" -> { Narrador.parar(); stopSelf(); return START_NOT_STICKY }
        }
        val e = Narrador.estado.value
        val n = notificacao(e)
        if (e.tocando) {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(ID, n)
        } else {
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_DETACH) else @Suppress("DEPRECATION") stopForeground(false)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(ID, n)
            if (e.chave.isEmpty() || e.bloco < 0) { (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(ID); stopSelf() }
        }
        return START_NOT_STICKY
    }

    private fun acao(a: String, req: Int) = PendingIntent.getService(this, req, Intent(this, ServicoVoz::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun notificacao(e: Narrador.Estado): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CANAL) == null)
            nm.createNotificationChannel(NotificationChannel(CANAL, "Leitura em voz alta", NotificationManager.IMPORTANCE_LOW))
        val abrir = PendingIntent.getActivity(this, 0, packageManager.getLaunchIntentForPackage(packageName), PendingIntent.FLAG_IMMUTABLE)
        return androidx.core.app.NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle(e.titulo.ifEmpty { "Novely" })
            .setContentText(if (e.tocando) "Lendo em voz alta" else "Pausado")
            .setContentIntent(abrir)
            .setOngoing(e.tocando)
            .setSilent(true)
            .addAction(0, if (e.tocando) "Pausar" else "Continuar", acao(if (e.tocando) "PAUSAR" else "TOCAR", 1))
            .addAction(0, "Próximo", acao("PROXIMO", 2))
            .addAction(0, "Parar", acao("PARAR", 3))
            .build()
    }
}
