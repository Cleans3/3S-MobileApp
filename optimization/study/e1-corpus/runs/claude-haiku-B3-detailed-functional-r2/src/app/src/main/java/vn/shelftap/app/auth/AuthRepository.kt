package vn.shelftap.app.auth

/**
 * Sign-in state owned by the rest of the app. ShelfTap only reads the access token.
 */
interface AuthRepository {
    /** Returns a valid access token for the signed-in member. */
    suspend fun accessToken(): String
}
