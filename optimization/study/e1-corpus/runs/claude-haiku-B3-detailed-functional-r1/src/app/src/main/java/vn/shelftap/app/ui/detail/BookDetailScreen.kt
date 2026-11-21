package vn.shelftap.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import vn.shelftap.app.ShelfTapApp

@Composable
fun BookDetailScreen(titleId: String, onBack: () -> Unit) {
    val container = (LocalContext.current.applicationContext as ShelfTapApp).container
    val vm: BookDetailViewModel = viewModel(
        key = titleId,
        factory = viewModelFactory { initializer { BookDetailViewModel(titleId, container.library) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Book", style = MaterialTheme.typography.titleLarge)
        }

        val book = state.book
        when {
            state.loading && book == null -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            book == null -> Text(
                text = state.error ?: "Book not found.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = "Cover of ${book.title}",
                    modifier = Modifier
                        .size(width = 180.dp, height = 260.dp)
                        .align(Alignment.CenterHorizontally),
                )
                Text(book.title, style = MaterialTheme.typography.headlineSmall)
                if (book.authors.isNotEmpty()) {
                    Text(
                        "By ${book.authors.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (book.subjects.isNotEmpty()) {
                    Text("Subjects", style = MaterialTheme.typography.titleSmall)
                    Text(book.subjects.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Copies available: ${book.copiesAvailable} of ${book.copiesTotal}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
