package vn.shelftap.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.domain.model.Title

class CatalogueRepositoryImpl(
    private val database: AppDatabase,
    private val apiService: ShelfTapApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CatalogueRepository {

    private val titleDao = database.titleDao()
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)

    override fun getSyncState(): Flow<SyncState> = _syncState.asStateFlow()

    override fun getTitlesPaged(query: String): Flow<PagingData<Title>> {
        val trimmedQuery = query.trim()
        val pagingSourceFactory = if (trimmedQuery.isEmpty()) {
            { titleDao.getAllTitles() }
        } else {
            val ftsQuery = sanitizeFtsQuery(trimmedQuery)
            { titleDao.searchTitlesFts(ftsQuery) }
        }

        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 15,
                enablePlaceholders = false,
                initialLoadSize = 50
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
            .map { pagingData ->
                pagingData.map { it.toDomain() }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getTitleById(id: String): Title? = withContext(ioDispatcher) {
        val local = titleDao.getTitleById(id)
        if (local != null) {
            return@withContext local.toDomain()
        }

        // Try remote if missing locally and save to cache
        try {
            val remote = apiService.getTitleById(id)
            val entity = TitleEntity.fromDomain(remote.toDomain())
            titleDao.upsertTitles(listOf(entity))
            remote.toDomain()
        } catch (_: Exception) {
            null
        }
    }

    override fun observeTitleById(id: String): Flow<Title?> {
        return titleDao.observeTitleById(id)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override suspend fun getLocalTitleCount(): Int = withContext(ioDispatcher) {
        titleDao.getTitleCount()
    }

    override suspend fun syncCatalogue(maxPages: Int) = withContext(ioDispatcher) {
        if (_syncState.value is SyncState.Syncing) return@withContext

        try {
            _syncState.value = SyncState.Syncing(downloadedCount = 0, totalCount = 0)
            var currentPage = 0
            val pageSize = 50
            var totalCount = 0
            var totalDownloaded = 0

            while (currentPage < maxPages) {
                val response = apiService.getTitles(page = currentPage, size = pageSize)
                totalCount = response.total
                val items = response.items.map { TitleEntity.fromDomain(it.toDomain()) }

                if (items.isEmpty()) break

                titleDao.upsertTitles(items)
                totalDownloaded += items.size
                _syncState.value = SyncState.Syncing(
                    downloadedCount = totalDownloaded,
                    totalCount = totalCount
                )

                if (items.size < pageSize || totalDownloaded >= totalCount) {
                    break
                }
                currentPage++
            }

            val finalCount = titleDao.getTitleCount()
            _syncState.value = SyncState.Success(totalTitles = finalCount)
        } catch (e: Exception) {
            val count = titleDao.getTitleCount()
            if (count > 0) {
                // If we already have titles stored locally, continue working offline
                _syncState.value = SyncState.Success(totalTitles = count)
            } else {
                _syncState.value = SyncState.Error(
                    message = e.localizedMessage ?: "Failed to download catalogue."
                )
            }
        }
    }

    /**
     * Sanitizes raw user search input into valid SQLite FTS4 prefix matching queries.
     * Special characters are stripped and words are appended with '*' wildcard.
     */
    private fun sanitizeFtsQuery(query: String): String {
        val words = query.trim()
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        return if (words.isEmpty()) {
            "\"\""
        } else {
            words.joinToString(" ") { "$it*" }
        }
    }
}
