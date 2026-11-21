package vn.shelftap.app.data

import kotlinx.serialization.Serializable

@Serializable data class Loan(val loanId: String, val copyId: String, val titleId: String, val title: String, val barcode: String, val outAt: String, val dueAt: String, val returnedAt: String? = null, val renewals: Int, val renewable: Boolean)
@Serializable data class HistoryItem(val titleId: String, val title: String, val outAt: String, val returnedAt: String)
@Serializable data class Member(val memberId: String, val fullName: String, val email: String, val memberType: String, val status: String, val historyEnabled: Boolean, val mustChangePassword: Boolean)
@Serializable data class TitleDetail(val titleId: String, val isbn: String, val title: String, val authors: List<String>, val subjects: List<String>, val coverUrl: String? = null, val copiesAvailable: Int, val copiesTotal: Int)
