package vn.shelftap.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the Baseline Profile for the app. Run with:
 *   ./gradlew :app:generateBaselineProfile
 * on a connected device or emulator (API 28+).
 * Covers app start. The app has no long list yet; add a scroll step here if one is added.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateStartupProfile() = baselineProfileRule.collect(
        packageName = "vn.shelftap.app",
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
