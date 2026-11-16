package shelftap.android.session

import java.util.Base64
import javax.crypto.Cipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import shelftap.android.api.ApiException
import shelftap.android.api.AuthApi
import shelftap.android.api.CardAuthRequest
import shelftap.android.api.ErrorCode
import shelftap.android.api.LoginRequest
import shelftap.android.api.LogoutRequest
import shelftap.android.api.RefreshRequest
import shelftap.android.api.TokenPair
import shelftap.android.api.TokenSource
import shelftap.android.api.apiCall
import shelftap.android.nfc.CardClient
import shelftap.android.nfc.CardLink
import shelftap.android.security.IntegritySource
import shelftap.android.security.RefreshTokenVault
import shelftap.android.security.VaultKeyInvalidatedException
import shelftap.android.security.VaultLockedException
import shelftap.android.security.VaultStatus

/** The random UUID of this install; a refresh token is bound to it server-side. */
fun interface DeviceIdProvider {
    suspend fun get(): String
}

/**
 * Owns the tokens. The access token lives in memory only; the refresh token goes through the [RefreshTokenVault] and is
 * re-stored on every rotation before the new access token is used. One refresh at a time ([refreshLock]).
 */
class SessionManager(
    private val authApi: AuthApi,
    private val vault: RefreshTokenVault,
    private val deviceIds: DeviceIdProvider,
    private val integrity: IntegritySource,
) : TokenSource {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    @Volatile
    private var access: String? = null
    private val refreshLock = Mutex()

    override fun accessToken(): String? = access

    /** Looks at the vault and either logs in with the stored refresh token or says why it cannot. Safe to call again to retry. */
    suspend fun restore() {
        refreshLock.withLock {
            try {
                when (vault.status()) {
                    VaultStatus.EMPTY -> _state.value = SessionState.LoggedOut()
                    VaultStatus.LOCKED -> _state.value = SessionState.Locked
                    VaultStatus.UNLOCKED -> doRefresh()
                }
            } catch (_: VaultKeyInvalidatedException) {
                wipe(LogoutReason.LOCK_KEY_INVALIDATED)
            }
        }
    }

    /**
     * Locked -> biometric -> refresh. [authenticate] shows BiometricPrompt on the cipher and returns it authenticated,
     * or null when the user cancelled (the app stays locked).
     */
    suspend fun unlock(authenticate: suspend (Cipher) -> Cipher?) {
        if (_state.value !is SessionState.Locked) return
        try {
            vault.finishUnlock(authenticate(vault.beginUnlock()) ?: return)
        } catch (_: VaultKeyInvalidatedException) {
            refreshLock.withLock { wipe(LogoutReason.LOCK_KEY_INVALIDATED) }
            return
        }
        restore()
    }

    suspend fun login(email: String, password: String) {
        val pair = apiCall { authApi.login(LoginRequest(email, password, deviceIds.get()), integrity.header()) }
        adopt(pair)
    }

    /**
     * Card session -> password session, after a 403 STEP_UP_REQUIRED (history and privacy need a password login).
     * A wrong password leaves the card session as it was; on success the card session's refresh token is revoked.
     */
    suspend fun stepUpLogin(email: String, password: String) {
        val cardSessionToken = try { vault.load() } catch (_: VaultLockedException) { null } catch (_: VaultKeyInvalidatedException) { null }
        login(email, password)
        if (cardSessionToken != null) {
            try {
                apiCall { authApi.logout(LogoutRequest(cardSessionToken)) }
            } catch (_: ApiException) {
                // Offline: the old token simply expires; the new password session is already in place.
            }
        }
    }

    /** SELECT + GET STATUS, server challenge, two AUTHENTICATE APDUs, server verifies. The card stays in the field throughout. */
    suspend fun cardLogin(link: CardLink) {
        val card = CardClient(link)
        val cardId = withContext(Dispatchers.IO) { card.readCardId() }
        val nonce = Base64.getDecoder().decode(apiCall { authApi.challenge(cardId) }.nonce)
        val signature = withContext(Dispatchers.IO) { card.sign(nonce) }
        val request = CardAuthRequest(
            nonce = Base64.getEncoder().encodeToString(nonce),
            signature = Base64.getEncoder().encodeToString(signature),
            deviceId = deviceIds.get(),
        )
        adopt(apiCall { authApi.cardAuth(cardId, request, integrity.header()) })
    }

    /** Server-side revoke when the vault is readable (best effort), then wipe everything local. */
    suspend fun logout(reason: LogoutReason? = null) {
        val token = try { vault.load() } catch (_: VaultLockedException) { null } catch (_: VaultKeyInvalidatedException) { null }
        if (token != null) {
            try {
                apiCall { authApi.logout(LogoutRequest(token)) }
            } catch (_: ApiException) {
                // Offline or already revoked: the local wipe below is what matters.
            }
        }
        wipe(reason)
    }

    /** Drops the access token and the unlock secret; with a biometric lock the next use needs the biometric. No-op without a lock. */
    suspend fun lockIfEnabled() {
        val s = _state.value
        if ((s is SessionState.Active || s is SessionState.PasswordChangeRequired) && vault.isLockEnabled()) {
            access = null
            vault.lock()
            _state.value = SessionState.Locked
        }
    }

    fun requirePasswordChange() {
        if (_state.value is SessionState.Active) _state.value = SessionState.PasswordChangeRequired
    }

    fun passwordChanged() {
        if (_state.value is SessionState.PasswordChangeRequired) _state.value = SessionState.Active
    }

    /** 401 TOKEN_INVALID path, on an OkHttp thread. Skips the refresh when another call already renewed the token. */
    override fun refreshAfterUnauthorized(failedToken: String): String? = runBlocking {
        refreshLock.withLock {
            val current = access
            if (current != null && current != failedToken) current else if (doRefresh()) access else null
        }
    }

    private suspend fun adopt(pair: TokenPair) {
        vault.save(pair.refreshToken)
        access = pair.accessToken
        _state.value = if (pair.mustChangePassword) SessionState.PasswordChangeRequired else SessionState.Active
    }

    /** Caller holds [refreshLock]. True when a fresh access token is in place. */
    private suspend fun doRefresh(): Boolean {
        val token = try {
            vault.load()
        } catch (_: VaultLockedException) {
            access = null
            _state.value = SessionState.Locked
            return false
        } catch (_: VaultKeyInvalidatedException) {
            wipe(LogoutReason.LOCK_KEY_INVALIDATED)
            return false
        }
        if (token == null) {
            wipe(null)
            return false
        }
        try {
            adopt(apiCall { authApi.refresh(RefreshRequest(token, deviceIds.get()), integrity.header()) })
            return true
        } catch (e: ApiException.Http) {
            when (e.code) {
                ErrorCode.TOKEN_REUSED -> wipe(LogoutReason.TOKEN_REUSED)
                ErrorCode.TOKEN_INVALID -> wipe(LogoutReason.SESSION_EXPIRED)
                else -> markOfflineIfStarting()
            }
        } catch (_: ApiException.Network) {
            markOfflineIfStarting()
        }
        return false
    }

    private fun markOfflineIfStarting() {
        val s = _state.value
        if (s is SessionState.Loading || s is SessionState.Locked || s is SessionState.Offline) _state.value = SessionState.Offline
    }

    private suspend fun wipe(reason: LogoutReason?) {
        access = null
        vault.clear()
        _state.value = SessionState.LoggedOut(reason)
    }
}
