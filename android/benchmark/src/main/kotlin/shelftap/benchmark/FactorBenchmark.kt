package shelftap.benchmark

import android.util.Log
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
 *   st.compilation full|none|profile   st.iters N   st.dump 0|1   st.metrics all|primary|frames
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
            metrics = metricsFor(factor, Args.metrics) + if (Args.dump) listOf(StDump()) else emptyList(),
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
            // The harness samples host memory/swap at these markers (logcat tag STBENCH).
            Log.i("STBENCH", "iter-begin")
            startActivityAndWait(launchIntent(target, factor, Args.variant, Args.rows))
            journey(factor)
            Log.i("STBENCH", "iter-end")
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

    private fun metricsFor(factor: String, phase: String): List<Metric> {
        val frames = FrameTimingMetric()
        if (phase == "frames") return listOf(frames)
        val memory = MemoryUsageMetric(MemoryUsageMetric.Mode.Max)
        val primary = when (factor) {
            "P08" -> listOf(memory, StCounters(listOf("rc.row", "fx.perItem")), StSlices(listOf("Recomposer:recompose", "Compose:recompose")))
            "P06" -> listOf(memory, StCounters(listOf("decodedBytes", "decodes")), StSlices(listOf("st.img.decode")))
            "P01P09" -> listOf(StartupTimingMetric(), memory, StCounters(listOf("rc.row")), StSlices(listOf("Recomposer:recompose", "Compose:recompose")))
            "P13" -> listOf(memory, StCounters(listOf("search.queries")), StSlices(listOf("st.search.query")))
            "P03P04" -> listOf(StCounters(listOf("rc.row", "ticks")), StSlices(listOf("st.sort", "Recomposer:recompose", "Compose:recompose")))
            // The same factors with A from corpus code (optlab corpus/*): same probes, same scenarios.
            "P08c" -> listOf(memory, StCounters(listOf("rc.row", "fx.perItem")), StSlices(listOf("Recomposer:recompose", "Compose:recompose")))
            "P06c" -> listOf(memory, StCounters(listOf("decodedBytes", "decodes")), StSlices(listOf("st.img.decode")))
            "P01c" -> listOf(StartupTimingMetric(), memory, StCounters(listOf("rc.row")), StSlices(listOf("Recomposer:recompose", "Compose:recompose")))
            "P09c" -> listOf(StartupTimingMetric(), memory, StCounters(listOf("rc.row")), StSlices(listOf("Recomposer:recompose", "Compose:recompose")))
            "P13c" -> listOf(memory, StCounters(listOf("search.queries")), StSlices(listOf("st.search.query")))
            "P03P04c" -> listOf(StCounters(listOf("rc.row", "ticks")), StSlices(listOf("Recomposer:recompose", "Compose:recompose")))
            "startup" -> listOf(StartupTimingMetric())
            else -> error("unknown factor $factor")
        }
        return if (phase == "all") primary + frames else primary
    }
}
