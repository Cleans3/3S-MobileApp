package shelftap.optlab.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import shelftap.optlab.data.BookGenerator

/** P01 + P09: long catalog. A composes every row eagerly in a scrolling Column; B is a LazyColumn with key and contentType. */
@Composable
fun ListScreen(variantA: Boolean, n: Int) {
    val books = remember { BookGenerator.all(n) }
    FullyDrawn()
    if (variantA) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            books.forEach { BookRowPlain(it) }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(books, key = { it.id }, contentType = { "row" }) { BookRowPlain(it) }
        }
    }
}
