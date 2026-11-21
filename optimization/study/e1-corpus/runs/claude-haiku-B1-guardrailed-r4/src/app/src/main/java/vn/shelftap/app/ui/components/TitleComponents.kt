package vn.shelftap.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import vn.shelftap.app.R
import vn.shelftap.app.model.Title

/** Lazy, keyed list of titles backed by Paging. Only visible rows are composed. */
@Composable
fun TitleList(
    pagingItems: LazyPagingItems<Title>,
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = pagingItems.itemContentType { "title" },
        ) { index ->
            val title = pagingItems[index]
            if (title != null) {
                TitleRow(title = title, onClick = { onTitleClick(title.id) })
            } else {
                TitleRowPlaceholder()
            }
        }
    }
}

@Composable
fun TitleRow(
    title: Title,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val authors = remember(title.authors) { title.authors.joinToString(", ") }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverUrl = title.coverUrl,
            contentDescription = stringResource(R.string.cover_description, title.title),
            modifier = Modifier.width(64.dp),
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
                text = authors,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (title.year > 0) {
                    stringResource(R.string.year_label, title.year)
                } else {
                    stringResource(R.string.year_unknown)
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
            AvailabilityBadge(copiesAvailable = title.copiesAvailable)
        }
    }
}

/** Fixed-height stand-in shown for a row that has not loaded yet. Never zero height. */
@Composable
fun TitleRowPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(120.dp))
}

/** 2:3 cover. The aspect ratio reserves the space before the image arrives. */
@Composable
fun CoverImage(
    coverUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = coverUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
fun AvailabilityBadge(copiesAvailable: Int) {
    val available = copiesAvailable > 0
    Surface(
        color = if (available) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = stringResource(if (available) R.string.badge_available else R.string.badge_all_out),
            style = MaterialTheme.typography.labelMedium,
            color = if (available) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
