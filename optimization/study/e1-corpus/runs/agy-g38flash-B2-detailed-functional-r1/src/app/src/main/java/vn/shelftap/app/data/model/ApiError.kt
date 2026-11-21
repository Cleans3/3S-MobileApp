package vn.shelftap.app.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * REST API Error response shape:
 * {"type","title","status","detail","code"}
 */
data class ErrorResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("detail") val detail: String? = null,
    @SerializedName("code") val code: String? = null
) {
    companion object {
        const val CODE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
        const val CODE_ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
        const val CODE_RATE_LIMITED = "RATE_LIMITED"
        const val CODE_TOKEN_INVALID = "TOKEN_INVALID"
        const val CODE_PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED"

        fun parse(json: String?): ErrorResponse? {
            if (json.isNullOrBlank()) return null
            return try {
                Gson().fromJson(json, ErrorResponse::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * App-level representation of an API error after branching on error code and headers.
 */
data class ShelfTapError(
    val code: String?,
    val message: String,
    val retryAfterSeconds: Int? = null,
    val rawDetail: String? = null
) {
    companion object {
        fun fromErrorResponse(
            errorResponse: ErrorResponse?,
            httpStatusCode: Int,
            retryAfterHeader: String? = null
        ): ShelfTapError {
            val retrySeconds = retryAfterHeader?.toIntOrNull()
            val code = errorResponse?.code
            val detail = errorResponse?.detail

            val message = when (code) {
                ErrorResponse.CODE_INVALID_CREDENTIALS -> {
                    detail ?: "Invalid email or password. Please check your credentials and try again."
                }
                ErrorResponse.CODE_ACCOUNT_LOCKED -> {
                    detail ?: "Your library account has been locked. Please contact library support."
                }
                ErrorResponse.CODE_RATE_LIMITED -> {
                    if (retrySeconds != null && retrySeconds > 0) {
                        "Too many requests. Please wait $retrySeconds seconds before trying again."
                    } else {
                        detail ?: "Rate limit reached. Please wait a moment before trying again."
                    }
                }
                ErrorResponse.CODE_TOKEN_INVALID -> {
                    detail ?: "Your session has expired. Please sign in again."
                }
                ErrorResponse.CODE_PASSWORD_CHANGE_REQUIRED -> {
                    detail ?: "A password change is required before proceeding."
                }
                else -> {
                    detail ?: errorResponse?.title ?: when (httpStatusCode) {
                        401 -> "Authentication failed. Please sign in again."
                        403 -> "You do not have permission to perform this action."
                        423 -> "Account locked. Please contact library support."
                        429 -> {
                            if (retrySeconds != null && retrySeconds > 0) {
                                "Too many attempts. Please try again in $retrySeconds seconds."
                            } else {
                                "Too many attempts. Please try again later."
                            }
                        }
                        in 500..599 -> "Server error occurred. Please try again later."
                        else -> "An unexpected error occurred ($httpStatusCode)."
                    }
                }
            }

            return ShelfTapError(
                code = code,
                message = message,
                retryAfterSeconds = retrySeconds,
                rawDetail = detail
            )
        }

        fun networkError(e: Throwable): ShelfTapError {
            return ShelfTapError(
                code = "NETWORK_ERROR",
                message = e.localizedMessage ?: "Unable to connect to the library server. Please check your connection."
            )
        }
    }
}
