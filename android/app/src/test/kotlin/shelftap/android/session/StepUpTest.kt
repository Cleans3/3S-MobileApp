package shelftap.android.session

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import shelftap.android.api.ApiException
import shelftap.android.api.ApiHarness
import shelftap.android.api.ErrorCode

/** API.md: a card session (amr=card) gets 403 STEP_UP_REQUIRED on history and privacy and must be swapped for a password session. */
class StepUpTest {
    private fun harness(block: suspend ApiHarness.() -> Unit) = ApiHarness().use { h -> runBlocking { h.block() } }

    private suspend fun ApiHarness.cardSession() {
        enqueueTokens("at-card", "rt-card")
        session.login("an@x.vn", "pw") // stands in for a card login: same token shape
        server.takeRequest()
    }

    @Test
    fun historyAndPrivacyStepUpErrorsKeepTheCardSession() = harness {
        cardSession()
        enqueueProblem(403, "STEP_UP_REQUIRED")
        enqueueProblem(403, "STEP_UP_REQUIRED")

        assertEquals(ErrorCode.STEP_UP_REQUIRED, assertFailsWith<ApiException.Http> { repo.history() }.code)
        assertEquals(ErrorCode.STEP_UP_REQUIRED, assertFailsWith<ApiException.Http> { repo.setHistoryEnabled(false) }.code)

        assertEquals("/api/me/history", server.takeRequest().url.encodedPath)
        assertEquals("/api/me/privacy", server.takeRequest().url.encodedPath)
        assertEquals(SessionState.Active, session.state.value) // not logged out, not routed away
        assertEquals("at-card", session.accessToken())
        assertEquals("rt-card", vault.token)
    }

    @Test
    fun stepUpLoginSwapsInThePasswordSessionAndRevokesTheCardOne() = harness {
        cardSession()
        enqueueTokens("at-pwd", "rt-pwd")
        enqueue(204)

        session.stepUpLogin("an@x.vn", "secret")

        assertEquals("/api/auth/login", server.takeRequest().url.encodedPath)
        val logout = server.takeRequest()
        assertEquals("/api/auth/logout", logout.url.encodedPath)
        assertTrue(logout.body!!.utf8().contains(""""refreshToken":"rt-card""""))
        assertEquals("at-pwd", session.accessToken())
        assertEquals("rt-pwd", vault.token)
        assertEquals(SessionState.Active, session.state.value)
    }

    @Test
    fun aWrongPasswordLeavesTheCardSessionUntouched() = harness {
        cardSession()
        enqueueProblem(401, "INVALID_CREDENTIALS")

        assertEquals(ErrorCode.INVALID_CREDENTIALS, assertFailsWith<ApiException.Http> { session.stepUpLogin("an@x.vn", "nope") }.code)

        assertEquals(1, server.requestCount - 1) // the login attempt only: no logout call
        assertEquals("at-card", session.accessToken())
        assertEquals("rt-card", vault.token)
        assertEquals(SessionState.Active, session.state.value)
    }

    @Test
    fun anOfflineRevokeDoesNotUndoTheNewSession() = harness {
        cardSession()
        enqueueTokens("at-pwd", "rt-pwd")
        enqueueProblem(500, "UNKNOWN")

        session.stepUpLogin("an@x.vn", "secret")

        assertEquals("at-pwd", session.accessToken())
        assertEquals("rt-pwd", vault.token)
    }
}
