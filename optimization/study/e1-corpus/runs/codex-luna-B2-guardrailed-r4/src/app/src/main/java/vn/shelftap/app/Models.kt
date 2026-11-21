package vn.shelftap.app

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val mustChangePassword: Boolean
)

data class MemberProfile(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean
)

class ApiException(
    val code: String?,
    val status: Int,
    val retryAfterSeconds: Int? = null
) : Exception()

class SessionExpiredException : Exception()
