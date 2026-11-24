package shelftap.android.ui

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

/** How far the colour field of a book's page shrinks: not on a normal phone, more on a narrow or a short window, and never under 60%. */
class HeroScaleTest {
    @Test
    fun aNormalPhoneKeepsTheFullSize() {
        assertEquals(1f, heroScale(411f, 914f))
        assertEquals(1f, heroScale(390f, 844f))
    }

    @Test
    fun aNarrowWindowShrinksWithItsWidth() {
        assertEquals(0.82f, heroScale(320f, 640f * 1.5f), 0.01f)
    }

    @Test
    fun aShortWindowShrinksWithItsHeight() {
        // 411 dp tall (a phone held sideways): 0.62 * 411 / 448 = 0.57, held at the 60% floor
        assertEquals(0.6f, heroScale(731f, 411f))
        assertTrue(heroScale(731f, 600f) in 0.8f..0.9f)
    }

    @Test
    fun neverBelowTheFloorOrAboveOne() {
        assertEquals(0.6f, heroScale(100f, 100f))
        assertEquals(1f, heroScale(2000f, 3000f))
    }
}
