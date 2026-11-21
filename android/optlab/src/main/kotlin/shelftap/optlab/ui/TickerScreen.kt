package shelftap.optlab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import shelftap.optlab.Probe
import shelftap.optlab.RecompositionProbe
import shelftap.optlab.data.Book
import shelftap.optlab.data.BookGenerator

/** Row model with a mutable member, the way generated code often shapes UI state: Compose cannot prove it stable. */
private class RowUi(val title: String, val author: String, val tags: MutableList<String>)

/**
 * P03 + P04: a catalog under a ticking "synced" label (10 Hz).
 * A reads the tick at the top of the screen, sorts and maps the whole list in composition, and passes fresh unstable row models.
 * B isolates the tick in its own composable, sorts once under remember, and passes an @Immutable Book keyed by id.
 */
@Composable
fun TickerScreen(variantA: Boolean, n: Int) {
    val books = remember { BookGenerator.all(n) }
    FullyDrawn()
    if (variantA) TickerA(books) else TickerB(books)
}

@Composable
private fun TickerA(books: List<Book>) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(100); tick++; Probe.count("ticks") } }
    Column(Modifier.fillMaxSize()) {
        Text("Synced $tick ticks ago", Modifier.padding(16.dp))
        val rows = Probe.section("sort") { books.sortedBy { it.title.lowercase() }.map { RowUi(it.title, it.author, mutableListOf(it.year.toString())) } }
        LazyColumn(Modifier.fillMaxSize()) { itemsIndexed(rows) { _, r -> TickRowA(r) } }
    }
}

@Composable
private fun TickRowA(r: RowUi) {
    RecompositionProbe("rc.row")
    // Formatting in the body: runs again on every recomposition.
    val label = "%s · %s".format(r.author.uppercase(), r.tags.joinToString())
    Row(Modifier.fillMaxWidth().padding(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TickerB(books: List<Book>) {
    val sorted = remember(books) { Probe.section("sort") { books.sortedBy { it.title.lowercase() } } }
    Column(Modifier.fillMaxSize()) {
        SyncBadge()
        LazyColumn(Modifier.fillMaxSize()) { items(sorted, key = { it.id }, contentType = { "row" }) { TickRowB(it) } }
    }
}

@Composable
private fun SyncBadge() {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(100); tick++; Probe.count("ticks") } }
    Text("Synced $tick ticks ago", Modifier.padding(16.dp))
}

@Composable
private fun TickRowB(b: Book) {
    RecompositionProbe("rc.row")
    val label = remember(b.id) { "%s · %d".format(b.author.uppercase(), b.year) }
    Row(Modifier.fillMaxWidth().padding(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(b.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

