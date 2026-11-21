package vn.shelftap.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
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
            // App startup
            pressHome()
            startActivityAndWait()

            // Main list scroll
            val list = device.wait(Until.findObject(By.res("catalogue_list")), 5_000)
            list?.setGestureMargin(device.displayWidth / 5)
            list?.fling(Direction.DOWN)
            device.waitForIdle()
        }
    }
}
