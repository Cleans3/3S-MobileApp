package vn.shelftap.baselineprofile

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
            // App start
            pressHome()
            startActivityAndWait()
            device.waitForIdle()

            // Scroll main catalogue list
            val catalogueList = device.findObject(By.res("catalogue_list"))
            catalogueList?.fling(Direction.DOWN)
            device.waitForIdle()
        }
    }
}
