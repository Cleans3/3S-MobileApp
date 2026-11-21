@file:OptIn(ExperimentalMaterial3Api::class)

package vn.shelftap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.TitleDetail

data class TitleUiState(
    val title: TitleDetail? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class TitleViewModel(
    private val library: LibraryRepository,
    private val titleId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(TitleUiState())
    val state: StateFlow<TitleUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val detail = library.title(titleId)
                _state.update { it.copy(title = detail) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.userMessage()) }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }
}

@Composable
fun TitleDetailScreen(
    library: LibraryRepository,
    titleId: String,
    onBack: () -> Unit,
) {
    val viewModel: TitleViewModel = viewModel(
        key = "title-$titleId",
        factory = viewModelFactory { initializer { TitleViewModel(library, titleId) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.title?.title ?: "Book",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val detail = state.title
        when {
            detail != null -> TitleBody(detail, Modifier.padding(padding))
            state.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            else -> ErrorState(
                message = state.error ?: "Couldn't load this book.",
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun TitleBody(detail: TitleDetail, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Cover(detail)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(detail.title, style = MaterialTheme.typography.headlineSmall)
                if (detail.authors.isNotEmpty()) {
                    Text(
                        text = detail.authors.joinToString(", "),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = "${detail.copiesAvailable} of ${detail.copiesTotal} copies available",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (detail.subjects.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Subjects", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = detail.subjects.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        detail.isbn?.let {
            Text("ISBN $it", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Cover(detail: TitleDetail) {
    val url = detail.coverUrl
    Box(
        modifier = Modifier
            .size(120.dp, 180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            Text("No cover", style = MaterialTheme.typography.bodySmall)
        } else {
            AsyncImage(
                model = url,
                contentDescription = "Cover of ${detail.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
