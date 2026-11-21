package vn.shelftap.app.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ShelfTapRepository
import vn.shelftap.app.model.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val Navy = Color(0xFF28345C)

@Composable fun ShelfTapTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Color(0xFF536AA5), surface = Color(0xFFF9F9FF)), content = content) }

@Composable fun ShelfTapApp(repo: ShelfTapRepository) {
    val nav = rememberNavController()
    val entries = listOf("loans" to "Loans", "history" to "History")
    Scaffold(bottomBar = { NavigationBar { entries.forEach { (route, label) -> NavigationBarItem(selected = nav.currentBackStackEntryAsState().value?.destination?.route == route, onClick = { nav.navigate(route) { launchSingleTop = true; popUpTo("loans") } }, icon = { Icon(if (route == "loans") Icons.Default.MenuBook else Icons.Default.History, null) }, label = { Text(label) }) } } }) { padding ->
        NavHost(nav, startDestination = "loans", Modifier.padding(padding)) {
            composable("loans") { LoansScreen(repo, { nav.navigate("detail/${it}") }) }
            composable("history") { HistoryScreen(repo, { nav.navigate("detail/${it}") }) }
            composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { BookDetailScreen(repo, it.arguments?.getString("id").orEmpty(), { nav.popBackStack() }) }
        }
    }
}

@Composable private fun LoansScreen(repo: ShelfTapRepository, open: (String) -> Unit) {
    val vm: LoansViewModel = viewModel(factory = LoansViewModel.Factory(repo)); val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("My loans", fontWeight = FontWeight.Bold) }) }) { p ->
        Column(Modifier.padding(p).fillMaxSize()) {
            if (state.saved) SavedNote()
            state.renewMessage?.let { Text(it, Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer); LaunchedEffect(it) { kotlinx.coroutines.delay(3500); vm.clearMessage() } }
            when { state.loading -> Loading(); state.error != null -> EmptyState(state.error!!); state.items.isEmpty() -> EmptyState("You have no current loans") else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(state.items, key = { it.loanId }) { loan -> LoanCard(loan, state.renewing == loan.loanId, { vm.renew(loan.loanId) }, { open(loan.titleId) }) } } }
        }
    }
}

@Composable private fun HistoryScreen(repo: ShelfTapRepository, open: (String) -> Unit) {
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(repo)); val state by vm.state.collectAsStateWithLifecycle(); var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Turn off reading history?") }, text = { Text("Your past loans will no longer be saved while history is off.") }, confirmButton = { TextButton(onClick = { confirm = false; vm.updatePrivacy(false) }) { Text("Turn off") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Reading history", fontWeight = FontWeight.Bold) }) }) { p ->
        Column(Modifier.padding(p).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text("Keep my reading history", Modifier.weight(1f), fontWeight = FontWeight.Medium); Switch(checked = state.historyEnabled, onCheckedChange = { if (!it) confirm = true else vm.updatePrivacy(true) }, enabled = !state.updating) }
            if (state.saved) SavedNote()
            when { state.loading -> Loading(); state.items.isEmpty() -> EmptyState(if (state.historyEnabled) "No reading history yet" else "Reading history is off"); else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(state.items) { HistoryRow(it) { open(it.titleId) } } } }
        }
    }
}

@Composable private fun LoanCard(loan: Loan, renewing: Boolean, renew: () -> Unit, open: () -> Unit) { val overdue = isOverdue(loan.dueAt); Card(Modifier.fillMaxWidth().clickable(onClick = open)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.Top) { Text(loan.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis); if (overdue) AssistChip(onClick = {}, label = { Text("Overdue") }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.errorContainer)) }; Spacer(Modifier.height(8.dp)); Text(if (overdue) "Due ${dateText(loan.dueAt)} · ${daysLate(loan.dueAt)} days overdue" else "Due ${dateText(loan.dueAt)} · ${daysLeft(loan.dueAt)} days left", color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(10.dp)); Button(onClick = renew, enabled = loan.renewable && !renewing) { if (renewing) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) } else Text(if (loan.renewable) "Renew" else "Renew unavailable") } } } }
@Composable private fun HistoryRow(item: HistoryItem, open: () -> Unit) { ListItem(headlineContent = { Text(item.title, fontWeight = FontWeight.Medium) }, supportingContent = { Text("Borrowed ${dateText(item.outAt)} · Returned ${item.returnedAt?.let(::dateText) ?: "—"}") }, modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = open)) }
@Composable private fun BookDetailScreen(repo: ShelfTapRepository, id: String, back: () -> Unit) { var detail by remember { mutableStateOf<TitleDetail?>(null) }; var loading by remember { mutableStateOf(true) }; LaunchedEffect(id) { try { detail = repo.title(id) } catch (_: Exception) { detail = null } finally { loading = false } }; Scaffold(topBar = { TopAppBar(title = { Text("Book details") }, navigationIcon = { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { p -> Column(Modifier.padding(p).padding(20.dp).fillMaxSize()) { if (loading) Loading() else detail?.let { BookContent(it) } ?: EmptyState("Book details unavailable") } } }
@Composable private fun BookContent(book: TitleDetail) { AsyncImage(model = book.coverUrl, contentDescription = "Cover of ${book.title}", modifier = Modifier.fillMaxWidth().height(240.dp).clip(MaterialTheme.shapes.large), contentScale = ContentScale.Fit); Spacer(Modifier.height(20.dp)); Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); if (book.authors.isNotEmpty()) Text(book.authors.joinToString(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp)); Spacer(Modifier.height(18.dp)); Text("Subjects", fontWeight = FontWeight.Bold); Text(if (book.subjects.isEmpty()) "—" else book.subjects.joinToString(" · "), modifier = Modifier.padding(top = 4.dp)); Spacer(Modifier.height(18.dp)); Text("Availability", fontWeight = FontWeight.Bold); Text("${book.copiesAvailable} of ${book.copiesTotal} copies available", modifier = Modifier.padding(top = 4.dp)) }
@Composable private fun SavedNote() { Text("Showing saved data", Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(10.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) }
@Composable private fun Loading() { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
@Composable private fun EmptyState(message: String) { Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

private fun instant(value: String) = runCatching { Instant.parse(value) }.getOrNull()
private fun isOverdue(value: String) = instant(value)?.isBefore(Instant.now()) == true
private fun daysLeft(value: String): Long = instant(value)?.let { ChronoUnit.DAYS.between(LocalDate.now(), it.atZone(ZoneId.systemDefault()).toLocalDate()).coerceAtLeast(0) } ?: 0
private fun daysLate(value: String): Long = instant(value)?.let { ChronoUnit.DAYS.between(it.atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()).coerceAtLeast(1) } ?: 1
private fun dateText(value: String): String = instant(value)?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: value
