package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

data class ApiErrorResponse(
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("message")
    val message: String? = null
)
