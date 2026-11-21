package vn.shelftap.app.data.remote

import com.google.gson.Gson
import vn.shelftap.app.data.model.ApiError

/**
 * Typed exception representing an API error response.
 */
class ApiException(
    val error: ApiError,
    val httpStatusCode: Int,
    cause: Throwable? = null
) : Exception(
    error.detail ?: error.title ?: "API Error (status=$httpStatusCode, code=${error.code})",
    cause
) {
    companion object {
        private val gson = Gson()

        fun parse(
            errorBody: String?,
            statusCode: Int,
            retryAfterSeconds: Int?,
            cause: Throwable? = null
        ): ApiException {
            val parsedError = try {
                if (!errorBody.isNullOrBlank()) {
                    gson.fromJson(errorBody, ApiError::class.java)
                } else {
                    ApiError(status = statusCode)
                }
            } catch (e: Exception) {
                ApiError(status = statusCode, detail = errorBody)
            }

            val finalError = if (retryAfterSeconds != null) {
                parsedError.copy(retryAfterSeconds = retryAfterSeconds)
            } else {
                parsedError
            }

            return ApiException(finalError, statusCode, cause)
        }
    }
}

/**
 * Converts an ApiError code and detail into a friendly user-facing message.
 */
fun ApiError.toUserMessage(fallbackMessage: String = "An unexpected error occurred."): String {
    return when (code) {
        ErrorCodes.INVALID_CREDENTIALS -> {
            detail ?: "Invalid email or password. Please try again."
        }
        ErrorCodes.ACCOUNT_LOCKED -> {
            detail ?: "Your account has been locked. Please contact library support."
        }
        ErrorCodes.RATE_LIMITED -> {
            val seconds = retryAfterSeconds
            if (seconds != null) {
                "Too many requests. Please wait $seconds seconds before trying again."
            } else {
                detail ?: "Too many requests. Please try again later."
            }
        }
        ErrorCodes.TOKEN_INVALID -> {
            detail ?: "Session expired. Please sign in again."
        }
        ErrorCodes.PASSWORD_CHANGE_REQUIRED -> {
            detail ?: "You must change your password before continuing."
        }
        else -> {
            detail ?: title ?: fallbackMessage
        }
    }
}
