package za.co.tenderbase.nativeoffline

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun DiscoverScreen(
    all:List<Tender>,filters:SearchCriteria,saved:Set<String>,lastSync:String?,
    onQuery:(String)->Unit,onApply:(SearchCriteria)->Unit,onRemove:(String)->Unit,
    onSort:(String)->Unit,onOpen:(String)->Unit,onSave:(String)->Unit,
    scrollState:LazyListState,modifier:Modifier=Modifier
){
    var sheet by remember { mutableStateOf(false) }
    val focus=LocalFocusManager.current
    // Keep search/sort work away from Compose's UI thread; rapid typing cancels obsolete searches.
    var results by remember { mutableStateOf<List<Tender>?>(null) }
    LaunchedEffect(all,filters){
        delay(120)
        val next=withContext(Dispatchers.Default){searchOffline(all,filters)}
        results=next
    }
    val rendered=results
    LazyColumn(modifier.fillMaxSize().testTag("discover-screen"),state=scrollState,
        contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=28.dp),
        verticalArrangement=Arrangement.spacedBy(0.dp)){
        item{
            Spacer(Modifier.height(10.dp))
            Headline("Find your next\nopportunity.")
            Spacer(Modifier.height(6.dp))
            Text("National Treasury eTenders",style=MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
                TextField(
                    value=filters.query,onValueChange=onQuery,
                    modifier=Modifier.weight(1f).testTag("discover-query"),
                    placeholder={Text("Keyword or tender number",maxLines=1)},
                    leadingIcon={Icon(Icons.Outlined.Search,contentDescription=null)},
                    trailingIcon={
                        if(filters.query.isNotEmpty())IconButton(onClick={onQuery("")}){
                            Icon(Icons.Outlined.Close,contentDescription="Clear search")
                        }
                    },
                    singleLine=true,shape=RoundedCornerShape(10.dp),
                    colors=TextFieldDefaults.colors(
                        focusedContainerColor=TB.text,unfocusedContainerColor=TB.text,
                        focusedTextColor=Color.White,unfocusedTextColor=Color.White,
                        focusedPlaceholderColor=Color.White.copy(alpha=.78f),
                        unfocusedPlaceholderColor=Color.White.copy(alpha=.78f),
                        focusedLeadingIconColor=Color.White,unfocusedLeadingIconColor=Color.White,
                        focusedTrailingIconColor=Color.White,unfocusedTrailingIconColor=Color.White,
                        focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent
                    ),
                    keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
                    keyboardActions=KeyboardActions(onSearch={focus.clearFocus()})
                )
                FilledIconButton(onClick={sheet=true},
                    modifier=Modifier.size(54.dp).testTag("open-filters"),
                    shape=RoundedCornerShape(10.dp),
                    colors=IconButtonDefaults.filledIconButtonColors(
                        containerColor=TB.text,contentColor=Color.White)){
                    Icon(Icons.Outlined.Tune,contentDescription="Refine search")
                }
            }
            Spacer(Modifier.height(9.dp))
            val chips=buildList<Pair<String,String>>{
                if(filters.province.isNotBlank())add("province" to filters.province)
                if(filters.category.isNotBlank())add("category" to filters.category)
                if(filters.buyer.isNotBlank())add("buyer" to filters.buyer)
                if(filters.status!="all")add("status" to when(filters.status){
                    "open"->"Open";"closed"->"Closed";else->"Unspecified dates"
                })
            }
            if(chips.isNotEmpty()){
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    items(chips){(key,value)->
                        Row(
                            Modifier.border(1.dp,TB.border,RoundedCornerShape(50))
                                .clickable{onRemove(key)}
                                .padding(horizontal=12.dp,vertical=7.dp),
                            verticalAlignment=Alignment.CenterVertically){
                            Text(value,style=MaterialTheme.typography.bodySmall,maxLines=1)
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.Outlined.Close,contentDescription="Remove "+value+" filter",
                                modifier=Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(Modifier.height(11.dp))
            }
            DataFreshnessIndicator(lastSync)
            Spacer(Modifier.height(12.dp))
            SegmentTabs(tabs=listOf("Latest","Closing soon"),
                active=if(filters.sort=="closing")"Closing soon" else "Latest",
                onClick={onSort(if(it=="Closing soon")"closing" else "newest")})
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically){
                Text((rendered?.size?:0).toString()+" opportunities",
                    style=MaterialTheme.typography.labelLarge)
                Text(when(filters.sort){
                    "closing"->"Closing earliest"
                    "oldest"->"Oldest first"
                    else->"Newest"
                },style=MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
        }
        if(rendered==null){
            item{LinearProgressIndicator(modifier=Modifier.fillMaxWidth().padding(vertical=14.dp),color=TB.accent)}
        }else if(rendered.isEmpty()){
            item{
                StateMessage(
                    if(all.isEmpty())"No offline records" else "No matching tenders",
                    if(all.isEmpty())"The official tender snapshot could not be loaded."
                    else "Adjust your keywords or reset filters to see other notices.",
                    action="Reset filters",
                    onAction={onQuery("");onApply(SearchCriteria())}
                )
            }
        }else{
            itemsIndexed(rendered,key={_,t->t.id}){index,t->
                TenderListItem(t,saved=t.id in saved,onOpen={onOpen(t.id)},
                    onBookmark={onSave(t.id)},highlighted=index==0)
            }
        }
    }
    if(sheet)FilterSheet(
        current=filters,all=all,
        dismiss={sheet=false},
        apply={onApply(it);sheet=false}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(current:SearchCriteria,all:List<Tender>,
                dismiss:()->Unit,apply:(SearchCriteria)->Unit){
    var province by remember(current){mutableStateOf(current.province)}
    var category by remember(current){mutableStateOf(current.category)}
    var buyer by remember(current){mutableStateOf(current.buyer)}
    var status by remember(current){mutableStateOf(current.status)}
    var sort by remember(current){mutableStateOf(current.sort)}
    val provinces=remember(all){all.mapNotNull{it.province?.takeIf(String::isNotBlank)}.distinct().sorted()}
    val categories=remember(all){all.mapNotNull{it.category?.takeIf(String::isNotBlank)}.distinct().sorted()}
    ModalBottomSheet(onDismissRequest=dismiss,dragHandle={
        Box(Modifier.padding(vertical=10.dp).size(60.dp,5.dp)
            .background(TB.border,RoundedCornerShape(5.dp)))
    },containerColor=TB.background,modifier=Modifier.fillMaxHeight(.96f).testTag("refine-sheet")){
        Column(Modifier.fillMaxWidth().imePadding()){
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween){
                Text("Refine search",style=MaterialTheme.typography.headlineMedium)
                IconButton(onClick=dismiss){
                    Icon(Icons.Outlined.Close,contentDescription="Close filters")
                }
            }
            Text("Choose what matters to you.",
                style=MaterialTheme.typography.bodyLarge,
                modifier=Modifier.padding(horizontal=20.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal=20.dp,vertical=14.dp),
                verticalArrangement=Arrangement.spacedBy(16.dp)){
                DropdownFilter("Province",province.ifBlank{"All provinces"},
                    listOf("" to "All provinces")+provinces.map{it to it}){province=it}
                DropdownFilter("Category",category.ifBlank{"All categories"},
                    listOf("" to "All categories")+categories.map{it to it}){category=it}
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text("Buyer or department",style=MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value=buyer,onValueChange={buyer=it},
                        modifier=Modifier.fillMaxWidth(),singleLine=true,
                        placeholder={Text("Any department")},
                        shape=RoundedCornerShape(10.dp))
                }
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("Deadline",style=MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        listOf("open" to "Open","closed" to "Closed","all" to "Any date").forEach{(key,label)->
                            if(status==key)Button(onClick={status=key},
                                modifier=Modifier.weight(1f).heightIn(min=48.dp),
                                contentPadding=PaddingValues(horizontal=3.dp)){
                                Text(label,maxLines=1)
                            }else OutlinedButton(onClick={status=key},
                                modifier=Modifier.weight(1f).heightIn(min=48.dp),
                                contentPadding=PaddingValues(horizontal=3.dp)){
                                Text(label,maxLines=1)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().clickable{status="undated"}
                        .heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically){
                        RadioButton(selected=status=="undated",onClick={status="undated"})
                        Text("Unspecified dates",style=MaterialTheme.typography.bodyMedium)
                    }
                }
                AppDivider()
                Column {
                    Text("Sort results",style=MaterialTheme.typography.titleMedium)
                    listOf("newest" to "Recently published","closing" to "Closing earliest",
                        "oldest" to "Oldest first").forEach{(key,label)->
                        Row(Modifier.fillMaxWidth().clickable{sort=key}.heightIn(min=48.dp),
                            verticalAlignment=Alignment.CenterVertically){
                            RadioButton(selected=sort==key,onClick={sort=key})
                            Text(label,style=MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            AppDivider()
            Row(Modifier.fillMaxWidth().background(TB.background)
                .padding(horizontal=20.dp,vertical=12.dp),
                horizontalArrangement=Arrangement.spacedBy(12.dp),
                verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick={
                    province="";category="";buyer="";status="open";sort="newest"
                },modifier=Modifier.heightIn(min=48.dp)){Text("Reset")}
                Button(onClick={
                    apply(current.copy(province=province,category=category,
                        buyer=buyer,status=status,sort=sort))
                },modifier=Modifier.weight(1f).heightIn(min=52.dp),
                    shape=RoundedCornerShape(10.dp)){
                    Text("Show results")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.ArrowForward,contentDescription=null)
                }
            }
        }
    }
}
@Composable
fun DropdownFilter(label:String,current:String,choices:List<Pair<String,String>>,
                   change:(String)->Unit){
    var expanded by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text(label,style=MaterialTheme.typography.titleMedium)
        Box{
            OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()
                .heightIn(min=52.dp),
                shape=RoundedCornerShape(10.dp),
                colors=ButtonDefaults.outlinedButtonColors(containerColor=TB.subtle)){
                Text(current,modifier=Modifier.weight(1f),maxLines=1)
                Icon(Icons.Outlined.KeyboardArrowDown,contentDescription=null)
            }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){
                choices.forEach{(value,text)->
                    DropdownMenuItem(text={Text(text)},
                        onClick={change(value);expanded=false})
                }
            }
        }
    }
}
