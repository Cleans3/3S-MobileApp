package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("deviceId") val deviceId: String
)

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("accessExpiresIn") val accessExpiresIn: Long,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("mustChangePassword") val mustChangePassword: Boolean
)

data class RefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("deviceId") val deviceId: String
)

data class LogoutRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class ProfileResponse(
    @SerializedName("memberId") val memberId: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("memberType") val memberType: String,
    @SerializedName("status") val status: String,
    @SerializedName("historyEnabled") val historyEnabled: Boolean,
    @SerializedName("mustChangePassword") val mustChangePassword: Boolean
)

data class ChangePasswordRequest(
    @SerializedName("currentPassword") val currentPassword: String,
    @SerializedName("newPassword") val newPassword: String
)
