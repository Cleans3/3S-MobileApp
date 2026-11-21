package vn.shelftap.app.data

data class LoginRequest(val email: String, val password: String, val deviceId: String)
data class RefreshRequest(val refreshToken: String, val deviceId: String)
data class LogoutRequest(val refreshToken: String)
data class PasswordRequest(val currentPassword: String, val newPassword: String)

data class AuthResponse(
    val accessToken: String,
    val accessExpiresIn: Long,
    val refreshToken: String,
    val mustChangePassword: Boolean
)

data class Member(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean
)

data class ApiError(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val code: String? = null
)

class ShelfTapException(val error: ApiError, cause: Throwable? = null) : Exception(error.detail ?: error.title, cause)
