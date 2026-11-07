package com.huryel.novely.data

import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Aparelhos conectados à conta (users/{uid}/aparelhos/{id}): cada um avisa que está vivo na sincronização.
 * "Desconectar" marca sair = true; o aparelho, na próxima vez que sincroniza, sai da conta sozinho.
 */
object Aparelhos {
    data class Info(val id: String, val nome: String, val build: Int, val visto: Long, val esteAqui: Boolean)

    private fun col() = FirebaseAuth.getInstance().currentUser?.uid?.let { FirebaseFirestore.getInstance().collection("users").document(it).collection("aparelhos") }

    fun meuId(prefs: Preferencias): String = prefs.aparelhoId.valor.ifEmpty { novoNuvemId().take(16).also { prefs.aparelhoId.definir(it) } }

    /** Atualiza "visto agora" deste aparelho. Devolve true se outro aparelho pediu para desconectar este. */
    suspend fun registrar(prefs: Preferencias, build: Int): Boolean = withContext(Dispatchers.IO) {
        val c = col() ?: return@withContext false
        val doc = c.document(meuId(prefs))
        val atual = try { doc.get().await() } catch (_: Exception) { return@withContext false }
        if (atual.getBoolean("sair") == true) { try { doc.delete().await() } catch (_: Exception) {}; return@withContext true }
        try {
            doc.set(mapOf("nome" to "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}", "build" to build, "visto" to System.currentTimeMillis(), "sair" to false)).await()
        } catch (_: Exception) {}
        false
    }

    suspend fun lista(prefs: Preferencias): List<Info> = withContext(Dispatchers.IO) {
        val c = col() ?: return@withContext emptyList()
        val eu = meuId(prefs)
        c.get().await().documents.filter { it.getBoolean("sair") != true }.map { d ->
            Info(d.id, d.getString("nome") ?: "Aparelho", (d.getLong("build") ?: 0L).toInt(), d.getLong("visto") ?: 0L, d.id == eu)
        }.sortedWith(compareByDescending<Info> { it.esteAqui }.thenByDescending { it.visto })
    }

    suspend fun desconectar(id: String) = withContext(Dispatchers.IO) { col()?.document(id)?.update("sair", true)?.await(); Unit }
}
