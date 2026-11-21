package vn.shelftap.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.shelftap.app.data.local.TitleDao
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.data.sync.CatalogueSyncManager

class CatalogueRepository(
    private val titleDao: TitleDao,
    private val apiService: ShelfTapApiService,
    val syncManager: CatalogueSyncManager
) {
    fun getCataloguePagingData(): Flow<PagingData<Title>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 25,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { titleDao.getPagedTitles() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    fun searchCataloguePagingData(query: String): Flow<PagingData<Title>> {
        val trimmedQuery = query.trim()
        return if (trimmedQuery.isEmpty()) {
            getCataloguePagingData()
        } else {
            Pager(
                config = PagingConfig(
                    pageSize = 50,
                    prefetchDistance = 25,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { titleDao.searchPagedTitles(trimmedQuery) }
            ).flow.map { pagingData ->
                pagingData.map { it.toDomain() }
            }
        }
    }

    fun getTitleByIdFlow(id: String): Flow<Title?> {
        return titleDao.getTitleByIdFlow(id).map { it?.toDomain() }
    }

    suspend fun getTitleById(id: String): Title? {
        val cached = titleDao.getTitleById(id)
        if (cached != null) {
            return cached.toDomain()
        }
        return try {
            val remote = apiService.getTitle(id)
            titleDao.insertAll(listOf(TitleEntity.fromDomain(remote)))
            remote
        } catch (_: Exception) {
            null
        }
    }

    fun getCatalogueCountFlow(): Flow<Int> = titleDao.getTitleCountFlow()

    fun triggerSync(force: Boolean = false) {
        syncManager.startSync(force = force)
    }
}
