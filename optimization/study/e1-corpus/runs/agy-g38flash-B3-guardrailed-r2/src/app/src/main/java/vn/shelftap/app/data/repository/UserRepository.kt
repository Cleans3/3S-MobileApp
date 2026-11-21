package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.ShelfTapApi
import vn.shelftap.app.data.crypto.EncryptedStorageManager
import vn.shelftap.app.data.model.PrivacyRequest
import vn.shelftap.app.data.model.UserProfile

sealed class UserProfileResult {
    data class Success(val profile: UserProfile, val isSavedData: Boolean) : UserProfileResult()
    data class Error(val message: String) : UserProfileResult()
}

class UserRepository(
    private val api: ShelfTapApi,
    private val encryptedStorage: EncryptedStorageManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getUserProfile(): UserProfileResult = withContext(ioDispatcher) {
        try {
            val profile = api.getMe()
            encryptedStorage.saveProfile(profile)
            UserProfileResult.Success(profile = profile, isSavedData = false)
        } catch (e: Exception) {
            val cached = encryptedStorage.getProfile()
            if (cached != null) {
                UserProfileResult.Success(profile = cached, isSavedData = true)
            } else {
                UserProfileResult.Error(message = e.localizedMessage ?: "Failed to load profile")
            }
        }
    }

    suspend fun updateHistoryEnabled(enabled: Boolean): Result<UserProfile> = withContext(ioDispatcher) {
        try {
            val updated = api.updatePrivacy(PrivacyRequest(historyEnabled = enabled))
            encryptedStorage.saveProfile(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
