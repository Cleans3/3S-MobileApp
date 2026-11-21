package shelftap.benchmark

import androidx.test.platform.app.InstrumentationRegistry

/** Instrumentation arguments (adb shell am instrument -e key value). The Python harness sets them. */
object Args {
    private val raw get() = InstrumentationRegistry.getArguments()

    fun str(key: String, default: String? = null): String =
        raw.getString(key) ?: default ?: error("missing instrumentation argument -e $key")

    /** Package under test: shelftap.optlab (release) or shelftap.optlab.nor8. */
    val target get() = str("st.target", "shelftap.optlab")
    val factor get() = str("st.factor")
    val variant get() = str("st.variant", "B")
    val iterations get() = str("st.iters", "1").toInt()
    /** full | none | profile */
    val compilation get() = str("st.compilation", "full")
    val dump get() = str("st.dump", "0") == "1"
    /** Row count override for the list-shaped factors; empty = the app default. */
    val rows get() = raw.getString("st.n")
}
