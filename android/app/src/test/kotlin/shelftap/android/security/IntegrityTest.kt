package shelftap.android.security

import kotlin.test.assertEquals
import org.junit.Test

class IntegrityTest {
    private val cert = "ab".repeat(32)
    private val clean = IntegritySignals(
        suBinaryFound = false, buildTags = "release-keys", rootAppInstalled = false,
        fingerprint = "google/husky/husky:15/AP4A/1:user/release-keys", model = "Pixel 8 Pro", manufacturer = "Google",
        hardware = "husky", product = "husky", debuggerAttached = false, debuggable = false,
        signingCertSha256 = listOf(cert), expectedCertSha256 = cert,
    )

    private fun flags(s: IntegritySignals) = Integrity.evaluate(s)

    @Test
    fun aCleanDeviceIsOk() {
        assertEquals(emptySet(), flags(clean))
        assertEquals("ok", Integrity.header(flags(clean)))
    }

    @Test
    fun rootSignals() {
        assertEquals(setOf(IntegrityFlag.ROOT), flags(clean.copy(suBinaryFound = true)))
        assertEquals(setOf(IntegrityFlag.ROOT), flags(clean.copy(rootAppInstalled = true)))
        assertEquals(setOf(IntegrityFlag.ROOT), flags(clean.copy(buildTags = "test-keys")))
    }

    @Test
    fun emulatorSignals() {
        val emulator = clean.copy(fingerprint = "generic_x86_arm/sdk_gphone64_arm64/emu64a:15/AE3A/1:userdebug/dev-keys")
        assertEquals(setOf(IntegrityFlag.EMULATOR), flags(emulator))
        assertEquals(setOf(IntegrityFlag.EMULATOR), flags(clean.copy(hardware = "ranchu")))
        assertEquals(setOf(IntegrityFlag.EMULATOR), flags(clean.copy(model = "Android SDK built for x86")))
    }

    @Test
    fun debuggerOrDebuggableBuild() {
        assertEquals(setOf(IntegrityFlag.DEBUGGER), flags(clean.copy(debuggerAttached = true)))
        assertEquals(setOf(IntegrityFlag.DEBUGGER), flags(clean.copy(debuggable = true)))
    }

    @Test
    fun tamperedWhenTheSigningCertDiffers() {
        assertEquals(setOf(IntegrityFlag.TAMPERED), flags(clean.copy(signingCertSha256 = listOf("cd".repeat(32)))))
        assertEquals(setOf(IntegrityFlag.TAMPERED), flags(clean.copy(signingCertSha256 = emptyList())))
        assertEquals(emptySet(), flags(clean.copy(signingCertSha256 = listOf(cert.uppercase()))))
    }

    @Test
    fun anEmptyExpectedCertSkipsTheTamperCheck() {
        assertEquals(emptySet(), flags(clean.copy(expectedCertSha256 = "", signingCertSha256 = listOf("cd".repeat(32)))))
    }

    @Test
    fun headerListsFlagsInApiOrder() {
        val all = flags(clean.copy(suBinaryFound = true, hardware = "ranchu", debuggable = true, signingCertSha256 = emptyList()))
        assertEquals("root,emulator,debugger,tampered", Integrity.header(all))
    }
}
