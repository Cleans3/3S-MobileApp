package shelftap.android.security

import javax.crypto.Cipher

enum class VaultStatus { EMPTY, UNLOCKED, LOCKED }

/** The Keystore key behind the lock was invalidated (biometrics changed) or is gone; the stored token is unrecoverable. */
class VaultKeyInvalidatedException(cause: Throwable? = null) : Exception("vault key invalidated", cause)

/** The vault is biometric-locked and not unlocked in this process. */
class VaultLockedException : Exception("vault locked")

/**
 * Where the refresh token lives between launches. The access token is never stored.
 * Narrow seam: the Keystore implementation is the only Android-bound part, tests use a fake.
 */
interface RefreshTokenVault {
    suspend fun status(): VaultStatus
    suspend fun isLockEnabled(): Boolean

    /** Throws [VaultLockedException] while locked. */
    suspend fun save(token: String)

    /** Null when empty. Throws [VaultLockedException] while locked. */
    suspend fun load(): String?

    /** Removes the token, the lock and every Keystore key. */
    suspend fun clear()

    /** Forgets the unlock secret from memory; the next [load] needs a biometric unlock. No-op without a lock. */
    suspend fun lock()

    /** Decrypt cipher for the lock secret; hand it to BiometricPrompt, then call [finishUnlock] with the authenticated cipher. */
    suspend fun beginUnlock(): Cipher
    suspend fun finishUnlock(cipher: Cipher)

    /** Creates the auth-bound key and returns its encrypt cipher for BiometricPrompt; [finishEnableLock] commits. */
    suspend fun beginEnableLock(): Cipher
    suspend fun finishEnableLock(cipher: Cipher)

    /** Back to the plain Keystore key. Needs the vault unlocked. */
    suspend fun disableLock()

    /** "StrongBox", "TEE" or "software", for the key that protects the token right now. */
    suspend fun keyProtection(): String
}
