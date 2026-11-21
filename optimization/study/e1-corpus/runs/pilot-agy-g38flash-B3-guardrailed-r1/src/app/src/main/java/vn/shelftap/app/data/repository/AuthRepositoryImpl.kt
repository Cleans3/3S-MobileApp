package vn.shelftap.app.data.repository

import vn.shelftap.app.data.security.EncryptedStorage

class AuthRepositoryImpl(
    private val encryptedStorage: EncryptedStorage
) : AuthRepository {
    @Volatile
    private var runtimeToken: String? = null

    override suspend fun accessToken(): String {
        runtimeToken?.let { return it }
        val stored = encryptedStorage.readToken()
        if (!stored.isNullOrEmpty()) {
            runtimeToken = stored
            return stored
        }
        return ""
    }

    suspend fun setAccessToken(token: String) {
        runtimeToken = token
        encryptedStorage.saveToken(token)
    }
}
