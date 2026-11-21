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
