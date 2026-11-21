package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() { override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { ShelfTapApp() } } }

@Composable fun ShelfTapApp(vm: CatalogueViewModel = viewModel()) { MaterialTheme { val chosen by vm.selected; if (chosen == null) CatalogueScreen(vm) else DetailScreen(chosen!!, vm::close) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun CatalogueScreen(vm: CatalogueViewModel) {
    val items by vm.items; val q by vm.query; val syncing by vm.syncing
    Scaffold(topBar = { TopAppBar(title = { Text("ShelfTap") }, actions = { if (syncing) CircularProgressIndicator(Modifier.size(22.dp).padding(end = 8.dp), strokeWidth = 2.dp) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(value = q, onValueChange = { vm.query.value = it; vm.searchLocal() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), label = { Text("Search catalogue") }, singleLine = true)
            if (items.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No books found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) { items(items, key = { it.id }) { BookRow(it) { vm.open(it.id) } } }
        }
    }
}

@Composable private fun BookRow(book: Title, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
    Cover(book.coverUrl, Modifier.size(64.dp, 96.dp)); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(book.title, fontWeight = FontWeight.SemiBold); Text(book.authors.joinToString(", ").ifBlank { "Unknown author" }, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(book.year.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(5.dp)); Availability(book.copiesAvailable > 0) }
} }

@Composable private fun DetailScreen(book: Title, back: () -> Unit) { Scaffold(topBar = { TopAppBar(title = { Text("Title details") }, navigationIcon = { TextButton(onClick = back) { Text("‹ Back") } }) }) { pad -> Column(Modifier.padding(pad).fillMaxSize().padding(horizontal = 20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
    Cover(book.coverUrl, Modifier.fillMaxWidth().height(320.dp)); Spacer(Modifier.height(18.dp)); Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(book.authors.joinToString(", ").ifBlank { "Unknown author" }, style = MaterialTheme.typography.titleMedium); Text("Published ${book.year}", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(10.dp)); Availability(book.copiesAvailable > 0); Field("ISBN", book.isbn); Field("ID", book.id); Text("Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { book.subjects.forEach { AssistChip(onClick = {}, label = { Text(it) }) } }; Spacer(Modifier.height(24.dp))
} } }

@Composable private fun Field(label: String, value: String) { if (value.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Text(value) } }
@Composable private fun Availability(available: Boolean) { Surface(color = if (available) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(50)) { Text(if (available) "Available" else "All copies out", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = if (available) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelMedium) } }
@Composable private fun Cover(url: String?, modifier: Modifier) { if (url == null) Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No cover", color = MaterialTheme.colorScheme.onSurfaceVariant) } } else AsyncImage(model = url, contentDescription = "Book cover", modifier = modifier, contentScale = ContentScale.Crop) }
