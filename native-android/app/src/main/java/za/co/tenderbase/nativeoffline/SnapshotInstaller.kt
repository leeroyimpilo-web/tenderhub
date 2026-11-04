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
        // Upgrade v1 installations in-place, retaining bookmarks, notes and preferences.
        if(dao.count()>0 && dao.meta("bootstrapSchema")=="2")return dao.count()
        val all=ArrayList<Tender>()
        var exported=""
        var lastSync=""
        var expected=0
        context.assets.open("bootstrap.tenderpack").use { asset ->
            GZIPInputStream(asset).use { zip ->
                BufferedReader(InputStreamReader(zip, Charsets.UTF_8)).use { reader ->
                    val h=JSONObject(reader.readLine() ?: error("Offline snapshot missing"))
                    check(h.getString("type")=="tenderbase-offline-bootstrap-v1")
                    expected=h.getInt("count")
                    check(expected>=1000)
                    exported=h.getString("exportedAt")
                    lastSync=h.getString("lastServerSync")
                    while(true){
                        val line=reader.readLine()?:break
                        if(line.isBlank())continue
                        val t=JSONObject(line)
                        val id=t.getString("id")
                        val title=t.getString("title")
                        val url=t.getString("source_url")
                        check(id.isNotBlank() && title.isNotBlank() && url.startsWith("https://"))
                        all.add(Tender(id=id,title=title,sourceUrl=url,
                            reference=t.str("reference"),description=t.str("description"),
                            buyer=t.str("buyer"),province=t.str("province"),category=t.str("category"),
                            status=t.str("status"),publishedAt=t.str("published_at"),
                            closingAt=t.str("closing_at"),releaseAt=t.str("release_at"),
                            documentsJson=(t.optJSONArray("documents")?:JSONArray()).toString(),
                            syncedAt=t.str("synced_at"),
                            contactName=t.str("contact_name"),contactEmail=t.str("contact_email"),
                            contactPhone=t.str("contact_phone"),briefingAt=t.str("briefing_at"),
                            briefingVenue=t.str("briefing_venue")
                        ))
                    }
                }
            }
        }
        check(all.size==expected && all.map{it.id}.toSet().size==expected){
            "Official tender snapshot is incomplete"
        }
        database.withTransaction {
            all.chunked(100).forEach{dao.upsertTenders(it)}
            dao.upsertMeta(SnapshotMeta("exportedAt",exported))
            dao.upsertMeta(SnapshotMeta("lastServerSync",lastSync))
            dao.upsertMeta(SnapshotMeta("recordCount",expected.toString()))
            dao.upsertMeta(SnapshotMeta("bootstrapSchema","2"))
        }
        return expected
    }
}
