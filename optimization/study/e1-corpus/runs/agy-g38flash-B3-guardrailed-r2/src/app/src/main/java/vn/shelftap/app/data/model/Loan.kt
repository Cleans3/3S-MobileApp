package vn.shelftap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Loan(
    @SerialName("loanId") val loanId: String,
    @SerialName("copyId") val copyId: String,
    @SerialName("titleId") val titleId: String,
    @SerialName("title") val title: String,
    @SerialName("barcode") val barcode: String,
    @SerialName("outAt") val outAt: String,
    @SerialName("dueAt") val dueAt: String,
    @SerialName("returnedAt") val returnedAt: String? = null,
    @SerialName("renewals") val renewals: Int,
    @SerialName("renewable") val renewable: Boolean
)
