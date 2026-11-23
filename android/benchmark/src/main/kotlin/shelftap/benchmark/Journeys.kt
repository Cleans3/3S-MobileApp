package shelftap.benchmark

import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

const val MAIN_ACTIVITY = "shelftap.optlab.MainActivity"

/** Typed into the P13 search box one keystroke at a time. Words come from the optlab vocabulary (BookGenerator). */
const val QUERY = "silent river"

val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

fun launchIntent(pkg: String, factor: String, variant: String, rows: String? = null) =
    Intent().setClassName(pkg, MAIN_ACTIVITY).putExtra("factor", factor).putExtra("variant", variant)
        .apply { if (rows != null) putExtra("n", rows.toInt()) }

/** Fixed-length swipes up: the same finger path for A and B. No waitForIdle, because A of P08 never goes idle. */
fun swipeUp(times: Int, steps: Int = 25) {
    val w = device.displayWidth
    val h = device.displayHeight
    repeat(times) {
        device.swipe(w / 2, (h * 0.8).toInt(), w / 2, (h * 0.3).toInt(), steps)
        Thread.sleep(150)
    }
}

/** Journey for one factor, after the activity is up. */
fun MacrobenchmarkScope.journey(factor: String) {
    when (factor) {
        "P08", "P06", "P01P09", "P08c", "P06c", "P01c", "P09c" -> {
            swipeUp(6)
            if (factor == "P08" || factor == "P08c") Thread.sleep(3_000) // idle: A of P08 keeps animating (the corpus A does not; same journey for both)
        }
        "P03P04", "P03P04c" -> {
            swipeUp(2)
            Thread.sleep(5_000) // 50 ticks
        }
        "P13", "P13c" -> {
            check(device.wait(Until.hasObject(By.textStartsWith("ready")), 120_000) == true) { "search never became ready" }
            val box = device.findObject(By.clazz("android.widget.EditText")) ?: error("no search field")
            box.click()
            for (i in 1..QUERY.length) {
                box.text = QUERY.take(i)
                Thread.sleep(120) // ~8 keystrokes/s
            }
            Thread.sleep(1_500) // let B's debounce fire and results settle
        }
        "startup" -> swipeUp(3) // first scroll after launch: where a cold, uncompiled app janks
        else -> error("unknown factor $factor")
    }
}
