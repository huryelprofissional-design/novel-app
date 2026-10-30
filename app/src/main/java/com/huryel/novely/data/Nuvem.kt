package com.huryel.novely.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.room.withTransaction
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.CRC32

// ───────────────────────── Conta (login) ─────────────────────────

class Conta(private val ctx: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val _usuario = MutableStateFlow(auth.currentUser)
    val usuario: StateFlow<FirebaseUser?> = _usuario

    init { auth.addAuthStateListener { _usuario.value = it.currentUser } }

    private val idClienteWeb: String?
        get() = ctx.resources.getIdentifier("default_web_client_id", "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) }

    /** Cada função devolve null quando deu certo, ou a mensagem de erro em português. */
    suspend fun entrarEmail(email: String, senha: String): String? = tentar {
        auth.signInWithEmailAndPassword(email.trim(), senha).await()
    }

    suspend fun criarConta(nome: String, email: String, senha: String): String? = tentar {
        val r = auth.createUserWithEmailAndPassword(email.trim(), senha).await()
        if (nome.isNotBlank()) r.user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nome.trim()).build())?.await()
    }

    suspend fun recuperarSenha(email: String): String? = tentar { auth.sendPasswordResetEmail(email.trim()).await() }

    suspend fun entrarGoogle(atividade: Context): String? {
        val id = idClienteWeb ?: return "O login com Google ainda não foi configurado."
        return tentar {
            val pedido = GetCredentialRequest.Builder().addCredentialOption(GetSignInWithGoogleOption.Builder(id).build()).build()
            val r = CredentialManager.create(atividade).getCredential(atividade, pedido)
            val c = r.credential
            if (c is CustomCredential && c.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val g = GoogleIdTokenCredential.createFrom(c.data)
                auth.signInWithCredential(GoogleAuthProvider.getCredential(g.idToken, null)).await()
            } else throw IllegalStateException("resposta inesperada do Google")
        }
    }

    fun sair() = auth.signOut()

    private suspend fun tentar(acao: suspend () -> Unit): String? = try { acao(); null } catch (e: Exception) { traduzir(e) }

    private fun traduzir(e: Exception): String? = when (e) {
        is GetCredentialCancellationException -> null
        is NoCredentialException -> "Nenhuma conta Google encontrada neste aparelho."
        is FirebaseAuthWeakPasswordException -> "Senha fraca: use pelo menos 6 caracteres."
        is FirebaseAuthUserCollisionException -> "Já existe uma conta com esse e-mail. Toque em Entrar."
        is FirebaseAuthInvalidUserException -> "Não achei conta com esse e-mail."
        is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha incorretos."
        is FirebaseNetworkException -> "Sem internet. Conecte e tente de novo."
        else -> "Não deu certo: ${e.message ?: e.javaClass.simpleName}"
    }
}

// ───────────────────────── Sincronização ─────────────────────────

sealed interface EstadoSinc {
    data object Ocioso : EstadoSinc
    data class Sincronizando(val mensagem: String) : EstadoSinc
    data class Ok(val em: Long, val enviados: Int, val recebidos: Int) : EstadoSinc
    data class Erro(val mensagem: String) : EstadoSinc
}

/**
 * O banco do aparelho continua sendo o principal (funciona sem internet).
 * A cada sincronização: 1) envia o que mudou aqui (marcado pelos gatilhos do banco),
 * 2) envia as exclusões, 3) baixa o que mudou na nuvem desde a última vez.
 *
 * Na nuvem (só o dono lê/escreve):
 *   users/{uid}                      perfil
 *   users/{uid}/obras/{id}           dados da obra
 *   users/{uid}/itens/{id}           volumes, capítulos, fichas, eventos e marcadores (campo "tipo")
 *   users/{uid}/textos/{capId}[~n]   texto dos capítulos (dividido se for gigante)
 *   users/{uid}/imagens/{id}         capas, banners, fotos (comprimidas)
 */
class Sincronizador(private val ctx: Context, private val repo: Repositorio, private val prefs: Preferencias, private val conta: Conta) {
    private val trava = Mutex()
    private val _estado = MutableStateFlow<EstadoSinc>(EstadoSinc.Ocioso)
    val estado: StateFlow<EstadoSinc> = _estado
    private val dao get() = repo.dao
    private val sql get() = repo.banco.openHelper.writableDatabase

    companion object {
        private const val PARTE = 300_000
        private val ORDEM_TIPOS = listOf("volumes", "fichas", "capitulos", "eventos", "marcadores")
        fun configurar() {
            try {
                FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
            } catch (_: Exception) {}
        }
    }

    // ── estado local de cada item já enviado ──
    private class Item(var enviadoEm: Long = 0, var textoHash: String = "", var capaHash: String = "", var bannerHash: String = "")

    private fun lerSincronia(): HashMap<String, Item> {
        val m = HashMap<String, Item>()
        sql.query("SELECT chave, enviadoEm, textoHash, capaHash, bannerHash FROM sincronia").use { c ->
            while (c.moveToNext()) m[c.getString(0)] = Item(c.getLong(1), c.getString(2), c.getString(3), c.getString(4))
        }
        return m
    }
    private fun gravar(chave: String, i: Item) = sql.execSQL(
        "INSERT OR REPLACE INTO sincronia (chave, enviadoEm, textoHash, capaHash, bannerHash) VALUES (?, ?, ?, ?, ?)",
        arrayOf<Any>(chave, i.enviadoEm, i.textoHash, i.capaHash, i.bannerHash))

    /** Grava sem disparar os gatilhos (para não reenviar o que acabou de chegar). */
    private suspend fun <T> quieto(bloco: suspend () -> T): T = repo.banco.withTransaction {
        sql.execSQL("UPDATE sync_flag SET ativo = 1 WHERE id = 0")
        try { bloco() } finally { sql.execSQL("UPDATE sync_flag SET ativo = 0 WHERE id = 0") }
    }

    fun hash(f: File?): String {
        if (f == null || !f.exists()) return ""
        val crc = CRC32()
        f.inputStream().use { ins -> val b = ByteArray(64 * 1024); while (true) { val r = ins.read(b); if (r <= 0) break; crc.update(b, 0, r) } }
        return "%08x-%d".format(crc.value, f.length())
    }
    private fun hash(s: String): String { val b = s.toByteArray(Charsets.UTF_8); val c = CRC32(); c.update(b); return "%08x-%d".format(c.value, b.size) }

    /** Lote de escritas (o Firestore aceita até 500 por vez). */
    private inner class Lote(private val db: FirebaseFirestore) {
        private var b: WriteBatch = db.batch(); private var n = 0
        suspend fun set(ref: DocumentReference, dados: Map<String, Any?>, juntar: Boolean = false) {
            if (juntar) b.set(ref, dados, SetOptions.merge()) else b.set(ref, dados)
            if (++n >= 400) fechar()
        }
        suspend fun fechar() { if (n > 0) { b.commit().await(); b = db.batch(); n = 0 } }
    }

    suspend fun sincronizar(): Unit = trava.withLock {
        val uid = conta.usuario.value?.uid ?: return@withLock
        withContext(Dispatchers.IO) {
            try {
                _estado.value = EstadoSinc.Sincronizando("Enviando…")
                val db = FirebaseFirestore.getInstance()
                val eu = db.collection("users").document(uid)
                val enviados = enviar(db, eu)
                _estado.value = EstadoSinc.Sincronizando("Baixando…")
                val recebidos = receber(eu)
                perfil(eu)
                _estado.value = EstadoSinc.Ok(System.currentTimeMillis(), enviados, recebidos)
                prefs.ultimaSinc.definir(System.currentTimeMillis())
            } catch (e: Exception) {
                _estado.value = EstadoSinc.Erro(when (e) {
                    is FirebaseNetworkException -> "Sem internet — tudo continua salvo no aparelho e sobe depois."
                    else -> if (e.message?.contains("PERMISSION_DENIED") == true) "A nuvem recusou o acesso (confira as Regras do Firestore)."
                    else "A sincronização parou: ${e.message ?: e.javaClass.simpleName}"
                })
            }
        }
    }

    // ───────── ENVIAR ─────────

    private suspend fun enviar(db: FirebaseFirestore, eu: DocumentReference): Int {
        val sinc = lerSincronia()
        val lote = Lote(db)
        var n = 0
        val obras = dao.sincObras(); val volumes = dao.sincVolumes(); val caps = dao.sincCapitulos()
        val nidObra = obras.associate { it.id to it.nuvemId }
        val nidVol = volumes.associate { it.id to it.nuvemId }
        val nidCap = caps.associate { it.id to it.nuvemId }
        fun item(tab: String, nid: String) = sinc.getOrPut("$tab:$nid") { Item() }
        suspend fun imagem(nome: String, caminho: String?, lado: Int, atual: String): String {
            val h = hash(caminho?.let { File(it) })
            if (h == atual) return h
            val bytes = repo.imagens.comprimir(caminho, lado)
            if (bytes != null) eu.collection("imagens").document(nome).set(mapOf("dados" to Blob.fromBytes(bytes))).await()
            return h
        }
        val servidor = FieldValue.serverTimestamp()

        for (o in obras) {
            val e = item("obras", o.nuvemId)
            val capaH = imagem("${o.nuvemId}-capa", o.capa, 900, e.capaHash)
            val banH = imagem("${o.nuvemId}-banner", o.banner, 1400, e.bannerHash)
            if (o.alteradoEm > e.enviadoEm || capaH != e.capaHash || banH != e.bannerHash) {
                lote.set(eu.collection("obras").document(o.nuvemId), mapOf(
                    "apagado" to false, "alteradoEm" to o.alteradoEm, "servidorEm" to servidor, "capaHash" to capaH, "bannerHash" to banH,
                    "titulo" to o.titulo, "descricao" to o.descricao, "status" to o.status, "capaX" to o.capaX, "capaY" to o.capaY, "capaZoom" to o.capaZoom,
                    "bannerX" to o.bannerX, "bannerY" to o.bannerY, "bannerZoom" to o.bannerZoom, "corBase" to o.corBase, "criadaEm" to o.criadaEm,
                    "atualizadaEm" to o.atualizadaEm, "lidaEm" to o.lidaEm, "colecao" to o.colecao, "minha" to o.minha, "trancada" to o.trancada, "autor" to o.autor,
                    "ultimoCap" to (o.ultimoCapituloId?.let { nidCap[it] } ?: ""),
                    "publicada" to o.publicada, "publicaId" to o.publicaId, "generos" to o.generos, "tagsPub" to o.tagsPub,
                    "classificacao" to o.classificacao, "idioma" to o.idioma,
                ))
                e.enviadoEm = o.alteradoEm; e.capaHash = capaH; e.bannerHash = banH; n++
            }
        }
        lote.fechar()
        suspend fun itemDoc(tipo: String, nid: String, alterado: Long, obra: String, dados: Map<String, Any?>) {
            lote.set(eu.collection("itens").document(nid), mapOf("tipo" to tipo, "obra" to obra, "apagado" to false, "alteradoEm" to alterado,
                "servidorEm" to servidor, "dados" to dados))
            n++
        }
        for (v in volumes) {
            val e = item("volumes", v.nuvemId)
            if (v.alteradoEm > e.enviadoEm) { itemDoc("volumes", v.nuvemId, v.alteradoEm, nidObra[v.obraId] ?: continue, mapOf("titulo" to v.titulo, "ordem" to v.ordem)); e.enviadoEm = v.alteradoEm }
        }
        for (f in dao.sincFichas()) {
            val e = item("fichas", f.nuvemId)
            val imgH = imagem("${f.nuvemId}-foto", f.imagem, 500, e.capaHash)
            if (f.alteradoEm > e.enviadoEm || imgH != e.capaHash) {
                itemDoc("fichas", f.nuvemId, f.alteradoEm, nidObra[f.obraId] ?: continue, mapOf("tipo" to f.tipo, "nome" to f.nome, "descricao" to f.descricao,
                    "criadaEm" to f.criadaEm, "apelidos" to f.apelidos, "idade" to f.idade, "aparencia" to f.aparencia, "poder" to f.poder,
                    "faccao" to f.faccao, "relacoes" to f.relacoes, "imagemHash" to imgH))
                e.enviadoEm = f.alteradoEm; e.capaHash = imgH
            }
        }
        for (c in caps) {
            val e = item("capitulos", c.nuvemId)
            val arq = repo.textos.arquivo(c.id)
            val h = hash(arq)
            var partes = -1
            if (h != e.textoHash) {
                // texto primeiro: quem baixar o capítulo já encontra o texto lá
                val texto = if (arq.exists()) arq.readText(Charsets.UTF_8) else ""
                val pedacos = if (texto.isEmpty()) listOf("") else texto.chunked(PARTE)
                pedacos.forEachIndexed { i, t ->
                    eu.collection("textos").document(if (i == 0) c.nuvemId else "${c.nuvemId}~$i").set(mapOf("texto" to t)).await()
                }
                partes = pedacos.size
            }
            if (c.alteradoEm > e.enviadoEm || partes > 0) {
                val dados = mutableMapOf<String, Any?>("titulo" to c.titulo, "ordem" to c.ordem, "volume" to (c.volumeId?.let { nidVol[it] } ?: ""),
                    "rascunho" to c.rascunho, "palavras" to c.palavras, "progresso" to c.progresso, "posicaoBloco" to c.posicaoBloco,
                    "posicaoDeslocamento" to c.posicaoDeslocamento, "atualizadoEm" to c.atualizadoEm, "resumo" to c.resumo, "notas" to c.notas,
                    "tags" to c.tags, "textoHash" to h, "publicaId" to c.publicaId, "publicadoEm" to c.publicadoEm, "publicadoHash" to c.publicadoHash)
                if (partes > 0) dados["partes"] = partes
                lote.set(eu.collection("itens").document(c.nuvemId), mapOf("tipo" to "capitulos", "obra" to (nidObra[c.obraId] ?: continue),
                    "apagado" to false, "alteradoEm" to c.alteradoEm, "servidorEm" to servidor, "dados" to dados), juntar = true)
                n++
                e.enviadoEm = c.alteradoEm; e.textoHash = h
            }
        }
        for (ev in dao.sincEventos()) {
            val e = item("eventos", ev.nuvemId)
            if (ev.alteradoEm > e.enviadoEm) {
                itemDoc("eventos", ev.nuvemId, ev.alteradoEm, nidObra[ev.obraId] ?: continue, mapOf("cap" to (ev.capId?.let { nidCap[it] } ?: ""),
                    "quando" to ev.quando, "titulo" to ev.titulo, "descricao" to ev.descricao, "ordem" to ev.ordem))
                e.enviadoEm = ev.alteradoEm
            }
        }
        for (m in dao.sincMarcadores()) {
            val e = item("marcadores", m.nuvemId)
            if (m.alteradoEm > e.enviadoEm) {
                itemDoc("marcadores", m.nuvemId, m.alteradoEm, nidObra[m.obraId] ?: continue, mapOf("cap" to (nidCap[m.capId] ?: continue),
                    "bloco" to m.bloco, "trecho" to m.trecho, "cor" to m.cor, "nota" to m.nota, "criadoEm" to m.criadoEm))
                e.enviadoEm = m.alteradoEm
            }
        }
        // exclusões
        val apagar = ArrayList<Pair<String, String>>()
        sql.query("SELECT nuvemId, tipo FROM exclusoes").use { c -> while (c.moveToNext()) apagar.add(c.getString(0) to c.getString(1)) }
        for ((nid, tipo) in apagar) {
            val ref = if (tipo == "obras") eu.collection("obras").document(nid) else eu.collection("itens").document(nid)
            lote.set(ref, mapOf("apagado" to true, "tipo" to tipo, "servidorEm" to servidor, "alteradoEm" to System.currentTimeMillis()), juntar = true)
            n++
        }
        lote.fechar()
        for ((nid, tipo) in apagar) { sql.execSQL("DELETE FROM exclusoes WHERE nuvemId = ?", arrayOf(nid)); sinc.remove("$tipo:$nid") }
        sinc.forEach { (k, v) -> gravar(k, v) }
        return n
    }

    // ───────── RECEBER ─────────

    private fun lerUltimo(): Timestamp {
        val p = prefs.ultimoPull.valor.split(':')
        return if (p.size == 2) Timestamp(p[0].toLongOrNull() ?: 0, p[1].toIntOrNull() ?: 0) else Timestamp(0, 0)
    }

    private suspend fun receber(eu: DocumentReference): Int {
        val desde = lerUltimo()
        val obras = eu.collection("obras").whereGreaterThan("servidorEm", desde).orderBy("servidorEm").get().await().documents
        val itens = eu.collection("itens").whereGreaterThan("servidorEm", desde).orderBy("servidorEm").get().await().documents
        if (obras.isEmpty() && itens.isEmpty()) return 0
        val sinc = lerSincronia()
        var n = 0
        var maior = desde
        fun visto(d: DocumentSnapshot) { d.getTimestamp("servidorEm")?.let { if (it > maior) maior = it } }

        for (d in obras) { visto(d); if (aplicarObra(eu, d, sinc)) n++ }
        val porTipo = itens.groupBy { it.getString("tipo") ?: "" }
        for (t in ORDEM_TIPOS) for (d in porTipo[t].orEmpty()) { visto(d); if (aplicarItem(eu, t, d, sinc)) n++ }
        // ponteiro "continuar lendo" depois que os capítulos existem
        for (d in obras) {
            val cap = d.getString("ultimoCap")?.takeIf { it.isNotEmpty() } ?: continue
            val o = dao.obraNuvem(d.id) ?: continue
            val c = dao.capituloNuvem(cap) ?: continue
            if (o.ultimoCapituloId != c.id) quieto { dao.atualizarObra(o.copy(ultimoCapituloId = c.id)) }
        }
        sinc.forEach { (k, v) -> gravar(k, v) }
        prefs.ultimoPull.definir("${maior.seconds}:${maior.nanoseconds}")
        return n
    }

    private suspend fun baixarImagem(eu: DocumentReference, nome: String): String? = try {
        eu.collection("imagens").document(nome).get().await().getBlob("dados")?.toBytes()?.let { repo.imagens.salvarBytes(it) }
    } catch (_: Exception) { null }

    private suspend fun aplicarObra(eu: DocumentReference, d: DocumentSnapshot, sinc: HashMap<String, Item>): Boolean {
        val nid = d.id
        val local = dao.obraNuvem(nid)
        val chave = "obras:$nid"
        if (d.getBoolean("apagado") == true) {
            if (local != null) {
                val caps = dao.capitulosAgora(local.id)
                quieto { dao.apagarObraNuvem(nid) }
                caps.forEach { repo.textos.apagar(it.id) }
                repo.imagens.apagar(local.capa); repo.imagens.apagar(local.banner)
            }
            sinc.remove(chave); sql.execSQL("DELETE FROM sincronia WHERE chave = ?", arrayOf(chave))
            return local != null
        }
        val alt = d.getLong("alteradoEm") ?: 0
        val e = sinc.getOrPut(chave) { Item() }
        if (local != null && local.alteradoEm == alt) { e.enviadoEm = alt; return false }
        if (local != null && local.alteradoEm > e.enviadoEm && local.alteradoEm > alt) return false // mudou aqui depois: vale a daqui
        val capaH = d.getString("capaHash") ?: ""
        val banH = d.getString("bannerHash") ?: ""
        var capa = local?.capa; var banner = local?.banner
        if (capaH != e.capaHash) { repo.imagens.apagar(capa); capa = if (capaH.isEmpty()) null else baixarImagem(eu, "$nid-capa"); e.capaHash = capaH }
        if (banH != e.bannerHash) { repo.imagens.apagar(banner); banner = if (banH.isEmpty()) null else baixarImagem(eu, "$nid-banner"); e.bannerHash = banH }
        val o = Obra(
            id = local?.id ?: 0, titulo = d.getString("titulo") ?: "Sem título", descricao = d.getString("descricao") ?: "", status = d.getString("status") ?: StatusObra.LENDO,
            capa = capa, capaX = (d.getDouble("capaX") ?: .5).toFloat(), capaY = (d.getDouble("capaY") ?: .5).toFloat(), capaZoom = (d.getDouble("capaZoom") ?: 1.0).toFloat(),
            banner = banner, bannerX = (d.getDouble("bannerX") ?: .5).toFloat(), bannerY = (d.getDouble("bannerY") ?: .5).toFloat(), bannerZoom = (d.getDouble("bannerZoom") ?: 1.0).toFloat(),
            corBase = d.getLong("corBase") ?: 0xFF3B2A4A, ultimoCapituloId = local?.ultimoCapituloId, criadaEm = d.getLong("criadaEm") ?: System.currentTimeMillis(),
            atualizadaEm = d.getLong("atualizadaEm") ?: System.currentTimeMillis(), lidaEm = d.getLong("lidaEm") ?: 0, colecao = d.getString("colecao") ?: "",
            minha = d.getBoolean("minha") ?: true, trancada = d.getBoolean("trancada") ?: false, autor = d.getString("autor") ?: "",
            publicada = d.getBoolean("publicada") ?: false, publicaId = d.getString("publicaId") ?: "", generos = d.getString("generos") ?: "",
            tagsPub = d.getString("tagsPub") ?: "", classificacao = d.getString("classificacao") ?: "LIVRE", idioma = d.getString("idioma") ?: "pt",
            nuvemId = nid, alteradoEm = alt,
        )
        quieto { if (local == null) dao.inserirObra(o) else dao.atualizarObra(o) }
        e.enviadoEm = alt
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun aplicarItem(eu: DocumentReference, tipo: String, d: DocumentSnapshot, sinc: HashMap<String, Item>): Boolean {
        val nid = d.id
        val chave = "$tipo:$nid"
        if (d.getBoolean("apagado") == true) {
            val existia = when (tipo) {
                "volumes" -> dao.volumeNuvem(nid)?.also { quieto { dao.apagarVolumeNuvem(nid) } } != null
                "fichas" -> dao.fichaNuvem(nid)?.also { repo.imagens.apagar(it.imagem); quieto { dao.apagarFichaNuvem(nid) } } != null
                "capitulos" -> dao.capituloNuvem(nid)?.also { c -> quieto { dao.apagarCapituloNuvem(nid) }; repo.textos.apagar(c.id) } != null
                "eventos" -> dao.eventoNuvem(nid)?.also { quieto { dao.apagarEventoNuvem(nid) } } != null
                else -> dao.marcadorNuvem(nid)?.also { quieto { dao.apagarMarcadorNuvem(nid) } } != null
            }
            sinc.remove(chave); sql.execSQL("DELETE FROM sincronia WHERE chave = ?", arrayOf(chave))
            return existia
        }
        val obra = dao.obraNuvem(d.getString("obra") ?: "") ?: return false
        val alt = d.getLong("alteradoEm") ?: 0
        val x = d.get("dados") as? Map<String, Any?> ?: return false
        fun s(k: String) = x[k] as? String ?: ""
        fun l(k: String) = (x[k] as? Number)?.toLong() ?: 0L
        fun dbl(k: String) = (x[k] as? Number)?.toDouble() ?: 0.0
        fun b(k: String) = x[k] as? Boolean ?: false
        val e = sinc.getOrPut(chave) { Item() }

        when (tipo) {
            "volumes" -> {
                val local = dao.volumeNuvem(nid)
                if (local != null && (local.alteradoEm == alt || (local.alteradoEm > e.enviadoEm && local.alteradoEm > alt))) { if (local.alteradoEm == alt) e.enviadoEm = alt; return false }
                val v = Volume(id = local?.id ?: 0, obraId = obra.id, titulo = s("titulo"), ordem = dbl("ordem"), nuvemId = nid, alteradoEm = alt)
                quieto { if (local == null) dao.inserirVolumeS(v) else dao.atualizarVolumeS(v) }
            }
            "fichas" -> {
                // ficha com o mesmo nome criada aqui pelo @: adota a identidade da nuvem em vez de duplicar
                val local = dao.fichaNuvem(nid) ?: dao.fichasAgora(obra.id).firstOrNull { it.nome.equals(s("nome"), true) && it.nuvemId != nid }
                if (local != null && local.nuvemId == nid && (local.alteradoEm == alt || (local.alteradoEm > e.enviadoEm && local.alteradoEm > alt))) { if (local.alteradoEm == alt) e.enviadoEm = alt; return false }
                var img = local?.imagem
                val imgH = s("imagemHash")
                if (imgH != e.capaHash) { repo.imagens.apagar(img); img = if (imgH.isEmpty()) null else baixarImagem(eu, "$nid-foto"); e.capaHash = imgH }
                val f = Ficha(id = local?.id ?: 0, obraId = obra.id, tipo = s("tipo").ifEmpty { TipoFicha.PERSONAGEM }, nome = s("nome"), descricao = s("descricao"),
                    imagem = img, criadaEm = l("criadaEm"), apelidos = s("apelidos"), idade = s("idade"), aparencia = s("aparencia"), poder = s("poder"),
                    faccao = s("faccao"), relacoes = s("relacoes"), nuvemId = nid, alteradoEm = alt)
                quieto { if (local == null) dao.inserirFicha(f) else dao.atualizarFicha(f) }
            }
            "capitulos" -> {
                val local = dao.capituloNuvem(nid)
                val remHash = s("textoHash")
                var textoNovo: String? = null
                if (remHash != e.textoHash) {
                    val partes = (x["partes"] as? Number)?.toInt() ?: 1
                    val sb = StringBuilder()
                    for (i in 0 until partes) sb.append(eu.collection("textos").document(if (i == 0) nid else "$nid~$i").get().await().getString("texto") ?: "")
                    val remoto = sb.toString()
                    if (local != null) {
                        val locHash = hash(repo.textos.arquivo(local.id))
                        if (locHash != e.textoHash && locHash != remHash) {
                            // os dois aparelhos mudaram o texto: mantém o daqui, guarda o outro no histórico e avisa
                            repo.textos.gravarVersao(local.id, remoto)
                            prefs.adicionarConflito(local.id, local.titulo)
                        } else textoNovo = remoto
                    } else textoNovo = remoto
                    e.textoHash = remHash
                }
                val metaIgual = local != null && local.alteradoEm == alt
                val localVence = local != null && local.alteradoEm > e.enviadoEm && local.alteradoEm > alt
                var id = local?.id ?: 0
                if (!metaIgual && !localVence) {
                    val vol = s("volume").takeIf { it.isNotEmpty() }?.let { dao.volumeNuvem(it)?.id }
                    val c = Capitulo(id = id, obraId = obra.id, volumeId = vol, titulo = s("titulo"), ordem = dbl("ordem"), rascunho = b("rascunho"),
                        palavras = l("palavras").toInt(), progresso = dbl("progresso").toFloat(), posicaoBloco = l("posicaoBloco").toInt(),
                        posicaoDeslocamento = l("posicaoDeslocamento").toInt(), atualizadoEm = l("atualizadoEm"), resumo = s("resumo"),
                        notas = s("notas"), tags = s("tags"), publicaId = s("publicaId"), publicadoEm = l("publicadoEm"), publicadoHash = s("publicadoHash"),
                        nuvemId = nid, alteradoEm = alt)
                    quieto { if (local == null) id = dao.inserirCapituloS(c) else dao.atualizarCapituloS(c) }
                    e.enviadoEm = alt
                } else if (metaIgual) e.enviadoEm = alt
                if (textoNovo != null && id != 0L) {
                    repo.textos.gravar(id, textoNovo)
                    dao.capituloAgora(id)?.let { try { repo.indexarFichas(it, textoNovo) } catch (_: Exception) {} }
                }
                return !metaIgual || textoNovo != null
            }
            "eventos" -> {
                val local = dao.eventoNuvem(nid)
                if (local != null && (local.alteradoEm == alt || (local.alteradoEm > e.enviadoEm && local.alteradoEm > alt))) { if (local.alteradoEm == alt) e.enviadoEm = alt; return false }
                val cap = s("cap").takeIf { it.isNotEmpty() }?.let { dao.capituloNuvem(it)?.id }
                val ev = Evento(id = local?.id ?: 0, obraId = obra.id, capId = cap, quando = s("quando"), titulo = s("titulo"), descricao = s("descricao"),
                    ordem = dbl("ordem"), nuvemId = nid, alteradoEm = alt)
                quieto { if (local == null) dao.inserirEventoS(ev) else dao.atualizarEventoS(ev) }
            }
            else -> {
                val local = dao.marcadorNuvem(nid)
                if (local != null && (local.alteradoEm == alt || (local.alteradoEm > e.enviadoEm && local.alteradoEm > alt))) { if (local.alteradoEm == alt) e.enviadoEm = alt; return false }
                val cap = dao.capituloNuvem(s("cap"))?.id ?: return false
                val m = Marcador(id = local?.id ?: 0, obraId = obra.id, capId = cap, bloco = l("bloco").toInt(), trecho = s("trecho"), cor = s("cor"),
                    nota = s("nota"), criadoEm = l("criadoEm"), nuvemId = nid, alteradoEm = alt)
                quieto { if (local == null) dao.inserirMarcadorS(m) else dao.atualizarMarcadorS(m) }
            }
        }
        e.enviadoEm = alt
        return true
    }

    // ───────── PERFIL ─────────

    private suspend fun perfil(eu: DocumentReference) {
        val remoto = eu.get().await()
        val altRemoto = remoto.getLong("alteradoEm") ?: 0
        val altLocal = prefs.perfilAlteradoEm.valor
        if (remoto.exists() && altRemoto > altLocal) {
            prefs.autorNome.definir(remoto.getString("nome") ?: "")
            prefs.bio.definir(remoto.getString("bio") ?: "")
            val fh = remoto.getString("fotoHash") ?: ""
            if (fh != prefs.fotoHash.valor) {
                val antigo = prefs.fotoPerfil.valor
                val nova = if (fh.isEmpty()) "" else baixarImagem(eu, "perfil") ?: ""
                prefs.fotoPerfil.definir(nova); prefs.fotoHash.definir(fh)
                if (antigo.isNotEmpty() && antigo != nova) File(antigo).delete()
            }
            prefs.perfilAlteradoEm.definir(altRemoto)
        } else if (altLocal > altRemoto || !remoto.exists()) {
            val nome = prefs.autorNome.valor.ifBlank { conta.usuario.value?.displayName ?: "" }
            if (prefs.autorNome.valor.isBlank() && nome.isNotBlank()) prefs.autorNome.definir(nome)
            val foto = prefs.fotoPerfil.valor.takeIf { it.isNotEmpty() }
            val fh = hash(foto?.let { File(it) })
            if (fh != prefs.fotoHash.valor && foto != null) {
                repo.imagens.comprimir(foto, 400)?.let { eu.collection("imagens").document("perfil").set(mapOf("dados" to Blob.fromBytes(it))).await() }
                prefs.fotoHash.definir(fh)
            }
            val alt = altLocal.coerceAtLeast(1)
            eu.set(mapOf("nome" to nome, "bio" to prefs.bio.valor, "fotoHash" to fh, "alteradoEm" to alt, "email" to (conta.usuario.value?.email ?: ""))).await()
            prefs.perfilAlteradoEm.definir(alt)
        }
    }
}
