package com.library.app.data.model

import com.google.gson.annotations.SerializedName

data class SignInRequest(
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
    @SerializedName("refreshToken")
    val refreshToken: String? = null,
    @SerializedName("member")
    val member: MemberProfile
)

data class MemberProfile(
    @SerializedName("id")
    val id: String,
    @SerializedName("cardNumber")
    val cardNumber: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phone")
    val phone: String = "",
    @SerializedName("membershipType")
    val membershipType: String = "Standard Reader",
    @SerializedName("status")
    val status: String = "ACTIVE",
    @SerializedName("memberSince")
    val memberSince: String = "2023-01-15",
    @SerializedName("expiresAt")
    val expiresAt: String = "2027-12-31",
    @SerializedName("borrowedBooksCount")
    val borrowedBooksCount: Int = 0,
    @SerializedName("overdueBooksCount")
    val overdueBooksCount: Int = 0,
    @SerializedName("finesDue")
    val finesDue: Double = 0.0,
    @SerializedName("recentLoans")
    val recentLoans: List<LoanItem> = emptyList()
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}

data class LoanItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("bookTitle")
    val bookTitle: String,
    @SerializedName("author")
    val author: String,
    @SerializedName("dueDate")
    val dueDate: String,
    @SerializedName("isOverdue")
    val isOverdue: Boolean = false
)
