package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.domain.Title

sealed interface TitleDetailState {
    data object Loading : TitleDetailState
    data object Missing : TitleDetailState
    data class Found(val title: Title) : TitleDetailState
}

private const val STOP_TIMEOUT_MS = 5_000L

class TitleDetailViewModel(
    repository: CatalogueRepository,
    private val titleId: String,
) : ViewModel() {

    private val refreshFinished = MutableStateFlow(false)

    /**
     * Shows the local copy as soon as it exists. Missing is only reported after the network
     * refresh has finished, so a title that is only on the server does not flash as missing.
     */
    val state: StateFlow<TitleDetailState> = combine(
        repository.observeTitle(titleId),
        refreshFinished,
    ) { title, finished ->
        when {
            title != null -> TitleDetailState.Found(title)
            finished -> TitleDetailState.Missing
            else -> TitleDetailState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TitleDetailState.Loading)

    init {
        viewModelScope.launch {
            repository.refreshTitle(titleId)
            refreshFinished.value = true
        }
    }

    companion object {
        fun factory(repository: CatalogueRepository, titleId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TitleDetailViewModel(repository, titleId) }
            }
    }
}
