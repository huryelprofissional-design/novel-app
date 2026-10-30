package com.huryel.novely.data

import android.content.Context
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

/** Obra no catálogo público. */
data class ObraPublica(
    val id: String, val autorUid: String, val autorNome: String, val titulo: String, val sinopse: String,
    val generos: List<String>, val tags: List<String>, val classificacao: String, val idioma: String, val status: String,
    val capaMini: ByteArray?, val leituras: Long, val leiturasSemana: Long, val semanaId: String, val seguidores: Long,
    val somaEstrelas: Long, val numEstrelas: Long, val numCaps: Long, val palavras: Long, val atualizadaEm: Long, val publicadaEm: Long,
) {
    val nota: Float get() = if (numEstrelas > 0) somaEstrelas.toFloat() / numEstrelas else 0f
}

data class CapPublico(val id: String, val titulo: String, val ordem: Double, val palavras: Long, val publicadoEm: Long,
                      val atualizadoEm: Long, val partes: Int, val curtidas: Long, val comentarios: Long, val leituras: Long = 0)

data class PerfilPublico(val uid: String, val nome: String, val bio: String, val foto: ByteArray?, val seguidores: Long)

data class Comentario(val id: String, val uid: String, val nome: String, val texto: String, val em: Long)

/** Gêneros do catálogo, agrupados por família (a ordem vira a ordem dos cartões). */
val GENEROS_GRUPOS = linkedMapOf(
    "Fantasia" to listOf("Fantasia", "Alta fantasia", "Fantasia sombria", "Fantasia urbana", "Romantasy", "Contos de fadas", "Mitologia"),
    "Oriental" to listOf("Murim", "Wuxia", "Xianxia", "Cultivo", "Artes marciais", "Dorama"),
    "Poder e progressão" to listOf("Sistema", "LitRPG", "Torre / Masmorra", "Regressão", "Reencarnação", "Isekai", "Protagonista OP", "Construção de reino"),
    "Romance" to listOf("Romance", "Romance de época", "Slow burn", "Inimigos para amantes", "Vilã", "BL", "GL", "Harém"),
    "Ação e aventura" to listOf("Ação", "Aventura", "Militar / Guerra", "Sobrevivência", "Pós-apocalíptico", "Esportes"),
    "Mistério e medo" to listOf("Mistério", "Suspense", "Thriller", "Policial", "Terror", "Horror cósmico", "Sobrenatural", "Psicológico"),
    "Ficção e mundo real" to listOf("Ficção científica", "Cyberpunk", "Distopia", "Histórico", "Político", "Drama", "Tragédia"),
    "Leve" to listOf("Comédia", "Slice of life", "Escolar / Academia", "Culinária", "Música", "Conto"),
)
val GENEROS = GENEROS_GRUPOS.values.flatten()
val CLASSIFICACOES = listOf("LIVRE", "12", "14", "16", "18")

/**
 * Catálogo público no Firestore (qualquer pessoa logada lê; só o autor escreve):
 *   publicas/{obra}                      dados, capa pequena, contadores
 *   publicas/{obra}/arquivos/{capa|banner}
 *   publicas/{obra}/caps/{cap}           capítulo (publicadoEm no futuro = agendado)
 *   publicas/{obra}/textos/{cap}[~n]     texto
 *   publicas/{obra}/fichas/{ficha}
 *   publicas/{obra}/seguidores/{uid}, avaliacoes/{uid}
 *   publicas/{obra}/caps/{cap}/curtidas/{uid}, comentarios/{id}
 * A obra do autor usa o próprio nuvemId como id público (e o capítulo também).
 */
class Publicador(private val ctx: Context, private val repo: Repositorio, private val prefs: Preferencias, private val conta: Conta, private val sinc: Sincronizador) {
    private val db get() = FirebaseFirestore.getInstance()
    private val col get() = db.collection("publicas")
    private val uid get() = conta.usuario.value?.uid ?: ""
    private val dao get() = repo.dao
    private companion object { const val PARTE = 300_000 }

    fun semanaAtual(): String { val c = Calendar.getInstance(); return "${c.get(Calendar.YEAR)}-${c.get(Calendar.WEEK_OF_YEAR)}" }

    // ───────── AUTOR ─────────

    /** O que falta para poder publicar (vazio = pode). */
    fun pendencias(o: Obra): List<String> = buildList {
        if (o.capa == null) add("Coloque uma capa")
        if (o.banner == null) add("Coloque um banner")
        if (o.descricao.isBlank()) add("Escreva a sinopse")
        if (o.generos.isBlank()) add("Escolha pelo menos um gênero")
    }

    /** Publica/atualiza os dados da obra (não mexe nos capítulos). */
    suspend fun publicarObra(o: Obra): String? = withContext(Dispatchers.IO) {
        try {
            val ref = col.document(o.nuvemId)
            val caps = dao.capitulosAgora(o.id).filter { it.publicadoEm > 0 }
            val mini = repo.imagens.comprimir(o.capa, 260, 70)
            val dados = mutableMapOf<String, Any?>(
                "autorUid" to uid, "autorNome" to prefs.autorNome.valor.ifBlank { conta.usuario.value?.displayName ?: "Autor" },
                "titulo" to o.titulo, "sinopse" to o.descricao, "generos" to o.generos.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                "tags" to o.tagsPub.split(',').map { it.trim().removePrefix("#").lowercase() }.filter { it.isNotEmpty() },
                "classificacao" to o.classificacao, "idioma" to o.idioma, "status" to o.status, "exportavel" to o.exportavel,
                "capaMini" to mini?.let { Blob.fromBytes(it) }, "numCaps" to caps.size, "palavras" to caps.sumOf { it.palavras.toLong() },
                "atualizadaEm" to System.currentTimeMillis(),
            )
            val existe = ref.get().await().exists()
            if (!existe) dados += mapOf("publicadaEm" to System.currentTimeMillis(), "leituras" to 0L, "leiturasSemana" to 0L, "semanaId" to semanaAtual(),
                "seguidores" to 0L, "somaEstrelas" to 0L, "numEstrelas" to 0L)
            ref.set(dados, com.google.firebase.firestore.SetOptions.merge()).await()
            repo.imagens.comprimir(o.capa, 900)?.let { ref.collection("arquivos").document("capa").set(mapOf("dados" to Blob.fromBytes(it))).await() }
            repo.imagens.comprimir(o.banner, 1400)?.let { ref.collection("arquivos").document("banner").set(mapOf("dados" to Blob.fromBytes(it))).await() }
            // fichas (os leitores veem; nada de anotações privadas além do que o autor escreveu na ficha)
            val fichas = dao.fichasAgora(o.id)
            val remotas = ref.collection("fichas").get().await().documents.map { it.id }.toSet()
            val lote = db.batch()
            fichas.forEach { f ->
                lote.set(ref.collection("fichas").document(f.nuvemId), mapOf("tipo" to f.tipo, "nome" to f.nome, "apelidos" to f.apelidos, "idade" to f.idade,
                    "aparencia" to f.aparencia, "poder" to f.poder, "faccao" to f.faccao, "relacoes" to f.relacoes, "descricao" to f.descricao))
            }
            (remotas - fichas.map { it.nuvemId }.toSet()).forEach { lote.delete(ref.collection("fichas").document(it)) }
            lote.commit().await()
            if (!o.publicada) repo.salvarObra(o.copy(publicada = true))
            null
        } catch (e: Exception) { erro(e) }
    }

    /** Publica (ou republica) capítulos. quando > agora = agendado. */
    suspend fun publicarCapitulos(o: Obra, caps: List<Capitulo>, quando: Long = System.currentTimeMillis()): String? = withContext(Dispatchers.IO) {
        try {
            val ref = col.document(o.nuvemId)
            // a obra precisa existir no catálogo antes dos capítulos (as regras conferem quem é o autor nela)
            if (!ref.get().await().exists()) publicarObra(o)?.let { return@withContext it }
            for (c in caps) {
                val texto = repo.textos.ler(c.id)
                val pedacos = if (texto.isEmpty()) listOf("") else texto.chunked(PARTE)
                pedacos.forEachIndexed { i, t -> ref.collection("textos").document(if (i == 0) c.nuvemId else "${c.nuvemId}~$i").set(mapOf("texto" to t)).await() }
                val jaPublicado = c.publicadoEm > 0
                val em = if (jaPublicado && c.publicadoEm <= System.currentTimeMillis()) c.publicadoEm else quando
                ref.collection("caps").document(c.nuvemId).set(mapOf(
                    "titulo" to c.titulo, "ordem" to c.ordem, "palavras" to c.palavras.toLong(), "publicadoEm" to em,
                    "atualizadoEm" to System.currentTimeMillis(), "partes" to pedacos.size,
                ), com.google.firebase.firestore.SetOptions.merge()).await()
                repo.salvarCapitulo(c.copy(publicadoEm = em, publicadoHash = sinc.hash(repo.textos.arquivo(c.id)), rascunho = false))
            }
            publicarObra(dao.obraAgora(o.id) ?: o)
        } catch (e: Exception) { erro(e) }
    }

    suspend fun despublicarCapitulo(o: Obra, c: Capitulo): String? = withContext(Dispatchers.IO) {
        try {
            val ref = col.document(o.nuvemId)
            ref.collection("caps").document(c.nuvemId).delete().await()
            ref.collection("textos").whereGreaterThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), c.nuvemId)
                .whereLessThan(com.google.firebase.firestore.FieldPath.documentId(), c.nuvemId + "~~").get().await().documents.forEach { it.reference.delete().await() }
            repo.salvarCapitulo(c.copy(publicadoEm = 0, publicadoHash = ""))
            publicarObra(dao.obraAgora(o.id) ?: o)
        } catch (e: Exception) { erro(e) }
    }

    suspend fun despublicarObra(o: Obra): String? = withContext(Dispatchers.IO) {
        try {
            val ref = col.document(o.nuvemId)
            for (sub in listOf("caps", "textos", "arquivos", "fichas")) {
                ref.collection(sub).get().await().documents.chunked(400).forEach { grupo -> val b = db.batch(); grupo.forEach { b.delete(it.reference) }; b.commit().await() }
            }
            ref.delete().await()
            repo.salvarObra(o.copy(publicada = false))
            dao.capitulosAgora(o.id).filter { it.publicadoEm > 0 }.forEach { repo.salvarCapitulo(it.copy(publicadoEm = 0, publicadoHash = "")) }
            null
        } catch (e: Exception) { erro(e) }
    }

    // ───────── CATÁLOGO ─────────

    private fun obraDe(d: DocumentSnapshot) = ObraPublica(
        id = d.id, autorUid = d.getString("autorUid") ?: "", autorNome = d.getString("autorNome") ?: "", titulo = d.getString("titulo") ?: "",
        sinopse = d.getString("sinopse") ?: "", generos = (d.get("generos") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        tags = (d.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(), classificacao = d.getString("classificacao") ?: "LIVRE",
        idioma = d.getString("idioma") ?: "pt", status = d.getString("status") ?: StatusObra.ESCREVENDO, capaMini = d.getBlob("capaMini")?.toBytes(),
        leituras = d.getLong("leituras") ?: 0, leiturasSemana = d.getLong("leiturasSemana") ?: 0, semanaId = d.getString("semanaId") ?: "",
        seguidores = d.getLong("seguidores") ?: 0, somaEstrelas = d.getLong("somaEstrelas") ?: 0, numEstrelas = d.getLong("numEstrelas") ?: 0,
        numCaps = d.getLong("numCaps") ?: 0, palavras = d.getLong("palavras") ?: 0, atualizadaEm = d.getLong("atualizadaEm") ?: 0, publicadaEm = d.getLong("publicadaEm") ?: 0,
    )

    /** modo: ALTA (mais lidas) | SEMANA (ranking da semana) | NOVIDADES | CONCLUIDAS | genero:X */
    suspend fun catalogo(modo: String): List<ObraPublica> = withContext(Dispatchers.IO) {
        val q: Query = when {
            modo == "ALTA" -> col.orderBy("leituras", Query.Direction.DESCENDING).limit(40)
            modo == "SEMANA" -> col.orderBy("leiturasSemana", Query.Direction.DESCENDING).limit(40)
            modo.startsWith("genero:") -> col.whereArrayContains("generos", modo.removePrefix("genero:")).limit(60)
            else -> col.orderBy("atualizadaEm", Query.Direction.DESCENDING).limit(60)
        }
        val lista = q.get().await().documents.map(::obraDe).filter { it.numCaps > 0 }
        when (modo) {
            "SEMANA" -> lista.filter { it.semanaId == semanaAtual() && it.leiturasSemana > 0 }
            "CONCLUIDAS" -> lista.filter { it.status == StatusObra.CONCLUIDA }
            else -> if (modo.startsWith("genero:")) lista.sortedByDescending { it.leituras } else lista
        }
    }

    suspend fun obra(pid: String): ObraPublica? = withContext(Dispatchers.IO) { col.document(pid).get().await().takeIf { it.exists() }?.let(::obraDe) }

    suspend fun imagem(pid: String, qual: String): ByteArray? = withContext(Dispatchers.IO) {
        try { col.document(pid).collection("arquivos").document(qual).get().await().getBlob("dados")?.toBytes() } catch (_: Exception) { null }
    }

    suspend fun capitulos(pid: String, autor: Boolean = false): List<CapPublico> = withContext(Dispatchers.IO) {
        val agora = System.currentTimeMillis()
        col.document(pid).collection("caps").get().await().documents.map { d ->
            CapPublico(d.id, d.getString("titulo") ?: "", d.getDouble("ordem") ?: 0.0, d.getLong("palavras") ?: 0, d.getLong("publicadoEm") ?: 0,
                d.getLong("atualizadoEm") ?: 0, (d.getLong("partes") ?: 1).toInt(), d.getLong("curtidas") ?: 0, d.getLong("comentarios") ?: 0, d.getLong("leituras") ?: 0)
        }.filter { autor || it.publicadoEm <= agora }.sortedBy { it.ordem }
    }

    // ───────── LEITOR ─────────

    /** Traz (ou atualiza) a obra para a biblioteca, só leitura, com todos os capítulos para ler offline. Devolve o id local. */
    suspend fun baixar(pid: String, aoProgresso: (String) -> Unit = {}): Long = withContext(Dispatchers.IO) {
        val p = obra(pid) ?: throw IllegalStateException("Obra não encontrada (pode ter sido despublicada).")
        var local = dao.obraPublica(pid)
        var capa = local?.capa; var banner = local?.banner
        if (local == null) {
            aoProgresso("Baixando capa…")
            capa = imagem(pid, "capa")?.let { repo.imagens.salvarBytes(it) }
            banner = imagem(pid, "banner")?.let { repo.imagens.salvarBytes(it) }
        }
        val base = Obra(titulo = p.titulo, descricao = p.sinopse, status = StatusObra.LENDO, minha = false, trancada = true, autor = p.autorNome,
            publicaId = pid, capa = capa, banner = banner, exportavel = exportavelRemoto(pid), generos = p.generos.joinToString(","), tagsPub = p.tags.joinToString(","), classificacao = p.classificacao, idioma = p.idioma)
        val obraId = if (local == null) dao.inserirObra(base) else {
            repo.salvarObra(local.copy(titulo = p.titulo, descricao = p.sinopse, autor = p.autorNome, exportavel = base.exportavel, generos = base.generos, tagsPub = base.tagsPub, classificacao = p.classificacao))
            local.id
        }
        val caps = capitulos(pid)
        caps.forEachIndexed { i, c ->
            val lc = dao.capituloPublico(obraId, c.id)
            if (lc == null || c.atualizadoEm > lc.publicadoEm) {
                aoProgresso("Baixando capítulo ${i + 1} de ${caps.size}…")
                val sb = StringBuilder()
                for (k in 0 until c.partes) sb.append(col.document(pid).collection("textos").document(if (k == 0) c.id else "${c.id}~$k").get().await().getString("texto") ?: "")
                val texto = sb.toString()
                val novo = (lc ?: Capitulo(obraId = obraId, titulo = c.titulo, ordem = c.ordem)).copy(
                    titulo = c.titulo, ordem = c.ordem, palavras = ArquivosTexto.contarPalavras(texto), rascunho = false, publicaId = c.id, publicadoEm = c.atualizadoEm)
                val id = if (lc == null) dao.inserirCapitulo(novo) else { dao.atualizarCapitulo(novo); lc.id }
                repo.textos.gravar(id, texto)
                dao.capituloAgora(id)?.let { try { repo.indexarFichas(it, texto) } catch (_: Exception) {} }
            }
        }
        // fichas do autor (campos que ele preencheu)
        try {
            val locais = dao.fichasAgora(obraId)
            col.document(pid).collection("fichas").get().await().documents.forEach { d ->
                val nome = d.getString("nome") ?: return@forEach
                val f = locais.firstOrNull { it.nome.equals(nome, true) } ?: Ficha(obraId = obraId, nome = nome)
                val nova = f.copy(tipo = d.getString("tipo") ?: f.tipo, apelidos = d.getString("apelidos") ?: "", idade = d.getString("idade") ?: "",
                    aparencia = d.getString("aparencia") ?: "", poder = d.getString("poder") ?: "", faccao = d.getString("faccao") ?: "",
                    relacoes = d.getString("relacoes") ?: "", descricao = d.getString("descricao") ?: "")
                if (f.id == 0L) dao.inserirFicha(nova) else if (nova != f) dao.atualizarFicha(nova)
            }
        } catch (_: Exception) {}
        obraId
    }

    /** Confere as obras seguidas e baixa capítulos novos/alterados. Devolve "título|quantos" das que tiveram novidade. */
    suspend fun atualizarSeguidas(): List<Pair<Obra, Int>> = withContext(Dispatchers.IO) {
        val saida = ArrayList<Pair<Obra, Int>>()
        for (o in dao.obrasSeguidas()) {
            try {
                val caps = capitulos(o.publicaId)
                val locais = dao.capitulosAgora(o.id).associateBy { it.publicaId }
                val novos = caps.count { locais[it.id] == null }
                val mudou = caps.any { c -> locais[c.id]?.let { c.atualizadoEm > it.publicadoEm } ?: true }
                if (mudou) { baixar(o.publicaId); if (novos > 0) saida.add(o to novos) }
            } catch (_: Exception) {}
        }
        if (saida.isNotEmpty()) prefs.adicionarNovidades(saida.map { (o, n) -> Triple(o.id, o.titulo, n) })
        saida
    }

    suspend fun seguindo(pid: String): Boolean = try { col.document(pid).collection("seguidores").document(uid).get().await().exists() } catch (_: Exception) { false }

    suspend fun seguir(pid: String, sim: Boolean) = withContext(Dispatchers.IO) {
        val ref = col.document(pid).collection("seguidores").document(uid)
        val ja = ref.get().await().exists()
        if (sim && !ja) { ref.set(mapOf("em" to System.currentTimeMillis())).await(); col.document(pid).update("seguidores", FieldValue.increment(1)).await() }
        if (!sim && ja) { ref.delete().await(); col.document(pid).update("seguidores", FieldValue.increment(-1)).await() }
    }

    suspend fun minhaNota(pid: String): Int = try { (col.document(pid).collection("avaliacoes").document(uid).get().await().getLong("estrelas") ?: 0).toInt() } catch (_: Exception) { 0 }

    suspend fun avaliar(pid: String, estrelas: Int) = withContext(Dispatchers.IO) {
        val antes = minhaNota(pid)
        col.document(pid).collection("avaliacoes").document(uid).set(mapOf("estrelas" to estrelas, "em" to System.currentTimeMillis())).await()
        col.document(pid).update(mapOf("somaEstrelas" to FieldValue.increment((estrelas - antes).toLong()),
            "numEstrelas" to FieldValue.increment(if (antes == 0) 1 else 0))).await()
    }

    /** Conta uma leitura por capítulo por pessoa (guardado no aparelho). */
    suspend fun contarLeitura(pid: String, cid: String) = withContext(Dispatchers.IO) {
        val chave = "$pid/$cid"
        if (prefs.jaContado(chave)) return@withContext
        try {
            val ref = col.document(pid)
            db.runTransaction { t ->
                val d = t.get(ref)
                val sem = semanaAtual()
                if (d.getString("semanaId") == sem) t.update(ref, mapOf("leituras" to FieldValue.increment(1), "leiturasSemana" to FieldValue.increment(1)))
                else t.update(ref, mapOf("leituras" to FieldValue.increment(1), "leiturasSemana" to 1L, "semanaId" to sem))
                true
            }.await()
            try { ref.collection("caps").document(cid).update("leituras", FieldValue.increment(1)).await() } catch (_: Exception) {}
            prefs.marcarContado(chave)
        } catch (_: Exception) {}
    }

    suspend fun curtiu(pid: String, cid: String): Boolean = try { col.document(pid).collection("caps").document(cid).collection("curtidas").document(uid).get().await().exists() } catch (_: Exception) { false }

    suspend fun curtir(pid: String, cid: String, sim: Boolean) = withContext(Dispatchers.IO) {
        val cap = col.document(pid).collection("caps").document(cid)
        val ref = cap.collection("curtidas").document(uid)
        val ja = ref.get().await().exists()
        if (sim && !ja) { ref.set(mapOf("em" to System.currentTimeMillis())).await(); cap.update("curtidas", FieldValue.increment(1)).await() }
        if (!sim && ja) { ref.delete().await(); cap.update("curtidas", FieldValue.increment(-1)).await() }
    }

    suspend fun contadores(pid: String, cid: String): Pair<Long, Long> = try {
        val d = col.document(pid).collection("caps").document(cid).get().await()
        (d.getLong("curtidas") ?: 0) to (d.getLong("comentarios") ?: 0)
    } catch (_: Exception) { 0L to 0L }

    suspend fun comentarios(pid: String, cid: String): List<Comentario> = withContext(Dispatchers.IO) {
        col.document(pid).collection("caps").document(cid).collection("comentarios").orderBy("em", Query.Direction.DESCENDING).limit(200).get().await()
            .documents.map { Comentario(it.id, it.getString("uid") ?: "", it.getString("nome") ?: "", it.getString("texto") ?: "", it.getLong("em") ?: 0) }
    }

    suspend fun comentar(pid: String, cid: String, texto: String) = withContext(Dispatchers.IO) {
        val cap = col.document(pid).collection("caps").document(cid)
        cap.collection("comentarios").add(mapOf("uid" to uid, "nome" to prefs.autorNome.valor.ifBlank { conta.usuario.value?.displayName ?: "Leitor" },
            "texto" to texto.trim().take(2000), "em" to System.currentTimeMillis())).await()
        cap.update("comentarios", FieldValue.increment(1)).await()
    }

    suspend fun apagarComentario(pid: String, cid: String, c: Comentario) = withContext(Dispatchers.IO) {
        val cap = col.document(pid).collection("caps").document(cid)
        cap.collection("comentarios").document(c.id).delete().await()
        cap.update("comentarios", FieldValue.increment(-1)).await()
    }

    val meuUid: String get() = uid

    // ───────── DONO DO APP ─────────

    suspend fun perfis(): List<PerfilPublico> = withContext(Dispatchers.IO) {
        db.collection("perfis").limit(500).get().await().documents.map {
            PerfilPublico(it.id, it.getString("nome") ?: "", it.getString("bio") ?: "", null, it.getLong("seguidores") ?: 0)
        }.sortedBy { it.nome.lowercase() }
    }

    /** Remove uma obra do catálogo (só o dono do app; as regras conferem o e-mail). */
    suspend fun removerComoDono(pid: String): String? = withContext(Dispatchers.IO) {
        try {
            val ref = col.document(pid)
            for (sub in listOf("caps", "textos", "arquivos", "fichas")) {
                ref.collection(sub).get().await().documents.chunked(400).forEach { g -> val b = db.batch(); g.forEach { b.delete(it.reference) }; b.commit().await() }
            }
            ref.delete().await(); null
        } catch (e: Exception) { erro(e) }
    }

    private suspend fun exportavelRemoto(pid: String): Boolean = try { col.document(pid).get().await().getBoolean("exportavel") ?: true } catch (_: Exception) { true }

    // ───────── PERFIL PÚBLICO E SEGUIR AUTOR ─────────

    private val perfis get() = db.collection("perfis")

    /** Mantém o perfil público (nome, bio, foto pequena) igual ao do app. */
    suspend fun publicarPerfilSePreciso() = withContext(Dispatchers.IO) {
        if (uid.isEmpty()) return@withContext
        val alt = prefs.perfilAlteradoEm.valor.coerceAtLeast(1)
        if (prefs.perfilPublicadoEm.valor >= alt) return@withContext
        try {
            val foto = prefs.fotoPerfil.valor.takeIf { it.isNotEmpty() }?.let { repo.imagens.comprimir(it, 200, 70) }
            perfis.document(uid).set(mapOf("nome" to prefs.autorNome.valor.ifBlank { conta.usuario.value?.displayName ?: "Autor" },
                "bio" to prefs.bio.valor, "foto" to foto?.let { Blob.fromBytes(it) }, "alteradoEm" to alt), com.google.firebase.firestore.SetOptions.merge()).await()
            prefs.perfilPublicadoEm.definir(alt)
        } catch (_: Exception) {}
    }

    suspend fun perfil(autor: String): PerfilPublico? = withContext(Dispatchers.IO) {
        val d = perfis.document(autor).get().await()
        if (!d.exists()) null else PerfilPublico(autor, d.getString("nome") ?: "", d.getString("bio") ?: "", d.getBlob("foto")?.toBytes(), d.getLong("seguidores") ?: 0)
    }

    suspend fun obrasDoAutor(autor: String): List<ObraPublica> = withContext(Dispatchers.IO) {
        col.whereEqualTo("autorUid", autor).get().await().documents.map(::obraDe).filter { it.numCaps > 0 || autor == uid }.sortedByDescending { it.atualizadaEm }
    }

    fun segueAutor(autor: String) = autor in prefs.autoresSeguidos.valor.split(',')

    suspend fun seguirAutor(autor: String, sim: Boolean) = withContext(Dispatchers.IO) {
        val ref = perfis.document(autor).collection("seguidores").document(uid)
        val ja = ref.get().await().exists()
        if (sim && !ja) { ref.set(mapOf("em" to System.currentTimeMillis())).await(); try { perfis.document(autor).update("seguidores", FieldValue.increment(1)).await() } catch (_: Exception) {} }
        if (!sim && ja) { ref.delete().await(); try { perfis.document(autor).update("seguidores", FieldValue.increment(-1)).await() } catch (_: Exception) {} }
        val lista = prefs.autoresSeguidos.valor.split(',').filter { it.isNotBlank() }.toMutableSet()
        if (sim) lista.add(autor) else lista.remove(autor)
        prefs.autoresSeguidos.definir(lista.joinToString(","))
    }

    /** Obras novas de autores seguidos desde a última olhada. */
    suspend fun novasDeAutores(): List<ObraPublica> = withContext(Dispatchers.IO) {
        val autores = prefs.autoresSeguidos.valor.split(',').filter { it.isNotBlank() }
        val desde = prefs.autoresChecadoEm.valor
        val agora = System.currentTimeMillis()
        if (autores.isEmpty()) { prefs.autoresChecadoEm.definir(agora); return@withContext emptyList() }
        val novas = ArrayList<ObraPublica>()
        for (a in autores) try {
            novas += col.whereEqualTo("autorUid", a).get().await().documents.map(::obraDe)
                .filter { it.publicadaEm > desde && desde > 0 && it.numCaps > 0 && dao.obraPublica(it.id) == null }
        } catch (_: Exception) {}
        prefs.autoresChecadoEm.definir(agora)
        if (novas.isNotEmpty()) prefs.adicionarNovidadesPublicas(novas.map { it.id to "${it.autorNome}: ${it.titulo}" })
        novas
    }

    private fun erro(e: Exception): String = when {
        e.message?.contains("PERMISSION_DENIED") == true -> "A nuvem recusou (confira se colou as Regras novas do Firestore)."
        e is com.google.firebase.FirebaseNetworkException -> "Sem internet."
        else -> "Não deu certo: ${e.message ?: e.javaClass.simpleName}"
    }
}
