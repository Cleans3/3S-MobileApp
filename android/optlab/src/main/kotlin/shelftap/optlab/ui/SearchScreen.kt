package shelftap.optlab.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import shelftap.optlab.Probe
import shelftap.optlab.data.Book
import shelftap.optlab.data.BookDb
import shelftap.optlab.data.BookGenerator

/**
 * P13: offline search over 100k books, one query per keystroke.
 * A filters the whole in-memory list inside composition (main thread), no debounce.
 * B queries an FTS5 index on Dispatchers.IO behind a 150 ms debounce.
 * Blank query shows nothing in both, so the initial state costs nothing.
 */
@Composable
fun SearchScreen(variantA: Boolean) = if (variantA) SearchA() else SearchB()

@Composable
private fun SearchA() {
    val all by produceState<List<Book>?>(null) { value = withContext(Dispatchers.Default) { BookGenerator.all() } }
    var q by remember { mutableStateOf("") }
    val books = all
    FullyDrawn(books != null)
    SearchLayout(q, { q = it }, status = if (books == null) "loading" else null) { 
        val results = if (books == null || q.isBlank()) emptyList() else Probe.section("search.query") {
            Probe.count("search.queries")
            books.filter { it.title.contains(q, ignoreCase = true) || it.author.contains(q, ignoreCase = true) }
        }
        results
    }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Composable
private fun SearchB() {
    val ctx = LocalContext.current
    val db by produceState<BookDb?>(null) { value = withContext(Dispatchers.IO) { BookDb.open(ctx) } }
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<Book>()) }
    FullyDrawn(db != null)
    LaunchedEffect(db) {
        val d = db ?: return@LaunchedEffect
        snapshotFlow { q }.debounce(150).distinctUntilChanged().mapLatest { query ->
            withContext(Dispatchers.IO) {
                if (query.isBlank()) emptyList() else Probe.section("search.query") {
                    Probe.count("search.queries")
                    d.search(query)
                }
            }
        }.collect { results = it }
    }
    SearchLayout(q, { q = it }, status = if (db == null) "loading" else null) { results }
}

/** Shared chrome. The status line starts with "ready" once data is available; the benchmark waits for it. */
@Composable
private fun SearchLayout(q: String, onQuery: (String) -> Unit, status: String?, results: @Composable () -> List<Book>) {
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(q, onQuery, label = { Text("Search books") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        val list = results()
        Text(status ?: "ready · ${list.size} results")
        LazyColumn(Modifier.fillMaxSize()) { items(list, key = { it.id }) { BookRowPlain(it) } }
    }
}
