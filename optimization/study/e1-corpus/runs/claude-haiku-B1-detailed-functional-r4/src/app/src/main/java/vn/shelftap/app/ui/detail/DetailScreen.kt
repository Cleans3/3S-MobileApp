package vn.shelftap.app.ui.detail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.TitleEntity
import vn.shelftap.app.ui.components.AvailabilityBadge
import vn.shelftap.app.ui.components.CoverImage

class DetailViewModel(
    private val repository: CatalogueRepository,
    titleId: String,
) : ViewModel() {

    val title: StateFlow<TitleEntity?> = repository.observeTitle(titleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Opportunistic refresh. When offline the local copy stays on screen.
        viewModelScope.launch {
            try {
                repository.refreshTitle(titleId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the downloaded copy.
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    repository: CatalogueRepository,
    titleId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<DetailViewModel>(
        key = "detail-$titleId",
        factory = viewModelFactory { initializer { DetailViewModel(repository, titleId) } },
    )
    val title by viewModel.title.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Title details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = title
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "This title is not in the downloaded catalogue.",
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CoverImage(
                    coverUrl = current.coverUrl,
                    title = current.title,
                    modifier = Modifier
                        .width(240.dp)
                        .aspectRatio(2f / 3f),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = current.authors.joinToString(", ").ifBlank { "Unknown author" },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                AvailabilityBadge(copiesAvailable = current.copiesAvailable)
                Spacer(Modifier.height(16.dp))

                DetailField(label = "Catalogue ID", value = current.id)
                DetailField(label = "ISBN", value = current.isbn.ifBlank { "Not recorded" })
                DetailField(
                    label = "Year",
                    value = if (current.year > 0) current.year.toString() else "Unknown",
                )
                DetailField(label = "Authors", value = current.authors.joinToString(", ").ifBlank { "Unknown" })
                DetailField(label = "Copies available", value = current.copiesAvailable.toString())

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Subjects",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (current.subjects.isEmpty()) {
                    Text(
                        text = "No subjects listed",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        current.subjects.forEach { subject ->
                            AssistChip(
                                onClick = {},
                                label = { Text(subject) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
