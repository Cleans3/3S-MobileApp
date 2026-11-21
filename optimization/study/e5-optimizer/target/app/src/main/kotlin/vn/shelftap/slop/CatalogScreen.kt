package vn.shelftap.slop

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size

@Composable
fun CatalogScreen(vm: CatalogViewModel = viewModel()) {
    val shown = vm.books.sortedBy { it.title }.filter { it.title.contains(vm.query, ignoreCase = true) }

    Column {
        OutlinedTextField(
            value = vm.query,
            onValueChange = vm::onQueryChange,
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        )
        Stats(vm.books)
        RecentRow(vm.books.take(10))
        LazyColumn {
            items(shown) { book -> BookRow(book) }
        }
    }
}

@Composable
private fun Stats(books: List<Book>) {
    val authors by remember(books) { derivedStateOf { books.map { it.author }.distinct().size } }
    Text("${books.size} books by $authors authors", modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
private fun RecentRow(recent: List<Book>) {
    LazyRow {
        items(recent, key = { it.id }) { book ->
            Text(book.title, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun BookRow(book: Book) {
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(0.9f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .shadow(12.dp)
            .graphicsLayer { alpha = pulse },
    ) {
        Box(Modifier.size(72.dp).blur(4.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(book.coverUrl).size(Size.ORIGINAL).build(),
                contentDescription = book.title,
                modifier = Modifier.size(72.dp),
            )
        }
        Column(Modifier.padding(start = 8.dp)) {
            Text(book.title)
            Text(book.author)
        }
    }
}
