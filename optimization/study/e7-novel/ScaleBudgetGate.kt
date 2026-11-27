// ScaleBudgetGate.kt — Phase-2 Macrobenchmark CI gate template for scale-amplified Android apps.
// Not executed during Phase-1 desk work (no emulator used). Ready to drop into an androidTest /
// macrobenchmark module once an emulator or target device is available.
//
// Budgets taken directly from ShelfTap course targets (CLAUDE.md / optimization/README.md):
//   - Peak RSS / PSS memory while browsing < 150 MB (153_600 KB)
//   - Search over 100k titles < 50 ms (measured via TraceSectionMetric on st_search_query)
//   - 60 fps scroll budget: on physical device p90 <= 16.67 ms and slowFrames <= 5%;
//     on software-GPU emulator, use A/B ratio gate (amplified vs baseline) instead of absolute ms.

package vn.shelftap.scaleprobe

import android.os.Build
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class ScaleBudgetGate {

    companion object {
        // Pre-stated course budgets (frozen before Phase-2 runs)
        const val MAX_RSS_ANON_KB: Double = 150.0 * 1024.0   // 150 MB browsing ceiling
        const val MAX_SEARCH_QUERY_MS: Double = 50.0         // 50 ms per query at N = 100,000
        const val MAX_DEVICE_FRAME_P90_MS: Double = 16.67    // 60 fps on physical phone only
        const val MAX_EMULATOR_TTID_MS: Double = 1500.0      // catch eager-list O(N) cold-start blowup
        const val TARGET_PACKAGE: String = "com.lumen.library" // override via instrumentation arg
    }

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scaleScrollAndMemoryBudgetGate() {
        val isEmulator = Build.FINGERPRINT.contains("generic") ||
            Build.MODEL.contains("Emulator") ||
            Build.HARDWARE.contains("ranchu")

        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(
                StartupTimingMetric(),
                FrameTimingMetric(),
                MemoryUsageMetric(MemoryUsageMetric.Mode.Max),
                TraceSectionMetric("st_search_query", TraceSectionMetric.Mode.Max),
            ),
            compilationMode = CompilationMode.Full(),
            iterations = 5,
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
            // Drive 6 vertical flings over the scale-amplified catalogue (N = 2,000 or 100,000)
            val scrollable = device.findObject(By.scrollable(true))
            if (scrollable != null) {
                repeat(6) {
                    scrollable.fling(Direction.DOWN)
                    device.waitForIdle()
                }
            }
            // Note: Macrobenchmark writes JSON metrics to outputs/benchmarkData.json;
            // the companion Python CI parser enforces:
            //   1. memoryRssAnonMaxKb.median < MAX_RSS_ANON_KB (device-independent hard gate)
            //   2. st_search_queryMaxMs.median < MAX_SEARCH_QUERY_MS (when search instrumented)
            //   3. timeToInitialDisplayMs.median < MAX_EMULATOR_TTID_MS (eager-list tripwire)
            //   4. if (!isEmulator) frameDurationCpuMs.P90 <= MAX_DEVICE_FRAME_P90_MS
            assertTrue("Target package launched", device.currentPackageName == TARGET_PACKAGE)
        }
    }
}
