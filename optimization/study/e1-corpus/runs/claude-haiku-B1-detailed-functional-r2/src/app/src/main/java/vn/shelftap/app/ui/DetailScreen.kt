package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import vn.shelftap.app.R
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.model.Title
import java.io.IOException

data class DetailUiState(
    val title: Title? = null,
    val loading: Boolean = true,
)

class DetailViewModel(
    private val repository: CatalogueRepository,
    private val titleId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Show the saved copy first, then try to refresh it when online.
            val cached = repository.getTitle(titleId)
            _state.value = DetailUiState(title = cached, loading = false)
            try {
                _state.value = DetailUiState(title = repository.refreshTitle(titleId), loading = false)
            } catch (e: IOException) {
                // Offline or server error: keep the saved copy.
            } catch (e: JSONException) {
                // Unexpected response: keep the saved copy.
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(repository: CatalogueRepository, titleId: String, onBack: () -> Unit) {
    val vm = viewModel<DetailViewModel>(
        key = titleId,
        factory = viewModelFactory { initializer { DetailViewModel(repository, titleId) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val title = state.title

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title?.title ?: stringResource(R.string.title_detail),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            title != null -> TitleDetailContent(title = title, modifier = Modifier.padding(padding))
            state.loading -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            else -> Box(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.title_not_found), textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleDetailContent(title: Title, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BookCover(
            url = title.coverUrl,
            title = title.title,
            modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(2f / 3f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        AvailabilityBadge(available = title.isAvailable)
        Spacer(Modifier.height(16.dp))

        Column(Modifier.fillMaxWidth()) {
            DetailField(
                label = stringResource(R.string.label_authors),
                value = title.authors.joinToString(", ").ifEmpty { "-" },
            )
            DetailField(
                label = stringResource(R.string.label_isbn),
                value = title.isbn.ifEmpty { "-" },
            )
            DetailField(
                label = stringResource(R.string.label_year),
                value = if (title.year > 0) title.year.toString() else "-",
            )
            DetailField(
                label = stringResource(R.string.label_copies),
                value = title.copiesAvailable.toString(),
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.label_subjects),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                title.subjects.forEach { subject ->
                    AssistChip(onClick = {}, label = { Text(subject) })
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
