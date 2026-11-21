package vn.shelftap.app.ui.book

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import vn.shelftap.app.R
import vn.shelftap.app.ui.LoadingBox
import vn.shelftap.app.ui.ProblemText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(state: BookUiState, onBack: () -> Unit) {
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
        ) {
            val problem = state.problem
            val detail = state.detail
            when {
                state.loading -> LoadingBox()
                problem != null -> ProblemText(problem)
                detail != null -> BookBody(detail)
            }
        }
    }
}

@Composable
private fun BookBody(detail: BookDetail) {
    AsyncImage(
        model = detail.coverUrl,
        contentDescription = stringResource(R.string.cover_of, detail.title),
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(width = 150.dp, height = 220.dp),
    )
    Text(
        text = detail.title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        text = stringResource(
            R.string.authors_label,
            detail.authors.ifEmpty { stringResource(R.string.not_listed) },
        ),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 12.dp),
    )
    Text(
        text = stringResource(
            R.string.subjects_label,
            detail.subjects.ifEmpty { stringResource(R.string.not_listed) },
        ),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
    Text(
        text = stringResource(
            R.string.copies_available,
            detail.copiesAvailable,
            detail.copiesTotal,
        ),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
}
