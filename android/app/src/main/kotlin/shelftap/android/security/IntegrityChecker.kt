package shelftap.android.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.File
import java.security.MessageDigest
import shelftap.android.BuildConfig

/** Reads the device and the installed APK into [IntegritySignals]. Cheap enough to run on every login and refresh. */
class IntegrityChecker(private val context: Context) : IntegritySource {

    fun flags(): Set<IntegrityFlag> = Integrity.evaluate(signals())

    override fun header(): String = Integrity.header(flags())

    private fun signals() = IntegritySignals(
        suBinaryFound = SU_PATHS.any { File(it).exists() },
        buildTags = Build.TAGS.orEmpty(),
        rootAppInstalled = ROOT_PACKAGES.any(::isInstalled),
        fingerprint = Build.FINGERPRINT.orEmpty(),
        model = Build.MODEL.orEmpty(),
        manufacturer = Build.MANUFACTURER.orEmpty(),
        hardware = Build.HARDWARE.orEmpty(),
        product = Build.PRODUCT.orEmpty(),
        debuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger(),
        debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        signingCertSha256 = signingCertDigests(),
        expectedCertSha256 = BuildConfig.SIGNING_CERT_SHA256,
    )

    private fun isInstalled(pkg: String): Boolean =
        try {
            @Suppress("DEPRECATION") context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    @Suppress("DEPRECATION")
    private fun signingCertDigests(): List<String> {
        val pm = context.packageManager
        val signatures = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
            } else {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        return signatures.orEmpty().map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).toHexString() }
    }

    private companion object {
        val SU_PATHS = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/su", "/su/bin/su",
            "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su", "/system/app/Superuser.apk",
        )
        val ROOT_PACKAGES = listOf(
            "com.topjohnwu.magisk", "io.github.vvb2060.magisk", "me.weishu.kernelsu", "eu.chainfire.supersu",
            "com.koushikdutta.superuser", "com.noshufou.android.su", "com.thirdparty.superuser", "com.yellowes.su",
        )
    }
}
