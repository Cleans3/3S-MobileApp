package vn.shelftap.app.data.api

import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String,
)

@Serializable
internal data class RefreshRequest(
    val refreshToken: String,
    val deviceId: String,
)

@Serializable
internal data class LogoutRequest(
    val refreshToken: String,
)

@Serializable
internal data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

@Serializable
internal data class TokenResponse(
    val accessToken: String,
    val accessExpiresIn: Long,
    val refreshToken: String,
    val mustChangePassword: Boolean,
)

@Serializable
internal data class MeResponse(
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean,
)

/** Only the error code is needed; the rest of the Problem body is ignored. */
@Serializable
internal data class ProblemResponse(
    val code: String? = null,
)
