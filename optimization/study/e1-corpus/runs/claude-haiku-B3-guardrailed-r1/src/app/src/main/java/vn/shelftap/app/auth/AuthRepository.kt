package vn.shelftap.app.auth

import vn.shelftap.app.api.ApiException

/** Supplies the access token of the signed-in member. The app only reads it. */
interface AuthRepository {
    suspend fun accessToken(): String
}

/**
 * Placeholder used until the real AuthRepository is wired into AppContainer.
 * It fails every request as "not signed in", so no network call is made.
 */
class UnconfiguredAuthRepository : AuthRepository {
    override suspend fun accessToken(): String = throw ApiException(401, "NOT_SIGNED_IN")
}
