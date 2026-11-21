package vn.shelftap.app.auth

/**
 * Supplies the signed-in member's access token. The app's real implementation is
 * wired in [vn.shelftap.app.AppContainer]; this project only consumes it.
 */
interface AuthRepository {
    suspend fun accessToken(): String
}

/** Placeholder used until the real AuthRepository is passed to AppContainer. */
class UnwiredAuthRepository : AuthRepository {
    override suspend fun accessToken(): String =
        throw IllegalStateException("AuthRepository is not wired into AppContainer")
}
