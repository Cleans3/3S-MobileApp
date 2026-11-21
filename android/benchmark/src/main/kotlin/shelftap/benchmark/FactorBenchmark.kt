package shelftap.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * One parameterised test; the harness (e2-bench/harness/run_bench.py) drives it with instrumentation arguments:
 *   st.factor P08|P06|P01P09|P13|P03P04|startup   st.variant A|B   st.target <package>
 *   st.compilation full|none|profile   st.iters N   st.dump 0|1
 * Cold start every iteration, so the app's counters begin at zero and the trace holds exactly one run.
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class FactorBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun run() {
        val factor = Args.factor
        val target = Args.target
        rule.measureRepeated(
            packageName = target,
            metrics = metricsFor(factor) + if (Args.dump) listOf(StDump()) else emptyList(),
            compilationMode = when (Args.compilation) {
                "full" -> CompilationMode.Full()
                "none" -> CompilationMode.None()
                "profile" -> CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require)
                else -> error("st.compilation must be full|none|profile")
            },
            startupMode = StartupMode.COLD,
            iterations = Args.iterations,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait(launchIntent(target, factor, Args.variant, Args.rows))
            journey(factor)
        }
    }

    /** One-time data setup (covers + FTS DB) for the package; not a measurement. */
    @Test
    fun seed() {
        val out = device.executeShellCommand("am start -W -n ${Args.target}/$MAIN_ACTIVITY --es factor seed")
        val done = device.wait(Until.findObject(By.text(Pattern.compile("seeded.*|seed failed.*"))), 600_000)
            ?: error("seed timed out: $out")
        check(done.text.startsWith("seeded")) { done.text }
        android.util.Log.i("STLAB", done.text)
    }

    private fun metricsFor(factor: String): List<Metric> {
        val frames = FrameTimingMetric()
        val memory = MemoryUsageMetric(MemoryUsageMetric.Mode.Max)
        return when (factor) {
            "P08" -> listOf(frames, memory, StCounters(listOf("rc.row", "fx.perItem")), StSlices(listOf("Recomposer:recompose", "Compose:recompose")))
            "P06" -> listOf(frames, memory, StCounters(listOf("decodedBytes", "decodes")), StSlices(listOf("st.img.decode")))
            "P01P09" -> listOf(StartupTimingMetric(), frames, memory, StCounters(listOf("rc.row")), StSlices(listOf("Recomposer:recompose", "Compose:recompose")))
            "P13" -> listOf(frames, memory, StCounters(listOf("search.queries")), StSlices(listOf("st.search.query")))
            "P03P04" -> listOf(frames, StCounters(listOf("rc.row", "ticks")), StSlices(listOf("st.sort", "Recomposer:recompose", "Compose:recompose")))
            "startup" -> listOf(StartupTimingMetric(), frames)
            else -> error("unknown factor $factor")
        }
    }
}
