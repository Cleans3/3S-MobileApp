package vn.shelftap.app.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CatalogueRepository(private val dao: TitleDao, private val api: CatalogueApi) {
    fun titles(query: String, scope: CoroutineScope): Flow<PagingData<TitleEntity>> = Pager(
        PagingConfig(pageSize = 50, initialLoadSize = 50, enablePlaceholders = false)
    ) { dao.paging(query.trim()) }.flow.cachedIn(scope)

    fun title(id: String): Flow<TitleDto?> = dao.observe(id).map { it?.toTitle() }

    suspend fun refreshEntireCatalogue(): Boolean = runCatching {
        var page = 0
        var total: Int
        do {
            val result = api.titles(page = page, size = 50)
            total = result.total
            if (result.items.isNotEmpty()) dao.insertAll(result.items.map(TitleDto::toEntity))
            page++
        } while (resultHasMore(page, total, result.items.size))
        true
    }.getOrDefault(false)

    private fun resultHasMore(nextPage: Int, total: Int, lastPageSize: Int) = lastPageSize > 0 && nextPage * 50 < total
}
