package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Standard error response from the ShelfTap API.
 * Format: {"type","title","status","detail","code"}
 */
data class ApiErrorResponse(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("detail")
    val detail: String? = null,
    @SerializedName("code")
    val code: String? = null
)

object ApiErrorCodes {
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val TOKEN_INVALID = "TOKEN_INVALID"
    const val PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED"
}

class ApiException(
    val error: ApiErrorResponse,
    val httpCode: Int,
    val retryAfterSeconds: Long? = null
) : Exception(error.detail ?: error.title ?: "API Error: ${error.code}")
