package vn.shelftap.app.data.repository

import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.TitleDetail

class RenewalNotAllowedException(
    message: String = "This loan cannot be renewed"
) : Exception(message)

data class DataResult<T>(
    val data: T,
    val isFromCache: Boolean
)

interface ShelfTapRepository {
    suspend fun getLoans(): Result<DataResult<List<Loan>>>
    suspend fun getHistory(): Result<DataResult<List<HistoryItem>>>
    suspend fun getProfile(): Result<DataResult<MemberProfile>>
    suspend fun updateHistoryPrivacy(historyEnabled: Boolean): Result<MemberProfile>
    suspend fun renewLoan(loanId: String): Result<Loan>
    suspend fun getTitleDetail(titleId: String): Result<TitleDetail>
}
