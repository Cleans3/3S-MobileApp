package vn.shelftap.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.shelftap.app.R
import vn.shelftap.app.model.Title
import vn.shelftap.app.ui.components.AvailabilityBadge
import vn.shelftap.app.ui.components.CoverImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleDetailScreen(
    onBack: () -> Unit,
    viewModel: TitleDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_heading)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding).fillMaxSize()
        when (val current = state) {
            DetailUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            DetailUiState.NotFound -> Box(contentModifier, contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.title_not_found), modifier = Modifier.padding(24.dp))
            }

            DetailUiState.Unavailable -> Box(contentModifier, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.title_unavailable), modifier = Modifier.padding(24.dp))
                    TextButton(onClick = viewModel::load) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            is DetailUiState.Loaded -> TitleDetailContent(current.title, contentModifier)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleDetailContent(title: Title, modifier: Modifier = Modifier) {
    val unknown = stringResource(R.string.unknown)
    val authors = remember(title.authors) { title.authors.joinToString(", ") }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        CoverImage(
            coverUrl = title.coverUrl,
            contentDescription = stringResource(R.string.cover_description, title.title),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(16.dp))
        Text(title.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        DetailField(stringResource(R.string.field_authors), authors.ifEmpty { unknown })
        DetailField(
            stringResource(R.string.field_year),
            if (title.year > 0) title.year.toString() else unknown,
        )
        DetailField(stringResource(R.string.field_isbn), title.isbn.ifEmpty { unknown })
        DetailField(stringResource(R.string.field_copies), title.copiesAvailable.toString())
        Spacer(Modifier.height(8.dp))
        AvailabilityBadge(copiesAvailable = title.copiesAvailable)
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.field_subjects), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (title.subjects.isEmpty()) {
            Text(stringResource(R.string.no_subjects), style = MaterialTheme.typography.bodyMedium)
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

@Composable
private fun SubjectChip(subject: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = subject,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
