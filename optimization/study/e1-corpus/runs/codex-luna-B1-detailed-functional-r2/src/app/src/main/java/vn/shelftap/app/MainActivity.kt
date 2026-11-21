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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import vn.shelftap.app.data.Title
import vn.shelftap.app.ui.CatalogueViewModel
import vn.shelftap.app.ui.CatalogueViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ShelfTapTheme { ShelfTapApp() } } }
    @Composable private fun ShelfTapTheme(content: @Composable () -> Unit) { MaterialTheme(content = content) }
    @Composable private fun ShelfTapApp() {
        val app = application as ShelfTapApplication
        val vm: CatalogueViewModel = viewModel(factory = CatalogueViewModelFactory(app.repository))
        ShelfTapRoot(vm)
    }
}

@Composable
private fun ShelfTapRoot(vm: CatalogueViewModel) {
    var tab by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    val query by vm.query.collectAsStateWithLifecycle()
    val titles by vm.titles.collectAsStateWithLifecycle()
    val detail = if (selected != null) vm.title(selected!!).collectAsStateWithLifecycle(initialValue = null).value else null
    if (selected != null) {
        TitleDetail(detail, onBack = { selected = null })
        LaunchedEffect(selected, detail == null) { if (detail == null) vm.fetchMissing(selected!!) }
        return
    }
    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(tab == 0, { tab = 0; vm.query.value = "" }, icon = { Icon(Icons.Filled.MenuBook, null) }, label = { Text("Catalogue") })
            NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Filled.Search, null) }, label = { Text("Search") })
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("ShelfTap", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 6.dp))
            if (tab == 1) OutlinedTextField(query, { vm.query.value = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Search title, author, ISBN or subject") }, singleLine = true)
            if (titles.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No books found") }
            else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(titles, key = { it.id }) { BookRow(it) { selected = it.id } } }
        }
    }
}

@Composable
private fun BookRow(book: Title, open: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = open)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(book.coverUrl, Modifier.size(62.dp, 92.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(book.authors.joinToString(", "), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Text(book.year.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Availability(book.copiesAvailable > 0)
            }
        }
    }
}

@Composable private fun Availability(available: Boolean) { Surface(color = if (available) Color(0xFFD9F2DE) else Color(0xFFFFE1E0), shape = RoundedCornerShape(50)) { Text(if (available) "Available" else "All copies out", color = if (available) Color(0xFF176B2A) else Color(0xFF9C2626), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TitleDetail(book: Title?, onBack: () -> Unit) {
    Scaffold(topBar = { Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }; Text(book?.title ?: "Loading…", style = MaterialTheme.typography.titleLarge, maxLines = 1) } }) { padding ->
        if (book == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            item { Cover(book.coverUrl, Modifier.fillMaxWidth().height(300.dp)); Spacer(Modifier.height(18.dp)); Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(book.authors.joinToString(", "), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 5.dp)); Text("${book.year}  •  ISBN ${book.isbn}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)); Spacer(Modifier.height(14.dp)); Availability(book.copiesAvailable > 0); Spacer(Modifier.height(20.dp)); Text("Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) { book.subjects.forEach { AssistChip(onClick = {}, label = { Text(it) }) } }; Spacer(Modifier.height(30.dp)) }
        }
    }
}

@Composable private fun Cover(url: String?, modifier: Modifier) { Box(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { if (url.isNullOrBlank()) Icon(Icons.Filled.MenuBook, "No cover", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp)) else AsyncImage(model = url, contentDescription = "Book cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
