package za.co.tenderbase.nativeoffline

import android.content.Context
import androidx.room.withTransaction
import org.json.JSONObject
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

object SnapshotInstaller {
    private fun JSONObject.str(key:String):String? =
        if(!has(key)||isNull(key)) null else optString(key).takeUnless {it=="null"||it.isBlank()}

    suspend fun seed(context:Context,database:TenderDatabase):Int {
        val dao=database.dao()
        // A completed prior install must preserve bookmarks, bid notes and settings.
        val count=dao.count()
        if(count>0 && dao.meta("bootstrapSchema")=="2")return count

        // Parse on the ViewModel's IO dispatcher and upsert a small batch at a time.
        // Previously the importer retained every JSON notice and document list in
        // an ArrayList before writing; this caused avoidable first-launch RAM spikes.
        // One transaction still guarantees that a malformed snapshot cannot leave
        // behind a partially imported offline database.
        return database.withTransaction {
            val uniqueIds=HashSet<String>()
            val batch=ArrayList<Tender>(100)
            var expected=0
            var records=0
            var exported=""
            var lastSync=""

            context.assets.open("bootstrap.tenderpack").use { asset ->
                GZIPInputStream(asset).use { zip ->
                    BufferedReader(InputStreamReader(zip,Charsets.UTF_8)).use { reader ->
                        val header=JSONObject(reader.readLine()?:error("Offline snapshot missing"))
                        check(header.getString("type")=="tenderbase-offline-bootstrap-v1")
                        expected=header.getInt("count")
                        check(expected>=1000){"Official snapshot is incomplete"}
                        exported=header.getString("exportedAt")
                        lastSync=header.getString("lastServerSync")
                        while(true){
                            val line=reader.readLine()?:break
                            if(line.isBlank())continue
                            val item=JSONObject(line)
                            val id=item.getString("id")
                            val title=item.getString("title")
                            val url=item.getString("source_url")
                            check(id.isNotBlank()&&title.isNotBlank()&&url.startsWith("https://"))
                            check(uniqueIds.add(id)){"Duplicate official tender id"}

                            batch.add(Tender(id=id,title=title,sourceUrl=url,
                                reference=item.str("reference"),description=item.str("description"),
                                buyer=item.str("buyer"),province=item.str("province"),
                                category=item.str("category"),status=item.str("status"),
                                publishedAt=item.str("published_at"),closingAt=item.str("closing_at"),
                                releaseAt=item.str("release_at"),
                                documentsJson=(item.optJSONArray("documents")?:JSONArray()).toString(),
                                syncedAt=item.str("synced_at"),
                                contactName=item.str("contact_name"),
                                contactEmail=item.str("contact_email"),
                                contactPhone=item.str("contact_phone"),
                                briefingAt=item.str("briefing_at"),
                                briefingVenue=item.str("briefing_venue")
                            ))
                            records++
                            if(batch.size>=100){
                                dao.upsertTenders(batch.toList())
                                batch.clear()
                            }
                        }
                    }
                }
            }
            if(batch.isNotEmpty())dao.upsertTenders(batch)
            check(records==expected && uniqueIds.size==expected){
                "Official tender snapshot is incomplete"
            }
            dao.upsertMeta(SnapshotMeta("exportedAt",exported))
            dao.upsertMeta(SnapshotMeta("lastServerSync",lastSync))
            dao.upsertMeta(SnapshotMeta("recordCount",expected.toString()))
            dao.upsertMeta(SnapshotMeta("bootstrapSchema","2"))
            expected
        }
    }
}
