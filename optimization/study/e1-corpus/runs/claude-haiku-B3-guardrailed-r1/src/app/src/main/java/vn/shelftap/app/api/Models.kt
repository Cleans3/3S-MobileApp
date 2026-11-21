package vn.shelftap.app.api

import kotlinx.serialization.Serializable

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
    val renewals: Int = 0,
    val renewable: Boolean = false,
)

@Serializable
data class HistoryItem(
    val titleId: String,
    val title: String,
    val outAt: String,
    val returnedAt: String? = null,
)

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
data class PrivacyUpdate(val historyEnabled: Boolean)

@Serializable
data class TitleDetail(
    val titleId: String,
    val isbn: String? = null,
    val title: String,
    val authors: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int = 0,
    val copiesTotal: Int = 0,
)
