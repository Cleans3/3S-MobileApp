package shelftap.android.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.ProviderException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Refresh token at rest, two modes (both AES-256-GCM, keys non-exportable in the Android Keystore, StrongBox when present):
 *
 *  - plain: the token is sealed by Keystore key [PLAIN_ALIAS].
 *  - biometric lock: the token is sealed by a random software secret S. S itself is sealed by Keystore key [LOCK_ALIAS], which needs
 *    a fresh BiometricPrompt for every use. S lives in memory only while unlocked, so refresh-token rotation can re-seal the token
 *    without a prompt, but nobody can read it from disk without the user's biometric.
 *
 * Lives in DataStore (file `datastore/shelftap.preferences_pb`, excluded from every backup).
 */
class KeystoreRefreshTokenVault(private val store: DataStore<Preferences>) : RefreshTokenVault {
    private val mutex = Mutex()

    @Volatile
    private var secret: ByteArray? = null

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { mutex.withLock { block() } }

    override suspend fun status(): VaultStatus = io { statusOf(store.data.first()) }

    override suspend fun isLockEnabled(): Boolean = io { store.data.first()[WRAPPED] != null }

    override suspend fun save(token: String) = io<Unit> {
        val blob = seal(store.data.first(), token.toByteArray(Charsets.UTF_8))
        store.edit { it[RT] = b64(blob) }
    }

    override suspend fun load(): String? = io {
        val prefs = store.data.first()
        val blob = prefs[RT]?.let(::unb64) ?: return@io null
        String(open(prefs, blob), Charsets.UTF_8)
    }

    override suspend fun clear() = io {
        store.edit { it.remove(RT); it.remove(WRAPPED) }
        wipeSecret()
        deleteKey(PLAIN_ALIAS)
        deleteKey(LOCK_ALIAS)
    }

    override suspend fun lock() = io { wipeSecret() }

    override suspend fun beginUnlock(): Cipher = io {
        val wrapped = store.data.first()[WRAPPED]?.let(::unb64) ?: throw VaultKeyInvalidatedException()
        val key = keyOrNull(LOCK_ALIAS) ?: throw VaultKeyInvalidatedException()
        try {
            AesGcm.decryptCipher(key, wrapped)
        } catch (e: InvalidKeyException) { // KeyPermanentlyInvalidatedException: a new fingerprint was enrolled
            throw VaultKeyInvalidatedException(e)
        }
    }

    override suspend fun finishUnlock(cipher: Cipher) = io {
        val wrapped = store.data.first()[WRAPPED]?.let(::unb64) ?: throw VaultKeyInvalidatedException()
        secret = invalidating { AesGcm.finish(cipher, wrapped, AAD_LOCK) }
    }

    override suspend fun beginEnableLock(): Cipher = io {
        check(statusOf(store.data.first()) == VaultStatus.UNLOCKED && store.data.first()[WRAPPED] == null) { "no plain token to lock" }
        deleteKey(LOCK_ALIAS)
        Cipher.getInstance(AesGcm.TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, generateKey(LOCK_ALIAS, authRequired = true)) }
    }

    override suspend fun finishEnableLock(cipher: Cipher) = io {
        val prefs = store.data.first()
        val token = open(prefs, prefs[RT]?.let(::unb64) ?: error("no token"))
        val s = AesGcm.newKeyBytes()
        cipher.updateAAD(AAD_LOCK)
        val wrapped = AesGcm.pack(cipher.iv, cipher.doFinal(s))
        val sealed = AesGcm.seal(AesGcm.key(s), token, AAD_RT)
        store.edit { it[WRAPPED] = b64(wrapped); it[RT] = b64(sealed) }
        secret = s
        deleteKey(PLAIN_ALIAS)
    }

    override suspend fun disableLock() = io {
        val prefs = store.data.first()
        if (prefs[WRAPPED] == null) return@io
        val token = open(prefs, prefs[RT]?.let(::unb64) ?: error("no token"))
        val sealed = keystoreSeal(token)
        store.edit { it[RT] = b64(sealed); it.remove(WRAPPED) }
        wipeSecret()
        deleteKey(LOCK_ALIAS)
    }

    override suspend fun keyProtection(): String = io {
        val alias = if (store.data.first()[WRAPPED] != null) LOCK_ALIAS else PLAIN_ALIAS
        val key = keyOrNull(alias) ?: return@io "none"
        val info = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE).getKeySpec(key, KeyInfo::class.java) as KeyInfo
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> when (info.securityLevel) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> "StrongBox"
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "TEE"
                else -> "software"
            }
            @Suppress("DEPRECATION") info.isInsideSecureHardware -> "TEE"
            else -> "software"
        }
    }

    private fun statusOf(prefs: Preferences): VaultStatus = when {
        prefs[RT] == null -> VaultStatus.EMPTY
        prefs[WRAPPED] == null || secret != null -> VaultStatus.UNLOCKED
        else -> VaultStatus.LOCKED
    }

    private fun seal(prefs: Preferences, plain: ByteArray): ByteArray =
        if (prefs[WRAPPED] == null) keystoreSeal(plain)
        else AesGcm.seal(AesGcm.key(secret ?: throw VaultLockedException()), plain, AAD_RT)

    private fun open(prefs: Preferences, blob: ByteArray): ByteArray = invalidating {
        if (prefs[WRAPPED] == null) {
            val key = keyOrNull(PLAIN_ALIAS) ?: throw VaultKeyInvalidatedException()
            AesGcm.open(key, blob, AAD_RT)
        } else {
            AesGcm.open(AesGcm.key(secret ?: throw VaultLockedException()), blob, AAD_RT)
        }
    }

    private fun keystoreSeal(plain: ByteArray): ByteArray {
        val key = keyOrNull(PLAIN_ALIAS) ?: generateKey(PLAIN_ALIAS, authRequired = false)
        val cipher = Cipher.getInstance(AesGcm.TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
        cipher.updateAAD(AAD_RT)
        return AesGcm.pack(cipher.iv, cipher.doFinal(plain))
    }

    /** A bad tag or a dead key means the stored token cannot be recovered: report it so the session wipes and asks for a login. */
    private inline fun <T> invalidating(block: () -> T): T =
        try {
            block()
        } catch (e: GeneralSecurityException) {
            throw VaultKeyInvalidatedException(e)
        }

    private fun wipeSecret() {
        secret?.fill(0)
        secret = null
    }

    private fun keystore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun keyOrNull(alias: String): SecretKey? = keystore().getKey(alias, null) as SecretKey?

    private fun deleteKey(alias: String) {
        val ks = keystore()
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
    }

    private fun generateKey(alias: String, authRequired: Boolean): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                generator.init(keySpec(alias, authRequired, strongBox = true))
                return generator.generateKey()
            } catch (_: ProviderException) {
                // StrongBoxUnavailableException: no StrongBox here, fall back to the TEE.
            }
        }
        generator.init(keySpec(alias, authRequired, strongBox = false))
        return generator.generateKey()
    }

    private fun keySpec(alias: String, authRequired: Boolean, strongBox: Boolean): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AesGcm.KEY_BYTES * 8)
            .setRandomizedEncryptionRequired(true)
        if (authRequired) {
            builder.setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG) // 0 = auth for every use
            } else {
                @Suppress("DEPRECATION") builder.setUserAuthenticationValidityDurationSeconds(-1)
            }
        }
        if (strongBox) setStrongBox(builder)
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setStrongBox(builder: KeyGenParameterSpec.Builder) {
        builder.setIsStrongBoxBacked(true)
    }

    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun unb64(text: String): ByteArray = Base64.getDecoder().decode(text)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val PLAIN_ALIAS = "shelftap.refresh.v1"
        const val LOCK_ALIAS = "shelftap.refresh.lock.v1"
        val AAD_RT = "shelftap.refresh-token.v1".toByteArray()
        val AAD_LOCK = "shelftap.lock-secret.v1".toByteArray()
        val RT = stringPreferencesKey("refresh_token_blob")
        val WRAPPED = stringPreferencesKey("lock_wrapped_secret")
    }
}
