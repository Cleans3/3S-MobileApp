package shelftap.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import shelftap.android.ui.theme.ScrollSize

/** A list state that prepares items ahead of the scroll ([ScrollSize]), so the frame that brings a card into view does not also have to compose it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberPreparedListState(): LazyListState =
    rememberLazyListState(cacheWindow = LazyLayoutCacheWindow(ahead = ScrollSize.cacheAhead, behind = ScrollSize.cacheBehind))
