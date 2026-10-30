package com.huryel.novely.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * O banco guarda só os DADOS DE ORGANIZAÇÃO (títulos, ordem, progresso).
 * O texto dos capítulos fica em arquivos separados (ver ArquivosTexto),
 * para que um capítulo de 100 mil palavras nunca pese nas listas e buscas de metadados.
 */

object StatusObra {
    const val LENDO = "LENDO"
    const val ESCREVENDO = "ESCREVENDO"
    const val CONCLUIDA = "CONCLUIDA"
    const val QUERO = "QUERO"
}

@Entity(tableName = "obras")
data class Obra(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descricao: String = "",
    val status: String = StatusObra.ESCREVENDO,
    val capa: String? = null,
    val capaX: Float = 0.5f,
    val capaY: Float = 0.5f,
    val capaZoom: Float = 1f,
    val banner: String? = null,
    val bannerX: Float = 0.5f,
    val bannerY: Float = 0.5f,
    val bannerZoom: Float = 1f,
    val corBase: Long = 0xFF3B2A4A,
    val ultimoCapituloId: Long? = null,
    val criadaEm: Long = System.currentTimeMillis(),
    val atualizadaEm: Long = System.currentTimeMillis(),
    val lidaEm: Long = 0,
    /** Coleção/pasta na estante (vazio = nenhuma). */
    @ColumnInfo(defaultValue = "") val colecao: String = "",
    /** true = obra escrita pelo usuário (pode editar); false = obra de outra pessoa (só leitura). */
    @ColumnInfo(defaultValue = "1") val minha: Boolean = true,
    /** Obra recebida de um autor: só leitura de verdade (não dá para virar "minha"). */
    @ColumnInfo(defaultValue = "0") val trancada: Boolean = false,
    /** Nome do autor (preenchido quando a obra foi publicada por alguém). */
    @ColumnInfo(defaultValue = "") val autor: String = "",
    /** Identidade na nuvem (igual em todos os aparelhos). */
    @ColumnInfo(defaultValue = "") val nuvemId: String = novoNuvemId(),
    /** Quando mudou neste aparelho (preenchido por gatilho do banco). */
    @ColumnInfo(defaultValue = "0") val alteradoEm: Long = 0,
)

@Entity(
    tableName = "volumes",
    foreignKeys = [ForeignKey(entity = Obra::class, parentColumns = ["id"], childColumns = ["obraId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("obraId")],
)
data class Volume(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val titulo: String,
    val ordem: Double,
    /** Identidade na nuvem (igual em todos os aparelhos). */
    @ColumnInfo(defaultValue = "") val nuvemId: String = novoNuvemId(),
    /** Quando mudou neste aparelho (preenchido por gatilho do banco). */
    @ColumnInfo(defaultValue = "0") val alteradoEm: Long = 0,
)

@Entity(
    tableName = "capitulos",
    foreignKeys = [ForeignKey(entity = Obra::class, parentColumns = ["id"], childColumns = ["obraId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("obraId"), Index("volumeId")],
)
data class Capitulo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val volumeId: Long? = null,
    val titulo: String,
    /** Ordem fracionária: inserir entre dois capítulos não exige renumerar a obra inteira. */
    val ordem: Double,
    val rascunho: Boolean = true,
    val palavras: Int = 0,
    val progresso: Float = 0f,
    val posicaoBloco: Int = 0,
    val posicaoDeslocamento: Int = 0,
    val atualizadoEm: Long = System.currentTimeMillis(),
    /** Resumo curto escrito pelo autor (aparece no início do capítulo e como "Anteriormente"). */
    @ColumnInfo(defaultValue = "") val resumo: String = "",
    /** Notas privadas do autor — nunca aparecem no leitor. */
    @ColumnInfo(defaultValue = "") val notas: String = "",
    /** Etiquetas separadas por vírgula (ex.: "luta,revelação"). */
    @ColumnInfo(defaultValue = "") val tags: String = "",
    /** Identidade na nuvem (igual em todos os aparelhos). */
    @ColumnInfo(defaultValue = "") val nuvemId: String = novoNuvemId(),
    /** Quando mudou neste aparelho (preenchido por gatilho do banco). */
    @ColumnInfo(defaultValue = "0") val alteradoEm: Long = 0,
)

/** Marcador ou grifo num parágrafo do leitor. */
@Entity(
    tableName = "marcadores",
    foreignKeys = [ForeignKey(entity = Obra::class, parentColumns = ["id"], childColumns = ["obraId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("obraId"), Index("capId")],
)
data class Marcador(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val capId: Long,
    val bloco: Int,
    val trecho: String,
    /** "" = só marcador; senão cor do grifo: AMBAR | VERDE | AZUL | ROSA */
    val cor: String = "",
    val nota: String = "",
    val criadoEm: Long = System.currentTimeMillis(),
    /** Identidade na nuvem (igual em todos os aparelhos). */
    @ColumnInfo(defaultValue = "") val nuvemId: String = novoNuvemId(),
    /** Quando mudou neste aparelho (preenchido por gatilho do banco). */
    @ColumnInfo(defaultValue = "0") val alteradoEm: Long = 0,
)

/** Evento da linha do tempo de uma obra. */
@Entity(
    tableName = "eventos",
    foreignKeys = [ForeignKey(entity = Obra::class, parentColumns = ["id"], childColumns = ["obraId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("obraId")],
)
data class Evento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val capId: Long? = null,
    val quando: String = "",
    val titulo: String,
    val descricao: String = "",
    val ordem: Double,
    /** Identidade na nuvem (igual em todos os aparelhos). */
    @ColumnInfo(defaultValue = "") val nuvemId: String = novoNuvemId(),
    /** Quando mudou neste aparelho (preenchido por gatilho do banco). */
    @ColumnInfo(defaultValue = "0") val alteradoEm: Long = 0,
)

fun novoNuvemId(): String = java.util.UUID.randomUUID().toString().replace("-", "")

object TipoFicha { const val PERSONAGEM = "PERSONAGEM"; const val LUGAR = "LUGAR"; const val OUTRO = "OUTRO" }

/** Fichas de personagens, lugares e outros elementos da obra. */
@Entity(
    tableName = "fichas",
    foreignKeys = [ForeignKey(entity = Obra::class, parentColumns = ["id"], childColumns = ["obraId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("obraId")],
)
data class Ficha(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val tipo: String = TipoFicha.PERSONAGEM,
    val nome: String,
    val descricao: String = "",
    val imagem: String? = null,
    val criadaEm: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "") val apelidos: String = "",
    @ColumnInfo(defaultValue = "") val idade: String = "",
    @ColumnInfo(defaultValue = "") val aparencia: String = "",
    @ColumnInfo(defaultValue = "") val poder: String = "",
    @ColumnInfo(defaultValue = "") val faccao: String = "",
    @ColumnInfo(defaultValue = "") val relacoes: String = "",
    /** Identidade na nuvem (igual em todos os aparelhos). */
    @ColumnInfo(defaultValue = "") val nuvemId: String = novoNuvemId(),
    /** Quando mudou neste aparelho (preenchido por gatilho do banco). */
    @ColumnInfo(defaultValue = "0") val alteradoEm: Long = 0,
)

/** Parágrafos do texto que descrevem uma ficha (escritos com @Nome, …). Servem para mostrar só o que o leitor já viu. */
@Entity(
    tableName = "ficha_trechos",
    foreignKeys = [
        ForeignKey(entity = Ficha::class, parentColumns = ["id"], childColumns = ["fichaId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Capitulo::class, parentColumns = ["id"], childColumns = ["capId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("fichaId"), Index("capId")],
)
data class FichaTrecho(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fichaId: Long,
    val capId: Long,
    val ordem: Int,
    val texto: String,
)

data class TrechoFicha(val texto: String, val capId: Long, val capOrdem: Double, val capTitulo: String)

/** Capítulos excluídos ficam aqui por 30 dias (texto em files/lixeira/<id>.md). */
@Entity(tableName = "lixeira")
data class ItemLixeira(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val obraTitulo: String,
    val volumeId: Long? = null,
    val titulo: String,
    val ordem: Double,
    val palavras: Int,
    val resumo: String,
    val notas: String,
    val excluidoEm: Long = System.currentTimeMillis(),
)

/** Resumo leve para listas: sem texto, só o necessário. */
data class ResumoObra(
    val id: Long,
    val titulo: String,
    val status: String,
    val capa: String?,
    val capaX: Float,
    val capaY: Float,
    val capaZoom: Float,
    val corBase: Long,
    val ultimoCapituloId: Long?,
    val lidaEm: Long,
    val totalCapitulos: Int,
    val totalPalavras: Long,
    val lidos: Int,
    val colecao: String,
    val atualizadaEm: Long,
    val criadaEm: Long,
    val minha: Boolean = false,
    val trancada: Boolean = false,
    val autor: String = "",
)

data class ItemIndice(val capId: Long, val obraId: Long, val capTitulo: String, val obraTitulo: String)

@Dao
interface NovelyDao {
    // ── sincronização ──
    @Query("SELECT * FROM obras") suspend fun sincObras(): List<Obra>
    @Query("SELECT * FROM volumes") suspend fun sincVolumes(): List<Volume>
    @Query("SELECT * FROM capitulos") suspend fun sincCapitulos(): List<Capitulo>
    @Query("SELECT * FROM fichas") suspend fun sincFichas(): List<Ficha>
    @Query("SELECT * FROM eventos") suspend fun sincEventos(): List<Evento>
    @Query("SELECT * FROM marcadores") suspend fun sincMarcadores(): List<Marcador>
    @Query("SELECT * FROM obras WHERE nuvemId = :n LIMIT 1") suspend fun obraNuvem(n: String): Obra?
    @Query("SELECT * FROM volumes WHERE nuvemId = :n LIMIT 1") suspend fun volumeNuvem(n: String): Volume?
    @Query("SELECT * FROM capitulos WHERE nuvemId = :n LIMIT 1") suspend fun capituloNuvem(n: String): Capitulo?
    @Query("SELECT * FROM fichas WHERE nuvemId = :n LIMIT 1") suspend fun fichaNuvem(n: String): Ficha?
    @Query("SELECT * FROM eventos WHERE nuvemId = :n LIMIT 1") suspend fun eventoNuvem(n: String): Evento?
    @Query("SELECT * FROM marcadores WHERE nuvemId = :n LIMIT 1") suspend fun marcadorNuvem(n: String): Marcador?
    @Insert suspend fun inserirVolumeS(v: Volume): Long
    @Update suspend fun atualizarVolumeS(v: Volume)
    @Insert suspend fun inserirCapituloS(c: Capitulo): Long
    @Update suspend fun atualizarCapituloS(c: Capitulo)
    @Insert suspend fun inserirEventoS(e: Evento): Long
    @Update suspend fun atualizarEventoS(e: Evento)
    @Insert suspend fun inserirMarcadorS(m: Marcador): Long
    @Update suspend fun atualizarMarcadorS(m: Marcador)
    @Query("DELETE FROM volumes WHERE nuvemId = :n") suspend fun apagarVolumeNuvem(n: String)
    @Query("DELETE FROM capitulos WHERE nuvemId = :n") suspend fun apagarCapituloNuvem(n: String)
    @Query("DELETE FROM fichas WHERE nuvemId = :n") suspend fun apagarFichaNuvem(n: String)
    @Query("DELETE FROM eventos WHERE nuvemId = :n") suspend fun apagarEventoNuvem(n: String)
    @Query("DELETE FROM marcadores WHERE nuvemId = :n") suspend fun apagarMarcadorNuvem(n: String)
    @Query("DELETE FROM obras WHERE nuvemId = :n") suspend fun apagarObraNuvem(n: String)

    @Query(
        """SELECT o.id, o.titulo, o.status, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
           o.ultimoCapituloId, o.lidaEm, o.colecao, o.atualizadaEm, o.criadaEm, o.minha, o.trancada, o.autor,
           (SELECT COUNT(*) FROM capitulos c WHERE c.obraId = o.id) AS totalCapitulos,
           (SELECT IFNULL(SUM(c.palavras),0) FROM capitulos c WHERE c.obraId = o.id) AS totalPalavras,
           (SELECT COUNT(*) FROM capitulos c WHERE c.obraId = o.id AND c.progresso >= 0.98) AS lidos
           FROM obras o ORDER BY o.atualizadaEm DESC"""
    )
    fun resumos(): Flow<List<ResumoObra>>

    @Query("SELECT * FROM obras WHERE id = :id")
    fun obra(id: Long): Flow<Obra?>

    @Query("SELECT * FROM obras WHERE id = :id")
    suspend fun obraAgora(id: Long): Obra?

    @Query("SELECT * FROM obras ORDER BY atualizadaEm DESC")
    suspend fun todasObras(): List<Obra>

    @Query("SELECT * FROM volumes WHERE obraId = :obraId ORDER BY ordem")
    suspend fun volumesAgora(obraId: Long): List<Volume>

    @Query("SELECT COUNT(*) FROM capitulos WHERE obraId = :obraId")
    suspend fun contarCapitulos(obraId: Long): Int

    @Insert suspend fun inserirObra(obra: Obra): Long
    @Update suspend fun atualizarObra(obra: Obra)
    @Delete suspend fun excluirObra(obra: Obra)

    @Query("SELECT * FROM volumes WHERE obraId = :obraId ORDER BY ordem")
    fun volumes(obraId: Long): Flow<List<Volume>>
    @Insert suspend fun inserirVolume(volume: Volume): Long
    @Update suspend fun atualizarVolume(volume: Volume)
    @Delete suspend fun excluirVolume(volume: Volume)
    @Query("SELECT IFNULL(MAX(ordem), 0) FROM volumes WHERE obraId = :obraId")
    suspend fun maiorOrdemVolume(obraId: Long): Double

    @Query("SELECT * FROM capitulos WHERE obraId = :obraId ORDER BY ordem")
    fun capitulos(obraId: Long): Flow<List<Capitulo>>
    @Query("SELECT * FROM capitulos WHERE obraId = :obraId ORDER BY ordem")
    suspend fun capitulosAgora(obraId: Long): List<Capitulo>
    @Query("SELECT c.id AS capId, c.obraId, c.titulo AS capTitulo, o.titulo AS obraTitulo FROM capitulos c JOIN obras o ON o.id = c.obraId ORDER BY o.atualizadaEm DESC, c.ordem")
    suspend fun indiceBusca(): List<ItemIndice>

    @Query("SELECT * FROM capitulos WHERE id = :id")
    fun capitulo(id: Long): Flow<Capitulo?>
    @Query("SELECT * FROM capitulos WHERE id = :id")
    suspend fun capituloAgora(id: Long): Capitulo?
    @Insert suspend fun inserirCapitulo(capitulo: Capitulo): Long
    @Update suspend fun atualizarCapitulo(capitulo: Capitulo)
    @Delete suspend fun excluirCapitulo(capitulo: Capitulo)
    @Query("SELECT IFNULL(MAX(ordem), 0) FROM capitulos WHERE obraId = :obraId")
    suspend fun maiorOrdemCapitulo(obraId: Long): Double

    @Query("UPDATE capitulos SET progresso = :progresso, posicaoBloco = :bloco, posicaoDeslocamento = :desloc WHERE id = :id")
    suspend fun salvarPosicao(id: Long, progresso: Float, bloco: Int, desloc: Int)

    @Query("UPDATE obras SET ultimoCapituloId = :capId, lidaEm = :quando WHERE id = :obraId")
    suspend fun marcarLeitura(obraId: Long, capId: Long, quando: Long)

    @Query("UPDATE capitulos SET palavras = :palavras, atualizadoEm = :quando WHERE id = :id")
    suspend fun atualizarPalavras(id: Long, palavras: Int, quando: Long)

    @Query("SELECT * FROM fichas WHERE obraId = :obraId ORDER BY tipo, nome COLLATE NOCASE")
    fun fichas(obraId: Long): Flow<List<Ficha>>
    @Query("SELECT * FROM fichas WHERE obraId = :obraId")
    suspend fun fichasAgora(obraId: Long): List<Ficha>
    @Insert suspend fun inserirFicha(f: Ficha): Long
    @Insert suspend fun inserirTrecho(t: FichaTrecho): Long
    @Query("DELETE FROM ficha_trechos WHERE capId = :capId")
    suspend fun apagarTrechosCap(capId: Long)
    @Query("SELECT COUNT(*) FROM ficha_trechos WHERE capId = :capId")
    suspend fun contarTrechosCap(capId: Long): Int
    @Query("SELECT t.texto AS texto, t.capId AS capId, c.ordem AS capOrdem, c.titulo AS capTitulo FROM ficha_trechos t JOIN capitulos c ON c.id = t.capId WHERE t.fichaId = :fichaId ORDER BY c.ordem, t.ordem")
    fun trechos(fichaId: Long): Flow<List<TrechoFicha>>
    @Update suspend fun atualizarFicha(f: Ficha)
    @Delete suspend fun excluirFicha(f: Ficha)

    @Query("SELECT * FROM lixeira ORDER BY excluidoEm DESC")
    fun lixeira(): Flow<List<ItemLixeira>>
    @Query("SELECT * FROM lixeira WHERE excluidoEm < :antes")
    suspend fun lixeiraAntiga(antes: Long): List<ItemLixeira>
    @Insert suspend fun inserirLixeira(i: ItemLixeira): Long
    @Delete suspend fun excluirLixeira(i: ItemLixeira)

    @Query("SELECT * FROM marcadores WHERE capId = :capId")
    fun marcadoresCap(capId: Long): Flow<List<Marcador>>
    @Query("SELECT * FROM marcadores WHERE obraId = :obraId ORDER BY capId, bloco")
    fun marcadoresObra(obraId: Long): Flow<List<Marcador>>
    @Insert suspend fun inserirMarcador(m: Marcador): Long
    @Update suspend fun atualizarMarcador(m: Marcador)
    @Delete suspend fun excluirMarcador(m: Marcador)

    @Query("SELECT * FROM eventos WHERE obraId = :obraId ORDER BY ordem")
    fun eventos(obraId: Long): Flow<List<Evento>>
    @Query("SELECT IFNULL(MAX(ordem), 0) FROM eventos WHERE obraId = :obraId")
    suspend fun maiorOrdemEvento(obraId: Long): Double
    @Insert suspend fun inserirEvento(e: Evento): Long
    @Update suspend fun atualizarEvento(e: Evento)
    @Delete suspend fun excluirEvento(e: Evento)

    @Query("SELECT DISTINCT colecao FROM obras WHERE colecao != '' ORDER BY colecao")
    fun colecoes(): Flow<List<String>>

    @Query("UPDATE obras SET atualizadaEm = :quando WHERE id = :id")
    suspend fun tocarObra(id: Long, quando: Long)
}

@Database(entities = [Obra::class, Volume::class, Capitulo::class, Ficha::class, ItemLixeira::class, Marcador::class, Evento::class, FichaTrecho::class], version = 8, exportSchema = true)
abstract class BancoNovely : RoomDatabase() {
    abstract fun dao(): NovelyDao

    companion object {
        fun criar(ctx: Context): BancoNovely =
            Room.databaseBuilder(ctx, BancoNovely::class.java, "novely.db")
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) { Sinc.prepararBanco(db) }
                })
                .addMigrations(object : androidx.room.migration.Migration(1, 2) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE capitulos ADD COLUMN resumo TEXT NOT NULL DEFAULT ''")
                    }
                }, object : androidx.room.migration.Migration(2, 3) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE capitulos ADD COLUMN notas TEXT NOT NULL DEFAULT ''")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `fichas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `obraId` INTEGER NOT NULL, `tipo` TEXT NOT NULL, `nome` TEXT NOT NULL, `descricao` TEXT NOT NULL, `imagem` TEXT, `criadaEm` INTEGER NOT NULL, FOREIGN KEY(`obraId`) REFERENCES `obras`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fichas_obraId` ON `fichas` (`obraId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `lixeira` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `obraId` INTEGER NOT NULL, `obraTitulo` TEXT NOT NULL, `volumeId` INTEGER, `titulo` TEXT NOT NULL, `ordem` REAL NOT NULL, `palavras` INTEGER NOT NULL, `resumo` TEXT NOT NULL, `notas` TEXT NOT NULL, `excluidoEm` INTEGER NOT NULL)")
                    }
                }, object : androidx.room.migration.Migration(3, 4) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE obras ADD COLUMN colecao TEXT NOT NULL DEFAULT ''")
                        db.execSQL("ALTER TABLE capitulos ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `marcadores` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `obraId` INTEGER NOT NULL, `capId` INTEGER NOT NULL, `bloco` INTEGER NOT NULL, `trecho` TEXT NOT NULL, `cor` TEXT NOT NULL, `nota` TEXT NOT NULL, `criadoEm` INTEGER NOT NULL, FOREIGN KEY(`obraId`) REFERENCES `obras`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_marcadores_obraId` ON `marcadores` (`obraId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_marcadores_capId` ON `marcadores` (`capId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `eventos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `obraId` INTEGER NOT NULL, `capId` INTEGER, `quando` TEXT NOT NULL, `titulo` TEXT NOT NULL, `descricao` TEXT NOT NULL, `ordem` REAL NOT NULL, FOREIGN KEY(`obraId`) REFERENCES `obras`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_eventos_obraId` ON `eventos` (`obraId`)")
                    }
                }, object : androidx.room.migration.Migration(4, 5) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE obras ADD COLUMN minha INTEGER NOT NULL DEFAULT 1")
                        db.execSQL("UPDATE obras SET minha = 0 WHERE status = 'LENDO'")
                    }
                }, object : androidx.room.migration.Migration(5, 6) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE obras ADD COLUMN trancada INTEGER NOT NULL DEFAULT 0")
                        db.execSQL("ALTER TABLE obras ADD COLUMN autor TEXT NOT NULL DEFAULT ''")
                    }
                }, object : androidx.room.migration.Migration(7, 8) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        for (t in Sinc.TABELAS) {
                            db.execSQL("ALTER TABLE $t ADD COLUMN nuvemId TEXT NOT NULL DEFAULT ''")
                            db.execSQL("ALTER TABLE $t ADD COLUMN alteradoEm INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("UPDATE $t SET nuvemId = lower(hex(randomblob(16))), alteradoEm = ${System.currentTimeMillis()}")
                        }
                    }
                }, object : androidx.room.migration.Migration(6, 7) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        for (c in listOf("apelidos", "idade", "aparencia", "poder", "faccao", "relacoes"))
                            db.execSQL("ALTER TABLE fichas ADD COLUMN $c TEXT NOT NULL DEFAULT ''")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `ficha_trechos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fichaId` INTEGER NOT NULL, `capId` INTEGER NOT NULL, `ordem` INTEGER NOT NULL, `texto` TEXT NOT NULL, FOREIGN KEY(`fichaId`) REFERENCES `fichas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`capId`) REFERENCES `capitulos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ficha_trechos_fichaId` ON `ficha_trechos` (`fichaId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ficha_trechos_capId` ON `ficha_trechos` (`capId`)")
                    }
                })
                .build()
    }
}


/**
 * Peças de banco para a sincronização:
 *  - gatilhos marcam alteradoEm (e geram nuvemId) em toda mudança feita pelo app;
 *  - exclusões viram "lápides" para apagar também na nuvem;
 *  - sync_flag = 1 enquanto a própria sincronização grava (aí os gatilhos ficam quietos).
 */
object Sinc {
    val TABELAS = listOf("obras", "volumes", "capitulos", "fichas", "eventos", "marcadores")
    private const val AGORA = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"

    fun prepararBanco(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_flag (id INTEGER PRIMARY KEY, ativo INTEGER NOT NULL)")
        db.execSQL("INSERT OR IGNORE INTO sync_flag (id, ativo) VALUES (0, 0)")
        db.execSQL("UPDATE sync_flag SET ativo = 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS exclusoes (nuvemId TEXT PRIMARY KEY NOT NULL, tipo TEXT NOT NULL, em INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS sincronia (chave TEXT PRIMARY KEY NOT NULL, enviadoEm INTEGER NOT NULL DEFAULT 0, textoHash TEXT NOT NULL DEFAULT '', capaHash TEXT NOT NULL DEFAULT '', bannerHash TEXT NOT NULL DEFAULT '')")
        val quieto = "(SELECT ativo FROM sync_flag WHERE id = 0) = 0"
        for (t in TABELAS) {
            // versão 1 dos gatilhos entrava em recursão em aparelhos com recursive_triggers ligado
            db.execSQL("DROP TRIGGER IF EXISTS nv_ins_$t")
            db.execSQL("DROP TRIGGER IF EXISTS nv_upd_$t")
            db.execSQL("DROP TRIGGER IF EXISTS nv_del_$t")
            // A marca nova é sempre MAIOR que a anterior; o gatilho de update só age quando alteradoEm
            // não subiu (escrita do app) — a própria correção sobe o valor, então nunca se repete.
            db.execSQL("CREATE TRIGGER IF NOT EXISTS nv2_ins_$t AFTER INSERT ON $t WHEN $quieto BEGIN " +
                "UPDATE $t SET alteradoEm = MAX($AGORA, NEW.alteradoEm + 1), nuvemId = CASE WHEN NEW.nuvemId = '' THEN lower(hex(randomblob(16))) ELSE NEW.nuvemId END WHERE id = NEW.id; END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS nv2_upd_$t AFTER UPDATE ON $t WHEN $quieto AND NEW.alteradoEm <= OLD.alteradoEm BEGIN " +
                "UPDATE $t SET alteradoEm = MAX($AGORA, OLD.alteradoEm + 1), nuvemId = CASE WHEN NEW.nuvemId = '' THEN OLD.nuvemId ELSE NEW.nuvemId END WHERE id = NEW.id; END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS nv2_del_$t AFTER DELETE ON $t WHEN $quieto AND OLD.nuvemId <> '' BEGIN " +
                "INSERT OR REPLACE INTO exclusoes (nuvemId, tipo, em) VALUES (OLD.nuvemId, '$t', $AGORA); END")
        }
    }
}
