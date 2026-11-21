package vn.shelftap.app.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Stores personal data as AES-256-GCM ciphertext in app-private files. The key lives in
 * the Android Keystore and is not exportable. File layout: [ivLength][iv][ciphertext+tag].
 */
class SecureCache(private val dir: File, private val json: Json) {

    @Synchronized
    fun <T> save(name: String, serializer: KSerializer<T>, value: T) {
        val plain = json.encodeToString(serializer, value).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val sealed = cipher.doFinal(plain)

        dir.mkdirs()
        val target = fileFor(name)
        val temp = File(dir, "$name.tmp")
        temp.outputStream().use { out ->
            out.write(iv.size)
            out.write(iv)
            out.write(sealed)
        }
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    /** Returns the saved value, or null when nothing usable is stored. */
    @Synchronized
    fun <T> load(name: String, serializer: KSerializer<T>): T? {
        val file = fileFor(name)
        if (!file.exists()) return null
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 2) return null
            val ivLength = bytes[0].toInt() and 0xFF
            if (ivLength == 0 || 1 + ivLength >= bytes.size) return null
            val iv = bytes.copyOfRange(1, 1 + ivLength)
            val sealed = bytes.copyOfRange(1 + ivLength, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            val plain = cipher.doFinal(sealed)
            json.decodeFromString(serializer, String(plain, Charsets.UTF_8))
        } catch (e: GeneralSecurityException) {
            null
        } catch (e: IOException) {
            null
        } catch (e: SerializationException) {
            null
        }
    }

    private fun fileFor(name: String): File = File(dir, "$name.bin")

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "shelftap.cache.aes256"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}
