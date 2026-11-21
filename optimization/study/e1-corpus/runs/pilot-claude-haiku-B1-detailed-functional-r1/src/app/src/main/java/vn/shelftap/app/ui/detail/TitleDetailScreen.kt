package vn.shelftap.app.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException
import vn.shelftap.app.R
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.db.TitleEntity
import vn.shelftap.app.ui.common.CoverImage
import vn.shelftap.app.ui.common.AvailabilityBadge
import vn.shelftap.app.ui.common.simpleFactory

sealed interface DetailState {
    data object Loading : DetailState
    data class Ready(val title: TitleEntity) : DetailState
    data object NotFound : DetailState
    data object Failed : DetailState
}

class TitleDetailViewModel(
    private val repository: CatalogueRepository,
    private val titleId: String,
) : ViewModel() {
    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = DetailState.Loading
            _state.value = try {
                repository.title(titleId)?.let { DetailState.Ready(it) } ?: DetailState.NotFound
            } catch (e: IOException) {
                DetailState.Failed
            } catch (e: SerializationException) {
                DetailState.Failed
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleDetailScreen(
    repository: CatalogueRepository,
    titleId: String,
    onBack: () -> Unit,
) {
    val viewModel = viewModel<TitleDetailViewModel>(
        key = "title-$titleId",
        factory = simpleFactory { TitleDetailViewModel(repository, titleId) },
    )
    val state by viewModel.state.collectAsState()
    val heading = (state as? DetailState.Ready)?.title?.title.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(heading, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val current = state) {
            DetailState.Loading -> Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            DetailState.NotFound -> CenteredMessage(
                modifier = modifier,
                message = stringResource(R.string.title_not_found),
            )
            DetailState.Failed -> CenteredMessage(
                modifier = modifier,
                message = stringResource(R.string.title_load_error),
            )
            is DetailState.Ready -> TitleDetailContent(title = current.title, modifier = modifier)
        }
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier, message: String) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleDetailContent(title: TitleEntity, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverImage(
            coverUrl = title.coverUrl,
            contentDescription = title.title,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = title.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (title.authors.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = title.authors.joinToString(", "),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        AvailabilityBadge(copiesAvailable = title.copiesAvailable)
        Spacer(Modifier.height(20.dp))

        DetailField(label = stringResource(R.string.label_year), value = title.year.toString())
        DetailField(label = stringResource(R.string.label_isbn), value = title.isbn)
        DetailField(label = stringResource(R.string.label_copies), value = title.copiesAvailable.toString())

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.label_subjects),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        if (title.subjects.isEmpty()) {
            Text(
                text = stringResource(R.string.no_subjects),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
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
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.6f),
        )
    }
}
