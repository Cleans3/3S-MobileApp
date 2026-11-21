package vn.shelftap.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.local.ShelfTapDatabaseHelper
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.PrivacyUpdateRequest

class MemberRepository(
    private val apiService: ShelfTapApiService,
    private val dbHelper: ShelfTapDatabaseHelper
) {

    suspend fun getProfile(): Result<Pair<MemberProfile, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val profile = apiService.getProfile()
            dbHelper.saveProfile(profile)
            Result.success(Pair(profile, false))
        } catch (e: Exception) {
            val cached = dbHelper.getProfile()
            if (cached != null) {
                Result.success(Pair(cached, true))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun updatePrivacy(historyEnabled: Boolean): Result<MemberProfile> = withContext(Dispatchers.IO) {
        try {
            val updated = apiService.updatePrivacy(PrivacyUpdateRequest(historyEnabled = historyEnabled))
            dbHelper.saveProfile(updated)
            Result.success(updated)
        } catch (e: Exception) {
            // Update local state even if offline request couldn't be completed, or report failure
            dbHelper.updateHistoryEnabled(historyEnabled)
            Result.failure(e)
        }
    }
}
