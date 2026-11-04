package za.co.tenderbase.nativeoffline

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MyBidsScreen(all:List<Tender>,saved:Set<String>,bids:List<Bid>,selectedTab:String,
                 onTab:(String)->Unit,onOpen:(String)->Unit,onSave:(String)->Unit,
                 onTrack:(String)->Unit,onRemoveBid:(String)->Unit,
                 onStage:(String,String)->Unit,onNotes:(String,String)->Unit,
                 noteStates:Map<String,String>,onDiscover:()->Unit,modifier:Modifier=Modifier){
    val allById=remember(all){all.associateBy{it.id}}
    val savedItems=all.filter{it.id in saved}
    val tracked=BID_STAGES // shared official app stages, all manual
    var confirmUnsave by remember {mutableStateOf<String?>(null)}
    var confirmUntrack by remember {mutableStateOf<String?>(null)}
    Column(modifier.fillMaxSize().testTag("my-bids-screen").padding(horizontal=20.dp)){
        Spacer(Modifier.height(8.dp))
        Headline("My bids")
        Text("From shortlist to submission.",style=MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().border(1.dp,TB.border,RoundedCornerShape(10.dp)),
            horizontalArrangement=Arrangement.spacedBy(0.dp)){
            listOf("Saved" to saved.size,"Tracking" to bids.size).forEach{(key,n)->
                val active=selectedTab==key
                if(active)Button(onClick={onTab(key)},
                    modifier=Modifier.weight(1f).heightIn(min=48.dp),
                    shape=RoundedCornerShape(9.dp)){
                    Text(key+" "+n)
                }else TextButton(onClick={onTab(key)},
                    modifier=Modifier.weight(1f).heightIn(min=48.dp)){
                    Text(key+" "+n)
                }
            }
        }
        Text("Stages and notes stay on this device.",
            modifier=Modifier.padding(top=8.dp,bottom=12.dp),
            style=MaterialTheme.typography.bodySmall)
        if(selectedTab=="Saved"){
            if(savedItems.isEmpty())StateMessage("No saved tenders",
                "Discover an official tender and tap Save to add it to your watchlist.",
                action="Discover tenders",onAction=onDiscover)
            else LazyColumn(Modifier.fillMaxSize(),
                contentPadding=PaddingValues(bottom=22.dp)){
                items(savedItems,key={it.id}){t->
                    Column(Modifier.padding(vertical=10.dp)){
                        Text(t.reference?:"Reference not supplied",
                            style=MaterialTheme.typography.labelMedium)
                        Text(t.title,style=MaterialTheme.typography.titleMedium,
                            modifier=Modifier.fillMaxWidth().clickable{onOpen(t.id)})
                        Text((t.buyer?:"Buyer not supplied")+" · "+(t.province?:"Province not supplied"),
                            style=MaterialTheme.typography.bodySmall)
                        Text("Closes "+formatDate(t.closingAt,true),
                            style=MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
                            verticalAlignment=Alignment.CenterVertically){
                            OutlinedButton(onClick={onOpen(t.id)}){
                                Text("Open tender")
                            }
                            Button(onClick={onTrack(t.id)}){
                                Text(if(bids.any{it.tenderId==t.id})"Open tracking" else "Track bid")
                            }
                            IconButton(onClick={confirmUnsave=t.id}){
                                Icon(Icons.Outlined.BookmarkRemove,contentDescription="Remove saved tender")
                            }
                        }
                    }
                    AppDivider()
                }
            }
        }else{
            if(bids.isEmpty())StateMessage("No bids tracked yet",
                "Track bid creates your private preparation record. It does not submit a tender.",
                action="Find opportunities",onAction=onDiscover)
            else LazyColumn(Modifier.fillMaxSize(),
                verticalArrangement=Arrangement.spacedBy(10.dp),
                contentPadding=PaddingValues(bottom=22.dp)){
                items(bids,key={it.tenderId}){bid->
                    val t=allById[bid.tenderId]
                    if(t!=null)BidEditorCard(
                        tender=t,bid=bid,noteStatus=noteStates[bid.tenderId],
                        onOpen={onOpen(bid.tenderId)},
                        onStage={onStage(bid.tenderId,it)},
                        onNotes={onNotes(bid.tenderId,it)},
                        onRemove={confirmUntrack=bid.tenderId})
                    else StateMessage("Tender no longer in snapshot",
                        "Tracking information remains saved on this phone.")
                }
            }
        }
    }
    if(confirmUnsave!=null)AlertDialog(
        onDismissRequest={confirmUnsave=null},
        title={Text("Remove saved tender?")},
        text={Text("This removes the watchlist entry. Any bid tracking record will remain.")},
        confirmButton={TextButton(onClick={confirmUnsave?.let(onSave);confirmUnsave=null}){
            Text("Remove")}},
        dismissButton={TextButton(onClick={confirmUnsave=null}){Text("Cancel")}}
    )
    if(confirmUntrack!=null)AlertDialog(
        onDismissRequest={confirmUntrack=null},
        title={Text("Remove tracked bid?")},
        text={Text("This will remove its stage and preparation notes from this device.")},
        confirmButton={TextButton(onClick={confirmUntrack?.let(onRemoveBid);confirmUntrack=null}){
            Text("Remove")}},
        dismissButton={TextButton(onClick={confirmUntrack=null}){Text("Cancel")}}
    )
}
@Composable
private fun BidEditorCard(tender:Tender,bid:Bid,noteStatus:String?,
                          onOpen:()->Unit,onStage:(String)->Unit,
                          onNotes:(String)->Unit,onRemove:()->Unit){
    var expanded by rememberSaveable(tender.id){mutableStateOf(false)}
    var notes by rememberSaveable(tender.id){mutableStateOf(bid.notes)}
    Column(Modifier.fillMaxWidth().border(1.dp,TB.border,RoundedCornerShape(10.dp))
        .padding(14.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){
            Column(Modifier.weight(1f).clickable{expanded=!expanded}){
                Eyebrow(tender.reference?:tender.id)
                Spacer(Modifier.height(6.dp))
                Text(tender.title,style=MaterialTheme.typography.titleMedium,
                    maxLines=if(expanded)5 else 2,overflow=TextOverflow.Ellipsis)
                Text("Closes "+formatDate(tender.closingAt,true),
                    style=MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick={expanded=!expanded}){
                Icon(if(expanded)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription=if(expanded)"Collapse bid" else "Edit bid")
            }
        }
        if(!expanded){
            Spacer(Modifier.height(8.dp))
            Text("Stage: "+bid.stage,modifier=Modifier.background(TB.subtle,
                RoundedCornerShape(7.dp)).padding(horizontal=10.dp,vertical=7.dp),
                style=MaterialTheme.typography.bodyMedium)
        }else{
            Spacer(Modifier.height(10.dp))
            Text("Bid stage",style=MaterialTheme.typography.labelLarge)
            DropdownFilter(
                label="",current=bid.stage,
                choices=BID_STAGES.map{it to it},change=onStage)
            Spacer(Modifier.height(12.dp))
            Text("Preparation notes",style=MaterialTheme.typography.labelLarge)
            OutlinedTextField(value=notes,onValueChange={
                notes=it;onNotes(it)
            },modifier=Modifier.fillMaxWidth(),minLines=3,maxLines=7,
                placeholder={Text("Add preparation steps and supplier notes")})
            val status=noteStatus ?: if(notes==bid.notes)"Saved on this device" else "Saving…"
            Text(status,style=MaterialTheme.typography.bodySmall,
                color=if(status.startsWith("Failed"))TB.urgent else TB.secondary,
                modifier=Modifier.padding(top=6.dp))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick=onOpen){
                    Text("Open tender")
                    Icon(Icons.Outlined.OpenInNew,contentDescription=null,
                        modifier=Modifier.size(17.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick=onRemove){
                    Icon(Icons.Outlined.DeleteOutline,contentDescription="Remove tracking")
                }
            }
        }
    }
}
