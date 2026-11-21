package vn.shelftap.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Records the startup path and a scroll through the main list for the baseline profile. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(packageName = "vn.shelftap.app") {
        pressHome()
        startActivityAndWait()
        device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 2f)
    }
}
