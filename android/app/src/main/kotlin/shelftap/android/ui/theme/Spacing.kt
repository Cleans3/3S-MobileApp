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

/** Corner radii of the glass design (plan section 5.5): small tile, card, banner and menu panel. */
object Radius {
    val tile = 14.dp
    val cover = 16.dp
    val card = 22.dp
    val banner = 28.dp
    val menu = 30.dp
    val hairline = 1.dp
}

/** Fixed sizes of the glass components, from the mockup. */
object GlassSize {
    val round = 48.dp
    val search = 52.dp
    val bannerHeight = 200.dp
    val bannerCoverWidth = 104.dp
    val bannerCoverHeight = 150.dp
    val cardWidth = 132.dp
    val cardHeight = 196.dp
    val scrimHeight = 110.dp
    val shadow = 8.dp
    val cardInset = 6.dp
    val panelInset = 10.dp
    val arrow = 44.dp
    val arrowIcon = 18.dp
    /** The tile behind a menu entry's icon, and the icon in it. */
    val tile = 36.dp
    val tileIcon = 20.dp
}
