package com.huryel.novely.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Escrever junto ao vivo (autor + coautores), parágrafo por parágrafo:
 *   colabs/{obra}/aovivo/{cap}                 título, aberta, autorUid
 *   colabs/{obra}/aovivo/{cap}/pars/{id}        ordem, texto, uid/nome/cor de quem escreveu por último, editando/ate (trava)
 *   colabs/{obra}/aovivo/{cap}/gente/{uid}      nome, cor, parágrafo onde está, em (presença)
 *   colabs/{obra}/aovivo/{cap}/chat/{id}        uid, nome, texto, em
 * Quem está escrevendo num parágrafo trava só aquele parágrafo (por 25 s sem mexer). O texto da obra só muda
 * quando o autor toca em "Trazer para a obra" (a versão antiga fica no histórico).
 */
data class ParVivo(val id: String, val ordem: Double, val texto: String, val uid: String, val nome: String, val cor: Long, val editando: String, val nomeEditando: String, val ate: Long) {
    fun travadoPara(eu: String) = editando.isNotEmpty() && editando != eu && ate > System.currentTimeMillis()
}
data class Pessoa(val uid: String, val nome: String, val cor: Long, val par: String, val em: Long) { val online get() = System.currentTimeMillis() - em < 60_000 }
data class MsgVivo(val id: String, val uid: String, val nome: String, val texto: String, val em: Long)
data class SalaVivo(val pid: String, val cid: String, val titulo: String, val obra: String)

object AoVivo {
    private val db get() = FirebaseFirestore.getInstance()
    private fun sala(pid: String, cid: String) = db.collection("colabs").document(pid).collection("aovivo").document(cid)
    val CORES = listOf(0xFFFF6A2BL, 0xFF4DA3FFL, 0xFF6FCF97L, 0xFFC13DFFL, 0xFFFFC94AL, 0xFFFF6FA8L)
    fun corDe(uid: String) = CORES[(uid.hashCode() and 0x7fffffff) % CORES.size]

    /** Autor abre a sala com o texto atual do capítulo (se já existe, só reabre). */
    suspend fun abrir(pid: String, cid: String, titulo: String, obra: String, texto: String, eu: String) = withContext(Dispatchers.IO) {
        val ref = sala(pid, cid)
        val existe = ref.get().await().exists()
        ref.set(mapOf("titulo" to titulo, "obra" to obra, "aberta" to true, "autorUid" to eu, "em" to System.currentTimeMillis()), SetOptions.merge()).await()
        if (!existe || ref.collection("pars").limit(1).get().await().isEmpty) {
            val pars = texto.split('\n').map { it.trimEnd() }.filter { it.isNotBlank() }.ifEmpty { listOf("") }
            pars.chunked(400).forEachIndexed { lote, grupo ->
                val b = db.batch()
                grupo.forEachIndexed { i, t ->
                    b.set(ref.collection("pars").document(), mapOf("ordem" to (lote * 400 + i + 1).toDouble(), "texto" to t, "uid" to eu, "nome" to "", "cor" to 0L, "editando" to "", "ate" to 0L))
                }
                b.commit().await()
            }
        }
    }

    suspend fun fechar(pid: String, cid: String) = withContext(Dispatchers.IO) { sala(pid, cid).update("aberta", false).await() }

    /** Salas abertas de uma obra (para o coautor entrar). */
    suspend fun abertas(pid: String): List<SalaVivo> = withContext(Dispatchers.IO) {
        try {
            db.collection("colabs").document(pid).collection("aovivo").whereEqualTo("aberta", true).get().await().documents.map {
                SalaVivo(pid, it.id, it.getString("titulo") ?: "", it.getString("obra") ?: "")
            }
        } catch (_: Exception) { emptyList() }
    }

    fun paragrafos(pid: String, cid: String): Flow<List<ParVivo>> = callbackFlow {
        val reg: ListenerRegistration = sala(pid, cid).collection("pars").addSnapshotListener { s, _ ->
            if (s == null) return@addSnapshotListener
            trySend(s.documents.map { d ->
                ParVivo(d.id, d.getDouble("ordem") ?: 0.0, d.getString("texto") ?: "", d.getString("uid") ?: "", d.getString("nome") ?: "", d.getLong("cor") ?: 0,
                    d.getString("editando") ?: "", d.getString("nomeEditando") ?: "", d.getLong("ate") ?: 0)
            }.sortedBy { it.ordem })
        }
        awaitClose { reg.remove() }
    }

    fun gente(pid: String, cid: String): Flow<List<Pessoa>> = callbackFlow {
        val reg = sala(pid, cid).collection("gente").addSnapshotListener { s, _ ->
            if (s != null) trySend(s.documents.map { Pessoa(it.id, it.getString("nome") ?: "", it.getLong("cor") ?: 0, it.getString("par") ?: "", it.getLong("em") ?: 0) })
        }
        awaitClose { reg.remove() }
    }

    fun chat(pid: String, cid: String): Flow<List<MsgVivo>> = callbackFlow {
        val reg = sala(pid, cid).collection("chat").orderBy("em").limitToLast(200).addSnapshotListener { s, _ ->
            if (s != null) trySend(s.documents.map { MsgVivo(it.id, it.getString("uid") ?: "", it.getString("nome") ?: "", it.getString("texto") ?: "", it.getLong("em") ?: 0) })
        }
        awaitClose { reg.remove() }
    }

    fun estouAqui(pid: String, cid: String, eu: String, nome: String, par: String) {
        sala(pid, cid).collection("gente").document(eu).set(mapOf("nome" to nome, "cor" to corDe(eu), "par" to par, "em" to System.currentTimeMillis()))
    }

    fun sair(pid: String, cid: String, eu: String) { sala(pid, cid).collection("gente").document(eu).delete() }

    /** Pega o parágrafo para escrever (trava por 25 s; renovar enquanto escreve). */
    fun travar(pid: String, cid: String, par: String, eu: String, nome: String) {
        sala(pid, cid).collection("pars").document(par).update(mapOf("editando" to eu, "nomeEditando" to nome, "ate" to System.currentTimeMillis() + 25_000))
    }

    fun soltar(pid: String, cid: String, par: String) {
        sala(pid, cid).collection("pars").document(par).update(mapOf("editando" to "", "ate" to 0L))
    }

    fun escrever(pid: String, cid: String, par: String, texto: String, eu: String, nome: String) {
        sala(pid, cid).collection("pars").document(par).update(mapOf("texto" to texto, "uid" to eu, "nome" to nome, "cor" to corDe(eu),
            "editando" to eu, "nomeEditando" to nome, "ate" to System.currentTimeMillis() + 25_000))
    }

    /** Novo parágrafo entre [antes] e [depois] (ordem fracionária: ninguém precisa renumerar). Devolve o id. */
    fun inserir(pid: String, cid: String, antes: Double, depois: Double?, texto: String, eu: String, nome: String): String {
        val ref = sala(pid, cid).collection("pars").document()
        val ordem = if (depois == null) antes + 1 else (antes + depois) / 2
        ref.set(mapOf("ordem" to ordem, "texto" to texto, "uid" to eu, "nome" to nome, "cor" to corDe(eu), "editando" to eu, "nomeEditando" to nome, "ate" to System.currentTimeMillis() + 25_000))
        return ref.id
    }

    fun apagar(pid: String, cid: String, par: String) { sala(pid, cid).collection("pars").document(par).delete() }

    fun mandar(pid: String, cid: String, eu: String, nome: String, texto: String) {
        sala(pid, cid).collection("chat").add(mapOf("uid" to eu, "nome" to nome, "texto" to texto.take(1000), "em" to System.currentTimeMillis()))
    }

    /** Texto da sala, na ordem (para o autor trazer para a obra). */
    suspend fun textoFinal(pid: String, cid: String): String = withContext(Dispatchers.IO) {
        sala(pid, cid).collection("pars").get().await().documents.sortedBy { it.getDouble("ordem") ?: 0.0 }
            .joinToString("\n\n") { it.getString("texto") ?: "" }
    }

}
