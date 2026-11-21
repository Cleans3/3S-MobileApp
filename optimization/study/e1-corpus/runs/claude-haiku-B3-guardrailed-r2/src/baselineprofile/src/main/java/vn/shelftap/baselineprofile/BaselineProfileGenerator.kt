package vn.shelftap.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the baseline profile for app start and for scrolling the main list.
 * The member must already be signed in on the device for the list to have content.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() = baselineProfileRule.collect(
        packageName = "vn.shelftap.app",
    ) {
        pressHome()
        startActivityAndWait()
        device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 2f)
        device.waitForIdle()
    }
}
