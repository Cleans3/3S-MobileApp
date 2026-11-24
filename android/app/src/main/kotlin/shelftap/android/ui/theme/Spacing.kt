package shelftap.android.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing tokens (plan §5.1). Screens use these, not `dp` literals. */
object Spacing {
    val none = 0.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp

    /** Smallest touch target (WCAG 2.2, Android: 48 dp). */
    val touch = 48.dp

    /** Room kept at the bottom of a main screen for the floating "Tap a book to borrow" button, so no content sits behind it. */
    val pillClearance = 88.dp
}

/** Fixed cover sizes, so the layout does not jump when an image arrives. */
object CoverSize {
    val rowWidth = 48.dp
    val rowHeight = 72.dp
    val detailWidth = 120.dp
    val detailHeight = 180.dp
}

/** The ☰ menu panel and its glyph. */
object MenuSize {
    val width = 240.dp
    val icon = 24.dp
}

/** Sizes for the wider windows (plan §6.4). */
object AdaptiveSize {
    val railWidth = 96.dp
    val sideWidth = 216.dp

    /** One pane of content never grows wider than this on a medium window, so lines stay readable. */
    val contentMax = 528.dp

    /** On a wide window, screens other than the catalog are not stretched past this. */
    val contentMaxWide = 720.dp
}

/** Icon sizes: next to text, and the large one that shows a result. */
object IconSize {
    val small = 24.dp
    val hero = 72.dp
}
