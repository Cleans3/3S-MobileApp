package shelftap.android.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import shelftap.android.ui.theme.Spacing

/**
 * The page is drawn edge to edge, under the status bar, the top bar and the floating button, so it can scroll beneath them (the glass
 * and the fades of the mockup need something behind them). These are the heights of what covers the page's top and bottom: the first
 * item must start below [top] and the last one end above [bottom] at rest, and scroll past them.
 */
@Immutable
data class PageInsets(val top: Dp = Spacing.none, val bottom: Dp = Spacing.none)

val LocalPageInsets = compositionLocalOf { PageInsets() }

/** Padding for the content of a scrolling page: its own [horizontal] and [vertical] margins plus the room the bars take. */
@Composable
fun pagePadding(horizontal: Dp = Spacing.none, vertical: Dp = Spacing.none): PaddingValues {
    val insets = LocalPageInsets.current
    return PaddingValues(start = horizontal, end = horizontal, top = insets.top + vertical, bottom = insets.bottom + vertical)
}
