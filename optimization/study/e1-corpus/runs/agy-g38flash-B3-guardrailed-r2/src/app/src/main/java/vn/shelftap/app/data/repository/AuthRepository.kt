package vn.shelftap.app.data.repository

interface AuthRepository {
    suspend fun accessToken(): String
}

class DefaultAuthRepository(
    private val tokenProvider: suspend () -> String = { "mock-shelftap-jwt-token" }
) : AuthRepository {
    override suspend fun accessToken(): String {
        return tokenProvider()
    }
}
