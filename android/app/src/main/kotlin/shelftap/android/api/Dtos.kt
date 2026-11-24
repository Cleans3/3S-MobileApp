package shelftap.android.api

import kotlinx.serialization.Serializable

// Field names are exactly server/API.md. Secrets override toString so they cannot reach a log by accident.

@Serializable
data class LoginRequest(val email: String, val password: String, val deviceId: String) {
    override fun toString() = "LoginRequest(***)"
}

@Serializable
data class RefreshRequest(val refreshToken: String, val deviceId: String) {
    override fun toString() = "RefreshRequest(***)"
}

@Serializable
data class LogoutRequest(val refreshToken: String) {
    override fun toString() = "LogoutRequest(***)"
}

@Serializable
data class ChallengeResponse(val nonce: String)

@Serializable
data class CardAuthRequest(val nonce: String, val signature: String, val deviceId: String)

@Serializable
data class TokenPair(
    val accessToken: String,
    val accessExpiresIn: Long,
    val refreshToken: String,
    val mustChangePassword: Boolean,
) {
    override fun toString() = "TokenPair(***)"
}

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String) {
    override fun toString() = "ChangePasswordRequest(***)"
}

@Serializable
data class PrivacyRequest(val historyEnabled: Boolean)

@Serializable
data class LoanRequest(val tagId: String)

@Serializable
data class ReservationRequest(val titleId: String)

@Serializable
data class Me(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean,
)

@Serializable
data class TitleSummary(
    val titleId: String,
    val isbn: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int,
)

@Serializable
data class TitleDetail(
    val titleId: String,
    val isbn: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int,
    val copiesTotal: Int,
)

@Serializable
data class Page<T>(val items: List<T>, val page: Int, val size: Int, val total: Long)

@Serializable
data class Loan(
    val loanId: String,
    val copyId: String,
    val titleId: String,
    val title: String,
    val barcode: String,
    val outAt: String,
    val dueAt: String,
    val returnedAt: String? = null,
    val renewals: Int,
    val renewable: Boolean,
)

/** A fine for an overdue loan; `paidAt` is null while it is unpaid. Read-only for the member (fines are settled at the desk). */
@Serializable
data class Fine(val fineId: String, val loanId: String, val amountVnd: Long, val paidAt: String? = null)

@Serializable
data class HistoryItem(val titleId: String, val title: String, val outAt: String, val returnedAt: String? = null)

@Serializable
enum class ReservationStatus { WAITING, READY, FULFILLED, CANCELLED, EXPIRED }

@Serializable
data class Reservation(
    val reservationId: String,
    val titleId: String,
    val title: String,
    val placedAt: String,
    val readyAt: String? = null,
    val status: ReservationStatus = ReservationStatus.WAITING,
)
