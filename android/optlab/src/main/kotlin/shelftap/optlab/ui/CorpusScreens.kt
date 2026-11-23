package shelftap.optlab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.decode.DataSource
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.delay
import shelftap.optlab.Probe
import shelftap.optlab.RecompositionProbe
import shelftap.optlab.corpus.p01.HistoryItem
import shelftap.optlab.corpus.p01.HistoryListA
import shelftap.optlab.corpus.p01.HistoryListB
import shelftap.optlab.corpus.p06.CoverBitmap
import shelftap.optlab.corpus.p08.BookCard
import shelftap.optlab.corpus.p08.BookCardFlat
import shelftap.optlab.corpus.p09.BookDetailSheet
import shelftap.optlab.corpus.p09.BookDetailSheetLazy
import shelftap.optlab.corpus.p09.BookDetailSheetStable
import shelftap.optlab.corpus.p09.LoanList
import shelftap.optlab.data.Book
import shelftap.optlab.data.BookGenerator
import shelftap.optlab.data.Covers
import java.io.File
import java.time.LocalDate
import shelftap.optlab.corpus.p08.Book as CoverBook
import shelftap.optlab.corpus.p09.Book as SheetBook
import shelftap.optlab.corpus.p09.Loan

// The five factors whose A variant is corpus code (see e2-bench/ports/README.md): A = the ported file, B = the tuned counterpart.
// The old hand-written A screens (EffectsScreen etc.) stay as the author-constructed pilot variants.

/** P08: the catalogue grid of the corpus app (LazyVerticalGrid, 150 dp adaptive, as in its CatalogueScreen). A = the corpus BookCard with BookCover (5 dp shadow, gradient overlay, canvas drawing), B = the same card with a flat cover box. */
@Composable
fun P08CorpusScreen(variantA: Boolean, n: Int) {
    val books = remember { BookGenerator.all(n).map { coverBookOf(it) } }
    LaunchedEffect(Unit) { Probe.gauge("fx.perItem", if (variantA) 2L else 0L) }  // shadow(elevation > 2 dp) and a gradient brush per item (the two detector findings)
    FullyDrawn()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(books, key = { it.id }, contentType = { "book" }) { b ->
            RecompositionProbe("rc.row")
            if (variantA) BookCard(b, onClick = {}) else BookCardFlat(b, onClick = {})
        }
    }
}

/** Binds a seeded book row to the corpus model class (data-source binding of the port: the corpus gets Book from its Room database). */
private fun coverBookOf(b: Book): CoverBook = CoverBook(
    b.id.toLong(), b.title, b.author, b.isbn, listOf("Philosophy", "Biography", "Classic Literature", "Science & Tech", "Arts & Architecture")[b.id % 5],
    b.year, 300, "", "", b.id % 7 != 0, b.coverIdx.toLong(), null,
)

private val CorpusCoverSlot = Modifier.size(150.dp, 210.dp)  // the slot the generated DetailScreen gives its cover

/** P06: covers from the 1500x2250 files. A is the corpus fragment (decodeStream, no sampling); B lets Coil size the decode to the same 150x210 dp slot. */
@Composable
fun P06CorpusScreen(variantA: Boolean, n: Int) {
    val ctx = LocalContext.current
    val books = remember { BookGenerator.all(n) }
    FullyDrawn()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(books, key = { it.id }, contentType = { "book" }) { book ->
            val path = Covers.file(ctx, book.coverIdx)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (variantA) CoverBitmap("file://" + path.path) else CoverCoil(path)
                Spacer(Modifier.width(12.dp))
                RecompositionProbe("rc.row")
                Column {
                    Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text("${book.author} · ${book.year}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CoverCoil(path: File) {
    val ctx = LocalContext.current
    val request = remember(path) {
        ImageRequest.Builder(ctx)
            .data(path)
            .listener(onSuccess = { _, result: SuccessResult ->
                if (result.dataSource != DataSource.MEMORY_CACHE) {
                    Probe.count("decodedBytes", result.image.size)
                    Probe.count("decodes")
                }
            })
            .build()
    }
    AsyncImage(request, contentDescription = null, modifier = CorpusCoverSlot, contentScale = ContentScale.Crop)
}

/** P01: A is the corpus HistoryScreen list (items without key), B the same list and card with key and contentType. */
@Composable
fun P01CorpusScreen(variantA: Boolean, n: Int) {
    val items = remember {
        List(n) { i ->
            val b = BookGenerator.book(i)
            val out = "2025-%02d-%02dT10:00:00Z".format(1 + i % 12, 1 + i % 28)
            HistoryItem("t${b.id}", b.title, out, if (i % 3 == 0) null else "2025-%02d-%02dT09:30:00Z".format(1 + i % 12, 1 + i % 28))
        }
    }
    FullyDrawn()
    if (variantA) HistoryListA(items) {} else HistoryListB(items) {}
}

private fun sheetLoans(n: Int, today: LocalDate = LocalDate.of(2025, 6, 1)): List<Loan> = List(n) { i ->
    val out = today.minusDays(400L - i % 300)
    Loan("L$i", "B1", out, out.plusDays(21), returnDate = out.plusDays(14), userRating = 1 + i % 5, userNotes = if (i % 4 == 0) "note $i" else null)
}

private val SheetBookSample = SheetBook("B1", "The Silent River", "Anh Nguyen", "9780000000001", "Lab Press", 2020, "Fiction", 320, "A long description. ".repeat(20))

/** P09: A is the corpus BookDetailSheet (Column + verticalScroll + forEach over n past loans), B the same content with the history in a LazyColumn. */
@Composable
fun P09CorpusScreen(variantA: Boolean, n: Int) {
    val loans = remember { sheetLoans(n) }
    FullyDrawn()
    if (variantA) BookDetailSheet(SheetBookSample, loans, {}, {}) else BookDetailSheetLazy(SheetBookSample, loans, {}, {})
}

/**
 * P03+P04: the same sheet while its parent state ticks at 10 Hz and hands it a fresh list instance with equal content each tick (what a
 * ViewModel emitting a new state does). A recomposes every tick, rebuilding the formatter, both filters and all rows; B has a stable
 * parameter and remember, so an equal list lets it skip.
 */
@Composable
fun P03P04CorpusScreen(variantA: Boolean, n: Int) {
    val base = remember { sheetLoans(n) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            tick++
            Probe.count("ticks")
        }
    }
    FullyDrawn()
    val fresh = remember(tick) { base.toList() }
    if (variantA) BookDetailSheet(SheetBookSample, fresh, {}, {}) else BookDetailSheetStable(SheetBookSample, LoanList(fresh), {}, {})
}
