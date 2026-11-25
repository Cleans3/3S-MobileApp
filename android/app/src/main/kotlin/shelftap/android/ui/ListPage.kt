package shelftap.android.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The catalog's list (all books, or the results of a search) is a page of its own in the mockup: the top bar's menu button turns into a back
 * button and the bar shows the page's name. The list publishes itself here ([LocalListPage]) while it is on screen; the bar reads it, and a tap
 * on the back button calls [onBack].
 */
class ListPage(@StringRes val title: Int, val onBack: () -> Unit)

val LocalListPage = staticCompositionLocalOf<MutableState<ListPage?>?> { null }
