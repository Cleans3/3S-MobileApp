package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import vn.shelftap.app.data.local.EncryptedShelfTapCache
import vn.shelftap.app.data.model.ApiErrorResponse
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.PrivacyRequest
import vn.shelftap.app.data.model.TitleDetail
import vn.shelftap.app.data.remote.ShelfTapApiService
import java.io.IOException

class ShelfTapRepositoryImpl(
    private val apiService: ShelfTapApiService,
    private val cache: EncryptedShelfTapCache,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ShelfTapRepository {

    override suspend fun getLoans(): Result<DataResult<List<Loan>>> = withContext(ioDispatcher) {
        try {
            val loans = apiService.getLoans()
            cache.saveLoans(loans)
            Result.success(DataResult(loans, isFromCache = false))
        } catch (e: IOException) {
            val cached = cache.loadLoans()
            if (cached != null) {
                Result.success(DataResult(cached, isFromCache = true))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            val cached = cache.loadLoans()
            if (cached != null) {
                Result.success(DataResult(cached, isFromCache = true))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getHistory(): Result<DataResult<List<HistoryItem>>> = withContext(ioDispatcher) {
        try {
            val history = apiService.getHistory()
            cache.saveHistory(history)
            Result.success(DataResult(history, isFromCache = false))
        } catch (e: IOException) {
            val cached = cache.loadHistory()
            if (cached != null) {
                Result.success(DataResult(cached, isFromCache = true))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            val cached = cache.loadHistory()
            if (cached != null) {
                Result.success(DataResult(cached, isFromCache = true))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getProfile(): Result<DataResult<MemberProfile>> = withContext(ioDispatcher) {
        try {
            val profile = apiService.getProfile()
            cache.saveProfile(profile)
            Result.success(DataResult(profile, isFromCache = false))
        } catch (e: IOException) {
            val cached = cache.loadProfile()
            if (cached != null) {
                Result.success(DataResult(cached, isFromCache = true))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            val cached = cache.loadProfile()
            if (cached != null) {
                Result.success(DataResult(cached, isFromCache = true))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateHistoryPrivacy(historyEnabled: Boolean): Result<MemberProfile> =
        withContext(ioDispatcher) {
            try {
                val updatedProfile = apiService.updatePrivacy(PrivacyRequest(historyEnabled = historyEnabled))
                cache.saveProfile(updatedProfile)
                if (!historyEnabled) {
                    // When history is turned off, clear local cached history as well
                    cache.saveHistory(emptyList())
                }
                Result.success(updatedProfile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun renewLoan(loanId: String): Result<Loan> = withContext(ioDispatcher) {
        try {
            val response = apiService.renewLoan(loanId)
            if (response.isSuccessful) {
                val updatedLoan = response.body()
                if (updatedLoan != null) {
                    // Update in cache as well
                    val currentLoans = cache.loadLoans()?.toMutableList() ?: mutableListOf()
                    val index = currentLoans.indexOfFirst { it.loanId == loanId }
                    if (index >= 0) {
                        currentLoans[index] = updatedLoan
                    } else {
                        currentLoans.add(updatedLoan)
                    }
                    cache.saveLoans(currentLoans)
                    Result.success(updatedLoan)
                } else {
                    Result.failure(IllegalStateException("Empty response body"))
                }
            } else {
                if (response.code() == 409) {
                    Result.failure(RenewalNotAllowedException())
                } else {
                    val errorBody = response.errorBody()?.string()
                    val parsedCode = try {
                        if (!errorBody.isNullOrBlank()) {
                            json.decodeFromString<ApiErrorResponse>(errorBody).code
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                    if (parsedCode == "RENEWAL_NOT_ALLOWED") {
                        Result.failure(RenewalNotAllowedException())
                    } else {
                        Result.failure(Exception("Renewal failed with HTTP ${response.code()}"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTitleDetail(titleId: String): Result<TitleDetail> = withContext(ioDispatcher) {
        try {
            val detail = apiService.getTitleDetail(titleId)
            Result.success(detail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
