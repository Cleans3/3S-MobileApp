package shelftap.android.ui

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

/** The start screen's motion, as numbers: the tile's pop (`st-pop`) and the running bar (`st-indet`) of the board `Splash`. */
class StartupMotionTest {
    @Test
    fun theTilePopsFromSixtyPercentThroughASmallOvershootToFull() {
        assertEquals(0.6f, popScale(0f), 0.001f)
        assertEquals(1.06f, popScale(0.7f), 0.001f)
        assertEquals(1f, popScale(1f), 0.001f)
        assertTrue(popScale(0.35f) in 0.6f..1.06f)
    }

    @Test
    fun theBarPillStartsBeforeTheTrackAndEndsPastItsRightEnd() {
        val width = 255f
        val pill = width * 0.4f
        assertEquals(-pill, barPillX(0f, width), 0.001f) // entirely out of sight on the left
        assertTrue(barPillX(1f, width) > width) // entirely out of sight on the right
        assertTrue(barPillX(0.5f, width) in 0f..width) // in the middle of the run it is on the track
    }
}
