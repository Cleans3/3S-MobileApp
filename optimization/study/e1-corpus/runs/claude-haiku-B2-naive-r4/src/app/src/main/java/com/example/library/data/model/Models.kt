package com.example.library.data.model

// Field names match the JSON keys used by the library REST API.
// Fields are nullable because Gson does not enforce Kotlin non-null types on deserialization.

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String?
)

data class MemberProfile(
    val id: String?,
    val fullName: String?,
    val email: String?,
    val memberNumber: String?,
    val membershipStatus: String?,
    val joinedOn: String?
)
