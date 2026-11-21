package vn.shelftap.app.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.local.ShelfTapDatabase
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.data.sync.SyncStatus
import vn.shelftap.app.model.Title

class CatalogueRepository(
    private val database: ShelfTapDatabase,
    private val apiService: ShelfTapApiService
) {
    private val titleDao = database.titleDao()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus = _syncStatus.asStateFlow()

    fun getLocalTitleCount(): Flow<Int> = titleDao.getTitleCountFlow()

    @OptIn(ExperimentalPagingApi::class)
    fun getTitles(query: String): Flow<PagingData<Title>> {
        val trimmedQuery = query.trim()
        val pagingConfig = PagingConfig(
            pageSize = 50,
            prefetchDistance = 20,
            enablePlaceholders = false,
            initialLoadSize = 50
        )

        return if (trimmedQuery.isEmpty()) {
            // When browsing all titles, use RemoteMediator with Room as single source of truth.
            // Works seamlessly offline if already cached!
            Pager(
                config = pagingConfig,
                remoteMediator = CatalogueRemoteMediator(
                    query = null,
                    database = database,
                    apiService = apiService
                ),
                pagingSourceFactory = { titleDao.getAllTitlesPagingSource() }
            ).flow.map { pagingData ->
                pagingData.map { it.toDomainModel() }
            }.flowOn(Dispatchers.IO)
        } else {
            // When searching, search the local database directly.
            // Matches title, author, ISBN, and subject offline!
            // Also triggers RemoteMediator search if network is available.
            Pager(
                config = pagingConfig,
                remoteMediator = CatalogueRemoteMediator(
                    query = trimmedQuery,
                    database = database,
                    apiService = apiService
                ),
                pagingSourceFactory = {
                    titleDao.searchTitlesPagingSource(trimmedQuery.lowercase())
                }
            ).flow.map { pagingData ->
                pagingData.map { it.toDomainModel() }
            }.flowOn(Dispatchers.IO)
        }
    }

    fun getTitleById(id: String): Flow<Title?> {
        return titleDao.getTitleById(id)
            .map { it?.toDomainModel() }
            .flowOn(Dispatchers.IO)
    }

    suspend fun refreshTitleById(id: String) {
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getTitleById(id)
                if (response.isSuccessful) {
                    response.body()?.let { dto ->
                        titleDao.insertAll(listOf(TitleEntity.fromDomain(dto.toDomainModel())))
                    }
                }
            } catch (ignored: Exception) {
                // Ignore offline errors on refresh
            }
        }
    }

    /**
     * Downloads full catalogue in batches for complete offline usage.
     * Essential for library members on the shelves where network connectivity is unavailable.
     */
    suspend fun syncEntireCatalogue(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.Syncing(0, 0)
            var currentPage = 0
            val pageSize = 50
            var totalItems = 0
            var downloadedItems = 0

            do {
                val response = apiService.getTitles(page = currentPage, size = pageSize)
                if (!response.isSuccessful) {
                    val errorMsg = "HTTP error: ${response.code()}"
                    _syncStatus.value = SyncStatus.Error(errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val body = response.body() ?: break
                totalItems = body.total
                val entities = body.items.map { TitleEntity.fromDomain(it.toDomainModel()) }
                titleDao.insertAll(entities)

                downloadedItems += entities.size
                _syncStatus.value = SyncStatus.Syncing(downloadedItems, totalItems)

                currentPage++
                val hasMore = downloadedItems < totalItems && entities.isNotEmpty()
            } while (hasMore)

            _syncStatus.value = SyncStatus.Success(downloadedItems)
            Result.success(downloadedItems)
        } catch (e: Exception) {
            val localCount = titleDao.getTitleCount()
            if (localCount > 0) {
                _syncStatus.value = SyncStatus.Success(localCount)
            } else {
                _syncStatus.value = SyncStatus.Error(e.localizedMessage ?: "Unknown sync error")
            }
            Result.failure(e)
        }
    }
}
