package vn.shelftap.app.data

import java.io.IOException

/** An HTTP error answered by the ShelfTap API. Branch on [code], not on [detail]. */
class ApiException(
    val httpStatus: Int,
    val code: String?,
    val detail: String?,
    val retryAfterSeconds: Int?,
) : Exception(detail ?: code ?: "HTTP $httpStatus") {

    val isTokenInvalid: Boolean
        get() = httpStatus == 401 && code == "TOKEN_INVALID"

    val isPasswordChangeRequired: Boolean
        get() = code == "PASSWORD_CHANGE_REQUIRED"
}

/** Raised when the session can no longer be renewed; the member must sign in again. */
class SessionEndedException : Exception("Session ended")

/** Message shown to the member for any failure. */
fun Throwable.userMessage(): String = when (this) {
    is ApiException -> when (code) {
        "INVALID_CREDENTIALS" -> "Email or password is incorrect."
        "ACCOUNT_LOCKED" -> "This account is locked. Please contact the library."
        "RATE_LIMITED" -> retryAfterSeconds
            ?.let { "Too many attempts. Try again in $it seconds." }
            ?: "Too many attempts. Try again later."
        "TOKEN_INVALID" -> "Your session has ended. Please sign in again."
        "PASSWORD_CHANGE_REQUIRED" -> "You must change your password first."
        else -> detail ?: "Request failed (HTTP $httpStatus)."
    }
    is SessionEndedException -> "Your session has ended. Please sign in again."
    is IOException -> "No network connection. Check your connection and try again."
    else -> "Something went wrong. Please try again."
}
