package vn.shelftap.app.ui.book

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import vn.shelftap.app.data.Title
import vn.shelftap.app.shelfRepository
import vn.shelftap.app.ui.common.ErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(titleId: String, onBack: () -> Unit) {
    val repo = LocalContext.current.shelfRepository()
    val vm: BookViewModel = viewModel(
        key = titleId,
        factory = viewModelFactory { initializer { BookViewModel(repo, titleId) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            val book = state.title
            when {
                book != null -> BookContent(book)
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> ErrorState(
                    message = state.error.orEmpty(),
                    onRetry = vm::load,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun BookContent(book: Title) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = book.coverUrl,
            contentDescription = "Cover of ${book.title}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.width(180.dp).aspectRatio(2f / 3f),
        )
        Text(
            book.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (book.authors.isNotEmpty()) {
            Text(
                "by ${book.authors.joinToString(", ")}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        if (book.subjects.isNotEmpty()) {
            Text(
                "Subjects: ${book.subjects.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Copies available: ${book.copiesAvailable} of ${book.copiesTotal}",
            style = MaterialTheme.typography.titleMedium,
        )
        book.isbn?.let {
            Text("ISBN $it", style = MaterialTheme.typography.bodySmall)
        }
    }
}
