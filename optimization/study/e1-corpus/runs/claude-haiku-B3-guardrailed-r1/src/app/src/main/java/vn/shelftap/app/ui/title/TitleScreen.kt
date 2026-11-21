package vn.shelftap.app.ui.title

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import vn.shelftap.app.R
import vn.shelftap.app.api.TitleDetail
import vn.shelftap.app.ui.common.CenteredProgress
import vn.shelftap.app.ui.common.FailedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    viewModel: TitleViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.book_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val modifier = Modifier.fillMaxSize().padding(innerPadding)
        val detail = state.title
        when {
            detail != null -> TitleContent(detail = detail, modifier = modifier)
            state.failed -> FailedState(onRetry = viewModel::load, modifier = modifier)
            else -> CenteredProgress(modifier)
        }
    }
}

@Composable
private fun TitleContent(detail: TitleDetail, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "cover", contentType = "cover") {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // The fixed aspect ratio reserves the cover's space before the image loads.
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .aspectRatio(2f / 3f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (detail.coverUrl != null) {
                        AsyncImage(
                            model = detail.coverUrl,
                            contentDescription = stringResource(R.string.cover_of, detail.title),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        item(key = "title", contentType = "text") {
            Text(
                text = detail.title,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        if (detail.authors.isNotEmpty()) {
            item(key = "authors", contentType = "text") {
                Text(
                    text = stringResource(R.string.by_authors, detail.authors.joinToString(", ")),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (detail.subjects.isNotEmpty()) {
            item(key = "subjects", contentType = "text") {
                Text(
                    text = stringResource(R.string.subjects_list, detail.subjects.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item(key = "copies", contentType = "text") {
            Text(
                text = stringResource(R.string.copies_available, detail.copiesAvailable, detail.copiesTotal),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
