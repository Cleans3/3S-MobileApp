package com.example.libraryapp.data.api

/** Body of POST api/v1/auth/login. */
data class LoginRequest(
    val email: String,
    val password: String
)

/** Response of POST api/v1/auth/login. The token is sent back as a Bearer token. */
data class LoginResponse(
    val token: String
)

/** Response of GET api/v1/members/me. */
data class MemberProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val membershipNumber: String?,
    val membershipType: String?,
    val memberSince: String?,
    val activeLoans: Int?
)
