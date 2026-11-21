package vn.shelftap.app.data

/** Failures from talking to the server. Messages never contain tokens or personal data. */
sealed class RemoteException(message: String) : Exception(message)

class ApiException(
    val httpStatus: Int,
    val code: String,
    val retryAfterSeconds: Int? = null,
) : RemoteException("API error $code")

class NetworkException : RemoteException("Network unavailable")

/** Thrown when the refresh token is rejected or missing: the member must sign in again. */
class SessionEndedException : Exception("Session ended")

/** What the UI needs to show: an error code plus optional retry delay. */
data class AppError(
    val code: String,
    val retryAfterSeconds: Int? = null,
)

fun RemoteException.toAppError(): AppError = when (this) {
    is ApiException -> AppError(code, retryAfterSeconds)
    is NetworkException -> AppError(ErrorCodes.NETWORK)
}
