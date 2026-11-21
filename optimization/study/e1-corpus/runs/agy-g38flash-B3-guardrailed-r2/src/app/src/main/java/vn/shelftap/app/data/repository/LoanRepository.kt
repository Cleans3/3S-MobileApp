package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.ShelfTapApi
import vn.shelftap.app.data.crypto.EncryptedStorageManager
import vn.shelftap.app.data.model.Loan

sealed class LoansResult {
    data class Success(val loans: List<Loan>, val isSavedData: Boolean) : LoansResult()
    data class Error(val message: String, val fallbackLoans: List<Loan>? = null) : LoansResult()
}

sealed class RenewResult {
    data class Success(val updatedLoan: Loan) : RenewResult()
    object RenewalNotAllowed : RenewResult()
    data class Error(val message: String) : RenewResult()
}

class LoanRepository(
    private val api: ShelfTapApi,
    private val encryptedStorage: EncryptedStorageManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getLoans(): LoansResult = withContext(ioDispatcher) {
        try {
            val networkLoans = api.getLoans()
            encryptedStorage.saveLoans(networkLoans)
            LoansResult.Success(loans = networkLoans, isSavedData = false)
        } catch (e: Exception) {
            val cached = encryptedStorage.getLoans()
            if (cached != null) {
                LoansResult.Success(loans = cached, isSavedData = true)
            } else {
                LoansResult.Error(message = e.localizedMessage ?: "Failed to fetch loans")
            }
        }
    }

    suspend fun renewLoan(loanId: String): RenewResult = withContext(ioDispatcher) {
        try {
            val response = api.renewLoan(loanId)
            if (response.isSuccessful) {
                val updated = response.body()
                if (updated != null) {
                    // Update cache with updated loan
                    val cachedLoans = encryptedStorage.getLoans()?.toMutableList() ?: mutableListOf()
                    val index = cachedLoans.indexOfFirst { it.loanId == updated.loanId }
                    if (index >= 0) {
                        cachedLoans[index] = updated
                    } else {
                        cachedLoans.add(updated)
                    }
                    encryptedStorage.saveLoans(cachedLoans)
                    RenewResult.Success(updated)
                } else {
                    RenewResult.Error("Empty response received")
                }
            } else if (response.code() == 409) {
                RenewResult.RenewalNotAllowed
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                if (errorBody.contains("RENEWAL_NOT_ALLOWED")) {
                    RenewResult.RenewalNotAllowed
                } else {
                    RenewResult.Error("Renewal failed with HTTP ${response.code()}")
                }
            }
        } catch (e: Exception) {
            RenewResult.Error(e.localizedMessage ?: "Network error during renewal")
        }
    }
}
