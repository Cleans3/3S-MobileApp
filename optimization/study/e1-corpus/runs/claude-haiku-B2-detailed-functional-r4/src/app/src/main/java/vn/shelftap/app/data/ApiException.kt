package vn.shelftap.app.data

import java.io.IOException

const val NETWORK_MESSAGE = "Can't reach ShelfTap. Check your connection and try again."
const val SESSION_ENDED_NOTICE = "Your session has ended. Please sign in again."

/** A non-2xx answer from the API. Branch on [code], never on [detail]. */
class ApiException(
    val status: Int,
    val code: String?,
    val detail: String?,
    val retryAfterSeconds: Long?,
) : Exception(code ?: "HTTP $status")

/** The refresh token was rejected or could not be used, so the member must sign in again. */
class SessionEndedException : Exception("Session ended")

fun Throwable.userMessage(): String = when (this) {
    is ApiException -> when (code) {
        "INVALID_CREDENTIALS" -> "Email or password is not correct."
        "ACCOUNT_LOCKED" -> "This account is locked. Please contact the library."
        "RATE_LIMITED" -> retryAfterSeconds
            ?.let { "Too many attempts. Try again in $it seconds." }
            ?: "Too many attempts. Try again later."
        "TOKEN_INVALID" -> SESSION_ENDED_NOTICE
        "PASSWORD_CHANGE_REQUIRED" -> "You must change your password first."
        else -> detail ?: "Something went wrong (HTTP $status). Please try again."
    }
    is SessionEndedException -> SESSION_ENDED_NOTICE
    is IOException -> NETWORK_MESSAGE
    else -> "Something went wrong. Please try again."
}
