package vn.shelftap.app.ui.book

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import vn.shelftap.app.R
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.ui.common.MessageNote
import vn.shelftap.app.ui.common.UiMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    library: LibraryRepository,
    titleId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<BookViewModel>(
        key = titleId,
        factory = viewModelFactory { initializer { BookViewModel(library, titleId) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Text(
                text = stringResource(R.string.book_details),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        val book = state.book
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            book == null -> {
                MessageNote(state.message ?: UiMessage.LOAD_FAILED)
                TextButton(onClick = viewModel::load) {
                    Text(stringResource(R.string.retry))
                }
            }
            else -> {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = stringResource(R.string.cover_description, book.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 160.dp, height = 240.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (book.authors.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.authors, book.authors),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (book.subjects.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.subjects, book.subjects),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.copies_available,
                        book.copiesAvailable,
                        book.copiesTotal,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
