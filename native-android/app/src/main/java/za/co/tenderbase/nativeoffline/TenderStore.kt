package za.co.tenderbase.nativeoffline

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName="tenders", indices=[Index("province"),Index("category"),Index("closingAt")])
data class Tender(
    @PrimaryKey val id:String,
    val reference:String?, val title:String, val description:String?,
    val buyer:String?, val province:String?, val category:String?, val status:String?,
    val publishedAt:String?, val closingAt:String?, val releaseAt:String?,
    val sourceUrl:String, val documentsJson:String, val syncedAt:String?,
    val contactName:String?=null, val contactEmail:String?=null, val contactPhone:String?=null,
    val briefingAt:String?=null, val briefingVenue:String?=null
)
@Entity(tableName="bookmarks") data class Bookmark(@PrimaryKey val tenderId:String)
@Entity(tableName="snapshot_meta") data class SnapshotMeta(@PrimaryKey val name:String,val value:String)
@Entity(tableName="bids") data class Bid(
    @PrimaryKey val tenderId:String, val stage:String="New match",
    val notes:String="", val updatedAt:String
)
@Entity(tableName="dismissed_alerts") data class DismissedAlert(@PrimaryKey val key:String)
@Entity(tableName="preferences") data class Preference(@PrimaryKey val name:String,val value:String)

val BID_STAGES = listOf("New match","Review","Bid planning","Preparing",
    "Ready to submit","Submitted","Award pending","Won","Lost")

@Dao interface TenderDao {
    @Query("SELECT COUNT(*) FROM tenders") suspend fun count():Int
    @Query("SELECT * FROM tenders ORDER BY COALESCE(publishedAt,releaseAt) DESC, id ASC")
    fun observeTenders():Flow<List<Tender>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsertTenders(items:List<Tender>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsertMeta(meta:SnapshotMeta)
    @Query("SELECT value FROM snapshot_meta WHERE name=:key LIMIT 1")
    fun observeMeta(key:String):Flow<String?>
    @Query("SELECT value FROM snapshot_meta WHERE name=:key LIMIT 1")
    suspend fun meta(key:String):String?
    @Query("SELECT tenderId FROM bookmarks") fun observeBookmarks():Flow<List<String>>
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun save(bookmark:Bookmark)
    @Query("DELETE FROM bookmarks WHERE tenderId=:id") suspend fun unsave(id:String)
    @Query("SELECT * FROM bids ORDER BY updatedAt DESC") fun observeBids():Flow<List<Bid>>
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun track(bid:Bid):Long
    @Query("UPDATE bids SET stage=:stage, updatedAt=:at WHERE tenderId=:id")
    suspend fun updateStage(id:String, stage:String, at:String)
    @Query("UPDATE bids SET notes=:notes, updatedAt=:at WHERE tenderId=:id")
    suspend fun updateNotes(id:String, notes:String, at:String)
    @Query("DELETE FROM bids WHERE tenderId=:id") suspend fun untrack(id:String)
    @Query("SELECT key FROM dismissed_alerts") fun observeDismissed():Flow<List<String>>
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun dismiss(item:DismissedAlert)
    @Query("DELETE FROM dismissed_alerts") suspend fun restoreAlerts()
    @Query("SELECT * FROM preferences") fun observePreferences():Flow<List<Preference>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun setPreference(item:Preference)
}

@Database(entities=[Tender::class,Bookmark::class,SnapshotMeta::class,Bid::class,
    DismissedAlert::class,Preference::class],version=2,exportSchema=false)
abstract class TenderDatabase:RoomDatabase(){
    abstract fun dao():TenderDao
    companion object{
        private val migrate1to2=object:Migration(1,2){
            override fun migrate(db:SupportSQLiteDatabase){
                db.execSQL("ALTER TABLE tenders ADD COLUMN contactName TEXT")
                db.execSQL("ALTER TABLE tenders ADD COLUMN contactEmail TEXT")
                db.execSQL("ALTER TABLE tenders ADD COLUMN contactPhone TEXT")
                db.execSQL("ALTER TABLE tenders ADD COLUMN briefingAt TEXT")
                db.execSQL("ALTER TABLE tenders ADD COLUMN briefingVenue TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS bids (tenderId TEXT NOT NULL PRIMARY KEY, stage TEXT NOT NULL, notes TEXT NOT NULL, updatedAt TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS dismissed_alerts (\`key\` TEXT NOT NULL PRIMARY KEY)")
                db.execSQL("CREATE TABLE IF NOT EXISTS preferences (name TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
            }
        }
        @Volatile private var instance:TenderDatabase?=null
        fun get(context:Context):TenderDatabase=
            instance?:synchronized(this){
                instance?:Room.databaseBuilder(context.applicationContext,TenderDatabase::class.java,
                    "tenderbase-native-v1.db").addMigrations(migrate1to2).build().also {instance=it}
            }
    }
}
