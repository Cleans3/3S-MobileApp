package vn.shelftap.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import vn.shelftap.app.R
import vn.shelftap.app.data.db.TitleEntity

/** Builds a ViewModelProvider.Factory from a lambda, so ViewModels can take constructor arguments. */
fun <T : ViewModel> simpleFactory(build: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = build() as VM
    }

/** Cover image; shows a placeholder when the title has no cover URL. */
@Composable
fun CoverImage(coverUrl: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (coverUrl.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.no_cover),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                model = coverUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun AvailabilityBadge(copiesAvailable: Int, modifier: Modifier = Modifier) {
    val available = copiesAvailable > 0
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (available) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (available) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = stringResource(if (available) R.string.badge_available else R.string.badge_all_out),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun TitleRow(title: TitleEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverUrl = title.coverUrl,
            contentDescription = title.title,
            modifier = Modifier
                .size(width = 60.dp, height = 90.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (title.authors.isNotEmpty()) {
                Text(
                    text = title.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(text = title.year.toString(), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            AvailabilityBadge(copiesAvailable = title.copiesAvailable)
        }
    }
}

/** Scrollable list of titles from a paging source. Shows [emptyMessage] when there are no titles. */
@Composable
fun TitleList(
    paging: LazyPagingItems<TitleEntity>,
    listState: LazyListState,
    emptyMessage: String,
    onOpenTitle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (paging.itemCount == 0) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (paging.loadState.refresh is LoadState.Loading) {
                CircularProgressIndicator()
            } else {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        return
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        items(count = paging.itemCount, key = paging.itemKey { it.id }) { index ->
            val item = paging[index]
            if (item != null) {
                TitleRow(title = item, onClick = { onOpenTitle(item.id) })
                HorizontalDivider()
            }
        }
    }
}
