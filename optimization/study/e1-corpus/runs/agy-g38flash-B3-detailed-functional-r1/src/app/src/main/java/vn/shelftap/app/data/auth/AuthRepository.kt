package vn.shelftap.app.data.auth

interface AuthRepository {
    suspend fun accessToken(): String
}

class DefaultAuthRepository(
    private var token: String = "valid-shelf-tap-session-token"
) : AuthRepository {
    override suspend fun accessToken(): String = token

    fun setToken(newToken: String) {
        token = newToken
    }
}
