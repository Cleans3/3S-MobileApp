package vn.shelftap.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.startActivityAndWait
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Records a baseline profile covering app start and scrolling the main loans list. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "vn.shelftap.app",
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.res(packageName, "loans_list")), 5_000)
        device.findObject(By.res(packageName, "loans_list"))?.fling(Direction.DOWN)
        device.waitForIdle()
    }
}
