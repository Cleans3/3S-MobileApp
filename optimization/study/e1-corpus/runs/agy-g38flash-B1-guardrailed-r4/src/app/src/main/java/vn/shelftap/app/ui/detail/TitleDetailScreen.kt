package vn.shelftap.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import vn.shelftap.app.R
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.ui.catalogue.AvailabilityBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleDetailScreen(
    titleId: String,
    viewModel: TitleDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(titleId) {
        viewModel.loadTitle(titleId)
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    // Rule 20: Icon-only back button has meaningful contentDescription
                    // Rule 21: Minimum touch target size 48x48dp
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            DetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            DetailUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_books_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is DetailUiState.Success -> {
                TitleDetailContent(
                    title = state.title,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun TitleDetailContent(
    title: Title,
    modifier: Modifier = Modifier
) {
    // Rule 14: Formatting inside remember
    val authorsText = remember(title.authors) { title.authorsFormatted }
    val yearText = remember(title.year) { title.year }

    // Rule 13: Lazy layout for unbounded scrollable content
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large cover image (600x900 JPEG -> 2:3 aspect ratio reserved space)
        item(key = "cover_image") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (title.coverUrl != null) {
                    // Rule 15: Coil AsyncImage with constraints and cache
                    AsyncImage(
                        model = title.coverUrl,
                        contentDescription = stringResource(R.string.cd_book_cover, title.title),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "📖",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
        }

        // Title
        item(key = "title_text") {
            Text(
                text = title.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Authors
        item(key = "authors_text") {
            Text(
                text = stringResource(R.string.detail_authors, authorsText),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Year & ISBN
        item(key = "year_and_isbn") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.detail_year, yearText),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.detail_isbn, title.isbn),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Copies available & Badge
        item(key = "availability") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.detail_copies, title.copiesAvailable),
                    style = MaterialTheme.typography.bodyLarge
                )
                AvailabilityBadge(copiesAvailable = title.copiesAvailable)
            }
        }

        // Subjects heading and chips
        if (title.subjects.isNotEmpty()) {
            item(key = "subjects_heading") {
                Text(
                    text = stringResource(R.string.detail_subjects),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Rule 11 & 21: LazyRow with stable keys and minimum 48dp touch targets
            item(key = "subjects_row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(title.subjects, key = { it }) { subject ->
                        SuggestionChip(
                            onClick = { /* Informational chip */ },
                            label = { Text(subject) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
        }
    }
}
