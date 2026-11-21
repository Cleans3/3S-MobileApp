package vn.shelftap.app
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import vn.shelftap.app.data.*

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{ShelfTapApp()}}}
private val green=Color(0xFF315F51);private val dateFmt=DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
private fun date(s:String)=runCatching{dateFmt.format(Instant.parse(s))}.getOrDefault(s.take(10));private fun days(s:String)=runCatching{ChronoUnit.DAYS.between(Instant.now(),Instant.parse(s)).toInt()}.getOrDefault(0)
@Composable fun ShelfTapApp(vm:MainViewModel=viewModel()){val st by vm.state.collectAsState();var tab by remember{mutableIntStateOf(0)};var detail by remember{mutableStateOf<String?>(null)};MaterialTheme(colorScheme=lightColorScheme(primary=green,background=Color(0xFFF8F9F6))){if(detail!=null)DetailScreen(st.title,detail!!,onBack={detail=null}) else Scaffold(topBar={TopAppBar(title={Text(if(tab==0)"My loans" else "Reading history")},actions={IconButton({vm.refresh()}){Icon(Icons.Default.Refresh,"Refresh")}})},bottomBar={NavigationBar{NavigationBarItem(tab==0,{tab=0},icon={Icon(Icons.Default.MenuBook,"Loans")},label={Text("Loans")});NavigationBarItem(tab==1,{tab=1},icon={Icon(Icons.Default.History,"History")},label={Text("History")})}}){pad->Column(Modifier.padding(pad).fillMaxSize()){if(st.message!=null)Text(st.message!!,Modifier.fillMaxWidth().background(Color(0xFFFFE0DC)).padding(12.dp),color=Color(0xFF8B1A10));if(tab==0)LoansScreen(st,vm){vm.loadTitle(it);detail=it}else HistoryScreen(st,vm){vm.loadTitle(it);detail=it}}}}}
@Composable fun LoansScreen(st:AppState,vm:MainViewModel,onDetail:(String)->Unit){if(st.loansOffline)SavedNote();if(st.loading&&st.loans.isEmpty())Loading();LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(st.loans){loan->val left=days(loan.dueAt);Card(Modifier.fillMaxWidth().clickable{onDetail(loan.titleId)} ){Column(Modifier.padding(16.dp)){Text(loan.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));if(left<0)Text("Overdue",color=Color(0xFFB3261E),fontWeight=FontWeight.Bold);Text(if(left<0)"Due ${date(loan.dueAt)} · ${-left} days overdue" else "Due ${date(loan.dueAt)} · $left days left",color=Color.Gray);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){Button({vm.renew(loan.loanId)},enabled=loan.renewable){Text("Renew")}}}}}}}
@Composable fun HistoryScreen(st:AppState,vm:MainViewModel,onDetail:(String)->Unit){var confirm by remember{mutableStateOf(false)};Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Text("Keep my reading history",Modifier.weight(1f),fontWeight=FontWeight.Medium);Switch(checked=st.member?.historyEnabled==true,onCheckedChange={if(!it)confirm=true else vm.setHistory(true)})};if(st.historyOffline)SavedNote();LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(st.history.sortedByDescending{it.outAt}){h->Card(Modifier.fillMaxWidth().clickable{onDetail(h.titleId)}){Column(Modifier.padding(16.dp)){Text(h.title,fontWeight=FontWeight.SemiBold);Text("Borrowed ${date(h.outAt)}",color=Color.Gray);Text("Returned ${date(h.returnedAt)}",color=Color.Gray)}}}};if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Turn off reading history?")},text={Text("Your future reading history will not be saved, and existing history will no longer be shown.")},confirmButton={TextButton({confirm=false;vm.setHistory(false)}){Text("Turn off")}},dismissButton={TextButton({confirm=false}){Text("Cancel")}})} }
@Composable fun DetailScreen(d:TitleDetails?,id:String,onBack:()->Unit){var bitmap by remember(d?.coverUrl){mutableStateOf<Bitmap?>(null)};LaunchedEffect(d?.coverUrl){val url=d?.coverUrl;if(url!=null)bitmap=withContext(Dispatchers.IO){runCatching{BitmapFactory.decodeStream(URL(url).openStream())}.getOrNull()}};Scaffold(topBar={TopAppBar(title={Text("Book details")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,"Back")}})}){p->Column(Modifier.padding(p).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){if(bitmap!=null)Image(bitmap!!.asImageBitmap(),"Cover",Modifier.size(150.dp,210.dp))else Box(Modifier.size(150.dp,210.dp).background(Color(0xFFDDE8E1)),contentAlignment=Alignment.Center){Icon(Icons.Default.MenuBook,"Book",Modifier.size(64.dp),tint=green)};Spacer(Modifier.height(20.dp));Text(d?.title?:"Loading…",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Title ID: $id",color=Color.Gray);Spacer(Modifier.height(18.dp));Text("Authors",fontWeight=FontWeight.Bold);Text(d?.authors?.joinToString()?:"Loading…");Spacer(Modifier.height(12.dp));Text("Subjects",fontWeight=FontWeight.Bold);Text(d?.subjects?.joinToString()?:"—");Spacer(Modifier.height(18.dp));Text("Copies available",fontWeight=FontWeight.Bold);Text("${d?.copiesAvailable?:0} of ${d?.copiesTotal?:0}")}}}
@Composable fun SavedNote(){Text("Showing saved data",Modifier.fillMaxWidth().background(Color(0xFFE3F1EA)).padding(12.dp),color=green)}
@Composable fun Loading(){Box(Modifier.fillMaxWidth().padding(24.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}}
