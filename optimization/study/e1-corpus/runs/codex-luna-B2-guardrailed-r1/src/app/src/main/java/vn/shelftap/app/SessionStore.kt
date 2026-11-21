package vn.shelftap.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val mustChangePassword: Boolean
)

class SessionStore(private val context: Context) {
    private val sessionFile get() = File(context.filesDir, "session.bin")
    private val deviceIdFile get() = File(context.filesDir, "device_id.txt")
    private val alias = "shelftap_session_key"

    suspend fun deviceId(): String = withContext(Dispatchers.IO) {
        if (deviceIdFile.exists()) deviceIdFile.readText(Charsets.UTF_8)
        else UUID.randomUUID().toString().also { deviceIdFile.writeText(it, Charsets.UTF_8) }
    }

    suspend fun read(): Session? = withContext(Dispatchers.IO) {
        if (!sessionFile.exists()) return@withContext null
        val encrypted = sessionFile.readBytes()
        if (encrypted.size <= 12) return@withContext null
        val iv = encrypted.copyOfRange(0, 12)
        val payload = encrypted.copyOfRange(12, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val json = JSONObject(String(cipher.doFinal(payload), Charsets.UTF_8))
        Session(json.getString("accessToken"), json.getString("refreshToken"), json.optBoolean("mustChangePassword"))
    }

    suspend fun write(session: Session) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val json = JSONObject()
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("mustChangePassword", session.mustChangePassword)
        sessionFile.writeBytes(cipher.iv + cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8)))
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        if (sessionFile.exists()) sessionFile.delete()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }
}
