package vn.shelftap.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.local.ShelfTapDatabaseHelper
import vn.shelftap.app.data.model.TitleDetail

class TitleRepository(
    private val apiService: ShelfTapApiService,
    private val dbHelper: ShelfTapDatabaseHelper
) {

    suspend fun getTitleDetail(titleId: String): Result<Pair<TitleDetail, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val detail = apiService.getTitleDetail(titleId)
            dbHelper.saveTitleDetail(detail)
            Result.success(Pair(detail, false))
        } catch (e: Exception) {
            val cached = dbHelper.getTitleDetail(titleId)
            if (cached != null) {
                Result.success(Pair(cached, true))
            } else {
                Result.failure(e)
            }
        }
    }
}
