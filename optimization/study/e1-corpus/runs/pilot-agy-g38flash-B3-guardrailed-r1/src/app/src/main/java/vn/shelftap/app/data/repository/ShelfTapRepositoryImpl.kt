package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import vn.shelftap.app.data.model.ApiError
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.PrivacyRequest
import vn.shelftap.app.data.model.TitleDetail
import vn.shelftap.app.data.network.ShelfTapApiService
import vn.shelftap.app.data.security.EncryptedStorage

class ShelfTapRepositoryImpl(
    private val apiService: ShelfTapApiService,
    private val encryptedStorage: EncryptedStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ShelfTapRepository {

    override suspend fun getLoans(): Result<LoansData> = withContext(ioDispatcher) {
        try {
            val remoteLoans = apiService.getLoans()
            encryptedStorage.saveLoans(remoteLoans)
            Result.success(LoansData(loans = remoteLoans, isSavedData = false))
        } catch (e: Exception) {
            val cachedLoans = encryptedStorage.loadLoans()
            if (cachedLoans != null) {
                Result.success(LoansData(loans = cachedLoans, isSavedData = true))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun renewLoan(loanId: String): RenewResult = withContext(ioDispatcher) {
        try {
            val response = apiService.renewLoan(loanId)
            if (response.isSuccessful) {
                val updatedLoan = response.body()
                if (updatedLoan != null) {
                    // Update cache with updated loan
                    val cachedLoans = encryptedStorage.loadLoans()
                    if (cachedLoans != null) {
                        val newLoans = cachedLoans.map {
                            if (it.loanId == updatedLoan.loanId) updatedLoan else it
                        }
                        encryptedStorage.saveLoans(newLoans)
                    }
                    return@withContext RenewResult.Success(updatedLoan)
                }
                return@withContext RenewResult.Error("Empty response body")
            }

            if (response.code() == 409) {
                val errorBody = response.errorBody()?.string()
                val isNotAllowed = if (!errorBody.isNullOrBlank()) {
                    try {
                        val parsed = json.decodeFromString<ApiError>(errorBody)
                        parsed.code == "RENEWAL_NOT_ALLOWED"
                    } catch (e: Exception) {
                        true
                    }
                } else {
                    true
                }
                if (isNotAllowed) {
                    return@withContext RenewResult.NotAllowed("This loan cannot be renewed")
                }
            }

            RenewResult.Error("Server error: ${response.code()}")
        } catch (e: Exception) {
            RenewResult.Error(e.localizedMessage ?: "Network error occurred")
        }
    }

    override suspend fun getHistory(): Result<HistoryData> = withContext(ioDispatcher) {
        try {
            val remoteHistory = apiService.getHistory()
            encryptedStorage.saveHistory(remoteHistory)
            Result.success(HistoryData(history = remoteHistory, isSavedData = false))
        } catch (e: Exception) {
            val cachedHistory = encryptedStorage.loadHistory()
            if (cachedHistory != null) {
                Result.success(HistoryData(history = cachedHistory, isSavedData = true))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getProfile(): Result<ProfileData> = withContext(ioDispatcher) {
        try {
            val remoteProfile = apiService.getProfile()
            encryptedStorage.saveProfile(remoteProfile)
            Result.success(ProfileData(profile = remoteProfile, isSavedData = false))
        } catch (e: Exception) {
            val cachedProfile = encryptedStorage.loadProfile()
            if (cachedProfile != null) {
                Result.success(ProfileData(profile = cachedProfile, isSavedData = true))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun updatePrivacy(historyEnabled: Boolean): Result<MemberProfile> = withContext(ioDispatcher) {
        try {
            val updated = apiService.updatePrivacy(PrivacyRequest(historyEnabled = historyEnabled))
            encryptedStorage.saveProfile(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTitleDetail(titleId: String): Result<TitleDetail> = withContext(ioDispatcher) {
        try {
            val titleDetail = apiService.getTitleDetail(titleId)
            Result.success(titleDetail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
