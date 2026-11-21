package vn.shelftap.app.data.repository

import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.TitleDetail

data class LoansData(
    val loans: List<Loan>,
    val isSavedData: Boolean
)

data class HistoryData(
    val history: List<HistoryItem>,
    val isSavedData: Boolean
)

data class ProfileData(
    val profile: MemberProfile,
    val isSavedData: Boolean
)

sealed interface RenewResult {
    data class Success(val updatedLoan: Loan) : RenewResult
    data class NotAllowed(val message: String) : RenewResult
    data class Error(val message: String) : RenewResult
}

interface ShelfTapRepository {
    suspend fun getLoans(): Result<LoansData>
    suspend fun renewLoan(loanId: String): RenewResult
    suspend fun getHistory(): Result<HistoryData>
    suspend fun getProfile(): Result<ProfileData>
    suspend fun updatePrivacy(historyEnabled: Boolean): Result<MemberProfile>
    suspend fun getTitleDetail(titleId: String): Result<TitleDetail>
}
