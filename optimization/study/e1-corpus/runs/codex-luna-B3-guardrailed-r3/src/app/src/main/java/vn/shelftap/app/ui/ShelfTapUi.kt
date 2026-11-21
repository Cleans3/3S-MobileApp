package vn.shelftap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import vn.shelftap.app.R
import vn.shelftap.app.data.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())
private fun date(value: String) = runCatching { dateFormatter.format(Instant.parse(value)) }.getOrDefault(value)
private fun days(value: String): Long = runCatching { ChronoUnit.DAYS.between(Instant.now(), Instant.parse(value)) }.getOrDefault(0)

@Composable fun ShelfTapApp(repo: ShelfTapRepository) {
    val nav = rememberNavController(); val vm: ShelfTapViewModel = viewModel(factory = ShelfTapViewModelFactory(repo))
    NavHost(nav, startDestination = "loans") { composable("loans") { LoansScreen(vm, { vm.loadTitle(it); nav.navigate("detail") }, { nav.navigate("history") }) }; composable("history") { HistoryScreen(vm, { vm.loadTitle(it); nav.navigate("detail") }, { nav.navigate("loans") }) }; composable("detail") { DetailScreen(vm) { nav.popBackStack() } } }
}

@Composable private fun Shell(title: String, selected: Int, onLoans: () -> Unit, onHistory: () -> Unit, body: @Composable ColumnScope.() -> Unit) { Scaffold(bottomBar = { NavigationBar { NavigationBarItem(selected == 0, onLoans, label = { Text("Loans") }, icon = { Text("L") }); NavigationBarItem(selected == 1, onHistory, label = { Text("History") }, icon = { Text("H") }) } }) { p -> Column(Modifier.padding(p).padding(horizontal = 16.dp)) { Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 20.dp)); body() } } }
@Composable private fun OfflineNote() { Text("Showing saved data", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp)) }

@Composable private fun LoansScreen(vm: ShelfTapViewModel, open: (String) -> Unit, history: () -> Unit) { val state by vm.loans.collectAsState(); Shell("My loans", 0, {}, history) { if (state.offline) OfflineNote(); state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(state.items, key = { it.loanId }, contentType = { "loan" }) { loan -> val left = days(loan.dueAt); Card(Modifier.fillMaxWidth().clickable { open(loan.titleId) }) { Column(Modifier.padding(16.dp)) { Text(loan.title, style = MaterialTheme.typography.titleLarge); Text("Due ${date(loan.dueAt)} · ${if (left < 0) "${-left} days overdue" else "$left days left"}"); if (left < 0) Text("Overdue", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge); Button(onClick = { vm.renew(loan.loanId) }, enabled = loan.renewable, modifier = Modifier.padding(top = 8.dp).sizeIn(minHeight = 48.dp)) { Text(stringResource(R.string.renew)) } } } } } } }

@Composable private fun HistoryScreen(vm: ShelfTapViewModel, open: (String) -> Unit, loans: () -> Unit) { val state by vm.history.collectAsState(); val member by vm.member.collectAsState(); var confirm by remember { mutableStateOf(false) }; Shell("Reading history", 1, loans, { }) { if (state.offline) OfflineNote(); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.keep_history)); Switch(checked = member?.historyEnabled == true, onCheckedChange = { if (!it) confirm = true else vm.setHistory(true) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) }; LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) { items(state.items, key = { "${it.titleId}-${it.outAt}" }, contentType = { "history" }) { item -> Card(Modifier.fillMaxWidth().clickable { open(item.titleId) }) { Column(Modifier.padding(16.dp)) { Text(item.title, style = MaterialTheme.typography.titleMedium); Text("Borrowed ${date(item.outAt)} · Returned ${date(item.returnedAt)}") } } } } }; if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.confirm_turn_off_title)) }, text = { Text(stringResource(R.string.confirm_turn_off_message)) }, confirmButton = { TextButton(onClick = { confirm = false; vm.setHistory(false) }) { Text(stringResource(R.string.turn_off)) } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } }) } }

@Composable private fun DetailScreen(vm: ShelfTapViewModel, back: () -> Unit) { val detail by vm.detail.collectAsState(); Column(Modifier.fillMaxSize().padding(16.dp)) { TextButton(onClick = back, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("‹ ${stringResource(R.string.back)}") }; detail?.let { d -> AsyncImage(model = d.coverUrl, contentDescription = stringResource(R.string.book_cover), modifier = Modifier.fillMaxWidth().height(220.dp)); Text(d.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp)); Text(d.authors.joinToString(), style = MaterialTheme.typography.bodyLarge); Text(d.subjects.joinToString(" · "), modifier = Modifier.padding(top = 8.dp)); Text("${d.copiesAvailable} of ${d.copiesTotal} copies available", modifier = Modifier.padding(top = 16.dp)) } ?: CircularProgressIndicator() } }
