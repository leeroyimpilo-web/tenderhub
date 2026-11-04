package za.co.tenderbase.nativeoffline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val vm=ViewModelProvider(this)[TenderViewModel::class.java]
        setContent { TenderTheme { TenderApp(vm) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenderApp(vm:TenderViewModel){
    val all by vm.tenders.collectAsState()
    val bookmarks by vm.bookmarks.collectAsState()
    val bids by vm.bids.collectAsState()
    val dismissed by vm.dismissed.collectAsState()
    val prefs by vm.preferences.collectAsState()
    val sync by vm.lastSync.collectAsState()
    val saved=remember(bookmarks){bookmarks.toSet()}
    val pref=remember(prefs){prefs.associate{it.name to it.value}}
    val tracked=remember(bids){bids.associateBy{it.tenderId}}
    var destination by rememberSaveable {mutableStateOf("Discover")}
    var selectedId by rememberSaveable {mutableStateOf<String?>(null)}
    var settings by rememberSaveable {mutableStateOf(false)}
    var query by rememberSaveable {mutableStateOf("")}
    var province by rememberSaveable {mutableStateOf("")}
    var initialDefaultApplied by rememberSaveable {mutableStateOf(false)}
    var category by rememberSaveable {mutableStateOf("")}
    var buyer by rememberSaveable {mutableStateOf("")}
    var deadline by rememberSaveable {mutableStateOf("open")}
    var sort by rememberSaveable {mutableStateOf("newest")}
    var bidsTab by rememberSaveable {mutableStateOf("Saved")}
    var deadlineTab by rememberSaveable {mutableStateOf("Upcoming")}
    var insightsTab by rememberSaveable {mutableStateOf("Provinces")}
    LaunchedEffect(pref["defaultProvince"]){
        if(!initialDefaultApplied && !pref["defaultProvince"].isNullOrBlank()){
            province=pref["defaultProvince"]?:""
            initialDefaultApplied=true
        }
    }
    val scroll=rememberLazyListState()
    val selected=all.firstOrNull{it.id==selectedId}
    val filters=SearchCriteria(query,province,category,buyer,deadline,sort)
    val reminderDays=pref["remindDays"]?.toIntOrNull()?.coerceIn(1,30)?:7
    val alerts=remember(all,saved,dismissed,reminderDays){
        activeAlertTenders(all.filter{it.id in saved},reminderDays,dismissed.toSet())
    }
    val snackbar=remember{SnackbarHostState()}
    val feedback=vm.message
    LaunchedEffect(feedback){
        if(feedback!=null){snackbar.showSnackbar(feedback);vm.clearMessage()}
    }
    BackHandler(selectedId!=null || settings){
        if(settings)settings=false else selectedId=null
    }
    Scaffold(
        containerColor=TB.background,
        contentWindowInsets=WindowInsets.safeDrawing,
        snackbarHost={SnackbarHost(snackbar)},
        topBar={
            if(selected!=null){
                DetailHeader(
                    onBack={selectedId=null},
                    tender=selected
                )
            }else if(settings){
                BackHeader("Settings",onBack={settings=false})
            }else{
                AppHeader(onSettings={settings=true})
            }
        },
        bottomBar={
            if(selected==null&&!settings)NavigationBar(
                containerColor=TB.surface,
                tonalElevation=0.dp
            ){
                listOf("Discover","My bids","Deadlines","Insights").forEach{item->
                    val active=destination==item
                    NavigationBarItem(
                        selected=active,
                        onClick={destination=item},
                        icon={
                            when(item){
                                "Discover"->Icon(Icons.Outlined.Search,contentDescription=null)
                                "My bids"->Icon(Icons.Outlined.FactCheck,contentDescription=null)
                                "Deadlines"->Icon(Icons.Outlined.CalendarMonth,contentDescription=null)
                                else->Icon(Icons.Outlined.BarChart,contentDescription=null)
                            }
                        },
                        label={Text(item)},
                        colors=NavigationBarItemDefaults.colors(
                            selectedIconColor=TB.text,selectedTextColor=TB.text,
                            indicatorColor=TB.accent,unselectedIconColor=TB.secondary,
                            unselectedTextColor=TB.secondary)
                    )
                }
            }
        }
    ){inner->
        val modifier=Modifier.fillMaxSize().padding(inner)
        when{
            vm.importStatus!="ready"&&all.isEmpty()->{
                DataLoadingOrError(vm.importStatus,modifier)
            }
            selected!=null->{
                TenderDetailScreen(
                    tender=selected,saved=selected.id in saved,tracked=tracked.containsKey(selected.id),
                    onSave={vm.save(selected.id,selected.id in saved)},
                    onTrack={vm.track(selected.id){selectedId=null;destination="My bids";bidsTab="Tracking"}},
                    modifier=modifier
                )
            }
            settings->SettingsScreen(
                count=all.size,lastSync=sync,all=all,preferences=pref,
                onSet={name,value->
                    vm.setting(name,value)
                    if(name=="defaultProvince"){province=value;initialDefaultApplied=true}
                },modifier=modifier
            )
            destination=="Discover"->DiscoverScreen(
                all=all,filters=filters,saved=saved,lastSync=sync,
                onQuery={query=it},
                onApply={f->
                    initialDefaultApplied=true
                    province=f.province;category=f.category;buyer=f.buyer
                    deadline=f.status;sort=f.sort
                    scroll.requestScrollToItem(0)
                },
                onRemove={field->
                    initialDefaultApplied=true
                    when(field){
                        "province"->province=""
                        "category"->category=""
                        "buyer"->buyer=""
                        "status"->deadline="open"
                        "query"->query=""
                    }
                    scroll.requestScrollToItem(0)
                },
                onSort={sort=it;scroll.requestScrollToItem(0)},
                onOpen={selectedId=it},
                onSave={id->vm.save(id,id in saved)},
                scrollState=scroll,modifier=modifier
            )
            destination=="My bids"->MyBidsScreen(
                all=all,saved=saved,bids=bids,selectedTab=bidsTab,
                onTab={bidsTab=it},
                onOpen={selectedId=it},
                onSave={id->vm.save(id,true)},
                onTrack={id->vm.track(id){bidsTab="Tracking"}},
                onRemoveBid=vm::removeBid,
                onStage=vm::stage,onNotes=vm::notes,noteStates=vm.notesStatus,
                onDiscover={destination="Discover"},
                modifier=modifier
            )
            destination=="Deadlines"->DeadlinesScreen(
                all=all,alerts=alerts,dismissed=dismissed,
                selectedTab=deadlineTab,onTab={deadlineTab=it},
                onOpen={selectedId=it},onDismiss=vm::dismiss,
                onRestore=vm::restoreAlerts,onDiscover={destination="Discover"},
                reminderDays=reminderDays,modifier=modifier
            )
            else->InsightsScreen(
                all=all,lastSync=sync,selectedTab=insightsTab,
                onTab={insightsTab=it},modifier=modifier
            )
        }
    }
}
