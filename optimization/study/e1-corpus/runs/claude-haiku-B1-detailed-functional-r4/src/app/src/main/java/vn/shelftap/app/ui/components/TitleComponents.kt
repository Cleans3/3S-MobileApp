package vn.shelftap.app.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.SubcomposeAsyncImage
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.SyncState
import vn.shelftap.app.data.TitleEntity
import androidx.compose.runtime.collectAsState

/** Paged list of titles. Shows [emptyText] when the query has no results. */
@Composable
fun TitleList(
    pagingItems: LazyPagingItems<TitleEntity>,
    emptyText: String,
    onOpenTitle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.Loading
    val isEmpty = pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            isEmpty -> Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id },
                ) { index ->
                    val title = pagingItems[index]
                    if (title != null) {
                        TitleRow(title = title, onClick = { onOpenTitle(title.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun TitleRow(title: TitleEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverUrl = title.coverUrl,
            title = title.title,
            modifier = Modifier.size(width = 56.dp, height = 84.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title.authors.joinToString(", ").ifBlank { "Unknown author" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (title.year > 0) title.year.toString() else "Year unknown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            AvailabilityBadge(copiesAvailable = title.copiesAvailable)
        }
    }
}

@Composable
fun AvailabilityBadge(copiesAvailable: Int, modifier: Modifier = Modifier) {
    val available = copiesAvailable > 0
    val containerColor = if (available) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (available) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = if (available) "Available" else "All copies out",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Cover image with a letter placeholder while loading, on error, or when no cover exists. */
@Composable
fun CoverImage(coverUrl: String?, title: String, modifier: Modifier = Modifier) {
    if (coverUrl.isNullOrBlank()) {
        CoverPlaceholder(title = title, modifier = modifier)
    } else {
        SubcomposeAsyncImage(
            model = coverUrl,
            contentDescription = "Cover of $title",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(6.dp)),
            loading = { CoverPlaceholder(title = title, modifier = Modifier.fillMaxSize()) },
            error = { CoverPlaceholder(title = title, modifier = Modifier.fillMaxSize()) },
        )
    }
}

@Composable
private fun CoverPlaceholder(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Shows download progress, or a retry prompt if the download failed. Hidden when complete. */
@Composable
fun SyncBanner(repository: CatalogueRepository, modifier: Modifier = Modifier) {
    val state by repository.syncState.collectAsState()
    when (val current = state) {
        is SyncState.Running -> Column(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(12.dp),
        ) {
            val text = if (current.total > 0) {
                "Downloading catalogue: ${current.fetched} of ${current.total} titles"
            } else {
                "Downloading catalogue..."
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            if (current.total > 0) {
                LinearProgressIndicator(
                    progress = { current.fetched / current.total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        is SyncState.Failed -> Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${current.message} Titles already downloaded stay available offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { repository.startSync() }) {
                Text("Retry")
            }
        }

        SyncState.Idle, SyncState.Complete -> Unit
    }
}
