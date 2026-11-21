package vn.shelftap.app.data

import kotlinx.serialization.Serializable

// ---- Requests (camelCase, matching the API) ----

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
    val deviceId: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

// ---- Responses ----

/** Returned by both /api/auth/login and /api/auth/refresh. */
@Serializable
data class TokenResponse(
    val accessToken: String,
    val accessExpiresIn: Long,
    val refreshToken: String,
    val mustChangePassword: Boolean,
)

/** Returned by GET /api/me. memberId is not shown in the UI, so it is not modelled. */
@Serializable
data class MemberDto(
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean,
)

/** Error body: {"type","title","status","detail","code"}. All fields optional for safety. */
@Serializable
data class ProblemDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val code: String? = null,
)
