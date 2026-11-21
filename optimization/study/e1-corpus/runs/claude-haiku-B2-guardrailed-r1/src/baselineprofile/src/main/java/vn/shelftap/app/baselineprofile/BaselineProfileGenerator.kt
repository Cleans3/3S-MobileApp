package vn.shelftap.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the startup path of the app for the Baseline Profile. The app has no list screen,
 * so this covers app start only. The profile screen needs a signed-in account and is not recorded.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(packageName = "vn.shelftap.app") {
        pressHome()
        startActivityAndWait()
    }
}
