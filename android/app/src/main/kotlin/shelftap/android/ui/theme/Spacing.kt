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
    /** The soft shadow under the panel (the mockup: 0 24px 60px). */
    val shadow = 16.dp
    /** The blur of the page right behind the panel, and of the whole page behind the scrim (CSS blur(26px) and blur(3px)). */
    val backdropBlur = 26.dp
    val pageBlur = 3.dp
    /** The wide menu floats this far from the window's edges. */
    val sideInset = 16.dp
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

/** How far past the visible part a lazy list prepares its items, so composing a card happens before it scrolls into view and not in the frame that needs it. */
object ScrollSize {
    val cacheAhead = 600.dp
    val cacheBehind = 200.dp
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
    /** A loan's or reservation's card (26 in the board) and a figure tile above them (20). */
    val panel = 26.dp
    val stat = 20.dp
    /** The card that says how a self-checkout went (32 in the board). */
    val result = 32.dp
    val banner = 28.dp
    val menu = 30.dp
    val hairline = 1.dp
    val group = 24.dp
    val sheet = 34.dp
    /** The spine side of a jacket: nearly square, as in the board. */
    val spine = 10.dp
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
    val menuIcon = 20.dp
    /** The side margin of a home screen in the mockup (20, where ordinary screens use 16). */
    val page = 20.dp
    /** The circle that carries the NFC glyph in the floating button, and the glyph. */
    val pillCircle = 40.dp
    val pillGlyph = 20.dp
    /** The cover on a loan's card. */
    val loanCoverWidth = 72.dp
    val loanCoverHeight = 104.dp
    val renewButton = 44.dp
    /** A full-width pill action on a result card. */
    val pillAction = 52.dp
}

/** The self-checkout screen, from the board `Checkout`: the disc that waits for a tag, the disc inside it, the tick badge. */
object NfcSize {
    val disc = 280.dp
    val inner = 200.dp
    val badge = 64.dp
    val badgeIcon = 30.dp
    val step = 24.dp
}

/** The login and the card login, from the boards `Login` and `CardLogin`. */
object LoginSize {
    val backdrop = 360.dp
    val formTop = 250.dp
    val button = 54.dp
    val field = 50.dp
    val stage = 300.dp
    val disc = 220.dp
    val cardWidth = 168.dp
    val cardHeight = 106.dp
}

/** Sizes on the account screen, from the board. */
object AccountSize {
    val avatar = 56.dp
    val row = 56.dp
    val rowTall = 64.dp
    val action = 54.dp
    val segment = 48.dp
}

/** Sizes on the book page, from the board `Detail`. */
object HeroSize {
    val height = 448.dp
    val fade = 200.dp
    val overlap = 32.dp
    val contentInset = 24.dp
    val jacketWidth = 200.dp
    val jacketHeight = 296.dp
    val jacketTop = 104.dp
    val jacketShadow = 24.dp
    val rule = 48.dp
    val ruleThickness = 3.dp
    val barButton = 52.dp
    /** Room under the page for the floating bar. */
    val barClearance = 112.dp
}
