package vn.shelftap.app.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.local.HistoryDao
import vn.shelftap.app.data.local.HistoryEntity
import vn.shelftap.app.data.local.LoanDao
import vn.shelftap.app.data.local.LoanEntity
import vn.shelftap.app.data.model.ApiError
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MeResponse
import vn.shelftap.app.data.model.PrivacyRequest
import vn.shelftap.app.data.model.TitleDetail

sealed class Resource<out T> {
    data class Success<T>(val data: T, val isOffline: Boolean = false) : Resource<T>()
    data class Error(val message: String, val cachedData: Any? = null) : Resource<Nothing>()
}

sealed class RenewResult {
    data class Success(val updatedLoan: Loan) : RenewResult()
    data class RenewalNotAllowed(val message: String = "This loan cannot be renewed") : RenewResult()
    data class Error(val message: String) : RenewResult()
}

class LibraryRepository(
    private val apiService: ShelfTapApiService,
    private val loanDao: LoanDao,
    private val historyDao: HistoryDao,
    private val gson: Gson = Gson()
) {

    suspend fun getLoans(): Resource<List<Loan>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getLoans()
            if (response.isSuccessful && response.body() != null) {
                val loans = response.body()!!
                loanDao.replaceLoans(loans.map { LoanEntity.fromDomain(it) })
                Resource.Success(loans, isOffline = false)
            } else {
                fallbackCachedLoans("Server returned ${response.code()}")
            }
        } catch (e: Exception) {
            fallbackCachedLoans(e.localizedMessage ?: "Network error")
        }
    }

    private suspend fun fallbackCachedLoans(errorMessage: String): Resource<List<Loan>> {
        val cached = loanDao.getAllLoans().map { it.toDomain() }
        return if (cached.isNotEmpty()) {
            Resource.Success(cached, isOffline = true)
        } else {
            Resource.Error(errorMessage)
        }
    }

    suspend fun renewLoan(loanId: String): RenewResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.renewLoan(loanId)
            if (response.isSuccessful && response.body() != null) {
                val updatedLoan = response.body()!!
                loanDao.insertLoan(LoanEntity.fromDomain(updatedLoan))
                RenewResult.Success(updatedLoan)
            } else {
                val errorBody = response.errorBody()?.string()
                val apiError = try {
                    if (!errorBody.isNullOrBlank()) gson.fromJson(errorBody, ApiError::class.java) else null
                } catch (e: Exception) {
                    null
                }

                if (response.code() == 409 ||
                    apiError?.code == "RENEWAL_NOT_ALLOWED" ||
                    (errorBody != null && errorBody.contains("RENEWAL_NOT_ALLOWED"))
                ) {
                    RenewResult.RenewalNotAllowed()
                } else {
                    RenewResult.Error(apiError?.message ?: "Failed to renew loan (${response.code()})")
                }
            }
        } catch (e: Exception) {
            RenewResult.Error(e.localizedMessage ?: "Network error while renewing")
        }
    }

    suspend fun getHistory(): Resource<List<HistoryItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getHistory()
            if (response.isSuccessful && response.body() != null) {
                val history = response.body()!!
                historyDao.replaceHistory(history.map { HistoryEntity.fromDomain(it) })
                Resource.Success(history, isOffline = false)
            } else {
                fallbackCachedHistory("Server returned ${response.code()}")
            }
        } catch (e: Exception) {
            fallbackCachedHistory(e.localizedMessage ?: "Network error")
        }
    }

    private suspend fun fallbackCachedHistory(errorMessage: String): Resource<List<HistoryItem>> {
        val cached = historyDao.getAllHistory().map { it.toDomain() }
        return if (cached.isNotEmpty()) {
            Resource.Success(cached, isOffline = true)
        } else {
            Resource.Error(errorMessage)
        }
    }

    suspend fun getMe(): Result<MeResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMe()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch user profile (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrivacy(historyEnabled: Boolean): Result<MeResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updatePrivacy(PrivacyRequest(historyEnabled = historyEnabled))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update privacy setting (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTitleDetail(titleId: String): Result<TitleDetail> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTitleDetail(titleId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch title detail (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
