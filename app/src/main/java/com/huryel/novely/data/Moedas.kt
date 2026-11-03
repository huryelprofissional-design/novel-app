package com.huryel.novely.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.huryel.novely.NovelyApp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Moedas do Novely (de graça, sem dinheiro): o leitor ganha lendo e gasta para ler capítulos
 * agendados antes de todo mundo. O autor vê quantos leitores liberaram cedo.
 *
 * Como ganhar: abrir o app (+3 por dia, +10 a cada 7 dias seguidos), terminar capítulo de outro autor (+1, até 10/dia),
 * comentar (+1, até 5/dia).
 */
object Moedas {
    private val prefs get() = NovelyApp.instancia.prefs
    val saldo: StateFlow<Long> get() = prefs.moedas.estado
    private fun hoje() = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    /** Ganha moedas com limite por dia por motivo ("leitura", "comentario"). [unico] evita ganhar duas vezes pelo mesmo capítulo. */
    fun ganhar(qtd: Int, motivo: String, limiteDia: Int, unico: String? = null): Int {
        if (unico != null && prefs.jaContado("moeda/$unico")) return 0
        val dia = hoje()
        val mapa = prefs.moedasDia.valor.split(';').mapNotNull { it.split('=').takeIf { p -> p.size == 2 } }
            .filter { it[0].startsWith("$dia|") }.associate { it[0] to (it[1].toIntOrNull() ?: 0) }.toMutableMap()
        val k = "$dia|$motivo"
        val ja = mapa[k] ?: 0
        val dar = minOf(qtd, limiteDia - ja).coerceAtLeast(0)
        if (dar == 0) return 0
        mapa[k] = ja + dar
        prefs.moedasDia.definir(mapa.entries.joinToString(";") { "${it.key}=${it.value}" })
        if (unico != null) prefs.marcarContado("moeda/$unico")
        prefs.moedas.definir(prefs.moedas.valor + dar)
        salvarNaNuvem()
        return dar
    }

    /** Presente do dia (uma vez por dia). Devolve quanto ganhou (0 se já pegou). */
    fun diario(): Int {
        val dia = hoje()
        if (prefs.moedaDiaria.valor == dia) return 0
        val ontem = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(System.currentTimeMillis() - 86_400_000L))
        val seq = if (prefs.moedaDiaria.valor == ontem) prefs.moedaSequencia.valor + 1 else 1
        prefs.moedaDiaria.definir(dia); prefs.moedaSequencia.definir(seq)
        val dar = 3 + if (seq % 7 == 0L) 10 else 0
        prefs.moedas.definir(prefs.moedas.valor + dar)
        salvarNaNuvem()
        return dar
    }

    fun liberado(pid: String, cid: String) = "$pid/$cid" in prefs.desbloqueados.valor.split('\n')

    fun gastar(qtd: Int, pid: String, cid: String): Boolean {
        if (liberado(pid, cid)) return true
        if (prefs.moedas.valor < qtd) return false
        prefs.moedas.definir(prefs.moedas.valor - qtd)
        prefs.desbloqueados.definir((prefs.desbloqueados.valor.split('\n').filter { it.isNotBlank() } + "$pid/$cid").takeLast(2000).joinToString("\n"))
        salvarNaNuvem()
        return true
    }

    private fun salvarNaNuvem() {
        val uid = NovelyApp.instancia.conta.usuario.value?.uid ?: return
        val agora = System.currentTimeMillis(); prefs.carteiraEm.definir(agora)
        val dados = mapOf("carteira" to mapOf("saldo" to prefs.moedas.valor, "liberados" to prefs.desbloqueados.valor, "em" to agora))
        NovelyApp.instancia.escopo.launch { try { FirebaseFirestore.getInstance().collection("users").document(uid).set(dados, SetOptions.merge()).await() } catch (_: Exception) {} }
    }

    /** Ao entrar (ou reinstalar): recupera a carteira guardada na conta, se ela for mais nova. */
    suspend fun carregarDaNuvem() {
        val uid = NovelyApp.instancia.conta.usuario.value?.uid ?: return
        try {
            val c = FirebaseFirestore.getInstance().collection("users").document(uid).get().await().get("carteira") as? Map<*, *> ?: return
            val em = (c["em"] as? Number)?.toLong() ?: 0
            if (em > prefs.carteiraEm.valor) {
                prefs.moedas.definir((c["saldo"] as? Number)?.toLong() ?: prefs.moedas.valor)
                prefs.desbloqueados.definir(c["liberados"] as? String ?: prefs.desbloqueados.valor)
                prefs.carteiraEm.definir(em)
            }
        } catch (_: Exception) {}
    }
}
