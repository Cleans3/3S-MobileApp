package vn.shelftap.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.NetworkClient
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.local.ShelfTapDatabaseHelper
import vn.shelftap.app.data.model.ApiErrorResponse
import vn.shelftap.app.data.model.Loan

class RenewalNotAllowedException(message: String = "This loan cannot be renewed") : Exception(message)

class LoansRepository(
    private val apiService: ShelfTapApiService,
    private val dbHelper: ShelfTapDatabaseHelper
) {

    /**
     * Loads loans. If network fails or is unavailable, falls back to locally saved loans.
     * Returns Pair<List<Loan>, Boolean> where the Boolean indicates whether the data is from local cache (saved data).
     */
    suspend fun getLoans(): Result<Pair<List<Loan>, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val remoteLoans = apiService.getLoans()
            dbHelper.saveLoans(remoteLoans)
            Result.success(Pair(remoteLoans, false))
        } catch (e: Exception) {
            val cachedLoans = dbHelper.getLoans()
            if (cachedLoans.isNotEmpty()) {
                Result.success(Pair(cachedLoans, true))
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Renews a loan by loanId.
     * If 409 RENEWAL_NOT_ALLOWED occurs, returns RenewalNotAllowedException.
     */
    suspend fun renewLoan(loanId: String): Result<Loan> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.renewLoan(loanId)
            if (response.isSuccessful && response.body() != null) {
                val updatedLoan = response.body()!!
                dbHelper.updateLoan(updatedLoan)
                Result.success(updatedLoan)
            } else {
                val errorBodyStr = response.errorBody()?.string().orEmpty()
                val apiError = try {
                    NetworkClient.gson.fromJson(errorBodyStr, ApiErrorResponse::class.java)
                } catch (e: Exception) {
                    null
                }

                if (response.code() == 409 || apiError?.code == "RENEWAL_NOT_ALLOWED") {
                    Result.failure(RenewalNotAllowedException(apiError?.message ?: "This loan cannot be renewed"))
                } else {
                    Result.failure(Exception("Renewal failed with status ${response.code()}: ${apiError?.message ?: "Unknown error"}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
