package com.huryel.novely.data

import com.huryel.novely.NovelyApp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Progresso de jogo do Novely: XP, nível, título, missões do dia com baú e os momentos de comemorar
 * (subiu de nível, conquista nova, baú aberto). Tudo no aparelho; o XP da semana sobe para o perfil público
 * para o ranking entre quem você segue.
 */
object Progresso {
    sealed interface Evento {
        data class Nivel(val nivel: Int, val titulo: String) : Evento
        data class Conquista(val icone: String, val titulo: String, val cor: Long) : Evento
        data class Bau(val moedas: Int, val xp: Int) : Evento
    }
    private val _eventos = MutableSharedFlow<Evento>(extraBufferCapacity = 8)
    val eventos: SharedFlow<Evento> = _eventos

    private val prefs get() = NovelyApp.instancia.prefs
    private fun hoje() = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    fun semanaAtual(): String { val c = Calendar.getInstance(); return "${c.get(Calendar.YEAR)}-${c.get(Calendar.WEEK_OF_YEAR)}" }

    // ───────── nível ─────────
    /** XP para passar do nível [n] para o seguinte. */
    fun custoNivel(n: Int) = 300 + 150 * (n - 1)
    data class InfoNivel(val nivel: Int, val noNivel: Long, val precisa: Long) { val fracao get() = noNivel.toFloat() / precisa }
    fun info(xp: Long = prefs.xp.valor): InfoNivel {
        var n = 1; var resto = xp
        while (resto >= custoNivel(n)) { resto -= custoNivel(n); n++ }
        return InfoNivel(n, resto, custoNivel(n).toLong())
    }
    private val TITULOS = listOf(1 to "Leitor Curioso", 3 to "Leitor Voraz", 6 to "Devorador de Capítulos", 10 to "Guardião de Histórias", 15 to "Lenda do Novely", 25 to "Mestre das Mil Páginas")
    fun titulo(n: Int) = TITULOS.last { n >= it.first }.second

    @Synchronized fun ganharXp(n: Int) {
        if (n <= 0) return
        val antes = info().nivel
        prefs.xp.definir(prefs.xp.valor + n)
        val sem = semanaAtual()
        if (prefs.xpSemanaId.valor != sem) { prefs.xpSemanaId.definir(sem); prefs.xpSemana.definir(0) }
        prefs.xpSemana.definir(prefs.xpSemana.valor + n)
        val depois = info().nivel
        if (depois > antes) _eventos.tryEmit(Evento.Nivel(depois, titulo(depois)))
    }

    // ───────── missões do dia ─────────
    data class Missao(val id: String, val titulo: String, val acao: String, val alvo: Int, val feito: Int) { val pronta get() = feito >= alvo }
    private val POOL = listOf(
        Triple("ler1", "Ler 1 capítulo", "ler" to 1), Triple("ler3", "Ler 3 capítulos", "ler" to 3),
        Triple("com1", "Comentar num capítulo", "comentar" to 1), Triple("esc300", "Escrever 300 palavras", "escrever" to 300),
        Triple("esc800", "Escrever 800 palavras", "escrever" to 800), Triple("gri1", "Grifar um trecho", "grifar" to 1),
        Triple("ouv1", "Ouvir um capítulo inteiro", "ouvir" to 1),
    )
    private fun contagens(): Map<String, Int> {
        val l = prefs.missoesDia.valor.split(';'); if (l.firstOrNull() != hoje()) return emptyMap()
        return l.drop(1).mapNotNull { e -> e.split('=').takeIf { it.size == 2 }?.let { it[0] to (it[1].toIntOrNull() ?: 0) } }.toMap()
    }
    /** 3 missões por dia, sempre as mesmas no mesmo dia. Quem não escreve não recebe missão de escrever. */
    fun missoes(): List<Missao> {
        val escreve = prefs.totalEscrito.valor > 0
        val r = java.util.Random(hoje().toLong())
        val ler = POOL.filter { it.third.first == "ler" }.let { it[r.nextInt(it.size)] }
        val outras = POOL.filter { it.third.first != "ler" && (escreve || it.third.first != "escrever") }.shuffled(r)
        val segunda = if (escreve) outras.first { it.third.first == "escrever" } else outras.first()
        val terceira = outras.first { it != segunda && it.third.first != segunda.third.first }
        val c = contagens()
        return listOf(ler, segunda, terceira).map { (id, t, a) -> Missao(id, t, a.first, a.second, minOf(c[a.first] ?: 0, a.second)) }
    }
    val bauAberto get() = prefs.bauDia.valor == hoje()
    fun bauPronto() = !bauAberto && missoes().all { it.pronta }

    /** Conta uma ação para as missões (ler, comentar, escrever, grifar, ouvir) e dá o XP dela. */
    @Synchronized fun registrar(acao: String, qtd: Int = 1) {
        val c = contagens().toMutableMap(); c[acao] = (c[acao] ?: 0) + qtd
        prefs.missoesDia.definir((listOf(hoje()) + c.map { "${it.key}=${it.value}" }).joinToString(";"))
        ganharXp(when (acao) { "ler" -> 20 * qtd; "comentar" -> 10 * qtd; "grifar" -> 5 * qtd; "ouvir" -> 25 * qtd; "escrever" -> qtd / 10; else -> 0 })
    }

    fun abrirBau(): Boolean {
        if (!bauPronto()) return false
        prefs.bauDia.definir(hoje())
        prefs.moedas.definir(prefs.moedas.valor + 10)
        ganharXp(50)
        _eventos.tryEmit(Evento.Bau(10, 50))
        return true
    }

    // ───────── conquistas novas ─────────
    /** Confere conquistas: as que acabaram de ser ganhas viram comemoração (e +100 XP cada). */
    suspend fun conferirConquistas(lista: List<Triple<String, String, Pair<String, Long>>>) {
        val vistas = prefs.conquistasVistas.valor.split(',').filter { it.isNotBlank() }.toSet()
        val primeiraVez = prefs.conquistasVistas.valor.isEmpty()
        val novas = lista.filter { it.first !in vistas }
        if (novas.isEmpty()) return
        prefs.conquistasVistas.definir((vistas + novas.map { it.first }).joinToString(","))
        if (primeiraVez) return // quem já tinha conquistas antes desta versão não recebe tudo de uma vez
        novas.forEach { (_, ic, t) -> ganharXp(100); _eventos.tryEmit(Evento.Conquista(ic, t.first.replace('\n', ' '), t.second)) }
    }

    // ───────── perfil público (ranking) ─────────
    fun publicar() {
        val app = NovelyApp.instancia
        val uid = app.conta.usuario.value?.uid ?: return
        val i = info()
        val dados = mapOf("xpSemana" to prefs.xpSemana.valor, "semanaId" to prefs.xpSemanaId.valor, "nivel" to i.nivel, "titulo" to titulo(i.nivel))
        app.escopo.launch { try { com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("perfis").document(uid).set(dados, com.google.firebase.firestore.SetOptions.merge()) } catch (_: Exception) {} }
    }
    data class NoRanking(val uid: String, val nome: String, val xp: Long, val eu: Boolean)
    suspend fun ranking(): List<NoRanking> {
        val app = NovelyApp.instancia
        val eu = app.conta.usuario.value?.uid ?: return emptyList()
        val sem = semanaAtual()
        val seguidos = prefs.autoresSeguidos.valor.split(',').filter { it.isNotBlank() && it != eu }.take(30)
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val outros = seguidos.mapNotNull { uid ->
            try {
                val d = db.collection("perfis").document(uid).get().await()
                NoRanking(uid, d.getString("nome") ?: "?", if (d.getString("semanaId") == sem) d.getLong("xpSemana") ?: 0 else 0, false)
            } catch (_: Exception) { null }
        }
        val meu = NoRanking(eu, "Você", if (prefs.xpSemanaId.valor == sem) prefs.xpSemana.valor else 0, true)
        return (outros + meu).sortedByDescending { it.xp }
    }
}

