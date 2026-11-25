package shelftap.android.ui.icons

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MorphIconTest {
    private val all = listOf(
        LucideMorphs.MenuToX, LucideMorphs.SearchToX, LucideMorphs.EyeToEyeOff, LucideMorphs.BookmarkToCheck,
        LucideMorphs.NfcToCheck, LucideMorphs.ClockToCalendarCheck, LucideMorphs.LockToLockOpen,
    )

    @Test
    fun everyGeneratedMorphHasTheSameShapeInEveryFrame() {
        all.forEach { spec ->
            assertTrue(spec.durationMs in 200..1000, "settle time ${spec.durationMs} ms")
            assertTrue(spec.frames.size >= 5)
            val expected = spec.subpaths * spec.points * 2
            spec.parsed.forEach { assertEquals(expected, it.size) }
        }
    }

    @Test
    fun theFirstFrameIsTheStartShapeAndTheLastTheEndShape() {
        val menu = LucideMorphs.MenuToX.parsed
        // three horizontal bars: y is constant along each subpath in the first frame
        val n = LucideMorphs.MenuToX.points
        for (s in 0 until 3) {
            val ys = (0 until n).map { menu.first()[s * n * 2 + 2 * it + 1] }
            assertTrue(ys.max() - ys.min() < 0.1f, "bar $s is flat")
        }
        assertTrue(menu.first().contentEquals(menu.last()).not())
    }

    @Test
    fun blendingAtTheEndsGivesTheEndFrames() {
        val frames = listOf(floatArrayOf(0f, 0f), floatArrayOf(10f, 20f), floatArrayOf(30f, 40f))
        val out = FloatArray(2)
        blendFrames(frames, 0f, out); assertContentEquals(floatArrayOf(0f, 0f), out)
        blendFrames(frames, 1f, out); assertContentEquals(floatArrayOf(30f, 40f), out)
        blendFrames(frames, 0.5f, out); assertContentEquals(floatArrayOf(10f, 20f), out)
        blendFrames(frames, 0.25f, out); assertContentEquals(floatArrayOf(5f, 10f), out)
        blendFrames(frames, 2f, out); assertContentEquals(floatArrayOf(30f, 40f), out) // out of range is clamped
    }
}
