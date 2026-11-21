package vn.shelftap.app.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.data.model.toEntity
import vn.shelftap.app.data.model.toModel
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.di.DispatcherProvider

interface CatalogueRepository {
    fun getTitles(query: String): Flow<PagingData<Title>>
    fun getTitleById(id: String): Flow<Title?>
    suspend fun refreshTitle(id: String): Result<Title>
    suspend fun syncAllCatalogue(onProgress: (page: Int, totalPages: Int) -> Unit = { _, _ -> }): Result<Int>
}

class CatalogueRepositoryImpl(
    private val apiService: ShelfTapApiService,
    private val database: AppDatabase,
    private val dispatcherProvider: DispatcherProvider
) : CatalogueRepository {

    companion object {
        private const val PAGE_SIZE = 30
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getTitles(query: String): Flow<PagingData<Title>> {
        val trimmed = query.trim()
        return if (trimmed.isBlank()) {
            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    prefetchDistance = 10,
                    enablePlaceholders = true,
                    maxSize = 200
                ),
                remoteMediator = TitleRemoteMediator(
                    query = null,
                    apiService = apiService,
                    database = database,
                    dispatcherProvider = dispatcherProvider
                ),
                pagingSourceFactory = { database.titleDao().getTitlesPaged() }
            ).flow.map { pagingData ->
                pagingData.map { it.toModel() }
            }.flowOn(dispatcherProvider.io)
        } else {
            // Rule 17: Query Room FTS4 table off main thread and page the results
            val ftsQuery = formatFtsQuery(trimmed)
            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    prefetchDistance = 10,
                    enablePlaceholders = false
                ),
                remoteMediator = TitleRemoteMediator(
                    query = trimmed,
                    apiService = apiService,
                    database = database,
                    dispatcherProvider = dispatcherProvider
                ),
                pagingSourceFactory = { database.titleDao().searchTitlesFtsPaged(ftsQuery) }
            ).flow.map { pagingData ->
                pagingData.map { it.toModel() }
            }.flowOn(dispatcherProvider.io)
        }
    }

    override fun getTitleById(id: String): Flow<Title?> {
        return database.titleDao().observeTitleById(id)
            .map { it?.toModel() }
            .flowOn(dispatcherProvider.io)
    }

    override suspend fun refreshTitle(id: String): Result<Title> = withContext(dispatcherProvider.io) {
        try {
            val dto = apiService.getTitleById(id)
            val entity = dto.toEntity()
            database.titleDao().upsertTitlesWithFts(listOf(entity))
            Result.success(dto.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncAllCatalogue(
        onProgress: (page: Int, totalPages: Int) -> Unit
    ): Result<Int> = withContext(dispatcherProvider.io) {
        try {
            var currentPage = 0
            var totalCount = 0
            var totalPages = 1
            val batchSize = 50

            do {
                val response = apiService.getTitles(page = currentPage, size = batchSize)
                totalPages = ((response.total + batchSize - 1) / batchSize).coerceAtLeast(1)
                val entities = response.items.map { it.toEntity() }

                database.titleDao().upsertTitlesWithFts(entities)
                totalCount += entities.size
                onProgress(currentPage + 1, totalPages)
                currentPage++
            } while (currentPage < totalPages && entities.isNotEmpty())

            Result.success(totalCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatFtsQuery(raw: String): String {
        val sanitized = raw.replace(Regex("[^a-zA-Z0-9\\s]"), " ").trim()
        val tokens = sanitized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return "\"*\""
        return tokens.joinToString(" ") { "$it*" }
    }
}
