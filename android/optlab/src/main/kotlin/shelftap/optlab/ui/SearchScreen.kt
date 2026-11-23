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
import shelftap.optlab.corpus.p13.FIRST_PAGE
import shelftap.optlab.corpus.p13.PAGING_SEARCH_SQL
import shelftap.optlab.corpus.p13.corpusResults

/**
 * P13: offline search over 100k books, one query per keystroke.
 * Both variants answer the same question: every query token must be a prefix of a word of title or author
 * (lower-cased, words = runs of letters/digits), results in id order. EquivScreen checks the result sets are equal.
 * A re-tokenises both columns of all 100k books inside composition (main thread), no debounce, returns every match.
 * B queries an FTS4 index (token-prefix MATCH) on Dispatchers.IO behind a 150 ms debounce and returns the first 50.
 * Blank or token-less query shows nothing in both, so the initial state costs nothing.
 */
@Composable
fun SearchScreen(variantA: Boolean) = if (variantA) SearchA() else SearchB()

/** P13 with A taken from corpus code (claude-haiku-B1-detailed-functional-r1): see corpus/p13/TitleSearch.kt. B is the same FTS4 screen. */
@Composable
fun SearchCorpusScreen(variantA: Boolean) = if (variantA) SearchC() else SearchB(corpusPair = true)

@Composable
private fun SearchC() {
    val ctx = LocalContext.current
    val db by produceState<BookDb?>(null) { value = withContext(Dispatchers.IO) { BookDb.openWithTitles(ctx) } }
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<Book>()) }
    FullyDrawn(db != null)
    LaunchedEffect(db) {
        val d = db ?: return@LaunchedEffect
        corpusResults(snapshotFlow { q }) { pattern ->
            Probe.section("search.query") {
                Probe.count("search.queries")
                d.rows(PAGING_SEARCH_SQL + " LIMIT " + FIRST_PAGE, pattern)
            }
        }.collect { results = it }
    }
    SearchLayout(q, { q = it }, status = if (db == null) "loading" else null) { results }
}

@Composable
private fun SearchA() {
    val all by produceState<List<Book>?>(null) { value = withContext(Dispatchers.Default) { BookGenerator.all() } }
    var q by remember { mutableStateOf("") }
    val books = all
    FullyDrawn(books != null)
    SearchLayout(q, { q = it }, status = if (books == null) "loading" else null) { 
        val results = if (books == null || q.isBlank()) emptyList() else Probe.section("search.query") {
            Probe.count("search.queries")
            naiveSearch(books, q)
        }
        results
    }
}

private val WORD = Regex("[\\p{L}\\p{N}]+")

/** The slop way: tokenise every row on every call. Same match rule as BookDb.search. */
internal fun naiveSearch(all: List<Book>, query: String): List<Book> {
    val tokens = WORD.findAll(query).map { it.value.lowercase() }.toList()
    if (tokens.isEmpty()) return emptyList()
    return all.filter { b ->
        val words = (WORD.findAll(b.title) + WORD.findAll(b.author)).map { it.value.lowercase() }.toList()
        tokens.all { t -> words.any { it.startsWith(t) } }
    }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Composable
private fun SearchB(corpusPair: Boolean = false) {  // corpusPair: B of P13c matches the corpus A's page size (120), order (title NOCASE, id) and debounce (200 ms)
    val ctx = LocalContext.current
    val db by produceState<BookDb?>(null) { value = withContext(Dispatchers.IO) { BookDb.open(ctx) } }
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<Book>()) }
    FullyDrawn(db != null)
    LaunchedEffect(db) {
        val d = db ?: return@LaunchedEffect
        snapshotFlow { q }.debounce(if (corpusPair) 200 else 150).distinctUntilChanged().mapLatest { query ->
            withContext(Dispatchers.IO) {
                if (query.isBlank()) emptyList() else Probe.section("search.query") {
                    Probe.count("search.queries")
                    if (corpusPair) d.search(query, FIRST_PAGE, titleOrder = true) else d.search(query)
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
