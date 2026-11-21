package vn.shelftap.app.ui.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.SyncStatus
import vn.shelftap.app.domain.Title

class CatalogueViewModel(private val repository: CatalogueRepository) : ViewModel() {

    val titles: Flow<PagingData<Title>> = repository.catalogue().cachedIn(viewModelScope)

    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus

    init {
        repository.startSync()
    }

    fun retrySync() {
        repository.startSync()
    }

    companion object {
        fun factory(repository: CatalogueRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { CatalogueViewModel(repository) }
            }
    }
}
