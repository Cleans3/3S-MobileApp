package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.model.Title

class TitleDetailViewModel(
    private val titleId: String,
    private val repository: CatalogueRepository
) : ViewModel() {

    val title: StateFlow<Title?> = repository.getTitleById(titleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.refreshTitleById(titleId)
        }
    }

    companion object {
        fun provideFactory(
            titleId: String,
            repository: CatalogueRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TitleDetailViewModel(titleId, repository) as T
                }
            }
    }
}
