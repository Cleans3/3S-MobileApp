package shelftap.optlab.ui

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shelftap.optlab.data.BookDb
import shelftap.optlab.data.Covers
import java.io.File

/** One-time data setup (covers + FTS DB). The benchmark waits for text starting with "seeded"; stats go to logcat tag STLAB. */
@Composable
fun SeedScreen() {
    val ctx = LocalContext.current
    val status by produceState("seeding") {
        value = withContext(Dispatchers.Default) {
            val marker = File(ctx.filesDir, "seed.ok")
            runCatching {
                if (!marker.exists()) {
                    val coversMs = Covers.ensure(ctx)
                    val db = BookDb.seed(ctx)
                    marker.writeText("covers_ms=$coversMs $db")
                }
                "seeded " + marker.readText()
            }.getOrElse { "seed failed: $it" }
        }
    }
    if (status.startsWith("seeded")) Log.i("STLAB", status)
    Text(status)
}
