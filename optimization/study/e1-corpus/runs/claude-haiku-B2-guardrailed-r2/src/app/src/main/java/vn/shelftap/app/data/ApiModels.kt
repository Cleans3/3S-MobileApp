package vn.shelftap.app.data

/** An error answer from the ShelfTap API. Branch on [code]; the detail text is never shown or logged. */
class ApiException(
    val code: String,
    val httpStatus: Int,
    val retryAfterSeconds: Int? = null,
) : Exception("ShelfTap API error $code")

/** Thrown when the stored session is gone (sign-in is required again). */
class SessionEndedException : Exception("Session ended")

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val mustChangePassword: Boolean,
)

data class Member(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean,
)
