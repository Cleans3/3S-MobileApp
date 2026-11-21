package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import vn.shelftap.app.data.*
import vn.shelftap.app.ui.*

private object SignedInAuthRepository : AuthRepository {
    override suspend fun accessToken(): String = error("Provide the signed-in AuthRepository in the host application")
}

class MainActivity : ComponentActivity() {
    private val model: ShelfTapViewModel by viewModels {
        ShelfTapViewModelFactory(ShelfTapApi(SignedInAuthRepository), EncryptedCache(applicationContext))
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ShelfTapTheme { ShelfTapApp(model) } } }
}

@Composable private fun ShelfTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF245B4A), secondary = androidx.compose.ui.graphics.Color(0xFF526B5D)), content = content)
}

private enum class Tab { LOANS, HISTORY }

@Composable private fun ShelfTapApp(vm: ShelfTapViewModel) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(Tab.LOANS) }
    if (state.detail != null) BookDetailScreen(state.detail!!, vm::closeDetail) else Scaffold(
        topBar = { TopAppBar(title = { Text("ShelfTap", fontWeight = FontWeight.Bold) }, actions = { TextButton(onClick = vm::refresh, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text(stringResource(R.string.refresh)) } }) },
        bottomBar = { NavigationBar { NavigationBarItem(selected = tab == Tab.LOANS, onClick = { tab = Tab.LOANS }, icon = { Text("▣") }, label = { Text("Loans") }); NavigationBarItem(selected = tab == Tab.HISTORY, onClick = { tab = Tab.HISTORY }, icon = { Text("◷") }, label = { Text("History") }) } }
    ) { padding -> Column(Modifier.padding(padding).fillMaxSize()) {
        if (state.saved) Text("Showing saved data", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (tab == Tab.LOANS) LoansScreen(state.loans, vm::renew, vm::openDetail) else HistoryScreen(state, vm::setHistory, vm::openDetail)
    } }
}

@Composable private fun LoansScreen(loans: List<Loan>, renew: (Loan) -> Unit, detail: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(loans, key = { it.loanId }, contentType = { "loan" }) { loan ->
        val days = remember(loan.dueAt) { loan.daysLeft() }; val dueDate = remember(loan.dueAt) { loan.dueAt.prettyDate() }
        Card(Modifier.fillMaxWidth().clickable { detail(loan.titleId) }) { Column(Modifier.padding(16.dp)) {
            Text(loan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (days < 0) Text("Overdue", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Text("Due $dueDate · ${if (days < 0) "${-days} days overdue" else "$days days left"}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Button(onClick = { renew(loan) }, enabled = loan.renewable, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("Renew") } }
        } }
    } }
}

@Composable private fun HistoryScreen(state: ShelfState, setHistory: (Boolean) -> Unit, detail: (String) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("Keep my reading history", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Switch(checked = state.member?.historyEnabled == true, onCheckedChange = { if (!it) confirm = true else setHistory(true) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(state.history, key = { "${it.titleId}-${it.outAt}" }, contentType = { "history" }) { item ->
            val outDate = remember(item.outAt) { item.outAt.prettyDate() }; val returnedDate = remember(item.returnedAt) { item.returnedAt?.prettyDate() ?: "Not returned" }
            Card(Modifier.fillMaxWidth().clickable { detail(item.titleId) }) { Column(Modifier.padding(16.dp)) { Text(item.title, style = MaterialTheme.typography.titleMedium); Text("Borrowed $outDate"); Text("Returned $returnedDate") } }
        } }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.confirm_history_off)) }, text = { Text(stringResource(R.string.history_off_message)) }, confirmButton = { TextButton(onClick = { confirm = false; setHistory(false) }) { Text(stringResource(R.string.turn_off)) } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable private fun BookDetailScreen(book: BookDetail, back: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Book details") }, navigationIcon = { TextButton(onClick = back, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("‹ ${stringResource(R.string.back)}") } }) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { AsyncImage(model = book.coverUrl, contentDescription = stringResource(R.string.cover), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(240.dp)) }
            item { Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item { Text(if (book.authors.isEmpty()) "Unknown author" else book.authors.joinToString()) }
            item { Text("Subjects: ${book.subjects.ifEmpty { listOf("—") }.joinToString()}") }
            item { Text("Copies available: ${book.copiesAvailable} of ${book.copiesTotal}", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
