package shelftap.android.uiaudit

import shelftap.android.api.AuthApi
import shelftap.android.api.CardAuthRequest
import shelftap.android.api.ChallengeResponse
import shelftap.android.api.ChangePasswordRequest
import shelftap.android.api.Fine
import shelftap.android.api.HistoryItem
import shelftap.android.api.Loan
import shelftap.android.api.LoanRequest
import shelftap.android.api.LoginRequest
import shelftap.android.api.LogoutRequest
import shelftap.android.api.Me
import shelftap.android.api.Page
import shelftap.android.api.PrivacyRequest
import shelftap.android.api.RefreshRequest
import shelftap.android.api.Reservation
import shelftap.android.api.ReservationRequest
import shelftap.android.api.ReservationStatus
import shelftap.android.api.ShelfTapApi
import shelftap.android.api.TitleDetail
import shelftap.android.api.TitleSummary
import shelftap.android.api.TokenPair

/**
 * Fixed, made-up data for the UI audit: Vietnamese titles with diacritics and one long title, so text expansion shows.
 * No real person, card or ID. Covers are null (the dev server is not reachable from the JVM), so every row shows the
 * same empty cover slot the app shows when a cover fails to load.
 */
object AuditData {
    const val EMAIL = "thanhvien.thu@example.vn"

    /** A few subjects, so the list shows a row of chips of different lengths (declared first: the titles below use it while the object is built). */
    private val SUBJECTS = listOf("Văn học Việt Nam", "Thiếu nhi", "Khoa học", "Lịch sử", "Kinh tế")

    val titles = listOf(
        title("t-1", "9786041234567", "Dế Mèn phiêu lưu ký", listOf("Tô Hoài"), 3),
        title("t-2", "9786041234574", "Số đỏ", listOf("Vũ Trọng Phụng"), 0),
        title("t-3", "9786041234581", "Truyện Kiều", listOf("Nguyễn Du"), 5),
        title(
            "t-4", "9786041234598", "Nhập môn lập trình hướng đối tượng với Kotlin và Jetpack Compose cho người mới bắt đầu",
            listOf("Nguyễn Văn A", "Trần Thị B"), 1,
        ),
        title("t-5", "9786041234604", "Lược sử thời gian", listOf("Stephen Hawking"), 2),
        title("t-6", "9786041234611", "Nhà giả kim", listOf("Paulo Coelho"), 0),
        title("t-7", "9786041234628", "Tắt đèn", listOf("Ngô Tất Tố"), 4),
        title("t-8", "9786041234635", "Cấu trúc dữ liệu và giải thuật", listOf("Lê Minh Hoàng"), 1),
        // The next four share a subject or an author with books the member has, so "Suggestions for you" has something to suggest.
        title("t-9", "9786041234642", "Tôi thấy hoa vàng trên cỏ xanh", listOf("Nguyễn Nhật Ánh"), 2, "Thiếu nhi"),
        title("t-10", "9786041234659", "Cho tôi xin một vé đi tuổi thơ", listOf("Nguyễn Nhật Ánh"), 0, "Thiếu nhi"),
        title("t-11", "9786041234666", "Vũ trụ trong vỏ hạt dẻ", listOf("Stephen Hawking"), 3, "Khoa học"),
        title("t-12", "9786041234673", "Lịch sử Việt Nam bằng tranh", listOf("Trần Bạch Đằng"), 1, "Lịch sử"),
    )

    val me = Me("m-1", "Phạm Thị Thu Hường", EMAIL, "STUDENT", "ACTIVE", historyEnabled = true, mustChangePassword = false)

    val loans = listOf(
        Loan("l-1", "c-1", "t-1", "Dế Mèn phiêu lưu ký", "BC000101", "2026-11-02T02:00:00Z", "2026-11-16T02:00:00Z", null, 1, false),
        Loan("l-2", "c-4", "t-4", titles[3].title, "BC000402", "2026-11-20T02:00:00Z", "2026-12-04T02:00:00Z", null, 0, true),
    )

    /** A fine on the overdue loan, so the loans screen shows its overdue state and its figure. */
    val fines = listOf(Fine("f-1", "l-1", 18000, null))

    val history = listOf(
        HistoryItem("t-3", "Truyện Kiều", "2026-10-02T02:00:00Z", "2026-10-15T02:00:00Z"),
        HistoryItem("t-7", "Tắt đèn", "2026-09-10T02:00:00Z", "2026-09-24T02:00:00Z"),
        HistoryItem("t-4", titles[3].title, "2026-11-20T02:00:00Z", null),
    )

    val reservations = listOf(
        Reservation("r-1", "t-2", "Số đỏ", "2026-11-18T02:00:00Z", null, ReservationStatus.WAITING),
        Reservation("r-2", "t-6", "Nhà giả kim", "2026-11-10T02:00:00Z", "2026-11-21T02:00:00Z", ReservationStatus.READY),
    )

    private fun title(id: String, isbn: String, name: String, authors: List<String>, available: Int, subject: String? = null) =
        TitleSummary(id, isbn, name, authors, listOf(subject ?: SUBJECTS[(id.filter { it.isDigit() }.toIntOrNull() ?: 0) % SUBJECTS.size]), null, available)

    object ApiFake : ShelfTapApi {
        override suspend fun titles(q: String, page: Int, size: Int): Page<TitleSummary> {
            // like the server: the words are looked for in the title, the authors and the subjects
            val hits = titles.filter { q.isBlank() || it.title.contains(q, ignoreCase = true) || it.authors.any { a -> a.contains(q, ignoreCase = true) } || it.subjects.any { s -> s.contains(q, ignoreCase = true) } }
            val items = hits.drop(page * size).take(size)
            return Page(items, page, size, hits.size.toLong())
        }

        override suspend fun title(titleId: String): TitleDetail = titles.first { it.titleId == titleId }.let {
            TitleDetail(it.titleId, it.isbn, it.title, it.authors, it.subjects, it.coverUrl, it.copiesAvailable, it.copiesAvailable + 2)
        }

        override suspend fun me(): Me = me
        override suspend fun changePassword(body: ChangePasswordRequest) = Unit
        override suspend fun setPrivacy(body: PrivacyRequest): Me = me.copy(historyEnabled = body.historyEnabled)
        override suspend fun loans(): List<Loan> = loans
        override suspend fun fines(): List<Fine> = fines
        override suspend fun history(): List<HistoryItem> = history
        override suspend fun reservations(): List<Reservation> = reservations
        override suspend fun checkout(body: LoanRequest): Loan = loans.first()
        override suspend fun renew(loanId: String): Loan = loans.first { it.loanId == loanId }
        override suspend fun reserve(body: ReservationRequest): Reservation = reservations.first()
        override suspend fun cancelReservation(reservationId: String) = Unit
    }

    object AuthApiFake : AuthApi {
        override suspend fun login(body: LoginRequest, integrity: String) = TokenPair("at-audit", 900, "rt-audit", false)
        override suspend fun refresh(body: RefreshRequest, integrity: String) = TokenPair("at-audit", 900, "rt-audit", false)
        override suspend fun logout(body: LogoutRequest) = Unit
        override suspend fun challenge(cardId: String): ChallengeResponse = throw UnsupportedOperationException()
        override suspend fun cardAuth(cardId: String, body: CardAuthRequest, integrity: String): TokenPair = throw UnsupportedOperationException()
    }
}
