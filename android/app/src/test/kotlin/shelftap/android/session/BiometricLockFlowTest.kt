package shelftap.android.session

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import shelftap.android.api.ApiHarness

/** Session behaviour around the biometric lock, against a fake vault (the Keystore half is device-only). */
class BiometricLockFlowTest {
    private fun locked(vault: FakeVault = FakeVault(token = "rt1", lockEnabled = true, unlocked = false)) = ApiHarness(vault)

    @Test
    fun aLockedVaultStartsLockedAndMakesNoNetworkCall() = locked().use { h ->
        runBlocking {
            h.session.restore()
            assertEquals(SessionState.Locked, h.session.state.value)
            assertEquals(0, h.server.requestCount)
        }
    }

    @Test
    fun unlockThenRefreshRotatesTheTokenAndActivates() = locked().use { h ->
        runBlocking {
            h.session.restore()
            h.enqueueTokens("at2", "rt2")
            h.session.unlock { it }
            assertEquals(SessionState.Active, h.session.state.value)
            assertEquals("at2", h.session.accessToken())
            assertEquals("rt2", h.vault.token)
            assertTrue(h.server.takeRequest().body!!.utf8().contains(""""refreshToken":"rt1""""))
        }
    }

    @Test
    fun cancellingThePromptStaysLockedWithoutANetworkCall() = locked().use { h ->
        runBlocking {
            h.session.restore()
            h.session.unlock { null }
            assertEquals(SessionState.Locked, h.session.state.value)
            assertEquals(0, h.server.requestCount)
        }
    }

    @Test
    fun aKilledLockKeyWipesEverythingAndTellsTheUser() = locked(FakeVault("rt1", lockEnabled = true, unlocked = false, invalidated = true)).use { h ->
        runBlocking {
            h.session.restore()
            h.session.unlock { it }
            assertEquals(SessionState.LoggedOut(LogoutReason.LOCK_KEY_INVALIDATED), h.session.state.value)
            assertNull(h.vault.token)
        }
    }

    @Test
    fun goingToTheBackgroundLocksAndDropsTheAccessToken() = ApiHarness(FakeVault(lockEnabled = true, unlocked = true)).use { h ->
        runBlocking {
            h.enqueueTokens("at1", "rt1")
            h.session.login("an@x.vn", "pw")
            h.session.lockIfEnabled()
            assertEquals(SessionState.Locked, h.session.state.value)
            assertNull(h.session.accessToken())
            assertEquals(false, h.vault.unlocked)
        }
    }

    @Test
    fun withoutALockTheBackgroundDoesNothing() = ApiHarness().use { h ->
        runBlocking {
            h.enqueueTokens("at1", "rt1")
            h.session.login("an@x.vn", "pw")
            h.session.lockIfEnabled()
            assertEquals(SessionState.Active, h.session.state.value)
            assertEquals("at1", h.session.accessToken())
        }
    }

    @Test
    fun logoutWhileLockedStillWipesLocally() = locked().use { h ->
        runBlocking {
            h.session.restore()
            h.session.logout()
            assertEquals(0, h.server.requestCount) // the token cannot be read without the biometric, so no server revoke
            assertNull(h.vault.token)
            assertEquals(SessionState.LoggedOut(null), h.session.state.value)
        }
    }
}
