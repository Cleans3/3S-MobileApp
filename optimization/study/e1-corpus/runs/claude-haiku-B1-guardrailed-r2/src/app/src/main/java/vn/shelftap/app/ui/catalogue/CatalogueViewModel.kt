package vn.shelftap.app.ui.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.SyncState
import vn.shelftap.app.data.TitleEntity

class CatalogueViewModel(private val repository: CatalogueRepository) : ViewModel() {

    val titles: Flow<PagingData<TitleEntity>> = Pager(
        config = PagingConfig(
            pageSize = CatalogueRepository.PAGE_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { repository.pagingAll() }
    ).flow.cachedIn(viewModelScope)

    val syncState: StateFlow<SyncState> = repository.syncState

    fun retrySync() {
        repository.startSync()
    }
}
