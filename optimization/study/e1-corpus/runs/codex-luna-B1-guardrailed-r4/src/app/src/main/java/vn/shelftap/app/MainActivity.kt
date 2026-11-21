package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.minimumInteractiveComponentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import vn.shelftap.app.data.Title

class MainActivity : ComponentActivity() {
    private val model by viewModels<CatalogueViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShelfTapTheme { ShelfTapApp(model) } }
    }
}

@Composable
private fun ShelfTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
private fun ShelfTapApp(model: CatalogueViewModel) {
    val detail by model.selected.collectAsStateWithLifecycle()
    if (detail == null) CatalogueScreen(model) else DetailScreen(detail!!, model::closeDetail)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogueScreen(model: CatalogueViewModel) {
    val titles by model.titles.collectAsStateWithLifecycle()
    val query by model.search.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("ShelfTap") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(value = query, onValueChange = model::setSearch, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), singleLine = true, label = { Text(stringResource(R.string.search_catalogue)) })
            if (titles.isEmpty()) {
                Text(stringResource(R.string.no_books_found), modifier = Modifier.padding(top = 32.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
                    items(titles, key = { it.id }, contentType = { "title" }) { title -> TitleRow(title) { model.open(title.id) } }
                }
            }
        }
    }
}

@Composable
private fun TitleRow(title: Title, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(title, Modifier.size(width = 64.dp, height = 96.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(title.authors.joinToString(", "), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text(title.year.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(5.dp))
                Badge(containerColor = if (title.copiesAvailable > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error) { Text(if (title.copiesAvailable > 0) stringResource(R.string.available) else stringResource(R.string.all_copies_out), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White) }
            }
        }
    }
}

@Composable
private fun Cover(title: Title, modifier: Modifier) {
    if (title.coverUrl != null) AsyncImage(model = title.coverUrl, contentDescription = stringResource(R.string.cover_description, title.title), modifier = modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
    else Surface(modifier = modifier.clip(RoundedCornerShape(6.dp)), color = MaterialTheme.colorScheme.surfaceVariant) { Text("No cover", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(title: Title, onBack: () -> Unit) {
    val backDescription = stringResource(R.string.back)
    Scaffold(topBar = { TopAppBar(title = { Text(title.title) }, navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.minimumInteractiveComponentSize().semantics { contentDescription = backDescription }) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
            item(contentType = "cover") { Cover(title, Modifier.size(width = 200.dp, height = 300.dp)); Spacer(Modifier.height(18.dp)) }
            item(contentType = "title") { Text(title.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)) }
            item(contentType = "metadata") { DetailText(stringResource(R.string.authors), title.authors.joinToString(", ")); DetailText(stringResource(R.string.year), title.year.toString()); DetailText(stringResource(R.string.isbn), title.isbn); DetailText(stringResource(R.string.copies_available), title.copiesAvailable.toString()); HorizontalDivider(Modifier.padding(vertical = 12.dp)) }
            item(contentType = "subjects") {
                Text(stringResource(R.string.subjects), style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                LazyRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(title.subjects, key = { index, subject -> "$index:$subject" }, contentType = { _, _ -> "subject" }) { _, subject -> AssistChip(onClick = {}, label = { Text(subject) }) }
                }
            }
        }
    }
}

@Composable
private fun DetailText(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("$label: ", fontWeight = FontWeight.SemiBold); Text(value) } }
