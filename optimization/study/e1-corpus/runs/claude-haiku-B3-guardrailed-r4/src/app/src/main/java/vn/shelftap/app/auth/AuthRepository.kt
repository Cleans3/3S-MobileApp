package vn.shelftap.app.auth

/**
 * Provided by the app's sign-in feature. This project only reads access tokens from it.
 */
interface AuthRepository {
    /** Returns a valid access token for the signed-in member. */
    suspend fun accessToken(): String
}

/**
 * Placeholder used until the app's real AuthRepository is wired into [vn.shelftap.app.ShelfTapApp].
 */
object UnavailableAuthRepository : AuthRepository {
    override suspend fun accessToken(): String =
        throw IllegalStateException("No signed-in session is available")
}
