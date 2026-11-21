package vn.shelftap.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val accessExpiresIn: Long,
    val refreshToken: String,
    val mustChangePassword: Boolean
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
    val deviceId: String
)

@Serializable
data class LogoutRequest(
    val refreshToken: String
)

@Serializable
data class MemberProfile(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class ApiErrorResponse(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val code: String? = null
)
