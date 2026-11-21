package com.example.library.data

/** Body of POST api/v1/auth/login. */
data class LoginRequest(
    val email: String,
    val password: String
)

/** Response of POST api/v1/auth/login. */
data class LoginResponse(
    val token: String
)

/** Response of GET api/v1/members/me. */
data class MemberProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val membershipStatus: String? = null,
    val memberSince: String? = null
)
