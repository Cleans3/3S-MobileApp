package vn.shelftap.app.data.api

/** An HTTP error answered by the ShelfTap API. [code] is the Problem `code` field, e.g. TOKEN_INVALID. */
internal class ApiException(
    val httpStatus: Int,
    val code: String?,
    val retryAfterSeconds: Int?,
) : Exception("API error $httpStatus ${code.orEmpty()}")

/** The member's session can no longer be used; the app must show the sign-in screen. */
internal class SessionEndedException : Exception("Session ended")
