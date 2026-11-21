package vn.shelftap.app

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.shelftap.app.data.model.ChangePasswordRequest
import vn.shelftap.app.data.model.ErrorResponse
import vn.shelftap.app.data.model.LoginRequest
import vn.shelftap.app.data.model.LoginResponse
import vn.shelftap.app.data.model.LogoutRequest
import vn.shelftap.app.data.model.ProfileResponse
import vn.shelftap.app.data.model.RefreshRequest
import vn.shelftap.app.data.model.ShelfTapError

class ApiErrorParsingTest {

    private val gson = Gson()

    @Test
    fun testInvalidCredentialsErrorParsing() {
        val json = """
            {
                "type": "https://api.shelftap.example/errors/invalid-credentials",
                "title": "Unauthorized",
                "status": 401,
                "detail": "Invalid member email or password.",
                "code": "INVALID_CREDENTIALS"
            }
        """.trimIndent()

        val parsed = ErrorResponse.parse(json)
        assertNotNull(parsed)
        assertEquals("INVALID_CREDENTIALS", parsed?.code)
        assertEquals(401, parsed?.status)

        val shelfTapError = ShelfTapError.fromErrorResponse(parsed, 401)
        assertEquals("INVALID_CREDENTIALS", shelfTapError.code)
        assertEquals("Invalid member email or password.", shelfTapError.message)
    }

    @Test
    fun testAccountLockedErrorParsing() {
        val json = """
            {
                "type": "https://api.shelftap.example/errors/account-locked",
                "title": "Locked",
                "status": 423,
                "detail": "Account locked due to consecutive failed attempts.",
                "code": "ACCOUNT_LOCKED"
            }
        """.trimIndent()

        val parsed = ErrorResponse.parse(json)
        assertNotNull(parsed)
        assertEquals("ACCOUNT_LOCKED", parsed?.code)

        val shelfTapError = ShelfTapError.fromErrorResponse(parsed, 423)
        assertEquals("ACCOUNT_LOCKED", shelfTapError.code)
        assertEquals("Account locked due to consecutive failed attempts.", shelfTapError.message)
    }

    @Test
    fun testRateLimitedWithRetryAfterHeader() {
        val json = """
            {
                "type": "https://api.shelftap.example/errors/rate-limited",
                "title": "Too Many Requests",
                "status": 429,
                "detail": "Rate limit exceeded.",
                "code": "RATE_LIMITED"
            }
        """.trimIndent()

        val parsed = ErrorResponse.parse(json)
        val shelfTapError = ShelfTapError.fromErrorResponse(parsed, 429, retryAfterHeader = "30")

        assertEquals("RATE_LIMITED", shelfTapError.code)
        assertEquals(30, shelfTapError.retryAfterSeconds)
        assertTrue(shelfTapError.message.contains("30 seconds"))
    }

    @Test
    fun testTokenInvalidErrorParsing() {
        val json = """
            {
                "type": "https://api.shelftap.example/errors/token-invalid",
                "title": "Unauthorized",
                "status": 401,
                "detail": "The access token provided has expired or is invalid.",
                "code": "TOKEN_INVALID"
            }
        """.trimIndent()

        val parsed = ErrorResponse.parse(json)
        val shelfTapError = ShelfTapError.fromErrorResponse(parsed, 401)

        assertEquals("TOKEN_INVALID", shelfTapError.code)
        assertEquals("The access token provided has expired or is invalid.", shelfTapError.message)
    }

    @Test
    fun testPasswordChangeRequiredErrorParsing() {
        val json = """
            {
                "type": "https://api.shelftap.example/errors/password-change-required",
                "title": "Forbidden",
                "status": 403,
                "detail": "Password change required before accessing member resources.",
                "code": "PASSWORD_CHANGE_REQUIRED"
            }
        """.trimIndent()

        val parsed = ErrorResponse.parse(json)
        val shelfTapError = ShelfTapError.fromErrorResponse(parsed, 403)

        assertEquals("PASSWORD_CHANGE_REQUIRED", shelfTapError.code)
        assertEquals("Password change required before accessing member resources.", shelfTapError.message)
    }

    @Test
    fun testLoginSerializationAndDeserialization() {
        val req = LoginRequest("member@library.org", "pass123", "device-uuid-123")
        val reqJson = gson.toJson(req)
        assertTrue(reqJson.contains("\"email\":\"member@library.org\""))
        assertTrue(reqJson.contains("\"password\":\"pass123\""))
        assertTrue(reqJson.contains("\"deviceId\":\"device-uuid-123\""))

        val respJson = """
            {
                "accessToken": "acc-token-abc",
                "accessExpiresIn": 900,
                "refreshToken": "ref-token-xyz",
                "mustChangePassword": true
            }
        """.trimIndent()
        val resp = gson.fromJson(respJson, LoginResponse::class.java)
        assertEquals("acc-token-abc", resp.accessToken)
        assertEquals(900L, resp.accessExpiresIn)
        assertEquals("ref-token-xyz", resp.refreshToken)
        assertTrue(resp.mustChangePassword)
    }

    @Test
    fun testRefreshAndLogoutSerialization() {
        val refreshReq = RefreshRequest("old-ref-token", "device-uuid")
        val refreshJson = gson.toJson(refreshReq)
        assertTrue(refreshJson.contains("\"refreshToken\":\"old-ref-token\""))
        assertTrue(refreshJson.contains("\"deviceId\":\"device-uuid\""))

        val logoutReq = LogoutRequest("ref-token")
        val logoutJson = gson.toJson(logoutReq)
        assertTrue(logoutJson.contains("\"refreshToken\":\"ref-token\""))
    }

    @Test
    fun testProfileResponseDeserialization() {
        val json = """
            {
                "memberId": "M-9876",
                "fullName": "Alice Johnson",
                "email": "alice@shelftap.example",
                "memberType": "ADULT",
                "status": "ACTIVE",
                "historyEnabled": true,
                "mustChangePassword": false
            }
        """.trimIndent()
        val profile = gson.fromJson(json, ProfileResponse::class.java)
        assertEquals("M-9876", profile.memberId)
        assertEquals("Alice Johnson", profile.fullName)
        assertEquals("alice@shelftap.example", profile.email)
        assertEquals("ADULT", profile.memberType)
        assertEquals("ACTIVE", profile.status)
        assertTrue(profile.historyEnabled)
        assertFalse(profile.mustChangePassword)
    }

    @Test
    fun testChangePasswordRequestSerialization() {
        val changeReq = ChangePasswordRequest("oldSecret", "newSecret")
        val json = gson.toJson(changeReq)
        assertTrue(json.contains("\"currentPassword\":\"oldSecret\""))
        assertTrue(json.contains("\"newPassword\":\"newSecret\""))
    }
}
