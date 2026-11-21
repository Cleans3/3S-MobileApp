package vn.shelftap.app.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Loan(val loanId: String, val copyId: String, val titleId: String, val title: String, val barcode: String, val outAt: String, val dueAt: String, val returnedAt: String? = null, val renewals: Int, val renewable: Boolean)

@JsonClass(generateAdapter = true)
data class HistoryItem(val titleId: String, val title: String, val outAt: String, val returnedAt: String?)

@JsonClass(generateAdapter = true)
data class Member(val memberId: String, val fullName: String, val email: String, val memberType: String, val status: String, val historyEnabled: Boolean, val mustChangePassword: Boolean)

@JsonClass(generateAdapter = true)
data class TitleDetail(val titleId: String, val isbn: String?, val title: String, val authors: List<String>, val subjects: List<String>, val coverUrl: String?, val copiesAvailable: Int, val copiesTotal: Int)
