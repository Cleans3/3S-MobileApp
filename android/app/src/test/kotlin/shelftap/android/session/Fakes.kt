package shelftap.android.session

import javax.crypto.Cipher
import shelftap.android.security.RefreshTokenVault
import shelftap.android.security.VaultKeyInvalidatedException
import shelftap.android.security.VaultLockedException
import shelftap.android.security.VaultStatus

/**
 * In-memory vault. The biometric lock is Keystore-bound and not JVM-testable, so only its state is modelled:
 * [lockEnabled] + not [unlocked] = locked. [invalidated] simulates a new fingerprint killing the key.
 */
class FakeVault(
    var token: String? = null,
    var lockEnabled: Boolean = false,
    var unlocked: Boolean = !lockEnabled,
    var invalidated: Boolean = false,
) : RefreshTokenVault {
    override suspend fun status() = when {
        token == null -> VaultStatus.EMPTY
        lockEnabled && !unlocked -> VaultStatus.LOCKED
        else -> VaultStatus.UNLOCKED
    }

    override suspend fun isLockEnabled() = lockEnabled

    override suspend fun save(token: String) {
        if (lockEnabled && !unlocked) throw VaultLockedException()
        this.token = token
    }

    override suspend fun load(): String? {
        if (lockEnabled && !unlocked) throw VaultLockedException()
        return token
    }

    override suspend fun clear() {
        token = null
        lockEnabled = false
        unlocked = true
    }

    override suspend fun lock() {
        if (lockEnabled) unlocked = false
    }

    override suspend fun beginUnlock(): Cipher {
        if (invalidated) throw VaultKeyInvalidatedException()
        return Cipher.getInstance("AES/GCM/NoPadding")
    }

    override suspend fun finishUnlock(cipher: Cipher) {
        unlocked = true
    }

    override suspend fun beginEnableLock(): Cipher = throw UnsupportedOperationException()
    override suspend fun finishEnableLock(cipher: Cipher) = throw UnsupportedOperationException()
    override suspend fun disableLock() = throw UnsupportedOperationException()
    override suspend fun keyProtection() = "fake"
}
