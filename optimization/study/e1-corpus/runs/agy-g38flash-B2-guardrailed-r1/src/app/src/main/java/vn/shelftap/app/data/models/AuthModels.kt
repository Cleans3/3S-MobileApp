package vn.shelftap.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("deviceId") val deviceId: String
)

@Serializable
data class RefreshRequest(
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("deviceId") val deviceId: String
)

@Serializable
data class LogoutRequest(
    @SerialName("refreshToken") val refreshToken: String
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("currentPassword") val currentPassword: String,
    @SerialName("newPassword") val newPassword: String
)

@Serializable
data class AuthTokenResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("accessExpiresIn") val accessExpiresIn: Long,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("mustChangePassword") val mustChangePassword: Boolean
)

@Serializable
data class MemberProfileResponse(
    @SerialName("memberId") val memberId: String,
    @SerialName("fullName") val fullName: String,
    @SerialName("email") val email: String,
    @SerialName("memberType") val memberType: String,
    @SerialName("status") val status: String,
    @SerialName("historyEnabled") val historyEnabled: Boolean,
    @SerialName("mustChangePassword") val mustChangePassword: Boolean
)

@Serializable
data class ApiErrorResponse(
    @SerialName("type") val type: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("status") val status: Int? = null,
    @SerialName("detail") val detail: String? = null,
    @SerialName("code") val code: String? = null
)

sealed class AppError(open val message: String) {
    data class InvalidCredentials(override val message: String) : AppError(message)
    data class AccountLocked(override val message: String) : AppError(message)
    data class RateLimited(val retryAfterSeconds: Long?, override val message: String) : AppError(message)
    data class TokenInvalid(override val message: String) : AppError(message)
    data class PasswordChangeRequired(override val message: String) : AppError(message)
    data class Network(override val message: String) : AppError(message)
    data class Unknown(val code: String?, override val message: String) : AppError(message)
}
