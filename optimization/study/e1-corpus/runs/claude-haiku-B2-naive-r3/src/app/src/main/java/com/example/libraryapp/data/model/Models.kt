package com.example.libraryapp.data.model

import kotlinx.serialization.Serializable

/** Body of POST api/v1/auth/login. */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

/** Response of POST api/v1/auth/login. */
@Serializable
data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: String? = null,
)

/** Response of GET api/v1/members/me. */
@Serializable
data class MemberProfile(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val memberNumber: String,
    val membershipType: String? = null,
    val joinedOn: String? = null,
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}
