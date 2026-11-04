package za.co.tenderbase.nativeoffline

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun InsightsScreen(all:List<Tender>,lastSync:String?,selectedTab:String,
                   preferredProvince:String,onTab:(String)->Unit,modifier:Modifier=Modifier){
    val grouped=remember(all,selectedTab){analytics(all,selectedTab)}
    val max=grouped.maxOfOrNull{it.second}?.coerceAtLeast(1)?:1
    LazyColumn(modifier.fillMaxSize().testTag("insights-screen"),
        contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp)){
        item{
            Eyebrow("Insights")
            Spacer(Modifier.height(7.dp))
            Headline("Market overview")
            Spacer(Modifier.height(5.dp))
            Text("What is being published.",style=MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text(all.size.toString(),style=MaterialTheme.typography.displaySmall,
                    modifier=Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(55.dp).background(TB.text))
                Spacer(Modifier.width(16.dp))
                Text("Imported\nnotices",style=MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(5.dp))
            Text("National Treasury eTenders · "+formatDate(lastSync),
                style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            AppDivider()
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                listOf("Provinces","Buyers","Categories").forEach{key->
                    if(selectedTab==key)Button(onClick={onTab(key)},modifier=Modifier.weight(1f),
                        shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(5.dp)){
                        Text(key,maxLines=1,color=androidx.compose.ui.graphics.Color.White,
                            style=MaterialTheme.typography.labelLarge)
                    }else OutlinedButton(onClick={onTab(key)},modifier=Modifier.weight(1f),
                        shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(5.dp)){
                        Text(key,maxLines=1)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(when(selectedTab){
                "Provinces"->"Where opportunities are"
                "Buyers"->"Most active buyers"
                else->"Procurement categories"
            },style=MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
        }
        if(grouped.isEmpty())item{
            StateMessage("No research data","The official snapshot did not contain grouped records.")
        }else{
            itemsIndexed(grouped.take(15),key={_,x->x.first}){idx,(name,count)->
                val preferred=selectedTab=="Provinces" && preferredProvince.isNotBlank() &&
                    name==preferredProvince
                Column(Modifier.fillMaxWidth()
                    .then(if(preferred)Modifier.background(TB.accentSoft,
                        RoundedCornerShape(9.dp))else Modifier)
                    .padding(horizontal=if(preferred)8.dp else 0.dp,vertical=6.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        Text((idx+1).toString().padStart(2,'0'),
                            style=MaterialTheme.typography.labelLarge,color=TB.secondary,
                            modifier=Modifier.width(26.dp))
                        Text(name,modifier=Modifier.weight(1f),
                            style=MaterialTheme.typography.titleMedium,
                            maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(count.toString(),style=MaterialTheme.typography.titleMedium)
                    }
                    Box(Modifier.fillMaxWidth().padding(start=36.dp,top=5.dp)
                        .height(9.dp).background(TB.subtle,RoundedCornerShape(10.dp))){
                        Box(Modifier.fillMaxWidth(count.toFloat()/max.toFloat()).fillMaxHeight()
                            .background(if(idx==0)TB.text else TB.secondary,
                                RoundedCornerShape(10.dp)))
                    }
                }
            }
        }
        item{
            Spacer(Modifier.height(16.dp))
            Text("Counts cover imported records only.",
                style=MaterialTheme.typography.bodySmall,
                modifier=Modifier.fillMaxWidth().background(TB.subtle,
                    RoundedCornerShape(8.dp)).padding(10.dp))
            Spacer(Modifier.height(14.dp))
            AppDivider()
            Spacer(Modifier.height(10.dp))
            Text("Explore the data",style=MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if(selectedTab!="Buyers")ResearchLink("Most active buyers"){onTab("Buyers")}
            if(selectedTab!="Categories")ResearchLink("Procurement categories"){onTab("Categories")}
            if(selectedTab!="Provinces")ResearchLink("Provinces"){onTab("Provinces")}
            Spacer(Modifier.height(28.dp))
        }
    }
}
@Composable
private fun ResearchLink(label:String,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=49.dp).clickable(onClick=onClick),
        verticalAlignment=Alignment.CenterVertically){
        Icon(Icons.Outlined.BarChart,contentDescription=null)
        Spacer(Modifier.width(10.dp))
        Text(label,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
        Icon(Icons.Outlined.ArrowForward,contentDescription=null)
    }
    AppDivider()
}
