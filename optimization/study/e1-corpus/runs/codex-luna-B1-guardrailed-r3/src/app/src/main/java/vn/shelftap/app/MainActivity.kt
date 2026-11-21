package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent {
        val db = CatalogueDb(this); val vm: ShelfTapViewModel = viewModel(factory = ShelfTapViewModelFactory(CatalogueRepository(db, TitleApi())))
        ShelfTapTheme { ShelfTapApp(vm) }
    } }
}

@Composable private fun ShelfTapTheme(content: @Composable () -> Unit) = MaterialTheme(content = content)

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ShelfTapApp(vm: ShelfTapViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.catalogue)) }) }) { padding ->
        if (state.selected != null) DetailScreen(state.selected!!, vm::close, Modifier.padding(padding)) else CatalogueScreen(state, vm::setQuery, vm::open, Modifier.padding(padding))
    }
}

@Composable private fun CatalogueScreen(state: CatalogueState, onQuery: (String) -> Unit, onOpen: (Title) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(state.query, onQuery, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text(stringResource(R.string.search_catalogue)) }, singleLine = true)
        if (state.loading && state.titles.isEmpty()) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
        else if (state.titles.isEmpty()) Text(stringResource(R.string.no_books_found), Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.titles, key = { it.id }, contentType = { "title" }) { TitleRow(it, onOpen) } }
    }
}

@Composable private fun TitleRow(title: Title, onOpen: (Title) -> Unit) {
    Card(onClick = { onOpen(title) }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AsyncImage(model = title.coverUrl, contentDescription = stringResource(R.string.cover_description), modifier = Modifier.size(width = 64.dp, height = 96.dp))
        Column(Modifier.weight(1f)) { Text(title.title, fontWeight = FontWeight.Bold); Text(title.authors.joinToString(), style = MaterialTheme.typography.bodyMedium); Text(title.year.toString()); Spacer(Modifier.height(4.dp)); Badge { Text(if (title.copiesAvailable > 0) stringResource(R.string.available) else stringResource(R.string.out_of_copies)) } }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun DetailScreen(title: Title, onBack: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack, modifier = Modifier.size(48.dp).semantics { contentDescription = stringResource(R.string.back) }) { Text("‹", style = MaterialTheme.typography.headlineLarge) }; Text(title.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(contentType = "cover") { AsyncImage(title.coverUrl, stringResource(R.string.cover_description), Modifier.fillMaxWidth().height(360.dp)) }
            item(contentType = "metadata") { Column { Text(title.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(title.authors.joinToString()); Text("${stringResource(R.string.year)}: ${title.year}"); Text("${stringResource(R.string.isbn)}: ${title.isbn}"); Text("${stringResource(R.string.copies)}: ${title.copiesAvailable}") } }
            item(contentType = "subjects") { Column { Text(stringResource(R.string.subjects), fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { title.subjects.forEach { AssistChip(onClick = {}, label = { Text(it) }) } } } }
        }
    }
}
