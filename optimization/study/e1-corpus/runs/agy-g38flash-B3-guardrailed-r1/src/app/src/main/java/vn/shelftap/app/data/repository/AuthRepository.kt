package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.security.EncryptedStorage

interface AuthRepository {
    suspend fun accessToken(): String
}

class EncryptedAuthRepository(
    private val encryptedStorage: EncryptedStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {
    companion object {
        private const val TOKEN_FILE = "auth_token.enc"
    }

    override suspend fun accessToken(): String = withContext(ioDispatcher) {
        encryptedStorage.readEncrypted(TOKEN_FILE) ?: ""
    }

    suspend fun saveToken(token: String) = withContext(ioDispatcher) {
        encryptedStorage.saveEncrypted(TOKEN_FILE, token)
    }
}
