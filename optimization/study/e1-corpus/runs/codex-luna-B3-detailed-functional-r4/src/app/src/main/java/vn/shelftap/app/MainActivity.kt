package vn.shelftap.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);val r=ShelfTapRepository(ShelfTapApi(this,SignedInAuthRepository()));setContent{App(r)}}}
private val fmt=DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
private fun date(s:String)=runCatching{fmt.format(Instant.parse(s))}.getOrDefault(s.take(10))
private fun days(s:String)=runCatching{ChronoUnit.DAYS.between(Instant.now(),Instant.parse(s)).toInt()}.getOrDefault(0)
@Composable fun App(repo:ShelfTapRepository){val vm:ShelfViewModel=viewModel(factory=ShelfViewModelFactory(repo));val nav=rememberNavController();val s by vm.state.collectAsState();MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF3F5F90),surface=Color(0xFFFBF8FF))){NavHost(nav,"loans"){composable("loans"){Scaffold(bottomBar={Bar(nav,"loans")}){p->Loans(s,vm,{nav.navigate("book/$it")},Modifier.padding(p))}};composable("history"){Scaffold(bottomBar={Bar(nav,"history")}){p->History(s,vm,{nav.navigate("book/$it")},Modifier.padding(p))}};composable("book/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})){Book(repo,it.arguments?.getString("id")?="",nav)}}}}
@Composable private fun Bar(nav:androidx.navigation.NavHostController,cur:String){NavigationBar{NavigationBarItem(cur=="loans",{nav.navigate("loans")},Icons.Default.MenuBook,{Text("Loans")});NavigationBarItem(cur=="history",{nav.navigate("history")},Icons.Default.History,{Text("History")})}}
@Composable private fun Head(t:String,off:Boolean,refresh:()->Unit){Column(Modifier.fillMaxWidth().padding(20.dp)){Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){Text(t,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(refresh){Icon(Icons.Default.Refresh,"Refresh")}};if(off)Text("Showing saved data",color=MaterialTheme.colorScheme.secondary)}}
@Composable private fun Loans(s:ShelfState,vm:ShelfViewModel,open:(String)->Unit,m:Modifier){var chosen by remember{mutableStateOf<Loan?>(null)};Column(m){Head("My loans",s.offlineLoans,vm::refresh);if(s.loading)LinearProgressIndicator(Modifier.fillMaxWidth());if(s.loans.isEmpty()&&!s.loading)Empty("No current loans");LazyColumn{items(s.loans,key={it.loanId}){x->val left=days(x.dueAt);Card(Modifier.padding(6.dp).fillMaxWidth().clickable{open(x.titleId)}){Column(Modifier.padding(16.dp)){Text(x.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text("Due ${date(x.dueAt)}");if(left<0)Text("Overdue",color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)else Text("$left days left",color=MaterialTheme.colorScheme.primary);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){Button(enabled=x.renewable,onClick={chosen=x}){Text("Renew")}}}}}}};chosen?.let{x->Confirm("Renew loan?","Renew ‘${x.title}’ now?",{vm.renew(x);chosen=null},{chosen=null})};s.message?.let{AlertDialog(onDismissRequest=vm::clearMessage,confirmButton={TextButton(vm::clearMessage){Text("OK")}},title={Text("ShelfTap")},text={Text(it)})}}}
@Composable private fun History(s:ShelfState,vm:ShelfViewModel,open:(String)->Unit,m:Modifier){var confirm by remember{mutableStateOf(false)};Column(m){Head("Reading history",s.offlineHistory,vm::refresh);Row(Modifier.fillMaxWidth().padding(20.dp,8.dp),verticalAlignment=Alignment.CenterVertically){Text("Keep my reading history",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);Switch(s.member?.historyEnabled?:true,{if(!it)confirm=true else vm.setHistory(true)})};if(s.history.isEmpty()&&!s.loading)Empty("No reading history");LazyColumn{items(s.history.sortedByDescending{it.outAt}){x->ListItem(headlineContent={Text(x.title,fontWeight=FontWeight.SemiBold)},supportingContent={Text("Borrowed ${date(x.outAt)} • Returned ${x.returnedAt?.let(::date)?:"—"}")},modifier=Modifier.clickable{open(x.titleId)})}}};if(confirm)Confirm("Turn off history?","Your past loans will no longer be saved or shown.",{vm.setHistory(false);confirm=false},{confirm=false})}
@Composable private fun Confirm(t:String,msg:String,yes:()->Unit,no:()->Unit){AlertDialog(onDismissRequest=no,title={Text(t)},text={Text(msg)},confirmButton={TextButton(yes){Text("Confirm")}},dismissButton={TextButton(no){Text("Cancel")}})}
@Composable private fun Empty(t:String){Box(Modifier.fillMaxWidth().padding(48.dp),contentAlignment=Alignment.Center){Text(t,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun Book(repo:ShelfTapRepository,id:String,nav:androidx.navigation.NavHostController){var b by remember{mutableStateOf<BookDetail?>(null)};var error by remember{mutableStateOf(false)};LaunchedEffect(id){runCatching{repo.book(id)}.onSuccess{b=it}.onFailure{error=true}};Scaffold(topBar={TopAppBar(title={Text("Book details")},navigationIcon={IconButton({nav.popBackStack()}){Icon(Icons.Default.ArrowBack,"Back")}})}){p->if(b==null){if(error)Empty("Unable to load book details")else Box(Modifier.fillMaxSize().padding(p),contentAlignment=Alignment.Center){CircularProgressIndicator()}}else Column(Modifier.padding(p).padding(20.dp)){b!!.coverUrl?.let{AsyncImage(it,null,Modifier.fillMaxWidth().height(230.dp),contentScale=ContentScale.Fit)};Spacer(Modifier.height(18.dp));Text(b!!.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);if(b!!.authors.isNotEmpty())Text(b!!.authors.joinToString(),style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(14.dp));Text("Subjects",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(b!!.subjects.joinToString(" • ").ifBlank{"No subjects listed"});Spacer(Modifier.height(20.dp));Text("${b!!.copiesAvailable} of ${b!!.copiesTotal} copies available",style=MaterialTheme.typography.titleMedium)}}}
