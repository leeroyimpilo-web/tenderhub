package za.co.tenderbase.nativeoffline

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tenders", indices = [Index("province"), Index("category"), Index("closingAt")])
data class Tender(
    @PrimaryKey val id: String,
    val reference: String?,
    val title: String,
    val description: String?,
    val buyer: String?,
    val province: String?,
    val category: String?,
    val status: String?,
    val publishedAt: String?,
    val closingAt: String?,
    val releaseAt: String?,
    val sourceUrl: String,
    val documentsJson: String,
    val syncedAt: String?
)
@Entity(tableName = "bookmarks")
data class Bookmark(@PrimaryKey val tenderId: String)
@Entity(tableName = "snapshot_meta")
data class SnapshotMeta(@PrimaryKey val name: String, val value: String)

@Dao
interface TenderDao {
    @Query("SELECT COUNT(*) FROM tenders")
    suspend fun count(): Int

    @Query("SELECT * FROM tenders ORDER BY COALESCE(publishedAt,releaseAt) DESC, id ASC")
    fun observeTenders(): Flow<List<Tender>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTenders(items: List<Tender>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: SnapshotMeta)

    @Query("SELECT value FROM snapshot_meta WHERE name = :key LIMIT 1")
    fun observeMeta(key: String): Flow<String?>

    @Query("SELECT tenderId FROM bookmarks")
    fun observeBookmarks(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun save(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE tenderId = :id")
    suspend fun unsave(id: String)
}

@Database(entities = [Tender::class, Bookmark::class, SnapshotMeta::class], version = 1, exportSchema = false)
abstract class TenderDatabase : RoomDatabase() {
    abstract fun dao(): TenderDao

    companion object {
        @Volatile private var instance: TenderDatabase? = null
        fun get(context: Context): TenderDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, TenderDatabase::class.java,
                    "tenderbase-native-v1.db").build().also { instance = it }
            }
    }
}
