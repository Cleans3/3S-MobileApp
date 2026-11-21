package vn.shelftap.app.data.api.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String
)

@Serializable
data class TokenResponse(
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
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class MemberProfileResponse(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean = false,
    val mustChangePassword: Boolean = false
)

@Serializable
data class UserSession(
    val accessToken: String,
    val accessExpiresIn: Long,
    val refreshToken: String,
    val mustChangePassword: Boolean,
    val profile: MemberProfileResponse? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
