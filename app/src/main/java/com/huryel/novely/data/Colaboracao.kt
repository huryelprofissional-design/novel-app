package com.huryel.novely.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class Membro(val uid: String, val nome: String, val papel: String, val em: Long)
data class CapRevisao(val id: String, val titulo: String, val ordem: Double, val enviadoEm: Long, val partes: Int, val notas: Long)
data class Nota(val id: String, val uid: String, val nome: String, val trecho: String, val texto: String, val em: Long)
data class Sugestao(val id: String, val uid: String, val nome: String, val titulo: String, val texto: String, val em: Long)
data class MinhaColab(val pid: String, val titulo: String, val papel: String, val autor: String)

/**
 * Coautores e revisores (por convite com código):
 *   convites/{código}                       pid, papel, autorUid
 *   colabs/{obra}                           autorUid, título (id = nuvemId da obra do autor)
 *   colabs/{obra}/membros/{uid}             papel COAUTOR | REVISOR
 *   colabs/{obra}/revisao/{cap} + notas     capítulos enviados para revisão e as notas
 *   colabs/{obra}/textos/{cap}[~n]          texto dos capítulos em revisão
 *   colabs/{obra}/sugestoes/{id}            capítulos sugeridos por coautores
 * O texto da obra continua só do autor; nada aqui edita o original sem ele aceitar.
 */
class Colaboracao(private val repo: Repositorio, private val prefs: Preferencias, private val conta: Conta) {
    private val db get() = FirebaseFirestore.getInstance()
    private val col get() = db.collection("colabs")
    private val uid get() = conta.usuario.value?.uid ?: ""
    private val meuNome get() = prefs.autorNome.valor.ifBlank { conta.usuario.value?.displayName ?: "Sem nome" }
    private companion object { const val PARTE = 300_000; const val LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" }

    val meuUid get() = uid

    private suspend fun garantir(o: Obra) {
        col.document(o.nuvemId).set(mapOf("autorUid" to uid, "titulo" to o.titulo, "autorNome" to meuNome), SetOptions.merge()).await()
    }

    suspend fun convidar(o: Obra, papel: String): String = withContext(Dispatchers.IO) {
        garantir(o)
        val codigo = (1..6).map { LETRAS.random() }.joinToString("")
        db.collection("convites").document(codigo).set(mapOf("pid" to o.nuvemId, "papel" to papel, "autorUid" to uid, "titulo" to o.titulo,
            "autorNome" to meuNome, "criadoEm" to System.currentTimeMillis())).await()
        codigo
    }

    suspend fun membros(pid: String): List<Membro> = withContext(Dispatchers.IO) {
        try {
            col.document(pid).collection("membros").get().await().documents.map {
                Membro(it.id, it.getString("nome") ?: "", it.getString("papel") ?: "REVISOR", it.getLong("em") ?: 0)
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun remover(pid: String, membro: String) = withContext(Dispatchers.IO) { col.document(pid).collection("membros").document(membro).delete().await() }

    /** Entra numa obra com o código recebido. Devolve null se deu certo ou a mensagem de erro. */
    suspend fun entrar(codigoDigitado: String): String? = withContext(Dispatchers.IO) {
        try {
            val codigo = codigoDigitado.trim().uppercase()
            val c = db.collection("convites").document(codigo).get().await()
            if (!c.exists()) return@withContext "Código não encontrado. Confira as letras."
            val pid = c.getString("pid") ?: return@withContext "Convite inválido."
            if (c.getString("autorUid") == uid) return@withContext "Esse convite é seu — mande para outra pessoa."
            val papel = c.getString("papel") ?: "REVISOR"
            col.document(pid).collection("membros").document(uid).set(mapOf("papel" to papel, "codigo" to codigo, "nome" to meuNome, "em" to System.currentTimeMillis())).await()
            salvarMinha(MinhaColab(pid, c.getString("titulo") ?: "", papel, c.getString("autorNome") ?: ""))
            null
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED") == true) "A nuvem recusou (faltam as Regras novas do Firestore)." else "Não deu: ${e.message}"
        }
    }

    fun minhas(): List<MinhaColab> = prefs.colabs.valor.split('\n').filter { it.isNotBlank() }.mapNotNull { l ->
        l.split('|').takeIf { it.size >= 4 }?.let { MinhaColab(it[0], it[1], it[2], it[3]) }
    }
    private fun salvarMinha(m: MinhaColab) {
        val l = minhas().filter { it.pid != m.pid } + m
        prefs.colabs.definir(l.joinToString("\n") { "${it.pid}|${it.titulo.replace('|', ' ')}|${it.papel}|${it.autor.replace('|', ' ')}" })
    }
    suspend fun sair(pid: String) = withContext(Dispatchers.IO) {
        try { col.document(pid).collection("membros").document(uid).delete().await() } catch (_: Exception) {}
        prefs.colabs.definir(minhas().filter { it.pid != pid }.joinToString("\n") { "${it.pid}|${it.titulo}|${it.papel}|${it.autor}" })
    }

    // ── revisão ──

    suspend fun enviarRevisao(o: Obra, caps: List<Capitulo>) = withContext(Dispatchers.IO) {
        garantir(o)
        val ref = col.document(o.nuvemId)
        for (c in caps) {
            val texto = repo.textos.ler(c.id)
            val pedacos = if (texto.isEmpty()) listOf("") else texto.chunked(PARTE)
            pedacos.forEachIndexed { i, t -> ref.collection("textos").document(if (i == 0) c.nuvemId else "${c.nuvemId}~$i").set(mapOf("texto" to t)).await() }
            ref.collection("revisao").document(c.nuvemId).set(mapOf("titulo" to c.titulo, "ordem" to c.ordem, "enviadoEm" to System.currentTimeMillis(),
                "partes" to pedacos.size), SetOptions.merge()).await()
        }
    }

    suspend fun tirarRevisao(pid: String, cid: String) = withContext(Dispatchers.IO) { col.document(pid).collection("revisao").document(cid).delete().await() }

    suspend fun capitulosRevisao(pid: String): List<CapRevisao> = withContext(Dispatchers.IO) {
        col.document(pid).collection("revisao").get().await().documents.map { d ->
            val n = try { d.reference.collection("notas").get().await().size().toLong() } catch (_: Exception) { 0L }
            CapRevisao(d.id, d.getString("titulo") ?: "", d.getDouble("ordem") ?: 0.0, d.getLong("enviadoEm") ?: 0, (d.getLong("partes") ?: 1).toInt(), n)
        }.sortedBy { it.ordem }
    }

    suspend fun textoRevisao(pid: String, c: CapRevisao): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        for (i in 0 until c.partes) sb.append(col.document(pid).collection("textos").document(if (i == 0) c.id else "${c.id}~$i").get().await().getString("texto") ?: "")
        sb.toString()
    }

    suspend fun notas(pid: String, cid: String): List<Nota> = withContext(Dispatchers.IO) {
        col.document(pid).collection("revisao").document(cid).collection("notas").orderBy("em", Query.Direction.ASCENDING).get().await().documents.map {
            Nota(it.id, it.getString("uid") ?: "", it.getString("nome") ?: "", it.getString("trecho") ?: "", it.getString("texto") ?: "", it.getLong("em") ?: 0)
        }
    }

    suspend fun anotar(pid: String, cid: String, trecho: String, texto: String) = withContext(Dispatchers.IO) {
        col.document(pid).collection("revisao").document(cid).collection("notas").add(mapOf("uid" to uid, "nome" to meuNome, "trecho" to trecho.take(600),
            "texto" to texto.trim().take(3000), "em" to System.currentTimeMillis())).await()
    }

    suspend fun apagarNota(pid: String, cid: String, n: Nota) = withContext(Dispatchers.IO) {
        col.document(pid).collection("revisao").document(cid).collection("notas").document(n.id).delete().await()
    }

    // ── sugestões de coautor ──

    suspend fun sugerir(pid: String, titulo: String, texto: String) = withContext(Dispatchers.IO) {
        col.document(pid).collection("sugestoes").add(mapOf("uid" to uid, "nome" to meuNome, "titulo" to titulo.trim(), "texto" to texto.take(PARTE),
            "em" to System.currentTimeMillis())).await()
    }

    suspend fun sugestoes(pid: String): List<Sugestao> = withContext(Dispatchers.IO) {
        try {
            col.document(pid).collection("sugestoes").orderBy("em", Query.Direction.DESCENDING).get().await().documents.map {
                Sugestao(it.id, it.getString("uid") ?: "", it.getString("nome") ?: "", it.getString("titulo") ?: "", it.getString("texto") ?: "", it.getLong("em") ?: 0)
            }
        } catch (_: Exception) { emptyList() }
    }

    /** Vira um capítulo novo no fim da obra (rascunho) e some da lista. */
    suspend fun aceitar(o: Obra, s: Sugestao): Long = withContext(Dispatchers.IO) {
        val id = repo.novoCapitulo(o.id, s.titulo.ifBlank { "Sugestão de ${s.nome}" }, null)
        repo.dao.capituloAgora(id)?.let { repo.gravarTexto(it, s.texto) }
        col.document(o.nuvemId).collection("sugestoes").document(s.id).delete().await()
        id
    }

    suspend fun descartar(pid: String, s: Sugestao) = withContext(Dispatchers.IO) { col.document(pid).collection("sugestoes").document(s.id).delete().await() }
}
