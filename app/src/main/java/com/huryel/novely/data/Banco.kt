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
)

data class ItemIndice(val capId: Long, val obraId: Long, val capTitulo: String, val obraTitulo: String)

@Dao
interface NovelyDao {
    @Query(
        """SELECT o.id, o.titulo, o.status, o.capa, o.capaX, o.capaY, o.capaZoom, o.corBase,
           o.ultimoCapituloId, o.lidaEm,
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

    @Query("UPDATE obras SET atualizadaEm = :quando WHERE id = :id")
    suspend fun tocarObra(id: Long, quando: Long)
}

@Database(entities = [Obra::class, Volume::class, Capitulo::class], version = 2, exportSchema = true)
abstract class BancoNovely : RoomDatabase() {
    abstract fun dao(): NovelyDao

    companion object {
        fun criar(ctx: Context): BancoNovely =
            Room.databaseBuilder(ctx, BancoNovely::class.java, "novely.db")
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(object : androidx.room.migration.Migration(1, 2) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE capitulos ADD COLUMN resumo TEXT NOT NULL DEFAULT ''")
                    }
                })
                .build()
    }
}
