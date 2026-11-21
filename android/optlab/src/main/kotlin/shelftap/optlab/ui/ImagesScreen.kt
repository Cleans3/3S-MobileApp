package shelftap.optlab.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.decode.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shelftap.optlab.Probe
import shelftap.optlab.RecompositionProbe
import shelftap.optlab.data.Book
import shelftap.optlab.data.BookGenerator
import shelftap.optlab.data.Covers
import java.io.File

private val CoverSize = Modifier.size(80.dp, 120.dp)

/** P06: cover thumbnails from 1500x2250 files. A decodes the full file per row, no cache; B lets Coil size and cache. */
@Composable
fun ImagesScreen(variantA: Boolean, n: Int) {
    val ctx = LocalContext.current
    val books = remember { BookGenerator.all(n) }
    FullyDrawn()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(books, key = { it.id }, contentType = { "book" }) { book ->
            val path = Covers.file(ctx, book.coverIdx)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (variantA) CoverA(path) else CoverB(path)
                Spacer(Modifier.width(12.dp))
                Title(book)
            }
        }
    }
}

@Composable
private fun Title(book: Book) {
    RecompositionProbe("rc.row")
    Column {
        Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text("${book.author} · ${book.year}", style = MaterialTheme.typography.bodySmall)
    }
}

/** A: BitmapFactory.decodeFile with no inSampleSize, nothing cached. Decoded off the main thread so P06 stays separate from P07. */
@Composable
private fun CoverA(path: File) {
    val bmp by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            Probe.section("img.decode") {
                BitmapFactory.decodeFile(path.path)?.also {
                    Probe.count("decodedBytes", it.allocationByteCount.toLong())
                    Probe.count("decodes")
                }?.asImageBitmap()
            }
        }
    }
    val b = bmp
    if (b != null) Image(b, null, CoverSize, contentScale = ContentScale.Crop) else Box(CoverSize.background(MaterialTheme.colorScheme.surfaceVariant))
}

/** B: Coil AsyncImage sizes the decode to the 80x120 dp slot (80x120 dp is the composable's constraint) and keeps memory/disk caches. */
@Composable
private fun CoverB(path: File) {
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
    AsyncImage(request, contentDescription = null, modifier = CoverSize, contentScale = ContentScale.Crop)
}
