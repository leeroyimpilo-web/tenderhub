package za.co.tenderbase.nativeoffline

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val navy = Color(0xFF102542)
private val blue = Color(0xFF245BDA)

class TenderViewModel(application: Application): AndroidViewModel(application) {
    private val db = TenderDatabase.get(application)
    val tenders = db.dao().observeTenders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bookmarks = db.dao().observeBookmarks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lastSync = db.dao().observeMeta("lastServerSync").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    var importStatus by mutableStateOf("Loading official offline tenders…")
        private set
    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                SnapshotInstaller.seed(application, db)
                withContext(Dispatchers.Main) { importStatus = "ready" }
            } catch (error: Exception) {
                android.util.Log.e("TenderBase", "Unable to install offline snapshot", error)
                withContext(Dispatchers.Main) {
                    importStatus = "Offline data error: " + (error.message ?: "unknown")
                }
            }
        }
    }
    fun toggleSave(id: String, saved: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (saved) db.dao().unsave(id) else db.dao().save(Bookmark(id))
        }
    }
}

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[TenderViewModel::class.java]
        setContent { NativeApp(model) }
    }
}

private fun parseDate(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try { Instant.parse(value) } catch (_: Exception) {
        try { OffsetDateTime.parse(value).toInstant() } catch (_: Exception) { null }
    }
}
private fun dateText(value: String?): String =
    parseDate(value)?.let {
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
            .withZone(ZoneId.of("Africa/Johannesburg")).format(it)
    } ?: "Not provided"

private fun openTender(t: Tender): Boolean =
    parseDate(t.closingAt)?.isAfter(Instant.now()) == true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NativeApp(vm: TenderViewModel) {
    val all by vm.tenders.collectAsState()
    val saves by vm.bookmarks.collectAsState()
    val sync by vm.lastSync.collectAsState()
    val savedIds = remember(saves) { saves.toSet() }
    var tab by rememberSaveable { mutableStateOf("Today") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var province by rememberSaveable { mutableStateOf("All provinces") }
    var category by rememberSaveable { mutableStateOf("All categories") }
    var status by rememberSaveable { mutableStateOf("Open") }
    var sort by rememberSaveable { mutableStateOf("Newest") }
    val selection = all.firstOrNull { it.id == selectedId }
    val filtered = remember(all, query, province, category, status, sort) {
        val found = all.filter { t ->
            (query.isBlank() || listOf(t.title, t.reference, t.buyer, t.description).any {
                it?.contains(query.trim(), ignoreCase = true) == true
            }) &&
            (province == "All provinces" || province == t.province) &&
            (category == "All categories" || category == t.category) &&
            (status == "All" || if (status == "Open") openTender(t) else
                parseDate(t.closingAt)?.isBefore(Instant.now()) == true)
        }
        when (sort) {
            "Closing" -> found.sortedWith(compareBy(nullsLast()) { parseDate(it.closingAt) })
            "Oldest" -> found.sortedBy { parseDate(it.publishedAt) ?: Instant.EPOCH }
            else -> found.sortedByDescending { parseDate(it.publishedAt) ?: Instant.EPOCH }
        }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = blue, onPrimary = Color.White, onSurface = navy)) {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text(if (selection == null) "TenderBase" else "Tender details", fontWeight = FontWeight.Bold)
                        Text("NATIVE / OFFLINE", style = MaterialTheme.typography.labelSmall, color = blue)
                    }
                }, navigationIcon = {
                    if (selection != null) TextButton(onClick = { selectedId = null }) { Text("Back") }
                })
            },
            bottomBar = {
                if (selection == null) NavigationBar {
                    listOf("Today", "Find", "Saved", "About").forEach { name ->
                        NavigationBarItem(selected = tab == name, onClick = { tab = name },
                            icon = { Text(when(name) {
                                "Today" -> "⌂"
                                "Find" -> "⌕"
                                "Saved" -> "★"
                                else -> "ⓘ"
                            }) }, label = { Text(name) })
                    }
                }
            }
        ) { padding ->
            if (vm.importStatus != "ready" && all.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!vm.importStatus.startsWith("Offline data error")) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                        }
                        Text(vm.importStatus)
                    }
                }
            } else if (selection != null) {
                TenderDetail(selection, saved = selection.id in savedIds,
                    toggle = { vm.toggleSave(selection.id, selection.id in savedIds) },
                    modifier = Modifier.padding(padding))
            } else when (tab) {
                "Today" -> {
                    val open = all.filter(::openTender)
                    val closing = open.sortedBy { parseDate(it.closingAt) }.take(15)
                    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                        Text("Official tenders available offline", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Stored: " + all.size + "   Open: " + open.size)
                        Text("Last official sync: " + dateText(sync), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        Text("Upcoming deadlines", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        TenderList(closing, savedIds, onOpen = { selectedId = it },
                            onSave = { id -> vm.toggleSave(id, id in savedIds) },
                            modifier = Modifier.weight(1f))
                    }
                }
                "Find" -> {
                    val provinces = remember(all) {
                        listOf("All provinces") + all.mapNotNull { it.province?.takeIf(String::isNotBlank) }.distinct().sorted()
                    }
                    val categories = remember(all) {
                        listOf("All categories") + all.mapNotNull { it.category?.takeIf(String::isNotBlank) }.distinct().sorted()
                    }
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                        Text("Find tenders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search title, buyer, reference, description") }, singleLine = true)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 8.dp)) {
                            item { FilterMenu(province, provinces) { province = it } }
                            item { FilterMenu(category, categories) { category = it } }
                            item { FilterMenu(status, listOf("Open", "Closed", "All")) { status = it } }
                            item { FilterMenu(sort, listOf("Newest", "Closing", "Oldest")) { sort = it } }
                        }
                        Text(filtered.size.toString() + " matching notices", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        TenderList(filtered, savedIds, onOpen = { selectedId = it },
                            onSave = { id -> vm.toggleSave(id, id in savedIds) },
                            modifier = Modifier.weight(1f))
                    }
                }
                "Saved" -> {
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                        Text("Saved tenders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Stored privately on this phone")
                        Spacer(Modifier.height(12.dp))
                        TenderList(all.filter { it.id in savedIds }, savedIds, onOpen = { selectedId = it },
                            onSave = { id -> vm.toggleSave(id, true) },
                            modifier = Modifier.weight(1f))
                    }
                }
                else -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                    Text("Offline database", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(all.size.toString() + " official tender records on this device")
                    Spacer(Modifier.height(12.dp))
                    Text("Source last updated: " + dateText(sync))
                    Spacer(Modifier.height(12.dp))
                    Text("This native MVP needs no internet or account to browse, search or save tenders.")
                    Text("New tender downloads, cached PDFs, research and bid pipeline come in later sprints.")
                }
            }
        }
    }
}

@Composable
private fun FilterMenu(value: String, options: List<String>, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(value + " ▾", maxLines = 1) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { change(option); expanded = false })
            }
        }
    }
}

@Composable
private fun TenderList(tenders: List<Tender>, saved: Set<String>, onOpen: (String) -> Unit,
                       onSave: (String) -> Unit, modifier: Modifier = Modifier) {
    if (tenders.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No matching tenders.") }
        return
    }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(bottom = 20.dp)) {
        items(tenders, key = { it.id }) { tender ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(tender.id) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA))) {
                Column(Modifier.padding(14.dp)) {
                    Text(tender.reference ?: tender.id, color = blue, style = MaterialTheme.typography.labelMedium)
                    Text(tender.title, fontWeight = FontWeight.SemiBold, maxLines = 3,
                        overflow = TextOverflow.Ellipsis)
                    Text(tender.buyer ?: "Buyer not specified", style = MaterialTheme.typography.bodySmall)
                    Text("Closes: " + dateText(tender.closingAt), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onSave(tender.id) }, modifier = Modifier.align(Alignment.End)) {
                        Text(if (tender.id in saved) "★ Unsave" else "☆ Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun TenderDetail(t: Tender, saved: Boolean, toggle: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val docs = remember(t.documentsJson) {
        try {
            val array = JSONArray(t.documentsJson)
            (0 until array.length()).mapNotNull { i ->
                array.optJSONObject(i)?.let { Pair(it.optString("title", "Document"), it.optString("url", "")) }
            }
        } catch (_: Exception) { emptyList() }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(t.reference ?: t.id, color = blue)
        Text(t.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Button(onClick = toggle) { Text(if (saved) "Remove saved tender" else "Save tender") }
        Spacer(Modifier.height(12.dp))
        DetailRow("Buyer", t.buyer)
        DetailRow("Province", t.province)
        DetailRow("Category", t.category)
        DetailRow("Published", dateText(t.publishedAt))
        DetailRow("Closing", dateText(t.closingAt))
        DetailRow("Description", t.description)
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("Official documents (" + docs.size + ")", fontWeight = FontWeight.Bold)
        Text("Document names are available offline. Downloading documents requires internet.",
            style = MaterialTheme.typography.bodySmall)
        docs.forEach { (name, url) ->
            if (url.startsWith("https://")) TextButton(onClick = {
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                catch (_: Exception) {
                    Toast.makeText(context, "No viewer installed", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Open online: " + name) }
        }
        Text("Official source: " + t.sourceUrl, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Spacer(Modifier.height(12.dp))
    Text(label, color = blue, style = MaterialTheme.typography.labelMedium)
    Text(value)
}
