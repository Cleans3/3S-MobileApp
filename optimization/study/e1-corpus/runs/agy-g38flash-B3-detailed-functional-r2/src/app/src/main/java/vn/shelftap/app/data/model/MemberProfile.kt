package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

data class MemberProfile(
    @SerializedName("memberId")
    val memberId: String,
    @SerializedName("fullName")
    val fullName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("memberType")
    val memberType: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("historyEnabled")
    val historyEnabled: Boolean,
    @SerializedName("mustChangePassword")
    val mustChangePassword: Boolean
)
