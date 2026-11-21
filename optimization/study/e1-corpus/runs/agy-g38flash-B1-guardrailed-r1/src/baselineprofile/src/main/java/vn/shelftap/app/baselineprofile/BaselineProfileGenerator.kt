package vn.shelftap.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline Profile generator covering app startup and scrolling the main catalogue list (Rule 19).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        baselineProfileRule.collect(
            packageName = "vn.shelftap.app",
            includeInStartupProfile = true
        ) {
            // App startup journey
            pressHome()
            startActivityAndWait()

            // Scroll main catalogue list journey
            val catalogueList = device.findObject(By.desc("catalogue_list"))
            catalogueList?.fling(Direction.DOWN)
            device.waitForIdle()
        }
    }
}
