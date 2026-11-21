package vn.shelftap.app.data.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import vn.shelftap.app.domain.model.Title

interface CatalogueRepository {
    fun getTitles(query: String): Flow<PagingData<Title>>
    suspend fun getTitleById(id: String): Title?
    fun observeTitleById(id: String): Flow<Title?>
    suspend fun refreshTitle(id: String): Result<Title>
}
