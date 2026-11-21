package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.shelftap.app.model.Title
import vn.shelftap.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ShelfTapTheme { ShelfTapApp() } } }
}

@Composable
private fun ShelfTapApp(vm: CatalogueViewModel = viewModel()) {
    var selected by remember { mutableStateOf<Title?>(null) }
    if (selected == null) CatalogueScreen(vm) { selected = it } else DetailScreen(selected!!) { selected = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogueScreen(vm: CatalogueViewModel, open: (Title) -> Unit) {
    val state by vm.state.collectAsState()
    Scaffold(topBar = {
        TopAppBar(title = { Text("ShelfTap", fontWeight = FontWeight.Bold) }, actions = {
            IconButton(onClick = { vm.refresh() }, enabled = !state.syncing) { Text("↻", style = MaterialTheme.typography.titleLarge) }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(value = state.query, onValueChange = vm::search, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true, label = { Text("Search catalogue") }, placeholder = { Text("Title, author, ISBN or subject") })
            if (state.syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.items.isEmpty() && !state.syncing) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No books found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) { items(state.items, key = { it.id }) { book -> BookRow(book, open) } }
        }
    }
}

@Composable
private fun BookRow(book: Title, open: (Title) -> Unit) {
    Card(onClick = { open(book) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BookImage(book.coverUrl, Modifier.size(64.dp, 96.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(book.authors.joinToString(", ").ifBlank { "Unknown author" }, style = MaterialTheme.typography.bodyMedium)
                Text(book.year.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AssistChip(onClick = {}, label = { Text(if (book.copiesAvailable > 0) "Available" else "All copies out") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(book: Title, back: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Book details") }, navigationIcon = { IconButton(onClick = back) { Text("‹", style = MaterialTheme.typography.headlineMedium) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BookImage(book.coverUrl, Modifier.size(180.dp, 270.dp))
            Spacer(Modifier.height(18.dp)); Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp)); Text(book.authors.joinToString(", ").ifBlank { "Unknown author" }, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp)); DetailLine("ID", book.id); DetailLine("Year", book.year.toString()); DetailLine("ISBN", book.isbn.ifBlank { "—" }); DetailLine("Copies", if (book.copiesAvailable > 0) "${book.copiesAvailable} available" else "All copies out")
            if (book.subjects.isNotEmpty()) { Spacer(Modifier.height(18.dp)); Text("Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); FlowChips(book.subjects) }
        }
    }
}

@Composable private fun DetailLine(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.Medium) } }

@Composable private fun FlowChips(values: List<String>) { Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { values.forEach { AssistChip(onClick = {}, label = { Text(it) }) } } }

@Composable private fun ShelfTapTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF315F72), secondary = androidx.compose.ui.graphics.Color(0xFF526B75), surface = androidx.compose.ui.graphics.Color(0xFFF7F9FA)), content = content) }
