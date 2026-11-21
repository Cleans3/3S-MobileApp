package vn.shelftap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("memberId") val memberId: String,
    @SerialName("fullName") val fullName: String,
    @SerialName("email") val email: String,
    @SerialName("memberType") val memberType: String,
    @SerialName("status") val status: String,
    @SerialName("historyEnabled") val historyEnabled: Boolean,
    @SerialName("mustChangePassword") val mustChangePassword: Boolean
)
