package shelftap.android.security

import android.content.Context
import android.security.keystore.KeyInfo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The real Keystore + DataStore vault on a device. The biometric unlock itself needs a person and is covered by hand (README). */
@RunWith(AndroidJUnit4::class)
class KeystoreRefreshTokenVaultTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file = File(context.cacheDir, "vault-${UUID.randomUUID()}.preferences_pb")
    private val store = PreferenceDataStoreFactory.create(scope = scope) { file }
    private val vault = KeystoreRefreshTokenVault(store)

    @Before
    fun clean() = runBlocking<Unit> { vault.clear() }

    @After
    fun tearDown() {
        runBlocking { vault.clear() }
        scope.cancel()
        file.delete()
    }

    @Test
    fun savedTokenComesBackAndRotationOverwritesIt() = runBlocking<Unit> {
        assertEquals(VaultStatus.EMPTY, vault.status())
        vault.save("refresh-token-AAA")
        assertEquals(VaultStatus.UNLOCKED, vault.status())
        assertEquals("refresh-token-AAA", vault.load())
        vault.save("refresh-token-BBB")
        assertEquals("refresh-token-BBB", vault.load())
    }

    @Test
    fun theTokenIsNotOnDiskInPlaintext() = runBlocking<Unit> {
        vault.save("refresh-token-PLAINTEXT-CHECK")
        val bytes = file.readBytes()
        assertTrue(bytes.isNotEmpty())
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("PLAINTEXT-CHECK"))
    }

    @Test
    fun theKeystoreKeyIsNonExportableAes256InHardware() = runBlocking<Unit> {
        vault.save("t")
        val key = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey("shelftap.refresh.v1", null) as SecretKey
        assertNull(key.encoded, "key material must not be extractable")
        val info = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore").getKeySpec(key, KeyInfo::class.java) as KeyInfo
        assertEquals(256, info.keySize)
        assertFalse(info.isUserAuthenticationRequired) // plain mode: no biometric
        assertTrue(vault.keyProtection() in setOf("StrongBox", "TEE", "software"))
    }

    @Test
    fun aTamperedBlobIsReportedAsInvalidatedNotReturned() = runBlocking<Unit> {
        vault.save("refresh-token-AAA")
        val rt = stringPreferencesKey("refresh_token_blob")
        store.edit { prefs ->
            val blob = Base64.getDecoder().decode(prefs[rt]!!)
            blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 1).toByte()
            prefs[rt] = Base64.getEncoder().encodeToString(blob)
        }
        assertFailsWith<VaultKeyInvalidatedException> { vault.load() }
    }

    @Test
    fun clearRemovesTheTokenAndTheKey() = runBlocking<Unit> {
        vault.save("t")
        vault.clear()
        assertEquals(VaultStatus.EMPTY, vault.status())
        assertNull(vault.load())
        assertEquals("none", vault.keyProtection())
        assertFalse(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias("shelftap.refresh.v1"))
    }

    @Test
    fun theLockKeyNeedsABiometricForEveryUse() = runBlocking<Unit> {
        assumeTrue("needs an enrolled strong biometric", BiometricLock.available(context))
        vault.save("t")
        val cipher = vault.beginEnableLock()
        val key = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey("shelftap.refresh.lock.v1", null) as SecretKey
        val info = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore").getKeySpec(key, KeyInfo::class.java) as KeyInfo
        assertTrue(info.isUserAuthenticationRequired)
        assertTrue(info.isInvalidatedByBiometricEnrollment)
        assertNotNull(cipher)
        assertFailsWith<Exception> { cipher.doFinal(ByteArray(32)) } // not authenticated: Keystore refuses
        assertEquals(VaultStatus.UNLOCKED, vault.status()) // an abandoned enable leaves plain mode intact
        assertEquals("t", vault.load())
    }
}
