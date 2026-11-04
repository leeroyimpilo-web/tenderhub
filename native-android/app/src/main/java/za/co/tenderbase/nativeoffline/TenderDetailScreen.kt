package za.co.tenderbase.nativeoffline

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray

@Composable
fun TenderDetailScreen(tender:Tender,saved:Boolean,tracked:Boolean,
                       onSave:()->Unit,onTrack:()->Unit,modifier:Modifier=Modifier){
    var section by remember(tender.id){mutableStateOf("Overview")}
    val ctx=LocalContext.current
    val docs=remember(tender.documentsJson){
        try {
            val a=JSONArray(tender.documentsJson)
            (0 until a.length()).mapNotNull{a.optJSONObject(it)}
        }catch(_:Exception){emptyList()}
    }
    Scaffold(modifier=modifier.testTag("review-screen"),containerColor=TB.background,
        contentWindowInsets=WindowInsets(0.dp),
        bottomBar={
            Row(Modifier.fillMaxWidth().background(TB.background)
                .padding(horizontal=20.dp,vertical=10.dp),
                horizontalArrangement=Arrangement.spacedBy(10.dp)){
                OutlinedButton(onClick=onSave,modifier=Modifier.weight(.9f).heightIn(min=54.dp),
                    shape=RoundedCornerShape(10.dp)){
                    Icon(if(saved)Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription=null)
                    Spacer(Modifier.width(6.dp))
                    Text(if(saved)"Saved" else "Save")
                }
                Button(onClick=onTrack,modifier=Modifier.weight(1.6f).heightIn(min=54.dp),
                    shape=RoundedCornerShape(10.dp)){
                    Text(if(tracked)"Open bid" else "Track bid")
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Outlined.ArrowForward,contentDescription=null)
                }
            }
        }
    ){inner->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
            .padding(horizontal=20.dp)){
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically){
                Text(tender.reference?:"Reference not supplied",
                    style=MaterialTheme.typography.labelLarge)
                Text(if(tender.closingAt==null)"Undated" else if(isOpenTender(tender))"Open" else "Closed",
                    style=MaterialTheme.typography.labelLarge,
                    modifier=Modifier.background(TB.accent,RoundedCornerShape(20.dp))
                        .padding(horizontal=13.dp,vertical=6.dp))
            }
            Spacer(Modifier.height(13.dp))
            Headline(tender.title)
            Spacer(Modifier.height(6.dp))
            Text(tender.buyer?:"Buyer not supplied",style=MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(18.dp))
            ClosingPanel(tender)
            Spacer(Modifier.height(16.dp))
            SegmentTabs(listOf("Overview","Documents","Contact"),section,{section=it})
            Spacer(Modifier.height(15.dp))
            when(section){
                "Overview"->{
                    FactRow("Province",tender.province)
                    FactRow("Category",tender.category)
                    FactRow("Published",formatDate(tender.publishedAt))
                    FactRow("Closing",if(tender.closingAt==null) "Not supplied"
                        else formatDate(tender.closingAt,true))
                    FactRow("Briefing",formatDate(tender.briefingAt,true))
                    FactRow("Briefing venue",tender.briefingVenue)
                    Spacer(Modifier.height(12.dp))
                    Text("Description",style=MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(tender.description?.takeIf{it.isNotBlank()}?:"No description supplied.",
                        style=MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    LinkRow("Documents",docs.size.toString()+" files"){
                        section="Documents"
                    }
                    LinkRow("Add to calendar",null,
                        enabled=tender.closingAt!=null){addToCalendar(ctx,tender)}
                    LinkRow("Contact",null){section="Contact"}
                    LinkRow("View official notice",null){openExternal(ctx,tender.sourceUrl)}
                    Spacer(Modifier.height(8.dp))
                    Text("Source: National Treasury eTenders",
                        style=MaterialTheme.typography.bodySmall)
                }
                "Documents"->{
                    Text("Tender documents",style=MaterialTheme.typography.titleMedium)
                    Text(docs.size.toString()+" documents listed in the official notice",
                        style=MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    if(docs.isEmpty())StateMessage("No documents listed",
                        "The official record did not provide document links.")
                    docs.forEach{doc->
                        val title=doc.optString("title","Document")
                        val url=doc.optString("url","")
                        val format=doc.optString("format","")
                        Column(Modifier.fillMaxWidth().padding(vertical=9.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Icon(Icons.Outlined.Description,contentDescription=null,
                                    modifier=Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)){
                                    Text(title,style=MaterialTheme.typography.bodyMedium,
                                        fontWeight=FontWeight.SemiBold)
                                    if(format.isNotBlank()&&format!="null")
                                        Text(format,style=MaterialTheme.typography.bodySmall)
                                }
                            }
                            if(url.startsWith("https://"))TextButton(onClick={openExternal(ctx,url)}){
                                Text("Open document online")
                                Icon(Icons.Outlined.OpenInNew,contentDescription=null)
                            }else Text("Document link unavailable",
                                style=MaterialTheme.typography.bodySmall)
                            AppDivider()
                        }
                    }
                    Text("Links are saved offline, but files have not been downloaded.",
                        style=MaterialTheme.typography.bodySmall)
                }
                else->{
                    Text("Tender contact",style=MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    FactRow("Contact person",tender.contactName)
                    FactRow("Email",tender.contactEmail)
                    if(!tender.contactEmail.isNullOrBlank())
                        LinkRow("Write email",null){emailContact(ctx,tender.contactEmail)}
                    FactRow("Telephone",tender.contactPhone)
                    if(!tender.contactPhone.isNullOrBlank())
                        LinkRow("Dial number",null){callContact(ctx,tender.contactPhone)}
                    FactRow("Briefing venue",tender.briefingVenue)
                    Spacer(Modifier.height(18.dp))
                    Text("Contact details come directly from the imported official notice.",
                        style=MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun ClosingPanel(t:Tender){
    val date=closingDate(t.closingAt)
    Row(Modifier.fillMaxWidth()
        .background(if(isUrgent(t))TB.urgentSoft else TB.accent,RoundedCornerShape(12.dp))
        .padding(18.dp),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.width(76.dp)){
            Text(date?.dayOfMonth?.toString()?.padStart(2,'0')?:"—",
                style=MaterialTheme.typography.headlineMedium,
                color=if(isUrgent(t))TB.urgent else TB.text)
            Text(date?.month?.name?.take(3)?:"NO DATE",
                style=MaterialTheme.typography.titleMedium,
                color=if(isUrgent(t))TB.urgent else TB.text)
        }
        Box(Modifier.width(1.dp).height(54.dp).background(TB.text))
        Spacer(Modifier.width(16.dp))
        Column{
            Text("Closing date",style=MaterialTheme.typography.labelLarge)
            Text(if(t.closingAt==null)"Not supplied"
                else formatDate(t.closingAt,true),style=MaterialTheme.typography.bodyMedium)
            Text(closingSummary(t),style=MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
private fun FactRow(label:String,value:String?){
    Row(Modifier.fillMaxWidth().padding(vertical=11.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Text(label,modifier=Modifier.weight(.85f),style=MaterialTheme.typography.bodySmall)
        Text(value?.takeIf{it.isNotBlank()}?:"Not supplied",
            modifier=Modifier.weight(1.35f),style=MaterialTheme.typography.bodyMedium,
            fontWeight=FontWeight.SemiBold)
    }
    AppDivider()
}
@Composable
private fun LinkRow(label:String,trailing:String?=null,enabled:Boolean=true,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=52.dp)
        .clickable(enabled=enabled,onClick=onClick),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Text(label,style=MaterialTheme.typography.bodyMedium,
            modifier=Modifier.weight(1f),
            color=if(enabled)TB.text else TB.muted)
        if(trailing!=null)Text(trailing,style=MaterialTheme.typography.bodySmall)
        Icon(Icons.Outlined.ArrowForward,contentDescription=null,
            tint=if(enabled)TB.text else TB.muted)
    }
    AppDivider()
}
