package vn.shelftap.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
            pressHome()
            startActivityAndWait()

            // Scroll main list if scrollable element is present
            device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.8f)
        }
    }
}
