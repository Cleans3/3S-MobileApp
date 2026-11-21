package vn.shelftap.app.data

/** An HTTP error response from the API, branched on [code]. */
class ApiException(
    val httpStatus: Int,
    val code: String?,
    val detail: String?,
    val retryAfterSeconds: Int?,
) : Exception(detail ?: code ?: "HTTP $httpStatus")

/** The member's session is gone (refresh failed or token rejected); the UI should show sign-in. */
class SessionEndedException : Exception("Session ended")

const val NETWORK_ERROR_MESSAGE = "Can't reach ShelfTap. Check your connection and try again."

/** User-facing text for an API error. */
fun ApiException.userMessage(): String = when (code) {
    "INVALID_CREDENTIALS" -> "Email or password is incorrect."
    "ACCOUNT_LOCKED" -> "This account is locked. Contact the library for help."
    "RATE_LIMITED" -> if (retryAfterSeconds != null) {
        "Too many attempts. Try again in $retryAfterSeconds seconds."
    } else {
        "Too many attempts. Try again later."
    }
    "TOKEN_INVALID" -> "Your session has ended. Please sign in again."
    else -> detail ?: "Something went wrong (HTTP $httpStatus). Please try again."
}
