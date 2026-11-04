package za.co.tenderbase.nativeoffline

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val southAfricaZone=ZoneId.of("Africa/Johannesburg")
@Composable
fun DeadlinesScreen(all:List<Tender>,alerts:List<Tender>,dismissed:List<String>,
                    selectedTab:String,onTab:(String)->Unit,onOpen:(String)->Unit,
                    onDismiss:(Tender)->Unit,onRestore:()->Unit,onDiscover:()->Unit,
                    reminderDays:Int,modifier:Modifier=Modifier){
    val today=LocalDate.now(southAfricaZone)
    val upcoming=remember(all){
        all.filter{
            val date=closingDate(it.closingAt)
            date!=null && !date.isBefore(today) && !date.isAfter(today.plusDays(90)) && isOpenTender(it)
        }.sortedBy{tenderInstant(it.closingAt)}
    }
    var chosenDay by rememberSaveable{mutableStateOf<String?>(null)}
    val visible=if(chosenDay==null)upcoming
        else upcoming.filter{closingDate(it.closingAt)?.toString()==chosenDay}
    val grouped=visible.groupBy{closingDate(it.closingAt)}
    val ctx=LocalContext.current
    Column(modifier.fillMaxSize().testTag("deadlines-screen").padding(horizontal=20.dp)){
        Spacer(Modifier.height(8.dp))
        Headline("Deadlines")
        Text("Keep the next date in view.",style=MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        SegmentTabs(listOf("Upcoming","Alerts "+alerts.size),
            if(selectedTab=="Upcoming")"Upcoming" else "Alerts "+alerts.size,
            onClick={onTab(if(it=="Upcoming")"Upcoming" else "Alerts")})
        Spacer(Modifier.height(14.dp))
        if(selectedTab=="Upcoming"){
            Row(Modifier.fillMaxWidth().background(TB.subtle,RoundedCornerShape(10.dp))
                .padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                Icon(Icons.Outlined.CalendarMonth,contentDescription=null)
                Spacer(Modifier.width(10.dp))
                Text(today.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style=MaterialTheme.typography.titleMedium)
            }
            LazyRow(Modifier.padding(vertical=10.dp),
                horizontalArrangement=Arrangement.spacedBy(5.dp)){
                item{
                    FilterDay("All",chosenDay==null){chosenDay=null}
                }
                items((0L..6L).toList()){offset->
                    val day=today.plusDays(offset)
                    FilterDay(day.format(DateTimeFormatter.ofPattern("EEE d")),
                        chosenDay==day.toString()){chosenDay=day.toString()}
                }
            }
            Text(if(chosenDay==null)"All official deadlines • next 90 days"
                else "Filtered to "+formatDate(chosenDay),
                style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            if(visible.isEmpty())StateMessage("No upcoming deadlines",
                if(chosenDay==null)"No open tenders are closing within the next 90 days in this offline snapshot."
                else "No official tenders close on this selected date.",
                action=if(chosenDay!=null)"Show all dates" else "Discover tenders",
                onAction=if(chosenDay!=null){{chosenDay=null}}else onDiscover)
            else LazyColumn(Modifier.fillMaxSize(),
                contentPadding=PaddingValues(bottom=24.dp)){
                grouped.forEach{(date,records)->
                    if(date!=null){
                        item(key="date-"+date){
                            Text(date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                                style=MaterialTheme.typography.labelLarge,
                                modifier=Modifier.padding(top=14.dp,bottom=6.dp))
                        }
                    }
                    items(records,key={it.id}){t->AgendaRow(t,
                        onOpen={onOpen(t.id)},onCalendar={addToCalendar(ctx,t)})}
                }
            }
        }else{
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("Saved tenders closing within "+reminderDays+" days",
                    style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1f))
                TextButton(enabled=dismissed.isNotEmpty(),onClick=onRestore){
                    Text("Restore alerts")
                }
            }
            Text("Alerts are checked when you open the app.",
                style=MaterialTheme.typography.bodySmall)
            if(alerts.isEmpty())StateMessage("No active deadline alerts",
                "Save a tender to monitor its closing date. You can adjust the reminder window in Settings.",
                action="Discover tenders",onAction=onDiscover)
            else LazyColumn(Modifier.fillMaxSize(),
                contentPadding=PaddingValues(bottom=24.dp)){
                items(alerts,key={it.id}){t->
                    Column(Modifier.fillMaxWidth().padding(vertical=10.dp)){
                        Text(closingSummary(t),color=if(isUrgent(t))TB.urgent else TB.secondary,
                            style=MaterialTheme.typography.labelLarge)
                        Text(t.title,style=MaterialTheme.typography.titleMedium,
                            modifier=Modifier.clickable{onOpen(t.id)})
                        Text(formatDate(t.closingAt,true),
                            style=MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            TextButton(onClick={addToCalendar(ctx,t)}){Text("Add to calendar")}
                            TextButton(onClick={onDismiss(t)}){Text("Dismiss")}
                        }
                    }
                    AppDivider()
                }
            }
        }
    }
}
@Composable
private fun FilterDay(label:String,active:Boolean,onClick:()->Unit){
    if(active)Button(onClick=onClick,contentPadding=PaddingValues(horizontal=12.dp),
        modifier=Modifier.heightIn(min=44.dp)){
        Text(label,color=androidx.compose.ui.graphics.Color.White)
    }else OutlinedButton(onClick=onClick,contentPadding=PaddingValues(horizontal=12.dp),
        modifier=Modifier.heightIn(min=44.dp)){
        Text(label)
    }
}
@Composable
private fun AgendaRow(t:Tender,onOpen:()->Unit,onCalendar:()->Unit){
    Row(Modifier.fillMaxWidth().padding(vertical=11.dp),
        horizontalArrangement=Arrangement.spacedBy(14.dp),
        verticalAlignment=Alignment.Top){
        val d=closingDate(t.closingAt)
        Column(Modifier.width(65.dp)){
            Text(d?.dayOfMonth?.toString()?.padStart(2,'0')?:"—",
                style=MaterialTheme.typography.headlineMedium,
                color=if(isUrgent(t))TB.urgent else TB.text)
            Text(d?.month?.name?.take(3)?:"",style=MaterialTheme.typography.titleMedium,
                color=if(isUrgent(t))TB.urgent else TB.text)
        }
        Column(Modifier.weight(1f)){
            Text(t.title,style=MaterialTheme.typography.titleMedium,
                modifier=Modifier.clickable(onClick=onOpen))
            Text(listOfNotNull(formatClock(t.closingAt),closingSummary(t)).joinToString(" · "),
                style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick=onCalendar,modifier=Modifier.padding(top=6.dp).heightIn(min=44.dp),
                shape=RoundedCornerShape(8.dp)){
                Icon(Icons.Outlined.CalendarMonth,contentDescription=null,modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add to calendar")
            }
        }
    }
    AppDivider()
}
