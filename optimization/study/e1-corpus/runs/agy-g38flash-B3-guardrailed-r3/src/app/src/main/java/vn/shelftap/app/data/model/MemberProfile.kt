package vn.shelftap.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MemberProfile(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean
)
