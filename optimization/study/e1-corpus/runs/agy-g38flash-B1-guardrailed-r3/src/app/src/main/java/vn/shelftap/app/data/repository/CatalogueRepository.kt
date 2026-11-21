package vn.shelftap.app.data.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import vn.shelftap.app.domain.model.Title

sealed interface SyncState {
    data object Idle : SyncState
    data class Syncing(val downloadedCount: Int, val totalCount: Int) : SyncState
    data class Success(val totalTitles: Int) : SyncState
    data class Error(val message: String) : SyncState
}

interface CatalogueRepository {
    fun getTitlesPaged(query: String): Flow<PagingData<Title>>
    suspend fun getTitleById(id: String): Title?
    fun observeTitleById(id: String): Flow<Title?>
    fun getSyncState(): Flow<SyncState>
    suspend fun syncCatalogue(maxPages: Int = 100)
    suspend fun getLocalTitleCount(): Int
}
