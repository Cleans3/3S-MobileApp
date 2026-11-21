package vn.shelftap.app.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.UserProfile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedStorageManager(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ShelfTapEncryptedKey"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val MIN_HEADER_SIZE = 13 // 1 byte IV length + minimum 12 bytes IV

        private const val FILE_LOANS = "encrypted_loans.bin"
        private const val FILE_HISTORY = "encrypted_history.bin"
        private const val FILE_PROFILE = "encrypted_profile.bin"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    private suspend fun writeEncryptedData(filename: String, plainText: String): Unit =
        withContext(ioDispatcher) {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            // Initializing in ENCRYPT_MODE without IV lets Cipher generate a secure random IV
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val file = File(context.filesDir, filename)
            val output = ByteArray(1 + iv.size + cipherText.size)
            output[0] = iv.size.toByte()
            System.arraycopy(iv, 0, output, 1, iv.size)
            System.arraycopy(cipherText, 0, output, 1 + iv.size, cipherText.size)
            file.writeBytes(output)
        }

    private suspend fun readDecryptedData(filename: String): String? =
        withContext(ioDispatcher) {
            val file = File(context.filesDir, filename)
            if (!file.exists() || file.length() < MIN_HEADER_SIZE) {
                return@withContext null
            }
            try {
                val bytes = file.readBytes()
                val ivSize = bytes[0].toInt()
                if (bytes.size < 1 + ivSize) return@withContext null

                val iv = bytes.copyOfRange(1, 1 + ivSize)
                val cipherText = bytes.copyOfRange(1 + ivSize, bytes.size)

                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val plainBytes = cipher.doFinal(cipherText)
                String(plainBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }

    suspend fun saveLoans(loans: List<Loan>) {
        val jsonStr = json.encodeToString(loans)
        writeEncryptedData(FILE_LOANS, jsonStr)
    }

    suspend fun getLoans(): List<Loan>? {
        val jsonStr = readDecryptedData(FILE_LOANS) ?: return null
        return try {
            json.decodeFromString<List<Loan>>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveHistory(history: List<HistoryItem>) {
        val jsonStr = json.encodeToString(history)
        writeEncryptedData(FILE_HISTORY, jsonStr)
    }

    suspend fun getHistory(): List<HistoryItem>? {
        val jsonStr = readDecryptedData(FILE_HISTORY) ?: return null
        return try {
            json.decodeFromString<List<HistoryItem>>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        val jsonStr = json.encodeToString(profile)
        writeEncryptedData(FILE_PROFILE, jsonStr)
    }

    suspend fun getProfile(): UserProfile? {
        val jsonStr = readDecryptedData(FILE_PROFILE) ?: return null
        return try {
            json.decodeFromString<UserProfile>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }
}
