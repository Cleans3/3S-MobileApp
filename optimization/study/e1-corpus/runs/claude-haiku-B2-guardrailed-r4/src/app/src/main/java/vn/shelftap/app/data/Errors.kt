package vn.shelftap.app.data

import java.io.IOException

/** An error answer from the API. [code] is the machine-readable error code used for branching. */
class ApiException(
    val code: String?,
    val httpStatus: Int,
    val retryAfterSeconds: Int?,
) : Exception("API error ${code ?: "HTTP $httpStatus"}")

class NetworkException(cause: IOException) : Exception("Network unavailable", cause)

/** Thrown when the session cannot be renewed; the member has to sign in again. */
class SignedOutException : Exception("The session has ended")

/** What the UI shows to the member. Strings are resolved in the UI layer. */
sealed interface Failure {
    data object MissingFields : Failure
    data object InvalidCredentials : Failure
    data object CurrentPasswordWrong : Failure
    data object AccountLocked : Failure
    data class RateLimited(val retryAfterSeconds: Int?) : Failure
    data object Network : Failure
    data object SessionEnded : Failure
    data object Unknown : Failure
}

fun Throwable.toFailure(duringPasswordChange: Boolean = false): Failure = when (this) {
    is ApiException -> when (code) {
        "INVALID_CREDENTIALS" ->
            if (duringPasswordChange) Failure.CurrentPasswordWrong else Failure.InvalidCredentials
        "ACCOUNT_LOCKED" -> Failure.AccountLocked
        "RATE_LIMITED" -> Failure.RateLimited(retryAfterSeconds)
        else -> Failure.Unknown
    }
    is NetworkException -> Failure.Network
    is SignedOutException -> Failure.SessionEnded
    else -> Failure.Unknown
}
