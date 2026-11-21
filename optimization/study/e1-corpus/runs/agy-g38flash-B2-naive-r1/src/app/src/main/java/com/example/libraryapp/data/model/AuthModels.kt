package com.example.libraryapp.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class AuthResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("tokenType")
    val tokenType: String = "Bearer",
    @SerializedName("expiresIn")
    val expiresIn: Long = 86400,
    @SerializedName("member")
    val member: MemberDto
)

data class LogoutResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)
