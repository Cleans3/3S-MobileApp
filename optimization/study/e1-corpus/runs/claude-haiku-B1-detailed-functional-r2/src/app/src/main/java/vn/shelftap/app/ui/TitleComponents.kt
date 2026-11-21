package vn.shelftap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
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
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import vn.shelftap.app.R
import vn.shelftap.app.model.Title

/** Scrollable list of titles from paged data. Each row opens the detail screen. */
@Composable
fun TitleList(
    paged: LazyPagingItems<Title>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(count = paged.itemCount, key = paged.itemKey { it.id }) { index ->
            paged[index]?.let { title ->
                TitleRow(title = title, onClick = { onOpen(title.id) })
            }
        }
    }
}

@Composable
fun TitleRow(title: Title, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(title.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(summaryLine(title), maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            BookCover(
                url = title.coverUrl,
                title = title.title,
                modifier = Modifier.size(width = 48.dp, height = 72.dp),
            )
        },
        trailingContent = {
            AvailabilityBadge(available = title.isAvailable)
        },
    )
}

/** Authors and year on one line, for example "Jane Doe, John Roe · 1999". */
private fun summaryLine(title: Title): String {
    val authors = title.authors.joinToString(", ")
    val year = if (title.year > 0) title.year.toString() else ""
    return listOf(authors, year).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Cover image, or the first letter of the title when there is no cover URL. */
@Composable
fun BookCover(url: String?, title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.cover_description, title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = title.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AvailabilityBadge(available: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = if (available) scheme.primaryContainer else scheme.errorContainer,
    ) {
        Text(
            text = stringResource(if (available) R.string.available else R.string.all_copies_out),
            style = MaterialTheme.typography.labelMedium,
            color = if (available) scheme.onPrimaryContainer else scheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
