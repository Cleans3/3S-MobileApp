package vn.shelftap.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import vn.shelftap.app.data.model.ApiError

class ApiExceptionTest {

    @Test
    fun testParseInvalidCredentialsError() {
        val json = """
            {
                "type": "https://api.shelftap.example/errors/invalid-credentials",
                "title": "Invalid Credentials",
                "status": 401,
                "detail": "Email or password incorrect",
                "code": "INVALID_CREDENTIALS"
            }
        """.trimIndent()

        val exception = ApiException.parse(json, 401, null)
        assertEquals(401, exception.httpStatusCode)
        assertEquals("INVALID_CREDENTIALS", exception.error.code)
        assertEquals("Email or password incorrect", exception.error.detail)
        assertEquals("Email or password incorrect", exception.error.toUserMessage())
    }

    @Test
    fun testParseRateLimitedWithRetryAfterHeader() {
        val json = """
            {
                "type": "https://api.shelftap.example/errors/rate-limited",
                "title": "Too Many Requests",
                "status": 429,
                "detail": "Rate limit exceeded",
                "code": "RATE_LIMITED"
            }
        """.trimIndent()

        val exception = ApiException.parse(json, 429, 60)
        assertEquals(429, exception.httpStatusCode)
        assertEquals("RATE_LIMITED", exception.error.code)
        assertEquals(60, exception.error.retryAfterSeconds)
        assertEquals(
            "Too many requests. Please wait 60 seconds before trying again.",
            exception.error.toUserMessage()
        )
    }

    @Test
    fun testParseAccountLockedError() {
        val json = """
            {
                "type": "https://api.shelftap.example/errors/account-locked",
                "title": "Account Locked",
                "status": 423,
                "detail": "Member account has been locked",
                "code": "ACCOUNT_LOCKED"
            }
        """.trimIndent()

        val exception = ApiException.parse(json, 423, null)
        assertEquals(423, exception.httpStatusCode)
        assertEquals("ACCOUNT_LOCKED", exception.error.code)
        assertEquals(
            "Member account has been locked",
            exception.error.toUserMessage()
        )
    }

    @Test
    fun testParsePasswordChangeRequiredError() {
        val json = """
            {
                "type": "https://api.shelftap.example/errors/password-change-required",
                "title": "Password Change Required",
                "status": 403,
                "detail": "You must change your password before proceeding",
                "code": "PASSWORD_CHANGE_REQUIRED"
            }
        """.trimIndent()

        val exception = ApiException.parse(json, 403, null)
        assertEquals(403, exception.httpStatusCode)
        assertEquals("PASSWORD_CHANGE_REQUIRED", exception.error.code)
        assertEquals(
            "You must change your password before proceeding",
            exception.error.toUserMessage()
        )
    }

    @Test
    fun testParseTokenInvalidError() {
        val json = """
            {
                "type": "https://api.shelftap.example/errors/token-invalid",
                "title": "Token Invalid",
                "status": 401,
                "detail": "The access token is expired or invalid",
                "code": "TOKEN_INVALID"
            }
        """.trimIndent()

        val exception = ApiException.parse(json, 401, null)
        assertEquals(401, exception.httpStatusCode)
        assertEquals("TOKEN_INVALID", exception.error.code)
        assertEquals(
            "The access token is expired or invalid",
            exception.error.toUserMessage()
        )
    }
}
