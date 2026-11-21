package vn.shelftap.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

interface AuthRepository { suspend fun accessToken(): String }
object SignedInAuthRepository : AuthRepository { override suspend fun accessToken() = "" }

data class Loan(val loanId:String,val copyId:String,val titleId:String,val title:String,val barcode:String,val outAt:String,val dueAt:String,val returnedAt:String?,val renewals:Int,val renewable:Boolean)
data class HistoryItem(val titleId:String,val title:String,val outAt:String,val returnedAt:String?)
data class Member(val memberId:String,val fullName:String,val email:String,val memberType:String,val status:String,val historyEnabled:Boolean,val mustChangePassword:Boolean)
data class Title(val titleId:String,val isbn:String,val title:String,val authors:List<String>,val subjects:List<String>,val coverUrl:String?,val copiesAvailable:Int,val copiesTotal:Int)

class ShelfApi(private val auth: AuthRepository = SignedInAuthRepository) {
    private val base = "https://api.shelftap.example"
    private suspend fun request(path:String, method:String="GET", body:String?=null): JSONObject? = withContext(Dispatchers.IO) {
        val c = URL(base+path).openConnection() as HttpURLConnection
        try { c.requestMethod=method; c.setRequestProperty("Authorization","Bearer ${auth.accessToken()}"); c.setRequestProperty("Accept","application/json"); c.setRequestProperty("Content-Type","application/json")
            if(body!=null){c.doOutput=true;c.outputStream.use{it.write(body.toByteArray())}}
            val text=(if(c.responseCode in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()} ?: "{}"
            if(c.responseCode !in 200..299) throw ApiException(c.responseCode, JSONObject(text).optString("code"))
            JSONObject(text)
        } finally { c.disconnect() }
    }
    private suspend fun array(path:String): JSONArray = withContext(Dispatchers.IO) { val c=URL(base+path).openConnection() as HttpURLConnection; try { c.setRequestProperty("Authorization","Bearer ${auth.accessToken()}"); val stream=if(c.responseCode in 200..299)c.inputStream else c.errorStream ?: c.inputStream; val text=stream.bufferedReader().use{it.readText()}; if(c.responseCode !in 200..299) throw ApiException(c.responseCode,""); JSONArray(text) } finally{c.disconnect()} }
    suspend fun loans()=array("/api/me/loans").let{(0 until it.length()).map{n->loan(it.getJSONObject(n))}}
    suspend fun history()=array("/api/me/history").let{(0 until it.length()).map{n->history(it.getJSONObject(n))}}
    suspend fun member()=request("/api/me")!!.let{member(it)}
    suspend fun privacy(enabled:Boolean)=request("/api/me/privacy","PUT",JSONObject().put("historyEnabled",enabled).toString())!!.let{member(it)}
    suspend fun renew(id:String)=request("/api/loans/$id/renew")!!.let{loan(it)}
    suspend fun title(id:String)=request("/api/titles/$id")!!.let{title(it)}
    private fun loan(o:JSONObject)=Loan(o.optString("loanId"),o.optString("copyId"),o.optString("titleId"),o.optString("title"),o.optString("barcode"),o.optString("outAt"),o.optString("dueAt"),o.optString("returnedAt").takeIf{it.isNotEmpty()},o.optInt("renewals"),o.optBoolean("renewable"))
    private fun history(o:JSONObject)=HistoryItem(o.optString("titleId"),o.optString("title"),o.optString("outAt"),o.optString("returnedAt").takeIf{it.isNotEmpty()})
    private fun member(o:JSONObject)=Member(o.optString("memberId"),o.optString("fullName"),o.optString("email"),o.optString("memberType"),o.optString("status"),o.optBoolean("historyEnabled"),o.optBoolean("mustChangePassword"))
    private fun title(o:JSONObject)=Title(o.optString("titleId"),o.optString("isbn"),o.optString("title"),o.optJSONArray("authors").strings(),o.optJSONArray("subjects").strings(),o.optString("coverUrl").ifEmpty{null},o.optInt("copiesAvailable"),o.optInt("copiesTotal"))
    private fun JSONArray?.strings()=(0 until (this?.length()?:0)).map{this!!.optString(it)}
}
class ApiException(val status:Int,val code:String):Exception()

class ShelfViewModel(private val api:ShelfApi=ShelfApi()):ViewModel(){
    var loans by mutableStateOf<List<Loan>>(emptyList()); private set
    var history by mutableStateOf<List<HistoryItem>>(emptyList()); private set
    var member by mutableStateOf<Member?>(null); private set
    var title by mutableStateOf<Title?>(null); private set
    var loading by mutableStateOf(false); private set
    var saved by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    private val prefs by lazy { App.context.getSharedPreferences("cache",Context.MODE_PRIVATE) }
    fun load(){ loading=true; viewModelScope.launch { try { loans=api.loans(); history=api.history(); member=api.member(); cache() ; saved=false } catch(_:Exception){ restore(); saved=true } finally{loading=false} } }
    fun renew(l:Loan){viewModelScope.launch{try{loans=loans.map{if(it.loanId==l.loanId)api.renew(l.loanId) else it};cache()}catch(e:ApiException){if(e.code=="RENEWAL_NOT_ALLOWED")message="This loan cannot be renewed" else message="Unable to renew this loan"}}}
    fun setHistory(enabled:Boolean){viewModelScope.launch{try{member=api.privacy(enabled)}catch(_:Exception){message="Unable to update privacy settings"}}}
    fun openTitle(id:String){viewModelScope.launch{try{title=api.title(id)}catch(_:Exception){message="Unable to load book details"}}}
    fun clearTitle(){title=null}; fun clearMessage(){message=null}
    private fun cache(){val m=member; prefs.edit().putString("loans",JSONArray(loans.map{JSONObject().put("loanId",it.loanId).put("copyId",it.copyId).put("titleId",it.titleId).put("title",it.title).put("barcode",it.barcode).put("outAt",it.outAt).put("dueAt",it.dueAt).put("returnedAt",it.returnedAt).put("renewals",it.renewals).put("renewable",it.renewable)}).toString()).putString("history",JSONArray(history.map{JSONObject().put("titleId",it.titleId).put("title",it.title).put("outAt",it.outAt).put("returnedAt",it.returnedAt)}).toString()).putString("member",m?.let{JSONObject().put("memberId",it.memberId).put("fullName",it.fullName).put("email",it.email).put("memberType",it.memberType).put("status",it.status).put("historyEnabled",it.historyEnabled).put("mustChangePassword",it.mustChangePassword)}?.toString()).apply()}
    private fun restore(){fun a(k:String)=JSONArray(prefs.getString(k,"[]")); loans=(0 until a("loans").length()).map{val o=a("loans").getJSONObject(it);Loan(o.optString("loanId"),o.optString("copyId"),o.optString("titleId"),o.optString("title"),o.optString("barcode"),o.optString("outAt"),o.optString("dueAt"),o.optString("returnedAt").takeIf{it.isNotEmpty()},o.optInt("renewals"),o.optBoolean("renewable"))}; history=(0 until a("history").length()).map{val o=a("history").getJSONObject(it);HistoryItem(o.optString("titleId"),o.optString("title"),o.optString("outAt"),o.optString("returnedAt").takeIf{it.isNotEmpty()})}; prefs.getString("member",null)?.let{val o=JSONObject(it);member=Member(o.optString("memberId"),o.optString("fullName"),o.optString("email"),o.optString("memberType"),o.optString("status"),o.optBoolean("historyEnabled"),o.optBoolean("mustChangePassword"))}}
}
object App { lateinit var context:Context }

private val dateFmt=DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
private fun date(s:String)=runCatching{dateFmt.format(Instant.parse(s))}.getOrDefault(s)
private fun days(s:String)=runCatching{ChronoUnit.DAYS.between(Instant.now(),Instant.parse(s)).toInt()}.getOrDefault(0)

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);App.context=applicationContext;setContent{ShelfTheme{ShelfApp()}}}}
@Composable fun ShelfTheme(content:@Composable()->Unit)=MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF49664B),secondary=Color(0xFF866A35),background=Color(0xFFF8FAF4)),content=content)

@Composable fun ShelfApp(vm:ShelfViewModel=viewModel()){var tab by remember{mutableIntStateOf(0)};var confirm by remember{mutableStateOf(false)};LaunchedEffect(Unit){vm.load()};if(vm.title!=null){Detail(vm.title!!){vm.clearTitle()};return};Scaffold(topBar={TopAppBar(title={Text("ShelfTap",fontWeight=FontWeight.Bold)},actions={IconButton({vm.load()}){Icon(Icons.Default.Refresh,"Refresh")}})},bottomBar={NavigationBar{listOf("Loans" to Icons.Default.MenuBook,"History" to Icons.Default.History).forEachIndexed{i,p->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(p.second,null)},label={Text(p.first)})}}}){pad->Column(Modifier.padding(pad).fillMaxSize()){if(vm.saved)Text("Showing saved data",Modifier.fillMaxWidth().background(Color(0xFFFFE8B2)).padding(12.dp),color=Color(0xFF634F00));if(tab==0)Loans(vm)else History(vm){confirm=true}}};if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Turn off reading history?")},text={Text("Your past loans will no longer be saved while this is off.")},confirmButton={TextButton({confirm=false;vm.setHistory(false)}){Text("Turn off")}},dismissButton={TextButton({confirm=false}){Text("Cancel")}})};vm.message?.let{AlertDialog(onDismissRequest={vm.clearMessage()},text={Text(it)},confirmButton={TextButton({vm.clearMessage()}){Text("OK")}})}}

@Composable fun Loans(vm:ShelfViewModel){if(vm.loading&&vm.loans.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}else LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(vm.loans){l->LoanRow(l,{vm.openTitle(l.titleId)},{vm.renew(l)})}}}
@Composable fun LoanRow(l:Loan,open:()->Unit,renew:()->Unit){val d=days(l.dueAt);Card(Modifier.fillMaxWidth().clickable{open()}){Column(Modifier.padding(16.dp)){Text(l.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text("Due ${date(l.dueAt)}",color=Color.Gray);if(d<0)Text("Overdue",color=Color(0xFFB3261E),fontWeight=FontWeight.Bold)else Text("$d days left",color=MaterialTheme.colorScheme.primary);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){Button(onClick=renew,enabled=l.renewable){Text("Renew")}}}}}
@Composable fun History(vm:ShelfViewModel,onOff:()->Unit){Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Text("Keep my reading history",Modifier.weight(1f),fontWeight=FontWeight.Medium);Switch(checked=vm.member?.historyEnabled?:true,onCheckedChange={if(!it)onOff()else vm.setHistory(true)})};if(vm.history.isEmpty())Text("No past loans",Modifier.padding(16.dp),color=Color.Gray)else LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(vm.history.sortedByDescending{it.outAt}){h->Card(Modifier.fillMaxWidth().clickable{vm.openTitle(h.titleId)}){Column(Modifier.padding(16.dp)){Text(h.title,fontWeight=FontWeight.SemiBold);Text("Borrowed ${date(h.outAt)}",color=Color.Gray);Text("Returned ${h.returnedAt?.let(::date)?:"—"}",color=Color.Gray)}}}}}}
@Composable fun Detail(t:Title,back:()->Unit){Scaffold(topBar={TopAppBar(title={Text("Book details")},navigationIcon={IconButton(back){Icon(Icons.Default.ArrowBack,"Back")}})}){p->Column(Modifier.padding(p).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Box(Modifier.fillMaxWidth().height(180.dp).background(Color(0xFFE2E8DD)),contentAlignment=Alignment.Center){Icon(Icons.Default.MenuBook,"Cover",Modifier.size(72.dp),tint=Color(0xFF49664B))};Text(t.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(t.authors.joinToString(),style=MaterialTheme.typography.bodyLarge);if(t.subjects.isNotEmpty())Text("Subjects: ${t.subjects.joinToString()}",color=Color.Gray);Text("${t.copiesAvailable} of ${t.copiesTotal} copies available",fontWeight=FontWeight.Medium)}}}
