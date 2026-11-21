@file:OptIn(ExperimentalMaterial3Api::class)

package vn.shelftap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.items
import vn.shelftap.app.model.Title

/** Cover image; shows a plain placeholder when there is no URL or it is not loaded yet. */
@Composable
fun CoverImage(url: String?, description: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = "No cover",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AvailabilityBadge(available: Boolean) {
    val container = if (available) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.errorContainer
    val content = if (available) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onErrorContainer
    Surface(shape = RoundedCornerShape(50), color = container) {
        Text(
            text = if (available) "Available" else "All copies out",
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun TitleRow(title: Title, onClick: () -> Unit) {
    val details = listOfNotNull(
        title.authors.joinToString(", ").takeIf { it.isNotBlank() },
        title.year.takeIf { it > 0 }?.toString(),
    ).joinToString(" · ")

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(title.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            if (details.isNotEmpty()) Text(details, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            CoverImage(
                url = title.coverUrl,
                description = title.title,
                modifier = Modifier.size(width = 56.dp, height = 84.dp),
            )
        },
        trailingContent = {
            AvailabilityBadge(available = title.isAvailable)
        },
    )
}

/** Scrollable list of paged titles, with a message when there is nothing to show. */
@Composable
fun TitleList(
    paging: LazyPagingItems<Title>,
    emptyMessage: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (paging.itemCount == 0) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (paging.loadState.refresh is LoadState.Loading) {
                CircularProgressIndicator()
            } else {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(count = paging.itemCount, key = paging.itemKey { it.id }) { index ->
            val title = paging[index]
            if (title != null) {
                TitleRow(title = title, onClick = { onOpen(title.id) })
                HorizontalDivider()
            }
        }
    }
}
