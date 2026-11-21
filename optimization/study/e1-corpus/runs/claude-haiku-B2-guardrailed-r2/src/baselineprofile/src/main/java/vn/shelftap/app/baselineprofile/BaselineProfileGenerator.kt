package vn.shelftap.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the startup path and the sign-in screen for the baseline profile.
 * Run with: ./gradlew :app:generateBaselineProfile (needs a connected device or emulator).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateStartupProfile() = baselineProfileRule.collect(
        packageName = "vn.shelftap.app",
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
    }
}
