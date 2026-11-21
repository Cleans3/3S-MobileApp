package com.example.libraryapp.data.model

import com.google.gson.annotations.SerializedName

data class MemberDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("memberNumber")
    val memberNumber: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("membershipTier")
    val membershipTier: String = "Standard",
    @SerializedName("status")
    val status: String = "Active",
    @SerializedName("joinedDate")
    val joinedDate: String,
    @SerializedName("expiryDate")
    val expiryDate: String,
    @SerializedName("borrowedBooksCount")
    val borrowedBooksCount: Int = 0,
    @SerializedName("maxLoans")
    val maxLoans: Int = 10,
    @SerializedName("outstandingFines")
    val outstandingFines: Double = 0.0
) {
    val fullName: String
        get() = "$firstName $lastName"
}

data class BorrowedBookDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("author")
    val author: String,
    @SerializedName("isbn")
    val isbn: String,
    @SerializedName("borrowDate")
    val borrowDate: String,
    @SerializedName("dueDate")
    val dueDate: String,
    @SerializedName("isOverdue")
    val isOverdue: Boolean = false,
    @SerializedName("coverUrl")
    val coverUrl: String? = null
)

data class MemberProfileResponse(
    @SerializedName("member")
    val member: MemberDto,
    @SerializedName("activeLoans")
    val activeLoans: List<BorrowedBookDto> = emptyList(),
    @SerializedName("loanHistoryCount")
    val loanHistoryCount: Int = 0,
    @SerializedName("reservationsCount")
    val reservationsCount: Int = 0
)
