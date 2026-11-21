package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.ShelfTapApi
import vn.shelftap.app.data.model.TitleDetails

class TitleRepository(
    private val api: ShelfTapApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getTitleDetails(titleId: String): Result<TitleDetails> = withContext(ioDispatcher) {
        try {
            val details = api.getTitleDetails(titleId)
            Result.success(details)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
