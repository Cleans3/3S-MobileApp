package com.library.account.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request payload for member authentication.
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

/**
 * Successful authentication response returned by the library REST API.
 */
data class LoginResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("tokenType")
    val tokenType: String = "Bearer",

    @SerializedName("expiresIn")
    val expiresIn: Long = 86400L,

    @SerializedName("memberId")
    val memberId: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("memberNumber")
    val memberNumber: String
)

/**
 * Generic API wrapper for responses that return status and message.
 */
data class ApiResponse<T>(
    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: T? = null
)
