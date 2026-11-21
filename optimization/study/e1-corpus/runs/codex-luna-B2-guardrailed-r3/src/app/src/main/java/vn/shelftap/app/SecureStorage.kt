package vn.shelftap.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class DeviceIdStore(private val context: Context) {
    private val file get() = File(context.filesDir, "device_id")
    fun getOrCreate(): String = synchronized(this) {
        file.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString().also { file.writeText(it) }
    }
}

class SessionStore(private val context: Context) {
    private val file get() = File(context.filesDir, "session.enc")
    private val alias = "shelftap_session_key"

    private fun key(): java.security.Key {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(alias)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
                generateKey()
            }
        }
        return (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun read(): Session? = runCatching {
        if (!file.exists()) return null
        val raw = file.readBytes(); val ivSize = raw[0].toInt()
        val iv = raw.copyOfRange(1, 1 + ivSize); val cipherText = raw.copyOfRange(1 + ivSize, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        val parts = String(cipher.doFinal(cipherText), StandardCharsets.UTF_8).split("\u001f")
        Session(parts[0], parts[1], parts[2] == "1")
    }.getOrNull()

    fun write(session: Session) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal("${session.accessToken}\u001f${session.refreshToken}\u001f${if (session.mustChangePassword) "1" else "0"}".toByteArray(StandardCharsets.UTF_8))
        file.writeBytes(byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted)
    }
    fun clear() { file.delete() }
}
