package shelftap.android.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing tokens (plan §5.1). Screens use these, not `dp` literals. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp

    /** Smallest touch target (WCAG 2.2, Android: 48 dp). */
    val touch = 48.dp
}

/** Fixed cover sizes, so the layout does not jump when an image arrives. */
object CoverSize {
    val rowWidth = 48.dp
    val rowHeight = 72.dp
    val detailWidth = 120.dp
    val detailHeight = 180.dp
}
