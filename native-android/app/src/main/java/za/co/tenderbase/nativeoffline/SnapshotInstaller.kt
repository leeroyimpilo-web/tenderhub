package za.co.tenderbase.nativeoffline

import android.content.Context
import androidx.room.withTransaction
import org.json.JSONObject
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

object SnapshotInstaller {
    private fun JSONObject.str(key: String): String? =
        if (isNull(key) || !has(key)) null else optString(key).takeUnless { it == "null" }

    suspend fun seed(context: Context, database: TenderDatabase): Int {
        val dao = database.dao()
        val existing = dao.count()
        if (existing > 0) return existing

        val all = ArrayList<Tender>()
        var snapshotCount = 0
        var exportTime = ""
        var serverSync = ""

        context.assets.open("bootstrap.jsonl.gz").use { asset ->
            GZIPInputStream(asset).use { gzip ->
                BufferedReader(InputStreamReader(gzip, Charsets.UTF_8)).use { reader ->
                    val header = JSONObject(reader.readLine() ?: error("Snapshot header missing"))
                    check(header.getString("type") == "tenderbase-offline-bootstrap-v1") {
                        "Unexpected data source in offline snapshot"
                    }
                    snapshotCount = header.getInt("count")
                    exportTime = header.getString("exportedAt")
                    serverSync = header.getString("lastServerSync")
                    check(snapshotCount >= 1000) { "Insufficient official tender records" }
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isBlank()) continue
                        val o = JSONObject(line)
                        val doc = o.optJSONArray("documents") ?: JSONArray()
                        val id = o.getString("id")
                        val title = o.getString("title")
                        val url = o.getString("source_url")
                        check(id.isNotBlank() && title.isNotBlank() && url.startsWith("https://"))
                        all.add(Tender(
                            id=id, reference=o.str("reference"), title=title,
                            description=o.str("description"), buyer=o.str("buyer"),
                            province=o.str("province"), category=o.str("category"),
                            status=o.str("status"), publishedAt=o.str("published_at"),
                            closingAt=o.str("closing_at"), releaseAt=o.str("release_at"),
                            sourceUrl=url, documentsJson=doc.toString(),
                            syncedAt=o.str("synced_at")
                        ))
                    }
                }
            }
        }
        check(all.size == snapshotCount && all.map { it.id }.toSet().size == snapshotCount) {
            "Snapshot incomplete or duplicate records"
        }
        database.withTransaction {
            all.chunked(100).forEach { dao.upsertTenders(it) }
            dao.upsertMeta(SnapshotMeta("exportedAt", exportTime))
            dao.upsertMeta(SnapshotMeta("lastServerSync", serverSync))
            dao.upsertMeta(SnapshotMeta("recordCount", snapshotCount.toString()))
        }
        return snapshotCount
    }
}
