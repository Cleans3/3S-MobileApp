package vn.shelftap.app.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.local.HistoryEntity
import vn.shelftap.app.data.local.LoanEntity
import vn.shelftap.app.data.local.MemberProfileEntity
import vn.shelftap.app.data.local.TitleDetailEntity
import vn.shelftap.app.data.model.ApiErrorResponse
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.PrivacyUpdateRequest
import vn.shelftap.app.data.model.TitleDetail
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.util.RenewResult
import vn.shelftap.app.util.Resource
import java.io.IOException

class ShelfTapRepository(
    private val apiService: ShelfTapApiService,
    private val database: AppDatabase
) {
    private val gson = Gson()

    suspend fun getLoans(): Resource<List<Loan>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getLoans()
            if (response.isSuccessful && response.body() != null) {
                val loans = response.body()!!
                database.loanDao().replaceAll(loans.map { LoanEntity.fromLoan(it) })
                return@withContext Resource.Success(loans, isSavedData = false)
            } else {
                return@withContext loadCachedLoans("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            // Network unavailable or connection error -> fallback to cache
            return@withContext loadCachedLoans("Network unavailable")
        } catch (e: Exception) {
            return@withContext loadCachedLoans(e.message ?: "Unexpected error")
        }
    }

    private suspend fun loadCachedLoans(fallbackReason: String): Resource<List<Loan>> {
        val cached = database.loanDao().getAllLoans().map { it.toLoan() }
        return if (cached.isNotEmpty()) {
            Resource.Success(cached, isSavedData = true)
        } else {
            Resource.Error(fallbackReason, isSavedData = false)
        }
    }

    suspend fun getHistory(): Resource<List<HistoryItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getHistory()
            if (response.isSuccessful && response.body() != null) {
                val history = response.body()!!
                database.historyDao().replaceAll(history.map { HistoryEntity.fromHistoryItem(it) })
                return@withContext Resource.Success(history, isSavedData = false)
            } else {
                return@withContext loadCachedHistory("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            return@withContext loadCachedHistory("Network unavailable")
        } catch (e: Exception) {
            return@withContext loadCachedHistory(e.message ?: "Unexpected error")
        }
    }

    private suspend fun loadCachedHistory(fallbackReason: String): Resource<List<HistoryItem>> {
        val cached = database.historyDao().getAllHistory().map { it.toHistoryItem() }
        return if (cached.isNotEmpty()) {
            Resource.Success(cached, isSavedData = true)
        } else {
            Resource.Error(fallbackReason, isSavedData = false)
        }
    }

    suspend fun renewLoan(loanId: String): RenewResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.renewLoan(loanId)
            if (response.isSuccessful && response.body() != null) {
                val updatedLoan = response.body()!!
                database.loanDao().insertLoan(LoanEntity.fromLoan(updatedLoan))
                return@withContext RenewResult.Success(updatedLoan)
            }

            val errorBodyString = response.errorBody()?.string()
            val apiError = parseErrorBody(errorBodyString)

            if (response.code() == 409 || apiError?.code == "RENEWAL_NOT_ALLOWED") {
                return@withContext RenewResult.RenewalNotAllowed("This loan cannot be renewed")
            }

            return@withContext RenewResult.Error(
                apiError?.message ?: "Renewal failed (${response.code()})"
            )
        } catch (e: IOException) {
            return@withContext RenewResult.Error("Network error. Could not renew loan.")
        } catch (e: Exception) {
            return@withContext RenewResult.Error(e.message ?: "Unexpected renewal error")
        }
    }

    suspend fun getMemberProfile(): Resource<MemberProfile> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMemberProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                database.profileDao().insertProfile(MemberProfileEntity.fromMemberProfile(profile))
                return@withContext Resource.Success(profile, isSavedData = false)
            } else {
                return@withContext loadCachedProfile("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            return@withContext loadCachedProfile("Network unavailable")
        } catch (e: Exception) {
            return@withContext loadCachedProfile(e.message ?: "Unexpected error")
        }
    }

    private suspend fun loadCachedProfile(fallbackReason: String): Resource<MemberProfile> {
        val cached = database.profileDao().getProfile()?.toMemberProfile()
        return if (cached != null) {
            Resource.Success(cached, isSavedData = true)
        } else {
            Resource.Error(fallbackReason, isSavedData = false)
        }
    }

    suspend fun updatePrivacy(historyEnabled: Boolean): Resource<MemberProfile> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updatePrivacy(PrivacyUpdateRequest(historyEnabled = historyEnabled))
            if (response.isSuccessful && response.body() != null) {
                val updatedProfile = response.body()!!
                database.profileDao().insertProfile(MemberProfileEntity.fromMemberProfile(updatedProfile))
                return@withContext Resource.Success(updatedProfile, isSavedData = false)
            } else {
                return@withContext Resource.Error("Failed to update privacy setting")
            }
        } catch (e: IOException) {
            return@withContext Resource.Error("Network error: Cannot update privacy while offline")
        } catch (e: Exception) {
            return@withContext Resource.Error(e.message ?: "Failed to update privacy")
        }
    }

    suspend fun getTitleDetail(titleId: String): Resource<TitleDetail> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTitleDetail(titleId)
            if (response.isSuccessful && response.body() != null) {
                val detail = response.body()!!
                database.titleDao().insertTitle(TitleDetailEntity.fromTitleDetail(detail))
                return@withContext Resource.Success(detail, isSavedData = false)
            } else {
                return@withContext loadCachedTitle(titleId, "Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            return@withContext loadCachedTitle(titleId, "Network unavailable")
        } catch (e: Exception) {
            return@withContext loadCachedTitle(titleId, e.message ?: "Unexpected error")
        }
    }

    private suspend fun loadCachedTitle(titleId: String, fallbackReason: String): Resource<TitleDetail> {
        val cached = database.titleDao().getTitleById(titleId)?.toTitleDetail()
        return if (cached != null) {
            Resource.Success(cached, isSavedData = true)
        } else {
            Resource.Error(fallbackReason, isSavedData = false)
        }
    }

    private fun parseErrorBody(errorBody: String?): ApiErrorResponse? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            gson.fromJson(errorBody, ApiErrorResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
