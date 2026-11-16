package shelftap.android.data

import shelftap.android.api.ApiException
import shelftap.android.api.ChangePasswordRequest
import shelftap.android.api.ErrorCode
import shelftap.android.api.HistoryItem
import shelftap.android.api.Loan
import shelftap.android.api.LoanRequest
import shelftap.android.api.Me
import shelftap.android.api.Page
import shelftap.android.api.PrivacyRequest
import shelftap.android.api.Reservation
import shelftap.android.api.ReservationRequest
import shelftap.android.api.ShelfTapApi
import shelftap.android.api.TitleDetail
import shelftap.android.api.TitleSummary
import shelftap.android.api.apiCall
import shelftap.android.session.SessionManager

/** Member and catalog calls. Every failure is an [ApiException]; the one cross-cutting reaction lives here. */
class Repository(private val api: ShelfTapApi, private val session: SessionManager) {

    private suspend fun <T> call(block: suspend () -> T): T =
        try {
            apiCall(block)
        } catch (e: ApiException.Http) {
            if (e.code == ErrorCode.PASSWORD_CHANGE_REQUIRED) session.requirePasswordChange()
            throw e
        }

    suspend fun titles(query: String, page: Int): Page<TitleSummary> = call { api.titles(query, page, PAGE_SIZE) }

    suspend fun title(titleId: String): TitleDetail = call { api.title(titleId) }

    suspend fun me(): Me = call { api.me() }

    suspend fun changePassword(current: String, new: String) {
        call { api.changePassword(ChangePasswordRequest(current, new)) }
        session.passwordChanged()
    }

    suspend fun setHistoryEnabled(enabled: Boolean): Me = call { api.setPrivacy(PrivacyRequest(enabled)) }

    suspend fun loans(): List<Loan> = call { api.loans() }

    suspend fun history(): List<HistoryItem> = call { api.history() }

    suspend fun reservations(): List<Reservation> = call { api.reservations() }

    /** Self-checkout: the server decides whether this copy may leave; the answer (a loan or an error code) is shown as is. */
    suspend fun checkout(tagId: String): Loan = call { api.checkout(LoanRequest(tagId)) }

    suspend fun renew(loanId: String): Loan = call { api.renew(loanId) }

    suspend fun reserve(titleId: String): Reservation = call { api.reserve(ReservationRequest(titleId)) }

    suspend fun cancelReservation(reservationId: String) = call { api.cancelReservation(reservationId) }

    companion object {
        const val PAGE_SIZE = 20
    }
}
