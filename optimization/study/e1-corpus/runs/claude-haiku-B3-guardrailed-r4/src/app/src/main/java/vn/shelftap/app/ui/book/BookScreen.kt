package vn.shelftap.app.ui.book

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.shelftap.app.R
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.ui.common.CenteredProgress
import vn.shelftap.app.ui.common.MessageWithRetry
import vn.shelftap.app.ui.common.messageRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(titleId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as ShelfTapApp
    val viewModel: BookViewModel = viewModel(
        key = "book-$titleId",
        factory = viewModelFactory {
            initializer { BookViewModel(app.container.libraryRepository, titleId) }
        },
    )
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val book = state.book
        val error = state.error
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                book != null -> BookContent(book)
                error != null -> MessageWithRetry(
                    message = stringResource(error.messageRes()),
                    onRetry = viewModel::load,
                )
                else -> CenteredProgress()
            }
        }
    }
}

@Composable
private fun BookContent(book: BookDetails) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "cover", contentType = "cover") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = stringResource(R.string.cover_of, book.title),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(160.dp).aspectRatio(2f / 3f),
                )
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (book.authors.isNotEmpty()) {
            item(key = "authors", contentType = "text") {
                LabeledText(stringResource(R.string.authors_label), book.authors)
            }
        }
        if (book.subjects.isNotEmpty()) {
            item(key = "subjects", contentType = "text") {
                LabeledText(stringResource(R.string.subjects_label), book.subjects)
            }
        }
        item(key = "copies", contentType = "text") {
            Text(
                text = stringResource(R.string.copies_available, book.copiesAvailable, book.copiesTotal),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun LabeledText(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
