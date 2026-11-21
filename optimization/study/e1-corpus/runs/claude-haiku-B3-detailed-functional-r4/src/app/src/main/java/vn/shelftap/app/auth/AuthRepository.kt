package vn.shelftap.app.auth

interface AuthRepository {
    /** Returns a valid access token for the signed-in library member. */
    suspend fun accessToken(): String
}
