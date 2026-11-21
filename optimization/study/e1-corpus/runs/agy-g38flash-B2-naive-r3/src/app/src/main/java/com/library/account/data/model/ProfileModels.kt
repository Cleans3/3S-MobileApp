package com.library.account.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the comprehensive profile of a library member.
 */
data class MemberProfile(
    @SerializedName("id")
    val id: String,

    @SerializedName("memberNumber")
    val memberNumber: String,

    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("phoneNumber")
    val phoneNumber: String,

    @SerializedName("membershipType")
    val membershipType: String,

    @SerializedName("membershipStatus")
    val membershipStatus: String, // e.g. "ACTIVE", "EXPIRED", "SUSPENDED"

    @SerializedName("expiryDate")
    val expiryDate: String,

    @SerializedName("joinedDate")
    val joinedDate: String,

    @SerializedName("homeBranch")
    val homeBranch: String,

    @SerializedName("activeLoansCount")
    val activeLoansCount: Int,

    @SerializedName("holdsReadyCount")
    val holdsReadyCount: Int,

    @SerializedName("outstandingFees")
    val outstandingFees: Double,

    @SerializedName("borrowedBooks")
    val borrowedBooks: List<BorrowedBook> = emptyList()
) {
    val isActive: Boolean
        get() = membershipStatus.equals("ACTIVE", ignoreCase = true)
}

/**
 * Represents a book or item currently borrowed by the member.
 */
data class BorrowedBook(
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

    @SerializedName("daysRemaining")
    val daysRemaining: Int,

    @SerializedName("isOverdue")
    val isOverdue: Boolean = false,

    @SerializedName("renewalsRemaining")
    val renewalsRemaining: Int = 2,

    @SerializedName("coverColorHex")
    val coverColorHex: String = "#1E88E5"
)
