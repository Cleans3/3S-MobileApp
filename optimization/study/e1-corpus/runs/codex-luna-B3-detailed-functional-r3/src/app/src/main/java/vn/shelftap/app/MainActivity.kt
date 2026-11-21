package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ShelfTapApp() } }
}

private enum class Tab { LOANS, HISTORY }
private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
private fun pretty(value: String?): String = value?.let { runCatching { dateFormatter.format(Instant.parse(it)) }.getOrElse { it } } ?: "—"
private fun daysLeft(value: String): Long = runCatching { ChronoUnit.DAYS.between(Instant.now(), Instant.parse(value)) }.getOrDefault(0)

@Composable fun ShelfTapApp(vm: ShelfViewModel = viewModel()) {
    val s by vm.state; var tab by remember { mutableStateOf(Tab.LOANS) }; var detailId by remember { mutableStateOf<String?>(null) }; var confirm by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF355C7D), secondary = Color(0xFFB26E3C), background = Color(0xFFF8F7F3))) {
        Scaffold(topBar = { TopAppBar(title = { Text(if (detailId == null) "ShelfTap" else "Book details") }, navigationIcon = { if (detailId != null) TextButton(onClick = { detailId = null }) { Text("Back") } }) }, bottomBar = { if (detailId == null) NavigationBar { NavigationBarItem(tab == Tab.LOANS, { tab = Tab.LOANS }, label = { Text("Loans") }, icon = { Text("▣") }); NavigationBarItem(tab == Tab.HISTORY, { tab = Tab.HISTORY }, label = { Text("History") }, icon = { Text("↺") }) } } }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                if (detailId != null) { if (s.detail == null) CircularProgressIndicator(Modifier.align(Alignment.Center)) else DetailScreen(s.detail!!) }
                else if (tab == Tab.LOANS) LoansScreen(s, vm) { id -> vm.loadDetail(id); detailId = id } else HistoryScreen(s, vm, { id -> vm.loadDetail(id); detailId = id }, { confirm = true })
            }
        }
        if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Turn off reading history?") }, text = { Text("Your past loans will no longer be kept. You can turn this back on at any time.") }, confirmButton = { TextButton(onClick = { confirm = false; vm.setHistory(false) }) { Text("Turn off") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
        s.message?.let { msg -> LaunchedEffect(msg) { kotlinx.coroutines.delay(3500); vm.clearMessage() }; Snackbar(Modifier.padding(16.dp).align(Alignment.BottomCenter), action = { TextButton(onClick = vm::clearMessage) { Text("Dismiss") } }) { Text(msg) } }
    }
}

@Composable private fun LoansScreen(s: ShelfState, vm: ShelfViewModel, open: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) { Text("My loans", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)); if (s.offlineLoans) SavedNote(); if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth()); if (s.loans.isEmpty() && !s.loading) Empty("No current loans") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(s.loans, key = { it.loanId }) { loan -> LoanCard(loan, { vm.renew(loan) }, { open(loan.titleId) }) } } }
}

@Composable private fun LoanCard(loan: Loan, renew: () -> Unit, open: () -> Unit) { val days = daysLeft(loan.dueAt); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(loan.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text("Due ${pretty(loan.dueAt)}", modifier = Modifier.padding(top = 6.dp)); if (days < 0) Text("Overdue · ${-days} days", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) else Text("$days days left", color = MaterialTheme.colorScheme.primary); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = open) { Text("Details") }; Button(onClick = renew, enabled = loan.renewable) { Text("Renew") } } } } }

@Composable private fun HistoryScreen(s: ShelfState, vm: ShelfViewModel, open: (String) -> Unit, turnOff: () -> Unit) { Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) { Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Reading history", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Keep track of books you’ve borrowed", style = MaterialTheme.typography.bodyMedium) }; Switch(checked = s.member?.historyEnabled ?: false, onCheckedChange = { if (!it) turnOff() else vm.setHistory(true) }) }; if (s.offlineHistory) SavedNote(); Spacer(Modifier.height(12.dp)); if (s.history.isEmpty() && !s.loading) Empty("No reading history") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(s.history, key = { it.titleId + it.outAt }) { item -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { TextButton(onClick = { open(item.titleId) }, contentPadding = PaddingValues(0.dp)) { Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }; Text("Borrowed ${pretty(item.outAt)}"); Text("Returned ${pretty(item.returnedAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } } }

@Composable private fun DetailScreen(d: TitleDetail) { LazyColumn(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { item { AsyncImage(model = d.coverUrl, contentDescription = "Cover of ${d.title}", modifier = Modifier.size(180.dp, 250.dp)); Text(d.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp)); Text(d.authors.joinToString(), modifier = Modifier.padding(top = 6.dp)); Text("${d.copiesAvailable} of ${d.copiesTotal} copies available", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 14.dp)); if (d.subjects.isNotEmpty()) Text("Subjects: ${d.subjects.joinToString()}", modifier = Modifier.padding(top = 16.dp)) } } }
@Composable private fun SavedNote() { Text("Showing saved data", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 10.dp)) }
@Composable private fun Empty(text: String) { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
