package vn.shelftap.app.data

import java.io.IOException

/** Maps any failure to a message that can be shown to the member. Branches on the API error code. */
fun Throwable.toUserMessage(): String = when (this) {
    is SessionExpiredException -> "Your session has expired. Please sign in again."
    is ApiException -> when (code) {
        "INVALID_CREDENTIALS" -> "Email or password is incorrect."
        "ACCOUNT_LOCKED" -> "This account is locked. Please contact the library."
        "RATE_LIMITED" -> retryAfterSeconds
            ?.let { "Too many attempts. Try again in $it seconds." }
            ?: "Too many attempts. Please try again later."
        "TOKEN_INVALID" -> "Your session has expired. Please sign in again."
        "PASSWORD_CHANGE_REQUIRED" -> "You must change your password before continuing."
        else -> message
    }
    is IOException -> "Can't reach ShelfTap. Check your connection and try again."
    else -> "Something went wrong. Please try again."
}
