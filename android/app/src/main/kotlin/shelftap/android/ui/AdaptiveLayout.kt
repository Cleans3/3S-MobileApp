package shelftap.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * How the app navigates at the current window size (plan §6.4, Material 3 window size classes). The window, not the
 * device: a phone in landscape, a split screen and a folded-open phone all land here by their size.
 * - [COMPACT] (under 600 dp wide): the ☰ menu and the floating checkout button.
 * - [RAIL] (600 to 840 dp): a vertical rail on the left, one pane at a time, content at most 528 dp wide.
 * - [SIDE] (840 dp and up): a fixed side menu on the left, and the catalog as list and detail side by side.
 *   A window 840 to 900 dp wide, or 480 dp high or less (a phone on its side), keeps the compact menu instead:
 *   a side menu would take too much of it.
 */
enum class NavLayout { COMPACT, RAIL, SIDE }

fun navLayoutFor(widthDp: Int, heightDp: Int): NavLayout = when {
    widthDp < 600 -> NavLayout.COMPACT
    widthDp < 840 -> NavLayout.RAIL
    widthDp < 900 || heightDp <= 480 -> NavLayout.COMPACT
    else -> NavLayout.SIDE
}

@Composable
fun rememberNavLayout(): NavLayout {
    val config = LocalConfiguration.current
    return navLayoutFor(config.screenWidthDp, config.screenHeightDp)
}
