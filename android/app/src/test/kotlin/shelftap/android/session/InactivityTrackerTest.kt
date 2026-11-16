package shelftap.android.session

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class InactivityTrackerTest {
    private var now = 1_000L
    private val tracker = InactivityTracker(limitMs = 300_000, clock = { now })

    @Test
    fun expiresAfterTheLimitAndTouchResetsIt() {
        assertFalse(tracker.expired())
        now += 299_999
        assertFalse(tracker.expired())
        tracker.touch()
        now += 299_999
        assertFalse(tracker.expired())
        now += 1
        assertTrue(tracker.expired())
        assertEquals(0, tracker.remainingMs())
    }
}
