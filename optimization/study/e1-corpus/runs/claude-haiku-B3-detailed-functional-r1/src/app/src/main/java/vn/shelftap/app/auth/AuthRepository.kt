package vn.shelftap.app.auth

/**
 * Provided by the app's existing sign-in code. This project only reads the token.
 * [PlaceholderAuthRepository] below is a stand-in so the project compiles; replace it
 * with the real implementation in AppContainer.
 */
interface AuthRepository {
    /** Returns a valid access token for the signed-in member. */
    suspend fun accessToken(): String
}

class PlaceholderAuthRepository : AuthRepository {
    override suspend fun accessToken(): String =
        throw IllegalStateException("AuthRepository is not wired up yet")
}
