package shelftap.android.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveLayoutTest {
    @Test
    fun phonesAreCompact() {
        assertEquals(NavLayout.COMPACT, navLayoutFor(360, 800))
        assertEquals(NavLayout.COMPACT, navLayoutFor(412, 960)) // 21:9
        assertEquals(NavLayout.COMPACT, navLayoutFor(599, 800))
    }

    @Test
    fun mediumWindowsGetTheRail() {
        assertEquals(NavLayout.RAIL, navLayoutFor(600, 800))
        assertEquals(NavLayout.RAIL, navLayoutFor(720, 840)) // a folded-open phone, nearly square
        assertEquals(NavLayout.RAIL, navLayoutFor(839, 700))
    }

    @Test
    fun wideWindowsGetTheSideMenu() {
        assertEquals(NavLayout.SIDE, navLayoutFor(900, 600))
        assertEquals(NavLayout.SIDE, navLayoutFor(960, 720))
    }

    @Test
    fun aPhoneOnItsSideKeepsTheCompactMenu() {
        assertEquals(NavLayout.COMPACT, navLayoutFor(844, 390)) // 840 to 900 wide
        assertEquals(NavLayout.COMPACT, navLayoutFor(960, 480)) // wide but 480 high or less
        assertEquals(NavLayout.SIDE, navLayoutFor(960, 481))
    }
}
