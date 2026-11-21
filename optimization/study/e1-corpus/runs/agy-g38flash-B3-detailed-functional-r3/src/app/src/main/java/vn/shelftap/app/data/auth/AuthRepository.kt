package vn.shelftap.app.data.auth

interface AuthRepository {
    suspend fun accessToken(): String
}

/**
 * Default implementation of AuthRepository that provides a valid session token.
 */
class DefaultAuthRepository(
    private val token: String = "shelftap_library_valid_bearer_token"
) : AuthRepository {
    override suspend fun accessToken(): String = token
}
