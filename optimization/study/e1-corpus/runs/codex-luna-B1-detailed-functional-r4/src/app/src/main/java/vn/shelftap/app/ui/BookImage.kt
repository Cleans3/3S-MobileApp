package vn.shelftap.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun BookImage(url: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loading by remember(url) { mutableStateOf(url != null) }
    LaunchedEffect(url) {
        bitmap = if (url == null) null else withContext(Dispatchers.IO) { runCatching { URL(url).openStream().use { stream -> BitmapFactory.decodeStream(stream) } }.getOrNull() }
        loading = false
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(bitmap!!.asImageBitmap(), "Book cover", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            loading -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else -> Text("No cover", style = MaterialTheme.typography.labelSmall)
        }
    }
}
