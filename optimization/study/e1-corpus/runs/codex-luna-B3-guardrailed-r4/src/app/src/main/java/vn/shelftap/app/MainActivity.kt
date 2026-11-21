package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.align
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ShelfTapTheme { val auth=remember { SignedInAuthRepository { "" } }; val vm: ShelfTapViewModel = viewModel(factory=ShelfTapViewModelFactory(ShelfTapApi(auth),SecureCache(this))); ShelfTapApp(vm) } }
    }
}

@Composable private fun ShelfTapTheme(content: @Composable () -> Unit) { MaterialTheme(content=content) }
private enum class Page { Loans, History, Detail }

@Composable private fun ShelfTapApp(vm: ShelfTapViewModel) {
    val state by vm.state.collectAsStateWithLifecycle(); val detail by vm.detail.collectAsStateWithLifecycle(); var page by remember { mutableStateOf(Page.Loans) }; var selected by remember { mutableStateOf<String?>(null) }; var confirmOff by remember { mutableStateOf(false) }
    Scaffold(topBar={ TopAppBar(title={Text(if(page==Page.Loans) "My loans" else if(page==Page.History) "Reading history" else "Book details")}, navigationIcon={ if(page==Page.Detail) TextButton(onClick={page=Page.Loans}) { Text(stringResource(R.string.back)) } }) }, bottomBar={ if(page!=Page.Detail) NavigationBar { NavigationBarItem(selected=page==Page.Loans,onClick={page=Page.Loans},icon={},label={Text("Loans")}); NavigationBarItem(selected=page==Page.History,onClick={page=Page.History},icon={},label={Text("History")}) } }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) { if(state.saved) Text("Showing saved data",Modifier.fillMaxWidth().padding(12.dp),color=MaterialTheme.colorScheme.primary); state.message?.let { Text(it,Modifier.fillMaxWidth().padding(horizontal=12.dp),color=MaterialTheme.colorScheme.error) }; if(state.loading && state.loans.isEmpty() && state.history.isEmpty()) CircularProgressIndicator(Modifier.padding(24.dp)) else when(page) { Page.Loans -> LoansPage(state,vm){ selected=it; vm.loadTitle(it); page=Page.Detail }; Page.History -> HistoryPage(state,vm,{ selected=it; vm.loadTitle(it); page=Page.Detail },{confirmOff=true}); Page.Detail -> DetailPage(detail) } }
    }
    if(confirmOff) AlertDialog(onDismissRequest={confirmOff=false},title={Text("Turn off reading history?" )},text={Text("Past loans will no longer be kept in your reading history.")},confirmButton={TextButton(onClick={confirmOff=false;vm.setHistory(false)}){Text("Turn off")}},dismissButton={TextButton(onClick={confirmOff=false}){Text("Cancel")}})
}

@Composable private fun LoansPage(state: ShelfState, vm: ShelfTapViewModel, open:(String)->Unit) { LazyColumn { items(state.loans,key={it.loan.loanId},contentType={"loan"}) { row -> LoanRowView(row, {open(row.loan.titleId)}) { vm.renew(row.loan) } } } }
@Composable private fun LoanRowView(row: LoanRow, open:()->Unit, renew:()->Unit) { Column(Modifier.fillMaxWidth().clickable(onClick=open).padding(16.dp)) { Text(row.loan.title,style=MaterialTheme.typography.titleMedium); Text(if(row.overdue) "Overdue · ${row.daysLabel}" else row.daysLabel,color=if(row.overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant); Row(verticalAlignment=Alignment.CenterVertically) { Text("Due ${row.dueLabel}",Modifier.weight(1f)); Button(onClick=renew,enabled=row.loan.renewable,modifier=Modifier.size(100.dp,48.dp)){Text(stringResource(R.string.renew))} }; HorizontalDivider(Modifier.padding(top=12.dp)) } }
@Composable private fun HistoryPage(state:ShelfState, vm:ShelfTapViewModel, open:(String)->Unit, turnOff:()->Unit) { Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) { Text(stringResource(R.string.history_switch),Modifier.weight(1f)); Switch(checked=state.historyEnabled,onCheckedChange={if(it) vm.setHistory(true) else turnOff()}) }; HorizontalDivider(); LazyColumn { items(state.history,key={"${it.item.titleId}-${it.item.outAt}"},contentType={"history"}) { row -> Column(Modifier.fillMaxWidth().clickable{open(row.item.titleId)}.padding(16.dp)) { Text(row.item.title,style=MaterialTheme.typography.titleMedium); Text("Borrowed ${row.borrowedLabel} · Returned ${row.returnedLabel}",color=MaterialTheme.colorScheme.onSurfaceVariant); HorizontalDivider(Modifier.padding(top=12.dp)) } } } }
@Composable private fun DetailPage(detail: TitleDetail?) { if(detail==null) CircularProgressIndicator(Modifier.padding(24.dp)) else Column(Modifier.fillMaxWidth().padding(20.dp)) { AsyncImage(model=detail.coverUrl,contentDescription=stringResource(R.string.book_cover),modifier=Modifier.size(180.dp).align(Alignment.CenterHorizontally)); Spacer(Modifier.height(16.dp)); Text(detail.title,style=MaterialTheme.typography.headlineSmall); Text(detail.authors.joinToString(),style=MaterialTheme.typography.bodyLarge); Spacer(Modifier.height(12.dp)); Text("Subjects: ${detail.subjects.joinToString()}"); Text("Copies available: ${detail.copiesAvailable} of ${detail.copiesTotal}") } }
