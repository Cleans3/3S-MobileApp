package shelftap.optlab

import android.os.Trace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Measurement hooks. Both variants of every factor call the same hooks, so their own cost cancels out.
 * Values reach Macrobenchmark through the Perfetto trace: counters as "st.<name>", slices as "st.<name>".
 */
object Probe {
    private val totals = ConcurrentHashMap<String, AtomicLong>()

    /** Adds to a running total, exported as counter st.<name>. The benchmark reads the maximum, i.e. the total. */
    fun count(name: String, by: Long = 1) {
        Trace.setCounter("st.$name", totals.getOrPut(name) { AtomicLong() }.addAndGet(by))
    }

    /** Sets an absolute value, exported as counter st.<name>. */
    fun gauge(name: String, value: Long) = Trace.setCounter("st.$name", value)

    /** Wraps a block in the trace slice st.<name>. Keep slices non-nested so their durations can be summed. */
    inline fun <T> section(name: String, block: () -> T): T {
        Trace.beginSection("st.$name")
        try {
            return block()
        } finally {
            Trace.endSection()
        }
    }
}

/**
 * Counts how often the calling composable's body ran (first composition plus every recomposition).
 * Inline on purpose: a separate composable would be skipped when its arguments are unchanged and would undercount.
 */
@Composable
inline fun RecompositionProbe(name: String) {
    SideEffect { Probe.count(name) }
}
