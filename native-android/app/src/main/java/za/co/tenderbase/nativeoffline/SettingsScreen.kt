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
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(count:Int,lastSync:String?,all:List<Tender>,preferences:Map<String,String>,
                   onSet:(String,String)->Unit,modifier:Modifier=Modifier){
    val provinces=remember(all){all.mapNotNull{it.province?.takeIf(String::isNotBlank)}.distinct().sorted()}
    val preference=preferences["defaultProvince"]?:""
    val days=preferences["remindDays"]?:"7"
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=20.dp,vertical=12.dp)){
        Headline("Settings")
        Text("Preferences saved privately on this device.",
            style=MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Text("Tender preferences",style=MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        DropdownFilter("Default province",preference.ifBlank{"All provinces"},
            listOf("" to "All provinces")+provinces.map{it to it}){onSet("defaultProvince",it)}
        Spacer(Modifier.height(12.dp))
        DropdownFilter("In-app reminder window",days+" days",
            listOf(1,3,7,14,30).map{it.toString() to it.toString()+" days"}) {
            onSet("remindDays",it)
        }
        Spacer(Modifier.height(22.dp))
        AppDivider()
        Spacer(Modifier.height(16.dp))
        Text("Official data status",style=MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(count.toString()+" official tender records stored offline",
            style=MaterialTheme.typography.bodyMedium)
        Text("Last official import: "+formatDate(lastSync,true),
            style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        DataFreshnessIndicator(lastSync)
        Spacer(Modifier.height(22.dp))
        Text("Offline availability",style=MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(9.dp))
        Text("Search, saved tenders, bid notes, alerts, the deadline agenda, and insights use the on-device database.",
            style=MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text("This build includes an official tender snapshot, but it cannot fetch new notices or download PDF documents. Check the National Treasury portal for recent amendments.",
            style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        Text("No sign-in, payments, or account synchronization.",
            style=MaterialTheme.typography.bodySmall)
    }
}
