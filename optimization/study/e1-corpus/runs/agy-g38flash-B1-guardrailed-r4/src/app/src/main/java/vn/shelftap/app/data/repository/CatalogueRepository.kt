package vn.shelftap.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.api.TitleDto
import vn.shelftap.app.data.local.TitleDao
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.model.Title

class CatalogueRepository(
    private val titleDao: TitleDao,
    private val apiService: ShelfTapApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    /**
     * Rule 16: No database/network access on main thread.
     * Rule 17: Paged query over local indexed Room FTS4 store.
     */
    fun getTitlesPaged(query: String): Flow<PagingData<Title>> {
        val sanitized = sanitizeFtsQuery(query)
        val pager = Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 25,
                enablePlaceholders = false
            )
        ) {
            if (sanitized.isBlank()) {
                titleDao.getAllTitlesPaged()
            } else {
                titleDao.searchTitlesFtsPaged(sanitized)
            }
        }

        return pager.flow
            .map { pagingData -> pagingData.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    suspend fun getTitleById(id: String): Title? = withContext(ioDispatcher) {
        val local = titleDao.getTitleById(id)
        if (local != null) {
            return@withContext local.toDomain()
        }
        try {
            val dto = apiService.getTitleById(id)
            val title = dto.toDomain()
            titleDao.insertTitle(TitleEntity.fromDomain(title))
            title
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getLocalTitleCount(): Int = withContext(ioDispatcher) {
        titleDao.getTitleCount()
    }

    /**
     * Downloads catalogue in pages from the REST API and caches it in Room DB.
     * Once downloaded, browsing and searching work fully offline.
     */
    suspend fun syncCatalogue(
        onProgress: suspend (downloaded: Int, total: Int) -> Unit
    ): Result<Int> = withContext(ioDispatcher) {
        try {
            var currentPage = 0
            val pageSize = 50
            var totalItems = 0
            var downloadedCount = 0

            do {
                val response = apiService.getTitles(page = currentPage, size = pageSize)
                totalItems = response.total
                val entities = response.items.map { TitleEntity.fromDomain(it.toDomain()) }
                if (entities.isNotEmpty()) {
                    titleDao.insertTitles(entities)
                    downloadedCount += entities.size
                }
                onProgress(downloadedCount, totalItems)
                currentPage++
            } while (downloadedCount < totalItems && response.items.isNotEmpty())

            Result.success(downloadedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitizeFtsQuery(query: String): String {
        val cleaned = query.trim().replace(Regex("[^a-zA-Z0-9\\s]"), " ")
        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return ""
        return words.joinToString(" ") { "$it*" }
    }
}

fun TitleDto.toDomain(): Title = Title(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors,
    year = year,
    subjects = subjects,
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable
)
