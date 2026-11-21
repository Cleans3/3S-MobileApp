package vn.shelftap.app.data.security

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import java.io.File

class EncryptedStorage(
    private val context: Context,
    private val crypto: KeystoreCrypto = KeystoreCrypto(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    companion object {
        private const val LOANS_FILE = "shelftap_loans.enc"
        private const val HISTORY_FILE = "shelftap_history.enc"
        private const val PROFILE_FILE = "shelftap_profile.enc"
        private const val TOKEN_FILE = "shelftap_token.enc"
    }

    private suspend fun writeEncrypted(fileName: String, data: ByteArray) = withContext(ioDispatcher) {
        val ciphertext = crypto.encrypt(data)
        val file = File(context.filesDir, fileName)
        file.writeBytes(ciphertext)
    }

    private suspend fun readDecrypted(fileName: String): ByteArray? = withContext(ioDispatcher) {
        val file = File(context.filesDir, fileName)
        if (!file.exists() || file.length() == 0L) {
            return@withContext null
        }
        try {
            val ciphertext = file.readBytes()
            crypto.decrypt(ciphertext)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveLoans(loans: List<Loan>) {
        val jsonString = json.encodeToString(loans)
        writeEncrypted(LOANS_FILE, jsonString.toByteArray(Charsets.UTF_8))
    }

    suspend fun loadLoans(): List<Loan>? {
        val decrypted = readDecrypted(LOANS_FILE) ?: return null
        return try {
            json.decodeFromString<List<Loan>>(String(decrypted, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveHistory(history: List<HistoryItem>) {
        val jsonString = json.encodeToString(history)
        writeEncrypted(HISTORY_FILE, jsonString.toByteArray(Charsets.UTF_8))
    }

    suspend fun loadHistory(): List<HistoryItem>? {
        val decrypted = readDecrypted(HISTORY_FILE) ?: return null
        return try {
            json.decodeFromString<List<HistoryItem>>(String(decrypted, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveProfile(profile: MemberProfile) {
        val jsonString = json.encodeToString(profile)
        writeEncrypted(PROFILE_FILE, jsonString.toByteArray(Charsets.UTF_8))
    }

    suspend fun loadProfile(): MemberProfile? {
        val decrypted = readDecrypted(PROFILE_FILE) ?: return null
        return try {
            json.decodeFromString<MemberProfile>(String(decrypted, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveToken(token: String) {
        writeEncrypted(TOKEN_FILE, token.toByteArray(Charsets.UTF_8))
    }

    suspend fun readToken(): String? {
        val decrypted = readDecrypted(TOKEN_FILE) ?: return null
        return String(decrypted, Charsets.UTF_8)
    }
}
