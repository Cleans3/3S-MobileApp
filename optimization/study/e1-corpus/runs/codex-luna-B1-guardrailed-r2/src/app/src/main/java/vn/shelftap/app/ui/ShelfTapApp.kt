package vn.shelftap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import vn.shelftap.app.R
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.TitleEntity

@Composable
fun ShelfTapApp(repository: CatalogueRepository) {
    val vm: ShelfTapViewModel = viewModel(factory = ShelfTapViewModel.Factory(repository))
    val selected by vm.selectedId.collectAsStateWithLifecycle()
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (selected == null) CatalogueScreen(vm) else DetailScreen(vm, selected!!)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogueScreen(vm: ShelfTapViewModel) {
    val items = vm.titles.collectAsLazyPagingItems()
    val query by vm.queryFlow.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("ShelfTap catalogue") }, colors = TopAppBarDefaults.topAppBarColors()) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(value = query, onValueChange = vm::setQuery, label = { Text(stringResource(R.string.search_catalogue)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
            if (items.itemCount == 0 && items.loadState.refresh is androidx.paging.LoadState.NotLoading && query.isNotBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_books_found)) }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items.itemCount, key = { items[it]?.id ?: "placeholder-$it" }, contentType = { "title" }) { index ->
                        items[index]?.let { title ->
                            TitleRow(title, Modifier.fillMaxWidth()) { vm.open(title.id) }
                            if (index >= items.itemCount - 5) vm.loadNextPage()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleRow(title: TitleEntity, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(title, Modifier.size(width = 64.dp, height = 96.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(title.authors.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                Text(title.year.toString(), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Badge(containerColor = if (title.copiesAvailable > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer) {
                    Text(if (title.copiesAvailable > 0) stringResource(R.string.available) else stringResource(R.string.all_copies_out), Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailScreen(vm: ShelfTapViewModel, id: String) {
    val title by vm.observeTitle(id).collectAsState(initial = null)
    val backDescription = stringResource(R.string.back)
    LaunchedEffect(id) { vm.refreshDetail(id) }
    Scaffold(topBar = { TopAppBar(title = { Text(title?.title ?: "") }, navigationIcon = { IconButton(onClick = vm::closeDetail, modifier = Modifier.semantics { contentDescription = backDescription }) { Text("←") } }) }) { padding ->
        title?.let { book ->
            LazyColumn(contentPadding = PaddingValues(20.dp), modifier = Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item(key = "cover") { Cover(book, Modifier.fillMaxWidth().height(300.dp)) }
                item(key = "title") { Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                item(key = "authors") { Text(book.authors.joinToString(", "), style = MaterialTheme.typography.titleMedium) }
                item(key = "year") { Text("${stringResource(R.string.year)}: ${book.year}") }
                item(key = "isbn") { Text("${stringResource(R.string.isbn)}: ${book.isbn}") }
                item(key = "copies") { Text("${stringResource(R.string.copies_available)}: ${book.copiesAvailable}") }
                item(key = "subjects") {
                    Column { Text(stringResource(R.string.subjects), fontWeight = FontWeight.Bold); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { book.subjects.forEach { subject -> AssistChip(onClick = {}, label = { Text(subject) }) } } }
                }
            }
        }
    }
}

@Composable
private fun Cover(title: TitleEntity, modifier: Modifier) {
    if (title.coverUrl?.startsWith("https://") == true) AsyncImage(model = title.coverUrl, contentDescription = stringResource(R.string.cover_description, title.title), modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    else Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_cover), style = MaterialTheme.typography.labelSmall) }
}
