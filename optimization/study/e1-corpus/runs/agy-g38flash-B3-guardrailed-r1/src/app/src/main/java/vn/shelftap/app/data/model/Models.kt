package vn.shelftap.app.data.model

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
    val renewals: Int,
    val renewable: Boolean
)

@Serializable
data class HistoryItem(
    val titleId: String,
    val title: String,
    val outAt: String,
    val returnedAt: String? = null
)

@Serializable
data class MemberProfile(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean
)

@Serializable
data class PrivacyRequest(
    val historyEnabled: Boolean
)

@Serializable
data class BookDetail(
    val titleId: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val subjects: List<String>,
    val coverUrl: String? = null,
    val copiesAvailable: Int,
    val copiesTotal: Int
)

@Serializable
data class ApiError(
    val code: String? = null,
    val message: String? = null
)
