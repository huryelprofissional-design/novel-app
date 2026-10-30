package com.huryel.novely.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Repositorio(ctx: Context) {
    val banco = BancoNovely.criar(ctx)
    val dao = banco.dao()
    val textos = ArquivosTexto(ctx)
    val imagens = ArquivosImagem(ctx)

    /** Chamado com as palavras novas escritas (para a meta diária). */
    var aoEscreverPalavras: (Int) -> Unit = {}
    private val ctxApp = ctx.applicationContext
    private val pastaLixeira get() = java.io.File(ctxApp.filesDir, "lixeira").apply { mkdirs() }

    private val cores = listOf(0xFF3B2A4A, 0xFF1F3B3A, 0xFF4A2420, 0xFF23283F, 0xFF3D3320, 0xFF2B3A22)

    fun resumos() = dao.resumos()
    fun obra(id: Long) = dao.obra(id)
    fun volumes(obraId: Long) = dao.volumes(obraId)
    fun capitulos(obraId: Long) = dao.capitulos(obraId)
    fun capitulo(id: Long) = dao.capitulo(id)

    suspend fun criarObra(titulo: String, descricao: String, status: String): Long =
        dao.inserirObra(
            Obra(titulo = titulo.trim().ifEmpty { "Sem título" }, descricao = descricao.trim(), status = status, minha = status != StatusObra.LENDO,
                corBase = cores[(System.nanoTime() % cores.size).toInt().let { if (it < 0) -it else it }])
        )

    suspend fun salvarObra(obra: Obra) = dao.atualizarObra(obra.copy(atualizadaEm = System.currentTimeMillis()))

    suspend fun excluirObra(obra: Obra) {
        val caps = dao.capitulosAgora(obra.id)
        caps.forEach { mandarParaLixeira(it, obra.titulo) }
        dao.excluirObra(obra) // capítulos e volumes saem junto (cascade); os textos ficam na lixeira
        caps.forEach { textos.apagar(it.id) }
        imagens.apagar(obra.capa); imagens.apagar(obra.banner)
    }

    /** tipo = "capa" ou "banner" */
    suspend fun trocarImagem(obraId: Long, tipo: String, uri: Uri): Boolean {
        val caminho = imagens.importar(uri, if (tipo == "banner") 2000 else 1200) ?: return false
        val o = dao.obraAgora(obraId) ?: return false
        if (tipo == "banner") {
            imagens.apagar(o.banner)
            salvarObra(o.copy(banner = caminho, bannerX = 0.5f, bannerY = 0.5f, bannerZoom = 1f))
        } else {
            imagens.apagar(o.capa)
            salvarObra(o.copy(capa = caminho, capaX = 0.5f, capaY = 0.5f, capaZoom = 1f))
        }
        return true
    }

    suspend fun removerImagem(obraId: Long, tipo: String) {
        val o = dao.obraAgora(obraId) ?: return
        if (tipo == "banner") { imagens.apagar(o.banner); salvarObra(o.copy(banner = null)) }
        else { imagens.apagar(o.capa); salvarObra(o.copy(capa = null)) }
    }

    suspend fun ajustarImagem(obraId: Long, tipo: String, x: Float, y: Float, zoom: Float) {
        val o = dao.obraAgora(obraId) ?: return
        salvarObra(if (tipo == "banner") o.copy(bannerX = x, bannerY = y, bannerZoom = zoom) else o.copy(capaX = x, capaY = y, capaZoom = zoom))
    }

    suspend fun novoVolume(obraId: Long, titulo: String): Long =
        dao.inserirVolume(Volume(obraId = obraId, titulo = titulo.trim().ifEmpty { "Volume" }, ordem = dao.maiorOrdemVolume(obraId) + 1))

    suspend fun renomearVolume(v: Volume, titulo: String) = dao.atualizarVolume(v.copy(titulo = titulo.trim().ifEmpty { v.titulo }))

    suspend fun excluirVolume(v: Volume) = banco.withTransaction {
        dao.capitulosAgora(v.obraId).filter { it.volumeId == v.id }.forEach { dao.atualizarCapitulo(it.copy(volumeId = null)) }
        dao.excluirVolume(v)
    }

    suspend fun novoCapitulo(obraId: Long, titulo: String, volumeId: Long?, emBranco: Boolean = false): Long {
        val id = dao.inserirCapitulo(
            Capitulo(obraId = obraId, volumeId = volumeId, titulo = if (emBranco) titulo.trim() else titulo.trim().ifEmpty { "Novo capítulo" },
                ordem = dao.maiorOrdemCapitulo(obraId) + 1)
        )
        textos.gravar(id, "")
        dao.tocarObra(obraId, System.currentTimeMillis())
        return id
    }

    /**
     * Divide um capítulo: o texto antes do título fica no capítulo atual; o título vira um capítulo
     * novo logo depois dele, com o texto que vinha abaixo. Retorna o id do capítulo novo.
     */
    suspend fun dividirCapitulo(c: Capitulo, textoAntes: String, titulo: String, textoDepois: String): Long {
        val lista = dao.capitulosAgora(c.obraId)
        val i = lista.indexOfFirst { it.id == c.id }
        val prox = lista.getOrNull(i + 1)
        val ordem = if (prox != null) (c.ordem + prox.ordem) / 2 else c.ordem + 1
        gravarTexto(c, textoAntes)
        val id = dao.inserirCapitulo(Capitulo(obraId = c.obraId, volumeId = c.volumeId, titulo = titulo, ordem = ordem))
        textos.gravar(id, textoDepois)
        dao.atualizarPalavras(id, Texto.contarPalavras(textoDepois), System.currentTimeMillis())
        return id
    }

    /**
     * Procura linhas de título ("Capítulo 3 — …", "Prólogo", "# …") que estejam sozinhas depois de uma
     * linha vazia (ou no começo) e separa cada uma em um capítulo próprio, logo após o atual.
     * Retorna quantos capítulos novos foram criados.
     */
    suspend fun separarPorTitulos(c: Capitulo, texto: String): Int {
        data class Parte(val titulo: String?, val sb: StringBuilder)
        val partes = arrayListOf(Parte(null, StringBuilder()))
        var anteriorVazia = true
        texto.lineSequence().forEach { linha ->
            if (Texto.ehTituloEstrito(linha)) partes.add(Parte(Texto.tituloLimpo(linha), StringBuilder()))
            else partes.last().sb.append(linha).append('\n')
            anteriorVazia = linha.isBlank()
        }
        if (partes.size == 1) return 0
        var atual = c
        val primeira = partes[0]
        var resto = partes.drop(1)
        if (primeira.sb.isBlank()) {
            atual = c.copy(titulo = resto[0].titulo!!)
            salvarCapitulo(atual)
            gravarTexto(atual, resto[0].sb.toString().trim('\n') + "\n")
            resto = resto.drop(1)
        } else gravarTexto(atual, primeira.sb.toString().trimEnd() + "\n")
        val lista = dao.capitulosAgora(c.obraId)
        val prox = lista.getOrNull(lista.indexOfFirst { it.id == c.id } + 1)
        val passo = if (prox != null) (prox.ordem - c.ordem) / (resto.size + 1) else 1.0
        resto.forEachIndexed { k, pt ->
            val id = dao.inserirCapitulo(Capitulo(obraId = c.obraId, volumeId = c.volumeId, titulo = pt.titulo!!, ordem = c.ordem + passo * (k + 1)))
            val t = pt.sb.toString().trim('\n') + "\n"
            textos.gravar(id, t)
            dao.atualizarPalavras(id, Texto.contarPalavras(t), System.currentTimeMillis())
        }
        return resto.size + if (primeira.sb.isBlank()) 0 else 0
    }

    suspend fun salvarCapitulo(c: Capitulo) = dao.atualizarCapitulo(c.copy(atualizadoEm = System.currentTimeMillis()))

    /** Excluir manda para a Lixeira (30 dias), nunca apaga na hora. */
    suspend fun excluirCapitulo(c: Capitulo) {
        mandarParaLixeira(c, dao.obraAgora(c.obraId)?.titulo ?: "")
        dao.excluirCapitulo(c)
        textos.apagar(c.id)
    }

    private suspend fun mandarParaLixeira(c: Capitulo, obraTitulo: String) {
        val id = dao.inserirLixeira(ItemLixeira(obraId = c.obraId, obraTitulo = obraTitulo, volumeId = c.volumeId, titulo = c.titulo,
            ordem = c.ordem, palavras = c.palavras, resumo = c.resumo, notas = c.notas))
        val origem = textos.arquivo(c.id)
        val destino = java.io.File(pastaLixeira, "$id.md")
        if (origem.exists()) origem.copyTo(destino, overwrite = true) else destino.writeText("")
    }

    fun lixeira() = dao.lixeira()

    suspend fun restaurarDaLixeira(i: ItemLixeira): Long {
        var obraId = i.obraId
        if (dao.obraAgora(obraId) == null) {
            obraId = dao.todasObras().firstOrNull { it.titulo == i.obraTitulo }?.id
                ?: criarObra(i.obraTitulo.ifEmpty { "Capítulos recuperados" }, "Recuperada da lixeira", StatusObra.ESCREVENDO)
        }
        val vol = i.volumeId?.takeIf { v -> dao.volumesAgora(obraId).any { it.id == v } }
        val id = dao.inserirCapitulo(Capitulo(obraId = obraId, volumeId = vol, titulo = i.titulo, ordem = i.ordem,
            palavras = i.palavras, resumo = i.resumo, notas = i.notas))
        val f = java.io.File(pastaLixeira, "${i.id}.md")
        textos.gravar(id, if (f.exists()) f.readText(Charsets.UTF_8) else "")
        f.delete()
        dao.excluirLixeira(i)
        dao.tocarObra(obraId, System.currentTimeMillis())
        return id
    }

    suspend fun apagarDeVez(i: ItemLixeira) {
        java.io.File(pastaLixeira, "${i.id}.md").delete()
        dao.excluirLixeira(i)
    }

    /** Remove da lixeira o que tem mais de 30 dias. */
    suspend fun limparLixeiraAntiga() {
        dao.lixeiraAntiga(System.currentTimeMillis() - 30L * 24 * 3600_000).forEach { apagarDeVez(it) }
    }

    /** Junta o próximo capítulo no fim deste; o próximo vai para a lixeira. */
    suspend fun juntarComProximo(c: Capitulo, textoAtual: String): Boolean {
        val lista = dao.capitulosAgora(c.obraId)
        val prox = lista.getOrNull(lista.indexOfFirst { it.id == c.id } + 1) ?: return false
        val tProx = textos.ler(prox.id)
        gravarTexto(c, textoAtual.trimEnd() + "\n\n" + tProx.trimStart())
        excluirCapitulo(prox)
        return true
    }

    /** Move um trecho para o fim do capítulo anterior (-1) ou começo do próximo (+1). */
    suspend fun moverTrecho(c: Capitulo, trecho: String, direcao: Int): Capitulo? {
        val lista = dao.capitulosAgora(c.obraId)
        val alvo = lista.getOrNull(lista.indexOfFirst { it.id == c.id } + direcao) ?: return null
        val t = textos.ler(alvo.id)
        val novo = if (direcao < 0) t.trimEnd() + "\n\n" + trecho.trim() + "\n" else trecho.trim() + "\n\n" + t.trimStart()
        gravarTexto(alvo, novo)
        return alvo
    }

    /** Substitui em todos os capítulos da obra (guarda versão antes). Retorna o nº de trocas. */
    suspend fun substituirNaObra(obraId: Long, de: String, para: String, ignorarCaixa: Boolean): Int {
        if (de.isEmpty()) return 0
        var total = 0
        dao.capitulosAgora(obraId).forEach { c ->
            val t = textos.ler(c.id)
            val n = Regex(Regex.escape(de), if (ignorarCaixa) setOf(RegexOption.IGNORE_CASE) else emptySet()).findAll(t).count()
            if (n > 0) {
                total += n
                textos.guardarVersaoSePreciso(c.id, forcar = true)
                gravarTexto(c, t.replace(de, para, ignoreCase = ignorarCaixa))
            }
        }
        return total
    }

    // ── Marcadores / grifos ──
    fun marcadoresCap(capId: Long) = dao.marcadoresCap(capId)
    fun marcadoresObra(obraId: Long) = dao.marcadoresObra(obraId)
    suspend fun salvarMarcador(m: Marcador) = if (m.id == 0L) dao.inserirMarcador(m) else { dao.atualizarMarcador(m); m.id }
    suspend fun excluirMarcador(m: Marcador) = dao.excluirMarcador(m)

    // ── Linha do tempo ──
    fun eventos(obraId: Long) = dao.eventos(obraId)
    suspend fun salvarEvento(e: Evento): Long =
        if (e.id == 0L) dao.inserirEvento(e.copy(ordem = dao.maiorOrdemEvento(e.obraId) + 1)) else { dao.atualizarEvento(e); e.id }
    suspend fun excluirEvento(e: Evento) = dao.excluirEvento(e)
    suspend fun moverEvento(lista: List<Evento>, e: Evento, direcao: Int) {
        val i = lista.indexOfFirst { it.id == e.id }; val j = i + direcao
        if (i < 0 || j !in lista.indices) return
        dao.atualizarEvento(lista[i].copy(ordem = lista[j].ordem)); dao.atualizarEvento(lista[j].copy(ordem = lista[i].ordem))
    }

    fun colecoes() = dao.colecoes()

    suspend fun marcarLidos(ids: List<Long>, lido: Boolean) = banco.withTransaction {
        ids.forEach { id -> dao.capituloAgora(id)?.let { dao.atualizarCapitulo(it.copy(progresso = if (lido) 1f else 0f)) } }
    }
    suspend fun moverParaVolume(ids: List<Long>, volumeId: Long?) = banco.withTransaction {
        ids.forEach { id -> dao.capituloAgora(id)?.let { dao.atualizarCapitulo(it.copy(volumeId = volumeId)) } }
    }

    /** Reordena capítulos para exatamente a ordem da lista dada (usado no arrastar). */
    suspend fun reordenar(ids: List<Long>) = banco.withTransaction {
        ids.forEachIndexed { i, id -> dao.capituloAgora(id)?.let { dao.atualizarCapitulo(it.copy(ordem = (i + 1).toDouble())) } }
    }

    // ── Fichas ──
    fun fichas(obraId: Long) = dao.fichas(obraId)
    suspend fun salvarFicha(f: Ficha): Long = if (f.id == 0L) dao.inserirFicha(f) else { dao.atualizarFicha(f); f.id }
    suspend fun excluirFicha(f: Ficha) { imagens.apagar(f.imagem); dao.excluirFicha(f) }
    suspend fun imagemFicha(f: Ficha, uri: Uri): Ficha? {
        val caminho = imagens.importar(uri, 900) ?: return null
        imagens.apagar(f.imagem)
        return f.copy(imagem = caminho)
    }

    /** Move um capítulo uma posição para cima (-1) ou para baixo (+1), trocando só as duas ordens. */
    suspend fun moverCapitulo(c: Capitulo, direcao: Int) = banco.withTransaction {
        val lista = dao.capitulosAgora(c.obraId)
        val i = lista.indexOfFirst { it.id == c.id }
        val j = i + direcao
        if (i < 0 || j !in lista.indices) return@withTransaction
        val outro = lista[j]
        dao.atualizarCapitulo(lista[i].copy(ordem = outro.ordem))
        dao.atualizarCapitulo(outro.copy(ordem = lista[i].ordem))
    }

    suspend fun lerTexto(capId: Long) = textos.ler(capId)

    /**
     * Lê as marcas @Personagem e &Lugar do capítulo: cria fichas que faltam, junta apelidos
     * e guarda os parágrafos de definição como trechos (por capítulo). Retorna os nomes das fichas novas.
     */
    suspend fun indexarFichas(c: Capitulo, texto: String): List<String> {
        val achados = Marcas.encontrar(texto)
        if (achados.isEmpty() && dao.contarTrechosCap(c.id) == 0) return emptyList()
        val novos = ArrayList<String>()
        banco.withTransaction {
            val fichas = dao.fichasAgora(c.obraId).toMutableList()
            dao.apagarTrechosCap(c.id)
            achados.forEachIndexed { i, (m, trecho) ->
                fun casa(f: Ficha) = f.nome.equals(m.nome, true) || f.apelidos.split(',').any { it.trim().equals(m.nome, true) }
                var f = fichas.firstOrNull { casa(it) }
                if (f == null) {
                    val nova = Ficha(obraId = c.obraId, tipo = if (m.lugar) TipoFicha.LUGAR else TipoFicha.PERSONAGEM, nome = m.nome, apelidos = m.apelidos.joinToString(", "))
                    f = nova.copy(id = dao.inserirFicha(nova)); fichas.add(f); novos.add(m.nome)
                } else if (m.apelidos.isNotEmpty()) {
                    val atuais = f.apelidos.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    val juntos = (atuais + m.apelidos).distinctBy { it.lowercase() }
                    if (juntos.size != atuais.size) { f = f.copy(apelidos = juntos.joinToString(", ")); dao.atualizarFicha(f); fichas[fichas.indexOfFirst { it.id == f.id }] = f }
                }
                if (trecho != null) dao.inserirTrecho(FichaTrecho(fichaId = f.id, capId = c.id, ordem = i, texto = trecho))
            }
        }
        return novos
    }

    /** Relê todos os capítulos de uma obra (depois de restaurar ou receber uma obra publicada). */
    suspend fun reindexarObra(obraId: Long) {
        dao.capitulosAgora(obraId).forEach { c -> indexarFichas(c, textos.ler(c.id)) }
    }

    fun trechosFicha(fichaId: Long) = dao.trechos(fichaId)

    suspend fun gravarTexto(c: Capitulo, texto: String): List<String> {
        textos.guardarVersaoSePreciso(c.id)
        textos.gravar(c.id, texto)
        val agora = System.currentTimeMillis()
        val antes = dao.capituloAgora(c.id)?.palavras ?: 0
        val depois = ArquivosTexto.contarPalavras(texto)
        if (depois > antes) aoEscreverPalavras(depois - antes)
        dao.atualizarPalavras(c.id, depois, agora)
        dao.tocarObra(c.obraId, agora)
        return try { indexarFichas(c, texto) } catch (e: Exception) { emptyList() }
    }

    suspend fun salvarPosicao(c: Capitulo, progresso: Float, bloco: Int, desloc: Int) {
        dao.salvarPosicao(c.id, progresso, bloco, desloc)
        dao.marcarLeitura(c.obraId, c.id, System.currentTimeMillis())
    }
}

/** Preferências guardadas no aparelho (tema, leitor, voz). */
class Preferencias(ctx: Context) {
    private val sp = ctx.getSharedPreferences("novely_prefs", Context.MODE_PRIVATE)

    inner class Pref<T>(private val chave: String, padrao: T, ler: (String, T) -> T, private val gravar: (String, T) -> Unit) {
        private val fluxo = MutableStateFlow(ler(chave, padrao))
        val estado: StateFlow<T> = fluxo
        val valor get() = fluxo.value
        fun definir(v: T) { gravar(chave, v); fluxo.value = v }
    }
    private fun texto(k: String, p: String) = Pref(k, p, { c, d -> sp.getString(c, d) ?: d }, { c, v -> sp.edit().putString(c, v).apply() })
    private fun real(k: String, p: Float) = Pref(k, p, { c, d -> sp.getFloat(c, d) }, { c, v -> sp.edit().putFloat(c, v).apply() })
    private fun bool(k: String, p: Boolean) = Pref(k, p, { c, d -> sp.getBoolean(c, d) }, { c, v -> sp.edit().putBoolean(c, v).apply() })
    private fun longo(k: String, p: Long) = Pref(k, p, { c, d -> sp.getLong(c, d) }, { c, v -> sp.edit().putLong(c, v).apply() })

    val temaP = texto("tema", "OLED")
    val tamanhoP = real("tamanho", 18f)
    val entrelinhas = real("entrelinhas", 1.7f)
    val margem = real("margem", 24f)
    val fonte = texto("fonteLeitura", "TEMA") // TEMA | SERIFA | SEM_SERIFA
    val destacarFalas = bool("falas", true)
    val destacarNomes = bool("nomes", true)
    val modoPaginas = bool("paginas", false)
    val telaLigada = bool("telaLigada", true)
    val velocidadeVoz = real("voz", 1.0f)
    val narrador = texto("narrador", "")
    /** Estilo visual do Início e da página da obra: CINEMA | REVISTA | ESTANTE */
    val estilo = texto("estilo", "CINEMA")
    val pastaBackup = texto("pastaBackup", "")
    val ultimoBackup = longo("ultimoBackup", 0L)
    val backupAuto = bool("backupAuto", false)
    val atualizarAuto = bool("atualizarAuto", true)
    val ultimaVerificacao = longo("ultimaVerificacao", 0L)
    val autorNome = texto("autorNome", "")
    val problemaVisto = longo("problemaVisto", 0L)
    val ultimaSinc = longo("ultimaSinc", 0L)
    val ultimoPull = texto("ultimoPull", "")
    val perfilAlteradoEm = longo("perfilAlteradoEm", 0L)
    val fotoHash = texto("fotoHash", "")
    /** Conflitos de texto entre aparelhos: "capId|título|quando" por linha. */
    val conflitos = texto("conflitos", "")
    @Synchronized fun adicionarConflito(capId: Long, titulo: String) {
        conflitos.definir((conflitos.valor.split('\n').filter { it.isNotBlank() } + "$capId|${titulo.replace('|', ' ')}|${System.currentTimeMillis()}").takeLast(20).joinToString("\n"))
    }
    /** Capítulos novos em obras seguidas: "obraId|título|quantos|quando" por linha. */
    val novidades = texto("novidades", "")
    @Synchronized fun adicionarNovidades(l: List<Triple<Long, String, Int>>) {
        val agora = System.currentTimeMillis()
        novidades.definir((novidades.valor.split('\n').filter { it.isNotBlank() } + l.map { (id, t, n) -> "$id|${t.replace('|', ' ')}|$n|$agora" }).takeLast(30).joinToString("\n"))
    }
    private val contados = texto("contados", "")
    fun jaContado(k: String) = contados.valor.split('\n').contains(k)
    @Synchronized fun marcarContado(k: String) = contados.definir((contados.valor.split('\n').filter { it.isNotBlank() } + k).takeLast(3000).joinToString("\n"))
    val bio = texto("bio", "")
    val fotoPerfil = texto("fotoPerfil", "")
    /** Seções visíveis no Início, separadas por vírgula. */
    val secoesInicio = texto("secoesInicio", "CONT,ATALHO,LENDO,QUERO,RECEBIDAS,LIDAS")

    // ── Meta diária ──
    val metaDiaria = real("metaDiaria", 1000f)
    val palavrasHoje = longo("palavrasHoje", 0L)
    private val diaMeta = texto("diaMeta", "")
    val sequencia = longo("sequencia", 0L)
    private val ultimoDiaBatido = texto("ultimoDiaBatido", "")
    private fun hoje() = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
    private fun ontem() = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date(System.currentTimeMillis() - 86_400_000))
    /** Zera a contagem quando vira o dia; quebra a sequência se ontem não bateu a meta. */
    fun conferirDia() {
        val h = hoje()
        if (diaMeta.valor != h) {
            diaMeta.definir(h); palavrasHoje.definir(0)
            if (ultimoDiaBatido.valor != ontem() && ultimoDiaBatido.valor != h) sequencia.definir(0)
        }
    }
    @Synchronized fun registrarPalavras(n: Int) {
        conferirDia()
        val antes = palavrasHoje.valor
        palavrasHoje.definir(antes + n)
        if (antes < metaDiaria.valor && antes + n >= metaDiaria.valor && ultimoDiaBatido.valor != hoje()) {
            ultimoDiaBatido.definir(hoje()); sequencia.definir(sequencia.valor + 1)
        }
    }

    // ── Leitor: rolagem automática, luz ──
    val velocidadeRolagem = real("velRolagem", 1.0f)
    val escurecer = real("escurecer", 0f)
    val luzQuente = real("luzQuente", 0f)
    // ── Estante ──
    val ordenacao = texto("ordenacao", "RECENTE") // RECENTE | TITULO | TAMANHO | PROGRESSO | CRIACAO
    // ── Segurança e primeira vez ──
    val bloqueio = bool("bloqueio", false)
    val tutorialVisto = bool("tutorialVisto", false)

    // ── Estatísticas de leitura: "yyyyMMdd" -> segundos;palavras ──
    private val estat = texto("estatLeitura", "")
    fun registrarLeitura(segundos: Int, palavras: Int) {
        if (segundos <= 0 && palavras <= 0) return
        val mapa = estatisticas().toMutableMap()
        val h = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        val (s0, p0) = mapa[h] ?: (0 to 0)
        mapa[h] = (s0 + segundos) to (p0 + palavras.coerceAtLeast(0))
        estat.definir(mapa.entries.sortedByDescending { it.key }.take(120).joinToString("|") { "${it.key}:${it.value.first};${it.value.second}" })
    }
    fun estatisticas(): Map<String, Pair<Int, Int>> = estat.valor.split('|').mapNotNull { e ->
        val k = e.substringBefore(':', ""); val v = e.substringAfter(':', "")
        val a = v.substringBefore(';').toIntOrNull(); val b = v.substringAfter(';').toIntOrNull()
        if (k.isEmpty() || a == null || b == null) null else k to (a to b)
    }.toMap()

    // ── Onde parei no editor (por capítulo) ──
    fun posicaoEditor(capId: Long): Pair<Int, Int> = sp.getInt("ed_p_$capId", 0) to sp.getInt("ed_c_$capId", -1)
    fun salvarPosicaoEditor(capId: Long, pedaco: Int, cursor: Int) { sp.edit().putInt("ed_p_$capId", pedaco).putInt("ed_c_$capId", cursor).apply() }

    // atalhos usados pelo resto do app
    val tema: StateFlow<String> get() = temaP.estado
    fun definirTema(nome: String) = temaP.definir(nome)
    val tamanho: StateFlow<Float> get() = tamanhoP.estado
    fun definirTamanho(v: Float) = tamanhoP.definir(v)
}
