package vn.shelftap.app.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreCrypto(
    private val keyAlias: String = "ShelfTapKeyStoreAesKey"
) {
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    @Synchronized
    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate IV")
        val ciphertext = cipher.doFinal(data)

        // Store format: [1 byte: IV length] + [IV bytes] + [Ciphertext + Tag]
        val result = ByteArray(1 + iv.size + ciphertext.size)
        result[0] = iv.size.toByte()
        System.arraycopy(iv, 0, result, 1, iv.size)
        System.arraycopy(ciphertext, 0, result, 1 + iv.size, ciphertext.size)
        return result
    }

    @Synchronized
    fun decrypt(encryptedData: ByteArray): ByteArray {
        require(encryptedData.isNotEmpty()) { "Encrypted data is empty" }
        val ivSize = encryptedData[0].toInt() and 0xFF
        require(encryptedData.size > 1 + ivSize) { "Invalid encrypted data size" }
        val iv = ByteArray(ivSize)
        System.arraycopy(encryptedData, 1, iv, 0, ivSize)
        val ciphertextLength = encryptedData.size - 1 - ivSize
        val ciphertext = ByteArray(ciphertextLength)
        System.arraycopy(encryptedData, 1 + ivSize, ciphertext, 0, ciphertextLength)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        return cipher.doFinal(ciphertext)
    }
}
