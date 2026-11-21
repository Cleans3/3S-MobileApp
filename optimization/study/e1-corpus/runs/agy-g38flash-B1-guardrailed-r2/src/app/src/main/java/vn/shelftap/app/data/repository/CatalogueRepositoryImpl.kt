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
import vn.shelftap.app.data.api.ShelfTapApi
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.mediator.CatalogueRemoteMediator
import vn.shelftap.app.domain.model.Title
import vn.shelftap.app.util.DispatcherProvider

@OptIn(ExperimentalPagingApi::class)
class CatalogueRepositoryImpl(
    private val api: ShelfTapApi,
    private val database: AppDatabase,
    private val dispatchers: DispatcherProvider
) : CatalogueRepository {

    override fun getTitles(query: String): Flow<PagingData<Title>> {
        val trimmed = query.trim()
        val isSearching = trimmed.isNotEmpty()
        val sanitized = if (isSearching) sanitizeFtsQuery(trimmed) else ""

        val pagingSourceFactory = if (!isSearching || sanitized.isEmpty()) {
            { database.titleDao().getAllTitles() }
        } else {
            { database.titleDao().searchTitlesFts(sanitized) }
        }

        return Pager(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = true,
                maxSize = 250,
                prefetchDistance = 10
            ),
            remoteMediator = if (!isSearching) {
                CatalogueRemoteMediator(api, database)
            } else {
                null
            },
            pagingSourceFactory = pagingSourceFactory
        ).flow
            .map { pagingData ->
                pagingData.map { it.toDomain() }
            }
            .flowOn(dispatchers.io)
    }

    override suspend fun getTitleById(id: String): Title? = withContext(dispatchers.io) {
        val local = database.titleDao().getTitleById(id)?.toDomain()
        if (local != null) return@withContext local

        try {
            val remote = api.getTitle(id)
            val entity = TitleEntity.fromDto(remote)
            database.titleDao().insertTitlesWithFts(listOf(entity))
            entity.toDomain()
        } catch (_: Exception) {
            null
        }
    }

    override fun observeTitleById(id: String): Flow<Title?> {
        return database.titleDao().observeTitleById(id)
            .map { it?.toDomain() }
            .flowOn(dispatchers.io)
    }

    override suspend fun refreshTitle(id: String): Result<Title> = withContext(dispatchers.io) {
        try {
            val remote = api.getTitle(id)
            val entity = TitleEntity.fromDto(remote)
            database.titleDao().insertTitlesWithFts(listOf(entity))
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitizeFtsQuery(query: String): String {
        val cleaned = query.replace(Regex("[^a-zA-Z0-9\\s]"), " ").trim()
        if (cleaned.isBlank()) return ""
        val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "$it*" }
    }
}
