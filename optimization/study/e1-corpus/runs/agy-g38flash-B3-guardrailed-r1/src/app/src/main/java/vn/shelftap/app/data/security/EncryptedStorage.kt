package vn.shelftap.app.data.security

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class EncryptedStorage(
    private val context: Context,
    private val crypto: KeystoreCrypto,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun saveEncrypted(fileName: String, data: String) = withContext(ioDispatcher) {
        val bytes = data.encodeToByteArray()
        val encrypted = crypto.encrypt(bytes)
        val file = File(context.filesDir, fileName)
        file.writeBytes(encrypted)
    }

    suspend fun readEncrypted(fileName: String): String? = withContext(ioDispatcher) {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return@withContext null
        try {
            val encrypted = file.readBytes()
            val decrypted = crypto.decrypt(encrypted)
            decrypted.decodeToString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun delete(fileName: String) = withContext(ioDispatcher) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }
}
