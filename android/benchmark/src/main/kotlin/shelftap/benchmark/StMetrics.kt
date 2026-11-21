package shelftap.benchmark

import android.util.Log
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.Row
import androidx.benchmark.traceprocessor.TraceProcessor

private fun Row.num(column: String): Double = runCatching { double(column) }.getOrElse { long(column).toDouble() }

private fun label(pattern: String) = pattern.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_')

/** Max of the app's Perfetto counters st.<name> (Probe.count: running total; Probe.gauge: value). 0 when the counter never appeared. */
@OptIn(ExperimentalMetricApi::class)
class StCounters(private val names: List<String>) : TraceMetric() {
    override fun getMeasurements(captureInfo: Metric.CaptureInfo, traceSession: TraceProcessor.Session): List<Metric.Measurement> {
        val max = traceSession.query(
            """
            SELECT t.name AS name, MAX(c.value) AS v
            FROM counter c JOIN process_counter_track t ON c.track_id = t.id JOIN process p USING(upid)
            WHERE t.name LIKE 'st.%' AND p.name = '${captureInfo.targetPackageName}'
            GROUP BY t.name
            """.trimIndent(),
        ).associate { it.string("name") to it.num("v") }
        return names.map { Metric.Measurement("st.$it", max["st.$it"] ?: 0.0) }
    }
}

/**
 * Per slice-name pattern (SQL LIKE) in the target process: <label>.count, .sumMs (all threads), .maxMs, .mainMs (main thread only).
 * Slices the app emits are st.<name> (Probe.section); Compose and the framework emit their own.
 */
@OptIn(ExperimentalMetricApi::class)
class StSlices(private val patterns: List<String>) : TraceMetric() {
    override fun getMeasurements(captureInfo: Metric.CaptureInfo, traceSession: TraceProcessor.Session): List<Metric.Measurement> =
        patterns.flatMap { pattern ->
            val r = traceSession.query(
                """
                SELECT COUNT(*) AS n, COALESCE(SUM(s.dur), 0) AS sum, COALESCE(MAX(s.dur), 0) AS max,
                       COALESCE(SUM(CASE WHEN t.tid = p.pid THEN s.dur ELSE 0 END), 0) AS main
                FROM slice s JOIN thread_track tt ON s.track_id = tt.id JOIN thread t USING(utid) JOIN process p USING(upid)
                WHERE p.name = '${captureInfo.targetPackageName}' AND s.dur >= 0 AND s.name LIKE '$pattern'
                """.trimIndent(),
            ).first()
            val l = label(pattern)
            listOf(
                Metric.Measurement("$l.count", r.num("n")),
                Metric.Measurement("$l.sumMs", r.num("sum") / 1e6),
                Metric.Measurement("$l.maxMs", r.num("max") / 1e6),
                Metric.Measurement("$l.mainMs", r.num("main") / 1e6),
            )
        }
}

/** Dev aid (-e st.dump 1): logs the 40 most expensive slice names of the target process to logcat tag STDUMP. Reports no metric. */
@OptIn(ExperimentalMetricApi::class)
class StDump : TraceMetric() {
    override fun getMeasurements(captureInfo: Metric.CaptureInfo, traceSession: TraceProcessor.Session): List<Metric.Measurement> {
        traceSession.query(
            """
            SELECT s.name AS name, COUNT(*) AS n, SUM(s.dur) AS sum
            FROM slice s JOIN thread_track tt ON s.track_id = tt.id JOIN thread t USING(utid) JOIN process p USING(upid)
            WHERE p.name = '${captureInfo.targetPackageName}' AND s.dur >= 0
            GROUP BY s.name ORDER BY sum DESC LIMIT 40
            """.trimIndent(),
        ).forEach { Log.i("STDUMP", "${it.string("name")} n=${it.num("n").toLong()} sumMs=${it.num("sum") / 1e6}") }
        return emptyList()
    }
}
