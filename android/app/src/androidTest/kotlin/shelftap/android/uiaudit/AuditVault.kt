package shelftap.android.uiaudit

import javax.crypto.Cipher
import shelftap.android.security.RefreshTokenVault
import shelftap.android.security.VaultStatus

/** In-memory, never locked: the UI audit signs in and out without touching the device Keystore or DataStore. */
class AuditVault : RefreshTokenVault {
    private var token: String? = null

    override suspend fun status() = if (token == null) VaultStatus.EMPTY else VaultStatus.UNLOCKED
    override suspend fun isLockEnabled() = false
    override suspend fun save(token: String) { this.token = token }
    override suspend fun load(): String? = token
    override suspend fun clear() { token = null }
    override suspend fun lock() = Unit
    override suspend fun beginUnlock(): Cipher = throw UnsupportedOperationException()
    override suspend fun finishUnlock(cipher: Cipher) = throw UnsupportedOperationException()
    override suspend fun beginEnableLock(): Cipher = throw UnsupportedOperationException()
    override suspend fun finishEnableLock(cipher: Cipher) = throw UnsupportedOperationException()
    override suspend fun disableLock() = throw UnsupportedOperationException()
    override suspend fun keyProtection() = "audit"
}
