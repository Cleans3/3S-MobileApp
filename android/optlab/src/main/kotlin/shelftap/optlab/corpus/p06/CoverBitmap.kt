package shelftap.optlab.corpus.p06

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shelftap.optlab.Probe
import java.net.URL

// Ported from codex-luna-B3-detailed-functional-r1 app/src/main/java/vn/shelftap/app/MainActivity.kt:39 (DetailScreen, one minified line):
// the cover-loading fragment, reformatted, with d?.coverUrl bound to the parameter. The cover URL is a file: URL of the local 1500x2250 cover,
// so URL(url).openStream() is unchanged. Probe.count lines are instrumentation (same hooks as B).
private val green = Color(0xFF315F51)

@Composable
fun CoverBitmap(coverUrl: String?) {
    var bitmap by remember(coverUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(coverUrl) {
        val url = coverUrl
        if (url != null) bitmap = withContext(Dispatchers.IO) {
            Probe.section("img.decode") {
                runCatching { BitmapFactory.decodeStream(URL(url).openStream()) }.getOrNull()?.also {
                    Probe.count("decodedBytes", it.allocationByteCount.toLong())
                    Probe.count("decodes")
                }
            }
        }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), "Cover", Modifier.size(150.dp, 210.dp))
    else Box(Modifier.size(150.dp, 210.dp).background(Color(0xFFDDE8E1)), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Info, "Book", Modifier.size(64.dp), tint = green)  // MenuBook is an extended icon
    }
}
