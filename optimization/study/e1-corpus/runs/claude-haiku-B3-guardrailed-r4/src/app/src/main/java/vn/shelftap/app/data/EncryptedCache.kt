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

/**
 * Stores personal data (loans, reading history) as AES-256-GCM ciphertext in app-private storage.
 * The key lives in the Android Keystore and cannot be exported. Each write uses a fresh random IV,
 * which is stored in front of the ciphertext.
 *
 * File layout: [12-byte IV][ciphertext + 16-byte GCM tag].
 */
class EncryptedCache(private val directory: File) {

    /** Encrypts [plaintext] and replaces the stored value for [name]. */
    fun write(name: String, plaintext: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        directory.mkdirs()
        val temp = File(directory, "$name.tmp")
        temp.writeBytes(iv + ciphertext)
        Files.move(temp.toPath(), fileFor(name).toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    /** Returns the decrypted value for [name], or null when nothing usable is stored. */
    fun read(name: String): String? {
        val file = fileFor(name)
        if (!file.isFile) return null
        return try {
            val bytes = file.readBytes()
            if (bytes.size <= IV_BYTES) return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_BYTES)),
            )
            String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
        } catch (e: IOException) {
            null
        } catch (e: GeneralSecurityException) {
            // The data cannot be authenticated (for example the key was lost): discard it.
            file.delete()
            null
        }
    }

    private fun fileFor(name: String): File = File(directory, "$name.bin")

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "shelftap_cache_aes256"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
