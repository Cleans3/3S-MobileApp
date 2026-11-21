package vn.shelftap.app.data.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val code: String? = null
)

class ApiException(
    val error: ApiErrorResponse,
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null
) : Exception(error.detail ?: error.title ?: "API Error code=${error.code} status=${error.status}", cause)

object ApiErrorCodes {
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val TOKEN_INVALID = "TOKEN_INVALID"
    const val PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED"
}
