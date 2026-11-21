package vn.shelftap.app.data.repository

/**
 * Repository providing authentication token.
 * Prompt: "Assume the app has an AuthRepository with suspend fun accessToken(): String that returns a valid access token; you only use it."
 */
interface AuthRepository {
    suspend fun accessToken(): String
}

class DefaultAuthRepository(
    private var currentToken: String = "mock-valid-jwt-token"
) : AuthRepository {
    override suspend fun accessToken(): String = currentToken

    fun setToken(token: String) {
        currentToken = token
    }
}
