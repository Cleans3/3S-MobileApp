package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request payload for POST /api/auth/login
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("deviceId")
    val deviceId: String
)

/**
 * Request payload for POST /api/auth/refresh
 */
data class RefreshRequest(
    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("deviceId")
    val deviceId: String
)

/**
 * Request payload for POST /api/auth/logout
 */
data class LogoutRequest(
    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * Response payload for login and refresh endpoints.
 * Shape: {"accessToken","accessExpiresIn","refreshToken","mustChangePassword"}
 */
data class AuthResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("accessExpiresIn")
    val accessExpiresIn: Long,

    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("mustChangePassword")
    val mustChangePassword: Boolean
)

/**
 * Response payload for GET /api/me
 * Shape: {"memberId","fullName","email","memberType","status","historyEnabled","mustChangePassword"}
 */
data class MemberProfile(
    @SerializedName("memberId")
    val memberId: String,

    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("memberType")
    val memberType: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("historyEnabled")
    val historyEnabled: Boolean,

    @SerializedName("mustChangePassword")
    val mustChangePassword: Boolean
)

/**
 * Request payload for POST /api/me/password
 * Shape: {"currentPassword","newPassword"}
 */
data class ChangePasswordRequest(
    @SerializedName("currentPassword")
    val currentPassword: String,

    @SerializedName("newPassword")
    val newPassword: String
)

/**
 * Error shape returned by the API:
 * {"type","title","status","detail","code"}
 */
data class ApiError(
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("status")
    val status: Int? = null,

    @SerializedName("detail")
    val detail: String? = null,

    @SerializedName("code")
    val code: String? = null,

    // Extracted from Retry-After HTTP response header if present
    val retryAfterSeconds: Int? = null
)
