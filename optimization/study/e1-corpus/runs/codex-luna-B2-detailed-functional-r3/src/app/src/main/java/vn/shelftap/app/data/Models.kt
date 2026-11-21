package vn.shelftap.app.data

data class Session(val accessToken: String, val refreshToken: String, val mustChangePassword: Boolean)
data class Member(val memberId: String, val fullName: String, val email: String, val memberType: String, val status: String, val historyEnabled: Boolean, val mustChangePassword: Boolean)
data class ApiFailure(val code: String, val message: String, val status: Int, val retryAfterSeconds: Long? = null) : Exception(message)
