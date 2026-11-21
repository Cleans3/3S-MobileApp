package vn.shelftap.app.data

data class TokenPair(
    val accessToken: String,
    val accessExpiresInSeconds: Long,
    val refreshToken: String,
    val mustChangePassword: Boolean,
)

data class Member(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean,
)
