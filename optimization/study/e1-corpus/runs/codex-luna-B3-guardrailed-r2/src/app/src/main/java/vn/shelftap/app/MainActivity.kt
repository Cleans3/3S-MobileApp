package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ShelfTapApp() } }
}
private enum class Tab { Loans, History }
@Composable fun ShelfTapApp(vm: ShelfTapViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle(); var tab by remember { mutableStateOf(Tab.Loans) }; var detailId by remember { mutableStateOf<String?>(null) }; val snack = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snack.showSnackbar(it) } }
    if (detailId != null) { BookScreen(detailId!!, vm) { detailId = null }; return }
    Scaffold(snackbarHost = { SnackbarHost(snack) }, bottomBar = { NavigationBar { NavigationBarItem(tab == Tab.Loans, { tab = Tab.Loans }, label = { Text("Loans") }, icon = {}); NavigationBarItem(tab == Tab.History, { tab = Tab.History }, label = { Text("History") }, icon = {}) } }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) { Text(if (tab == Tab.Loans) "My loans" else "Reading history", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 18.dp)); if (state.saved) Text("Showing saved data", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp)); if (state.loading) CircularProgressIndicator() else if (tab == Tab.Loans) LoansList(state.loans, vm, { detailId = it }) else HistoryList(state, vm, { detailId = it }) }
    }
}
@Composable private fun LoansList(list: List<Loan>, vm: ShelfTapViewModel, open: (String) -> Unit) { LazyColumn { items(list, key = { it.loanId }, contentType = { "loan" }) { loan -> val days = vm.daysLeft(loan.dueAt); Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { open(loan.titleId) }) { Column(Modifier.padding(16.dp)) { Text(loan.title, style = MaterialTheme.typography.titleMedium); Text("Due ${vm.format(loan.dueAt)}"); if (days < 0L) Text("Overdue", color = MaterialTheme.colorScheme.error); else Text("$days days left"); Spacer(Modifier.height(8.dp)); Button({ vm.renew(loan) }, enabled = loan.renewable, modifier = Modifier.size(110.dp, 48.dp)) { Text(stringResource(R.string.renew)) } } } } } }
@Composable private fun HistoryList(state: ShelfState, vm: ShelfTapViewModel, open: (String) -> Unit) { var confirm by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); Column { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.keep_history)); Switch(checked = state.member?.historyEnabled == true, onCheckedChange = { if (!it) confirm = true else vm.setHistory(true) }) }; HorizontalDivider(); LazyColumn { items(state.history, key = { "${it.titleId}-${it.outAt}" }, contentType = { "history" }) { item -> Row(Modifier.fillMaxWidth().clickable { open(item.titleId) }.padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(item.title, style = MaterialTheme.typography.titleMedium); Text("Borrowed ${vm.format(item.outAt)}"); item.returnedAt?.let { Text("Returned ${vm.format(it)}") } } } } } }; if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.confirm_history_title)) }, text = { Text(stringResource(R.string.confirm_history_message)) }, confirmButton = { TextButton({ confirm = false; scope.launch { vm.setHistory(false) } }) { Text(stringResource(R.string.turn_off)) } }, dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.cancel)) } }) }
}
@Composable private fun BookScreen(id: String, vm: ShelfTapViewModel, back: () -> Unit) { var book by remember { mutableStateOf<BookDetail?>(null) }; LaunchedEffect(id) { book = runCatching { vm.book(id) }.getOrNull() }; Column(Modifier.fillMaxSize().padding(16.dp)) { TextButton(back) { Text("‹  ${stringResource(R.string.back)}") }; val b = book; if (b == null) CircularProgressIndicator() else { AsyncImage(model = b.coverUrl, contentDescription = stringResource(R.string.cover), modifier = Modifier.fillMaxWidth().aspectRatio(0.67f)); Spacer(Modifier.height(16.dp)); Text(b.title, style = MaterialTheme.typography.headlineSmall); Text(b.authors.joinToString(), style = MaterialTheme.typography.bodyLarge); Spacer(Modifier.height(12.dp)); Text("Subjects: ${b.subjects.joinToString()}"); Text("${b.copiesAvailable} of ${b.copiesTotal} copies available") } }
}
