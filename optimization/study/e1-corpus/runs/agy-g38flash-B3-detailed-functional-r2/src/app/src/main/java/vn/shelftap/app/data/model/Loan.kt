package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

data class Loan(
    @SerializedName("loanId")
    val loanId: String,
    @SerializedName("copyId")
    val copyId: String,
    @SerializedName("titleId")
    val titleId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("barcode")
    val barcode: String,
    @SerializedName("outAt")
    val outAt: String,
    @SerializedName("dueAt")
    val dueAt: String,
    @SerializedName("returnedAt")
    val returnedAt: String? = null,
    @SerializedName("renewals")
    val renewals: Int,
    @SerializedName("renewable")
    val renewable: Boolean
)
