package vn.shelftap.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import vn.shelftap.app.data.TitleEntity
import vn.shelftap.app.ui.common.AvailabilityBadge
import vn.shelftap.app.ui.common.CoverImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            DetailUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            DetailUiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.detail_load_failed),
                    textAlign = TextAlign.Center
                )
            }

            is DetailUiState.Loaded -> TitleDetailContent(
                title = current.title,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun TitleDetailContent(title: TitleEntity, modifier: Modifier = Modifier) {
    val authorLine = remember(title.authors) { title.authors.joinToString(", ") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "cover") {
            CoverImage(
                coverUrl = title.coverUrl,
                contentDescription = stringResource(R.string.cover_description, title.title),
                // A 2:3 ratio reserves the image space before the image loads.
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )
        }
        item(key = "title") {
            Text(text = title.title, style = MaterialTheme.typography.headlineSmall)
        }
        item(key = "authors") {
            LabeledValue(label = stringResource(R.string.label_authors), value = authorLine)
        }
        item(key = "year") {
            LabeledValue(
                label = stringResource(R.string.label_year),
                value = title.year.toString()
            )
        }
        item(key = "isbn") {
            LabeledValue(label = stringResource(R.string.label_isbn), value = title.isbn)
        }
        item(key = "copies") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledValue(
                    label = stringResource(R.string.label_copies),
                    value = title.copiesAvailable.toString()
                )
                AvailabilityBadge(copiesAvailable = title.copiesAvailable)
            }
        }
        item(key = "subjects_heading") {
            Text(
                text = stringResource(R.string.label_subjects),
                style = MaterialTheme.typography.titleSmall
            )
        }
        item(key = "subjects") {
            SubjectChips(subjects = title.subjects)
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SubjectChips(subjects: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        subjects.forEach { subject ->
            // Display only, not clickable, so it needs no 48dp touch target.
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
