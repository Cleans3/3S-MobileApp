package com.example.libraryapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("member") val member: MemberProfile? = null
)

@Serializable
data class MemberProfile(
    @SerialName("id") val id: String,
    @SerialName("card_number") val cardNumber: String,
    @SerialName("email") val email: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("membership_tier") val membershipTier: String = "Standard",
    @SerialName("member_since") val memberSince: String,
    @SerialName("status") val status: String = "Active",
    @SerialName("active_loans_count") val activeLoansCount: Int = 0,
    @SerialName("holds_count") val holdsCount: Int = 0,
    @SerialName("outstanding_fines") val outstandingFines: Double = 0.0,
    @SerialName("loans") val loans: List<LoanItem> = emptyList()
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}

@Serializable
data class LoanItem(
    @SerialName("id") val id: String,
    @SerialName("book_title") val bookTitle: String,
    @SerialName("author") val author: String,
    @SerialName("isbn") val isbn: String,
    @SerialName("due_date") val dueDate: String,
    @SerialName("is_overdue") val isOverdue: Boolean = false
)

@Serializable
data class ApiError(
    @SerialName("message") val message: String,
    @SerialName("code") val code: String? = null
)
