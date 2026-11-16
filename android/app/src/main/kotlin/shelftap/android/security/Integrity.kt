package shelftap.android.security

enum class IntegrityFlag(val wire: String) {
    ROOT("root"), EMULATOR("emulator"), DEBUGGER("debugger"), TAMPERED("tampered")
}

/** Everything the checks read, gathered once by [IntegrityChecker]; kept plain so the decision logic is unit-tested. */
data class IntegritySignals(
    val suBinaryFound: Boolean,
    val buildTags: String,
    val rootAppInstalled: Boolean,
    val fingerprint: String,
    val model: String,
    val manufacturer: String,
    val hardware: String,
    val product: String,
    val debuggerAttached: Boolean,
    val debuggable: Boolean,
    /** Lowercase hex SHA-256 of each certificate the installed APK is signed with. */
    val signingCertSha256: List<String>,
    /** Lowercase hex, empty = skip the tamper check. */
    val expectedCertSha256: String,
)

/** Signals only: the app warns and tells the server, it never decides or blocks. */
object Integrity {
    fun evaluate(s: IntegritySignals): Set<IntegrityFlag> = buildSet {
        if (s.suBinaryFound || s.rootAppInstalled || s.buildTags.contains("test-keys")) add(IntegrityFlag.ROOT)
        if (isEmulator(s)) add(IntegrityFlag.EMULATOR)
        if (s.debuggerAttached || s.debuggable) add(IntegrityFlag.DEBUGGER)
        if (s.expectedCertSha256.isNotEmpty() && s.signingCertSha256.none { it.equals(s.expectedCertSha256, ignoreCase = true) }) {
            add(IntegrityFlag.TAMPERED)
        }
    }

    /** `ok`, or the comma list of API.md in enum order. */
    fun header(flags: Set<IntegrityFlag>): String =
        if (flags.isEmpty()) "ok" else IntegrityFlag.entries.filter { it in flags }.joinToString(",") { it.wire }

    private fun isEmulator(s: IntegritySignals): Boolean =
        s.fingerprint.startsWith("generic") || s.fingerprint.startsWith("unknown") || s.fingerprint.contains("emulator") ||
            s.model.contains("Emulator") || s.model.contains("Android SDK built for") || s.model.contains("google_sdk") ||
            s.manufacturer.contains("Genymotion") ||
            s.hardware == "goldfish" || s.hardware == "ranchu" ||
            s.product.contains("sdk") || s.product.contains("emulator") || s.product.contains("simulator")
}

/** Source of the `X-Device-Integrity` header value, sent on login and refresh. */
fun interface IntegritySource {
    fun header(): String
}
