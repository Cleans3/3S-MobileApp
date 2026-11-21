package vn.shelftap.app

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.shelftap.app.data.model.ApiErrorResponse
import vn.shelftap.app.data.model.AuthResponse
import vn.shelftap.app.data.model.ChangePasswordRequest
import vn.shelftap.app.data.model.LoginRequest
import vn.shelftap.app.data.model.LogoutRequest
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.RefreshRequest

class AuthModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testLoginRequestSerialization() {
        val req = LoginRequest("member@shelftap.example", "secret123", "device-uuid-1")
        val jsonStr = json.encodeToString(LoginRequest.serializer(), req)
        val deserialized = json.decodeFromString(LoginRequest.serializer(), jsonStr)
        assertEquals("member@shelftap.example", deserialized.email)
        assertEquals("secret123", deserialized.password)
        assertEquals("device-uuid-1", deserialized.deviceId)
    }

    @Test
    fun testAuthResponseDeserialization() {
        val jsonStr = """
            {
                "accessToken": "access_token_abc",
                "accessExpiresIn": 900,
                "refreshToken": "refresh_token_xyz",
                "mustChangePassword": false
            }
        """.trimIndent()
        val auth = json.decodeFromString(AuthResponse.serializer(), jsonStr)
        assertEquals("access_token_abc", auth.accessToken)
        assertEquals(900L, auth.accessExpiresIn)
        assertEquals("refresh_token_xyz", auth.refreshToken)
        assertFalse(auth.mustChangePassword)
    }

    @Test
    fun testMemberProfileDeserialization() {
        val jsonStr = """
            {
                "memberId": "mem_12345",
                "fullName": "Jane Doe",
                "email": "jane@shelftap.example",
                "memberType": "standard",
                "status": "active",
                "historyEnabled": true,
                "mustChangePassword": false
            }
        """.trimIndent()
        val profile = json.decodeFromString(MemberProfile.serializer(), jsonStr)
        assertEquals("mem_12345", profile.memberId)
        assertEquals("Jane Doe", profile.fullName)
        assertEquals("jane@shelftap.example", profile.email)
        assertEquals("standard", profile.memberType)
        assertEquals("active", profile.status)
        assertTrue(profile.historyEnabled)
        assertFalse(profile.mustChangePassword)
    }

    @Test
    fun testApiErrorResponseDeserialization() {
        val jsonStr = """
            {
                "type": "https://api.shelftap.example/errors/invalid-credentials",
                "title": "Invalid Credentials",
                "status": 401,
                "detail": "Invalid email or password",
                "code": "INVALID_CREDENTIALS"
            }
        """.trimIndent()
        val error = json.decodeFromString(ApiErrorResponse.serializer(), jsonStr)
        assertEquals(401, error.status)
        assertEquals("INVALID_CREDENTIALS", error.code)
        assertEquals("Invalid email or password", error.detail)
    }

    @Test
    fun testRefreshAndLogoutSerialization() {
        val refresh = RefreshRequest("old_refresh_token", "device-uuid-1")
        val refreshJson = json.encodeToString(RefreshRequest.serializer(), refresh)
        val decodedRefresh = json.decodeFromString(RefreshRequest.serializer(), refreshJson)
        assertEquals("old_refresh_token", decodedRefresh.refreshToken)

        val logout = LogoutRequest("refresh_token_to_revoke")
        val logoutJson = json.encodeToString(LogoutRequest.serializer(), logout)
        val decodedLogout = json.decodeFromString(LogoutRequest.serializer(), logoutJson)
        assertEquals("refresh_token_to_revoke", decodedLogout.refreshToken)
    }

    @Test
    fun testChangePasswordRequestSerialization() {
        val req = ChangePasswordRequest("oldPass123", "newPass456")
        val reqJson = json.encodeToString(ChangePasswordRequest.serializer(), req)
        val decoded = json.decodeFromString(ChangePasswordRequest.serializer(), reqJson)
        assertEquals("oldPass123", decoded.currentPassword)
        assertEquals("newPass456", decoded.newPassword)
    }
}
