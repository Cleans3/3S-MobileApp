package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import vn.shelftap.app.data.TitleDto
import vn.shelftap.app.ui.ShelfTapViewModel
import vn.shelftap.app.ui.ShelfTapViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: ShelfTapViewModel = viewModel(factory = ShelfTapViewModelFactory((application as ShelfTapApplication).repository))
            ShelfTapTheme { ShelfTapApp(vm) }
        }
    }
}

@Composable
private fun ShelfTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
private fun ShelfTapApp(vm: ShelfTapViewModel) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = if (selectedId != null) vm.title(selectedId!!).collectAsStateWithLifecycle().value else null
    if (selectedId == null) CatalogueScreen(vm) { selectedId = it }
    else DetailScreen(selected, onBack = { selectedId = null })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogueScreen(vm: ShelfTapViewModel, open: (String) -> Unit) {
    val search by vm.search.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val items = vm.titles.collectAsLazyPagingItems()
    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text("ShelfTap", fontWeight = FontWeight.Bold)
                Text("Library catalogue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, actions = { if (syncing) CircularProgressIndicator(Modifier.padding(end = 16.dp).size(22.dp), strokeWidth = 2.dp) })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(value = search, onValueChange = vm::setSearch, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), placeholder = { Text("Search titles, authors, ISBN or subjects") }, leadingIcon = { Icon(Icons.Default.Search, "Search") }, singleLine = true, shape = RoundedCornerShape(14.dp))
            if (items.itemCount == 0 && items.loadState.refresh is androidx.paging.LoadState.NotLoading && search.isNotBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No books found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(items.itemCount, key = { index -> items[index]?.id ?: index }) { index -> items[index]?.toTitle()?.let { TitleRow(it, open) } }
                    if (items.loadState.append is androidx.paging.LoadState.Loading) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
                }
            }
        }
    }
}

@Composable
private fun TitleRow(book: TitleDto, open: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { open(book.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(book.coverUrl, Modifier.size(width = 64.dp, height = 94.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(book.authors.ifEmpty { listOf("Unknown author") }.joinToString(", "), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(book.year.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Availability(book.copiesAvailable > 0)
        }
    }
}

@Composable
private fun Availability(available: Boolean) {
    BadgedBox(badge = { Badge(containerColor = if (available) Color(0xFF2E7D5B) else MaterialTheme.colorScheme.error) }) { Spacer(Modifier.size(1.dp)) }
    Text(if (available) "Available" else "All copies out", style = MaterialTheme.typography.labelMedium, color = if (available) Color(0xFF246749) else MaterialTheme.colorScheme.error)
}

@Composable
private fun Cover(url: String?, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) Box(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text("No cover", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    else AsyncImage(model = url, contentDescription = "Book cover", modifier = modifier.clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(book: TitleDto?, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(book?.title ?: "Title") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        if (book == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Spacer(Modifier.height(12.dp)); Cover(book.coverUrl, Modifier.size(width = 190.dp, height = 285.dp)); Spacer(Modifier.height(20.dp))
                Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                Info("Authors", book.authors.ifEmpty { listOf("Unknown author") }.joinToString(", ")); Info("Year", book.year.toString()); Info("ISBN", book.isbn); Info("Copies available", book.copiesAvailable.toString())
                Text("Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    book.subjects.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { AssistChip(onClick = {}, label = { Text(it) }) } }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun Info(label: String, value: String) { Column(Modifier.fillMaxWidth().padding(top = 10.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyLarge) } }
