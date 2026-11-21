package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import vn.shelftap.app.appRepository
import vn.shelftap.app.data.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(onOpen: (String) -> Unit) {
    val repository = appRepository()
    val viewModel: CatalogueViewModel = viewModel(
        factory = viewModelFactory { initializer { CatalogueViewModel(repository) } },
    )
    val titles = viewModel.titles.collectAsLazyPagingItems()
    val sync by viewModel.sync.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Catalogue") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = sync) {
                is SyncStatus.Running -> SyncBannerText(
                    if (s.total == 0) "Downloading catalogue..."
                    else "Downloading catalogue: ${s.loaded} of ${s.total}",
                )
                SyncStatus.Failed -> SyncBannerText(
                    "Could not update the catalogue. Showing saved titles.",
                )
                SyncStatus.Idle, SyncStatus.Done -> Unit
            }
            TitleList(
                items = titles,
                onOpen = onOpen,
                modifier = Modifier.weight(1f),
                emptyContent = {
                    Text(
                        text = emptyCatalogueMessage(sync),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
            )
        }
    }
}

private fun emptyCatalogueMessage(sync: SyncStatus): String = when (sync) {
    SyncStatus.Idle, is SyncStatus.Running -> "Downloading the catalogue..."
    SyncStatus.Failed -> "No titles saved yet. Connect to the network and reopen the app to download the catalogue."
    SyncStatus.Done -> "The catalogue is empty."
}
