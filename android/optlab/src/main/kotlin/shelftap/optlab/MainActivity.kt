package shelftap.optlab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import shelftap.optlab.ui.EffectsScreen
import shelftap.optlab.ui.P01CorpusScreen
import shelftap.optlab.ui.P03P04CorpusScreen
import shelftap.optlab.ui.P06CorpusScreen
import shelftap.optlab.ui.P08CorpusScreen
import shelftap.optlab.ui.P09CorpusScreen
import shelftap.optlab.ui.SearchCorpusScreen
import shelftap.optlab.ui.EquivScreen
import shelftap.optlab.ui.ImagesScreen
import shelftap.optlab.ui.ListScreen
import shelftap.optlab.ui.SearchScreen
import shelftap.optlab.ui.SeedScreen
import shelftap.optlab.ui.StartupScreen
import shelftap.optlab.ui.TickerScreen

/**
 * Intent extras pick the screen, so a benchmark (or adb) deep-links straight to a factor:
 *   factor  P08 | P06 | P01P09 | P13 | P03P04 | startup | seed | p13eq   (absent = menu)
 *           P08c | P06c | P01c | P09c | P13c | P03P04c   the same factors with A taken from corpus code (optlab corpus package, e2-bench/ports)
 *   variant A (slop) | B (tuned, default)
 *   n       row count, default per factor
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factor = intent.getStringExtra("factor")
        val a = intent.getStringExtra("variant") == "A"
        fun n(default: Int) = intent.getIntExtra("n", default)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    when (factor) {
                        "P08" -> EffectsScreen(a, n(200))
                        "P06" -> ImagesScreen(a, n(300))
                        "P01P09" -> ListScreen(a, n(5000))
                        "P13" -> SearchScreen(a)
                        "P08c" -> P08CorpusScreen(a, n(200))
                        "P06c" -> P06CorpusScreen(a, n(300))
                        "P01c" -> P01CorpusScreen(a, n(5000))
                        "P09c" -> P09CorpusScreen(a, n(2000))
                        "P13c" -> SearchCorpusScreen(a)
                        "P03P04c" -> P03P04CorpusScreen(a, n(300))
                        "p13eq" -> EquivScreen(intent.getStringExtra("query") ?: "silent river")
                        "P03P04" -> TickerScreen(a, n(3000))
                        "startup" -> StartupScreen()
                        "seed" -> SeedScreen()
                        else -> Menu()
                    }
                }
            }
        }
    }
}

@Composable
private fun Menu() {
    val ctx = LocalContext.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("ShelfTap optlab", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { ctx.startActivity(Intent(ctx, MainActivity::class.java).putExtra("factor", "seed")) }) { Text("seed data") }
        for (f in listOf("P08", "P06", "P01P09", "P13", "P03P04")) {
            Row {
                for (v in listOf("A", "B")) {
                    Button(
                        onClick = { ctx.startActivity(Intent(ctx, MainActivity::class.java).putExtra("factor", f).putExtra("variant", v)) },
                        Modifier.padding(end = 8.dp),
                    ) { Text("$f $v") }
                }
            }
        }
    }
}
