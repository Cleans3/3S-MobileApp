package vn.shelftap.app.data.local

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.security.KeystoreEncryptor
import java.io.File

/**
 * Cache for loans, reading history, and user profile.
 * All personal data is encrypted using AES-256-GCM via Android Keystore before writing
 * to app-private storage, complying with security rules.
 */
class EncryptedShelfTapCache(
    private val context: Context,
    private val encryptor: KeystoreEncryptor = KeystoreEncryptor(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    private val loansFile: File get() = File(context.filesDir, "loans_cache.enc")
    private val historyFile: File get() = File(context.filesDir, "history_cache.enc")
    private val profileFile: File get() = File(context.filesDir, "profile_cache.enc")

    suspend fun saveLoans(loans: List<Loan>) = withContext(ioDispatcher) {
        val jsonString = json.encodeToString(loans)
        val encrypted = encryptor.encrypt(jsonString.toByteArray(Charsets.UTF_8))
        loansFile.writeBytes(encrypted)
    }

    suspend fun loadLoans(): List<Loan>? = withContext(ioDispatcher) {
        if (!loansFile.exists()) return@withContext null
        try {
            val encrypted = loansFile.readBytes()
            val decrypted = encryptor.decrypt(encrypted)
            val jsonString = String(decrypted, Charsets.UTF_8)
            json.decodeFromString<List<Loan>>(jsonString)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveHistory(history: List<HistoryItem>) = withContext(ioDispatcher) {
        val jsonString = json.encodeToString(history)
        val encrypted = encryptor.encrypt(jsonString.toByteArray(Charsets.UTF_8))
        historyFile.writeBytes(encrypted)
    }

    suspend fun loadHistory(): List<HistoryItem>? = withContext(ioDispatcher) {
        if (!historyFile.exists()) return@withContext null
        try {
            val encrypted = historyFile.readBytes()
            val decrypted = encryptor.decrypt(encrypted)
            val jsonString = String(decrypted, Charsets.UTF_8)
            json.decodeFromString<List<HistoryItem>>(jsonString)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveProfile(profile: MemberProfile) = withContext(ioDispatcher) {
        val jsonString = json.encodeToString(profile)
        val encrypted = encryptor.encrypt(jsonString.toByteArray(Charsets.UTF_8))
        profileFile.writeBytes(encrypted)
    }

    suspend fun loadProfile(): MemberProfile? = withContext(ioDispatcher) {
        if (!profileFile.exists()) return@withContext null
        try {
            val encrypted = profileFile.readBytes()
            val decrypted = encryptor.decrypt(encrypted)
            val jsonString = String(decrypted, Charsets.UTF_8)
            json.decodeFromString<MemberProfile>(jsonString)
        } catch (_: Exception) {
            null
        }
    }
}
