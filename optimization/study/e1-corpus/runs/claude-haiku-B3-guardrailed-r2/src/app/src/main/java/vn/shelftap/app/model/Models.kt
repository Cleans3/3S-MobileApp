package vn.shelftap.app.model

import java.time.Instant

data class Loan(
    val loanId: String,
    val copyId: String,
    val titleId: String,
    val title: String,
    val barcode: String,
    val outAt: Instant,
    val dueAt: Instant,
    val returnedAt: Instant?,
    val renewals: Int,
    val renewable: Boolean,
)

data class HistoryItem(
    val titleId: String,
    val title: String,
    val outAt: Instant,
    val returnedAt: Instant?,
)

data class Member(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean,
)

data class TitleDetail(
    val titleId: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int,
    val copiesTotal: Int,
)

/** Past loans, newest first, plus whether the member keeps reading history. */
data class HistorySnapshot(
    val items: List<HistoryItem>,
    val historyEnabled: Boolean,
)

/** A result that may come from the encrypted local copy instead of the server. */
data class Loaded<out T>(
    val value: T,
    val fromSavedData: Boolean,
)
