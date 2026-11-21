package vn.shelftap.app.data.repository

interface AuthRepository {
    suspend fun accessToken(): String
}

/**
 * Default implementation of AuthRepository.
 * In a full production app, this would be backed by EncryptedSharedPreferences,
 * AccountManager, or an OAuth token manager.
 */
class DefaultAuthRepository(
    private var token: String = "mock_jwt_access_token_shelftap"
) : AuthRepository {
    override suspend fun accessToken(): String = token

    fun updateToken(newToken: String) {
        token = newToken
    }
}
