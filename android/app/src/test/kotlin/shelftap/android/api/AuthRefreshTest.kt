package shelftap.android.api

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import shelftap.android.session.FakeVault
import shelftap.android.session.LogoutReason
import shelftap.android.session.SessionState

class AuthRefreshTest {
    private fun harness(block: suspend ApiHarness.() -> Unit) = ApiHarness().use { h -> runBlocking { h.block() } }

    private suspend fun ApiHarness.loggedIn() {
        enqueueTokens("at1", "rt1")
        session.login("an@x.vn", "pw")
        server.takeRequest()
    }

    @Test
    fun loginSendsIntegrityAndDeviceIdAndStoresOnlyTheRefreshToken() = harness {
        enqueueTokens("at1", "rt1")
        session.login("an@x.vn", "pw")
        val request = server.takeRequest()
        assertEquals("/api/auth/login", request.url.encodedPath)
        assertEquals("ok", request.headers["X-Device-Integrity"])
        assertTrue(request.body!!.utf8().contains(""""deviceId":"device-1""""))
        assertEquals("rt1", vault.token)
        assertEquals("at1", session.accessToken())
        assertEquals(SessionState.Active, session.state.value)
    }

    @Test
    fun mustChangePasswordRoutesToPasswordChange() = harness {
        enqueueTokens("at1", "rt1", mustChangePassword = true)
        session.login("an@x.vn", "pw")
        assertEquals(SessionState.PasswordChangeRequired, session.state.value)
    }

    @Test
    fun tokenInvalidRefreshesOnceRetriesOnceAndRotatesTheStoredToken() = harness {
        loggedIn()
        enqueueProblem(401, "TOKEN_INVALID")
        enqueueTokens("at2", "rt2")
        enqueue(200, ApiHarness.ME_JSON)

        assertEquals("An Nguyen", repo.me().fullName)

        val first = server.takeRequest()
        assertEquals("Bearer at1", first.headers["Authorization"])
        val refresh = server.takeRequest()
        assertEquals("/api/auth/refresh", refresh.url.encodedPath)
        assertEquals("ok", refresh.headers["X-Device-Integrity"])
        assertNull(refresh.headers["Authorization"])
        assertTrue(refresh.body!!.utf8().contains(""""refreshToken":"rt1""""))
        assertEquals("Bearer at2", server.takeRequest().headers["Authorization"])
        assertEquals("rt2", vault.token)
        assertEquals(3, server.requestCount - 1)
    }

    @Test
    fun aSecond401AfterTheRefreshIsFinalAndDoesNotLoop() = harness {
        loggedIn()
        enqueueProblem(401, "TOKEN_INVALID")
        enqueueTokens("at2", "rt2")
        enqueueProblem(401, "TOKEN_INVALID")

        val e = assertFailsWith<ApiException.Http> { repo.me() }

        assertEquals(ErrorCode.TOKEN_INVALID, e.code)
        assertEquals(4, server.requestCount) // login, me, refresh, me: no fifth call
    }

    @Test
    fun tokenReusedOnRefreshWipesTheVaultAndLogsOut() = harness {
        loggedIn()
        enqueueProblem(401, "TOKEN_INVALID")
        enqueueProblem(401, "TOKEN_REUSED")

        assertFailsWith<ApiException.Http> { repo.me() }

        assertNull(vault.token)
        assertNull(session.accessToken())
        assertEquals(SessionState.LoggedOut(LogoutReason.TOKEN_REUSED), session.state.value)
    }

    @Test
    fun expiredRefreshTokenLogsOutAsSessionExpired() = harness {
        loggedIn()
        enqueueProblem(401, "TOKEN_INVALID")
        enqueueProblem(401, "TOKEN_INVALID")

        assertFailsWith<ApiException.Http> { repo.me() }

        assertEquals(SessionState.LoggedOut(LogoutReason.SESSION_EXPIRED), session.state.value)
    }

    @Test
    fun aServerErrorDuringRefreshKeepsTheSessionAndTheStoredToken() = harness {
        loggedIn()
        enqueueProblem(401, "TOKEN_INVALID")
        enqueueProblem(500, "UNKNOWN")

        assertFailsWith<ApiException.Http> { repo.me() }

        assertEquals("rt1", vault.token)
        assertEquals(SessionState.Active, session.state.value)
    }

    @Test
    fun anotherCallsFreshTokenIsReusedWithoutASecondRefresh() = harness {
        loggedIn()
        // The failed call carried at1, but the session already holds at2: retry with it, no refresh round trip.
        enqueueTokens("at2", "rt2")
        assertEquals("at2", session.refreshAfterUnauthorized("at1")) // does the one refresh
        server.takeRequest()
        assertEquals("at2", session.refreshAfterUnauthorized("at1")) // stale caller: no request
        assertEquals(1, server.requestCount - 1)
    }

    @Test
    fun passwordChangeRequiredFromAnEndpointMovesTheSessionAndChangeClearsIt() = harness {
        loggedIn()
        enqueueProblem(403, "PASSWORD_CHANGE_REQUIRED")
        assertFailsWith<ApiException.Http> { repo.loans() }
        assertEquals(SessionState.PasswordChangeRequired, session.state.value)

        enqueue(204)
        repo.changePassword("old", "new")
        assertEquals(SessionState.Active, session.state.value)
    }

    @Test
    fun logoutRevokesServerSideThenWipesLocalState() = harness {
        loggedIn()
        enqueue(204)
        session.logout()
        val request = server.takeRequest()
        assertEquals("/api/auth/logout", request.url.encodedPath)
        assertTrue(request.body!!.utf8().contains(""""refreshToken":"rt1""""))
        assertNull(vault.token)
        assertEquals(SessionState.LoggedOut(null), session.state.value)
    }

    @Test
    fun logoutStillWipesWhenTheServerIsUnreachable() = harness {
        loggedIn()
        enqueueProblem(500, "UNKNOWN")
        session.logout(LogoutReason.INACTIVITY)
        assertNull(vault.token)
        assertEquals(SessionState.LoggedOut(LogoutReason.INACTIVITY), session.state.value)
    }

    @Test
    fun restoreWithAStoredTokenRefreshesIt() = ApiHarness(FakeVault(token = "rtX")).use { h ->
        runBlocking {
            h.enqueueTokens("at9", "rt9")
            h.session.restore()
            assertEquals(SessionState.Active, h.session.state.value)
            assertEquals("rt9", h.vault.token)
        }
    }

    @Test
    fun restoreStatesForEmptyLockedAndOfflineVaults() {
        ApiHarness().use { h -> runBlocking { h.session.restore(); assertEquals(SessionState.LoggedOut(null), h.session.state.value) } }
        ApiHarness(FakeVault(token = "rt", lockEnabled = true, unlocked = false)).use { h ->
            runBlocking { h.session.restore(); assertEquals(SessionState.Locked, h.session.state.value) }
        }
        ApiHarness(FakeVault(token = "rt")).use { h ->
            runBlocking {
                h.enqueueProblem(500, "UNKNOWN")
                h.session.restore()
                assertEquals(SessionState.Offline, h.session.state.value)
                assertEquals("rt", h.vault.token) // kept: retry is safe
            }
        }
    }
}
