package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.AppGraph
import vn.shelftap.app.data.ShelfTapRepository
import vn.shelftap.app.data.TitleDetail

data class TitleUiState(
    val loading: Boolean = true,
    val detail: TitleDetail? = null,
    val error: String? = null,
)

class TitleViewModel(
    private val repo: ShelfTapRepository,
    private val titleId: String,
) : ViewModel() {
    private val _state = MutableStateFlow(TitleUiState())
    val state: StateFlow<TitleUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { TitleUiState(loading = true) }
            try {
                val detail = repo.loadTitle(titleId)
                _state.update { TitleUiState(loading = false, detail = detail) }
            } catch (e: Exception) {
                _state.update { TitleUiState(loading = false, error = describeError(e)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    titleId: String,
    onBack: () -> Unit,
) {
    val vm: TitleViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TitleViewModel(AppGraph.repository, titleId) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        val detail = state.detail
        when {
            detail != null -> Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                detail.coverUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Cover of ${detail.title}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(width = 180.dp, height = 260.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (detail.authors.isNotEmpty()) {
                    Text(
                        text = "By ${detail.authors.joinToString()}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (detail.subjects.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Subjects: ${detail.subjects.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Copies available: ${detail.copiesAvailable} of ${detail.copiesTotal}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                detail.isbn?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ISBN $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.loading -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            else -> Box(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.error ?: "This book could not be loaded.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
