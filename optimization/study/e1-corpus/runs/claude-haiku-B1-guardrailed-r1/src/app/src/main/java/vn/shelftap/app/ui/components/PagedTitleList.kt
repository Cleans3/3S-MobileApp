package vn.shelftap.app.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import vn.shelftap.app.domain.Title

/** Lazy, paged list of titles shared by the catalogue and search screens. */
@Composable
fun PagedTitleList(
    titles: LazyPagingItems<Title>,
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(
            count = titles.itemCount,
            key = titles.itemKey { it.id },
            contentType = titles.itemContentType { "title" },
        ) { index ->
            val title = titles[index]
            if (title != null) {
                TitleRow(title = title, onClick = { onTitleClick(title.id) })
                HorizontalDivider()
            }
        }
    }
}
