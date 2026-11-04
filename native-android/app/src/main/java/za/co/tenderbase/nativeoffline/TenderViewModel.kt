package za.co.tenderbase.nativeoffline

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant

class TenderViewModel(application:Application):AndroidViewModel(application){
    private val database=TenderDatabase.get(application)
    private val dao=database.dao()
    val tenders=dao.observeTenders().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val bookmarks=dao.observeBookmarks().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val bids=dao.observeBids().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val dismissed=dao.observeDismissed().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val preferences=dao.observePreferences().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val lastSync=dao.observeMeta("lastServerSync").stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
    var importStatus by mutableStateOf("Loading offline tenders…")
        private set
    var message by mutableStateOf<String?>(null)
        private set
    val notesStatus=mutableStateMapOf<String,String>()
    private val noteLock=Mutex()
    private val revisions=mutableMapOf<String,Int>()
    init {
        viewModelScope.launch(Dispatchers.IO){
            try {
                val n=SnapshotInstaller.seed(application,database)
                withContext(Dispatchers.Main){importStatus=if(n>0)"ready" else "No tenders available"}
            }catch(e:Exception){
                android.util.Log.e("TenderBase","Database initialization failed",e)
                withContext(Dispatchers.Main){importStatus="Offline import failed: "+(e.message?:"unknown")}
            }
        }
    }
    fun feedback(text:String){message=text}
    fun clearMessage(){message=null}
    fun save(id:String,saved:Boolean){
        viewModelScope.launch{
            try{
                withContext(Dispatchers.IO){if(saved)dao.unsave(id)else dao.save(Bookmark(id))}
                message=if(saved)"Removed from saved" else "Saved on this device"
            }catch(_:Exception){message="Unable to update saved tenders"}
        }
    }
    fun track(id:String,onDone:()->Unit={}){
        viewModelScope.launch{
            try{
                val changed=withContext(Dispatchers.IO){dao.track(Bid(id,"New match","",Instant.now().toString()))}
                message=if(changed==-1L)"Bid already being tracked" else "Bid added to tracking"
                onDone()
            }catch(_:Exception){message="Unable to track this bid"}
        }
    }
    fun removeBid(id:String){
        viewModelScope.launch{
            try{withContext(Dispatchers.IO){dao.untrack(id)};message="Removed from tracking"}
            catch(_:Exception){message="Could not remove tracked bid"}
        }
    }
    fun stage(id:String,stage:String){
        if(stage !in BID_STAGES)return
        viewModelScope.launch{
            try{
                withContext(Dispatchers.IO){dao.updateStage(id,stage,Instant.now().toString())}
                message="Bid stage saved on this device"
            }catch(_:Exception){message="Stage could not be saved"}
        }
    }
    fun notes(id:String,text:String){
        val version=(revisions[id]?:0)+1
        revisions[id]=version
        notesStatus[id]="Saving…"
        viewModelScope.launch{
            try{
                noteLock.withLock{
                    withContext(Dispatchers.IO){dao.updateNotes(id,text,Instant.now().toString())}
                }
                if(revisions[id]==version)notesStatus[id]="Saved on this device"
            }catch(_:Exception){
                if(revisions[id]==version)notesStatus[id]="Failed to save"
            }
        }
    }
    fun dismiss(t:Tender){
        viewModelScope.launch {
            try{withContext(Dispatchers.IO){dao.dismiss(DismissedAlert(t.id+"|"+(t.closingAt?:"")))}}
            catch(_:Exception){message="Unable to dismiss alert"}
        }
    }
    fun restoreAlerts(){
        viewModelScope.launch {
            try{withContext(Dispatchers.IO){dao.restoreAlerts()};message="Dismissed alerts restored"}
            catch(_:Exception){message="Unable to restore alerts"}
        }
    }
    fun setting(name:String,value:String){
        viewModelScope.launch{
            try{withContext(Dispatchers.IO){dao.setPreference(Preference(name,value))}}
            catch(_:Exception){message="Settings could not be saved"}
        }
    }
}
