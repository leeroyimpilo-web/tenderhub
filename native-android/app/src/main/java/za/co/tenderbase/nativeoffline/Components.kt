package za.co.tenderbase.nativeoffline

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId

@Composable
fun AppHeader(onSettings:()->Unit){
    Row(Modifier.fillMaxWidth().height(62.dp).background(TB.background)
        .padding(horizontal=20.dp),
        horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
        BrandWordmark()
        IconButton(onClick=onSettings,modifier=Modifier.size(48.dp)){
            Icon(Icons.Outlined.Settings,contentDescription="Settings",tint=TB.text)
        }
    }
}
@Composable
fun BackHeader(title:String,onBack:()->Unit){
    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal=12.dp),
        verticalAlignment=Alignment.CenterVertically){
        IconButton(onClick=onBack){
            Icon(Icons.Outlined.ArrowBack,contentDescription="Back")
        }
        Text(title,style=MaterialTheme.typography.titleMedium)
    }
}
@Composable
fun DetailHeader(onBack:()->Unit,tender:Tender){
    val ctx=androidx.compose.ui.platform.LocalContext.current
    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal=12.dp),
        horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
        Row(verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=onBack){Icon(Icons.Outlined.ArrowBack,contentDescription="Back to results")}
            Text("Discover",style=MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick={
            val msg=tender.title+"\n"+tender.sourceUrl
            share(ctx,msg)
        }){
            Icon(Icons.Outlined.Share,contentDescription="Share tender")
        }
    }
}
@Composable
fun AppDivider(){
    HorizontalDivider(color=TB.border,thickness=1.dp)
}
@Composable
fun SegmentTabs(tabs:List<String>,active:String,onClick:(String)->Unit,
                modifier:Modifier=Modifier){
    Row(modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(0.dp)){
        tabs.forEach{label->
            Column(Modifier.weight(1f)){
                TextButton(
                    onClick={onClick(label)},
                    modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
                    colors=ButtonDefaults.textButtonColors(contentColor=TB.text)
                ){
                    Text(label,style=MaterialTheme.typography.labelLarge,
                        fontWeight=if(active==label)FontWeight.ExtraBold else FontWeight.Medium,
                        maxLines=1)
                }
                Box(Modifier.fillMaxWidth().height(if(active==label)4.dp else 1.dp)
                    .background(if(active==label)TB.accent else TB.border))
            }
        }
    }
}
@Composable
fun Headline(text:String){
    Text(text,style=MaterialTheme.typography.headlineMedium)
}
@Composable
fun Eyebrow(text:String){
    Text(text.uppercase(),style=MaterialTheme.typography.labelMedium,
        fontWeight=FontWeight.Bold,color=TB.secondary)
}
@Composable
fun DataFreshnessIndicator(lastSync:String?){
    Row(Modifier.fillMaxWidth().background(TB.subtle,RoundedCornerShape(9.dp))
        .padding(horizontal=12.dp,vertical=9.dp)){
        Text(if(lastSync==null)"Last update unavailable — offline snapshot"
            else "Source updated "+formatDate(lastSync,true),
            style=MaterialTheme.typography.bodySmall)
    }
}
@Composable
fun DeadlineBadge(t:Tender){
    val closing=t.closingAt
    val urgent=isUrgent(t)
    val date=closingDate(closing)
    val label=if(date!=null)date.dayOfMonth.toString().padStart(2,'0')+" "+
        date.month.name.take(3) else "No date"
    Text(label,style=MaterialTheme.typography.labelLarge,
        color=if(urgent)TB.urgent else TB.text,
        modifier=Modifier.background(if(urgent)TB.urgentSoft else TB.accentSoft,
            RoundedCornerShape(8.dp)).padding(horizontal=10.dp,vertical=7.dp))
}
@Composable
fun TenderListItem(t:Tender,saved:Boolean,onOpen:()->Unit,onBookmark:()->Unit,
                   highlighted:Boolean=false){
    Column(Modifier.fillMaxWidth().then(if(highlighted)Modifier.background(TB.accentSoft,
        RoundedCornerShape(10.dp)) else Modifier).padding(horizontal=4.dp,vertical=10.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Text(t.category?.uppercase()?:"UNCATEGORISED",
                style=MaterialTheme.typography.labelMedium,
                fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f),
                maxLines=1,overflow=TextOverflow.Ellipsis)
            IconButton(onClick=onBookmark,modifier=Modifier.size(48.dp)){
                Icon(if(saved)Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription=if(saved)"Remove saved tender" else "Save tender",
                    tint=TB.text)
            }
        }
        Column(Modifier.fillMaxWidth().clickable(onClick=onOpen)
            .padding(bottom=8.dp)){
            Text(t.title,style=MaterialTheme.typography.titleMedium,
                maxLines=4,overflow=TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(t.buyer?:"Buyer not supplied",style=MaterialTheme.typography.bodySmall)
            Text(t.province?:"Province not supplied",style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp)){
                DeadlineBadge(t)
                Text(closingSummary(t),style=MaterialTheme.typography.bodySmall,
                    color=if(isUrgent(t))TB.urgent else TB.secondary)
            }
        }
    }
    AppDivider()
}
@Composable
fun StateMessage(title:String,explanation:String,action:String?=null,onAction:(()->Unit)?=null,
                 modifier:Modifier=Modifier){
    Column(modifier.fillMaxWidth().padding(22.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp),
        horizontalAlignment=Alignment.Start){
        Text(title,style=MaterialTheme.typography.titleMedium)
        Text(explanation,style=MaterialTheme.typography.bodyMedium,color=TB.secondary)
        if(action!=null&&onAction!=null){
            OutlinedButton(onClick=onAction){Text(action)}
        }
    }
}
@Composable
fun DataLoadingOrError(status:String,modifier:Modifier=Modifier){
    Box(modifier,contentAlignment=Alignment.Center){
        Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){
            if(!status.startsWith("Offline import failed"))CircularProgressIndicator(color=TB.text)
            Spacer(Modifier.height(16.dp))
            Text(status,style=MaterialTheme.typography.bodyMedium)
        }
    }
}
fun share(context:Context,text:String){
    val intent=Intent(Intent.ACTION_SEND).apply{
        type="text/plain";putExtra(Intent.EXTRA_TEXT,text)
    }
    try{context.startActivity(Intent.createChooser(intent,"Share TenderBase opportunity"))}
    catch(_:Exception){Toast.makeText(context,"No sharing app available",Toast.LENGTH_SHORT).show()}
}
fun openExternal(context:Context,url:String){
    if(!url.startsWith("https://"))return
    try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}
    catch(_:Exception){Toast.makeText(context,"No app available to open link",Toast.LENGTH_SHORT).show()}
}
fun emailContact(context:Context,address:String){
    if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches())return
    try{context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:"+Uri.encode(address))))}
    catch(_:Exception){Toast.makeText(context,"No email app installed",Toast.LENGTH_SHORT).show()}
}
fun callContact(context:Context,number:String){
    try{context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(number))))}
    catch(_:Exception){Toast.makeText(context,"No phone app available",Toast.LENGTH_SHORT).show()}
}
fun addToCalendar(context:Context,t:Tender){
    val instant=tenderInstant(t.closingAt)
    if(instant==null){
        Toast.makeText(context,"Closing date not supplied",Toast.LENGTH_SHORT).show();return
    }
    val timed=sourceHasTime(t.closingAt)
    val eventStart=if(timed)instant.toEpochMilli() else
        closingDate(t.closingAt)!!.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    val intent=Intent(Intent.ACTION_INSERT).apply{
        data=CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE,"Tender closes: "+t.title)
        putExtra(CalendarContract.Events.DESCRIPTION,
            "Reference: "+(t.reference?:"Not supplied")+"\n"+t.sourceUrl)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,eventStart)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
            eventStart+if(timed)30*60_000L else 24*60*60_000L)
        putExtra(CalendarContract.Events.ALL_DAY,!timed)
        putExtra(CalendarContract.Events.EVENT_TIMEZONE,
            if(timed)"Africa/Johannesburg" else "UTC")
    }
    try{context.startActivity(intent)}
    catch(_:Exception){Toast.makeText(context,"No calendar app installed",Toast.LENGTH_SHORT).show()}
}
