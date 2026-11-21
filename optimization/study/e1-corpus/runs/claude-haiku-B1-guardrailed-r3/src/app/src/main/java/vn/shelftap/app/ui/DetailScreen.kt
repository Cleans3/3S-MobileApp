package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.shelftap.app.R
import vn.shelftap.app.model.Title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(viewModel: DetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.detail_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
        )
        when (val current = state) {
            DetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            DetailUiState.NotFound -> CenteredMessage(stringResource(R.string.detail_not_found))
            DetailUiState.Unavailable -> CenteredMessage(stringResource(R.string.detail_unavailable))
            is DetailUiState.Loaded -> TitleDetails(current.title)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleDetails(title: Title) {
    val authorsText = remember(title.authors) { title.authors.joinToString(", ") }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Cover(
            url = title.coverUrl,
            contentDescription = stringResource(R.string.cover_description, title.title),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(2f / 3f)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(16.dp))
        Text(title.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.field_authors, authorsText), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.field_year, title.year), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.field_isbn, title.isbn), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.field_copies, title.copiesAvailable),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(8.dp))
        AvailabilityBadge(available = title.isAvailable)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.subjects_heading), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (title.subjects.isEmpty()) {
            Text(stringResource(R.string.subjects_none), style = MaterialTheme.typography.bodyMedium)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                title.subjects.forEach { subject -> SubjectChip(subject) }
            }
        }
    }
}

/** Static chip: subjects are shown, not clicked, so it is not a touch target. */
@Composable
private fun SubjectChip(subject: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = subject,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
    }
}
