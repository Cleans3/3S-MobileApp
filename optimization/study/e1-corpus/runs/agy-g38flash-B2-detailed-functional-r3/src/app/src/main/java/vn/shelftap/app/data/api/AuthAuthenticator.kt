package vn.shelftap.app.data.api

import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import vn.shelftap.app.data.local.PreferenceManager
import vn.shelftap.app.data.model.ApiErrorCodes
import vn.shelftap.app.data.model.ApiErrorResponse
import vn.shelftap.app.data.model.RefreshRequest

/**
 * When a call fails with 401 TOKEN_INVALID, the app refreshes the token once and repeats the call;
 * if the refresh also fails, the member is returned to the sign-in screen.
 */
class AuthAuthenticator(
    private val preferenceManager: PreferenceManager,
    private val refreshService: ShelfTapApiService,
    private val onSessionExpired: () -> Unit
) : Authenticator {

    private val lock = Any()
    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loops
        if (responseCount(response) >= 2) {
            return null
        }

        if (response.code != 401) {
            return null
        }

        // Avoid refreshing on login or refresh requests themselves
        val path = response.request.url.encodedPath
        if (path.contains("api/auth/login") || path.contains("api/auth/refresh")) {
            return null
        }

        // Verify if error is TOKEN_INVALID
        val errorBody = try {
            response.peekBody(64 * 1024).string()
        } catch (e: Exception) {
            ""
        }

        var isTokenInvalid = true
        if (errorBody.isNotBlank()) {
            try {
                val errorResponse = gson.fromJson(errorBody, ApiErrorResponse::class.java)
                if (errorResponse?.code != null && errorResponse.code != ApiErrorCodes.TOKEN_INVALID) {
                    isTokenInvalid = false
                }
            } catch (_: Exception) {
            }
        }

        if (!isTokenInvalid) {
            return null
        }

        synchronized(lock) {
            val currentToken = preferenceManager.getAccessToken()
            val usedToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // If another thread has already refreshed the access token, retry with the new one
            if (!currentToken.isNullOrBlank() && currentToken != usedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val currentRefreshToken = preferenceManager.getRefreshToken()
            if (currentRefreshToken.isNullOrBlank()) {
                preferenceManager.clearSession()
                onSessionExpired()
                return null
            }

            val deviceId = preferenceManager.getOrCreateDeviceId()

            // Perform synchronous refresh call
            val refreshCall = refreshService.refreshTokenSync(
                RefreshRequest(
                    refreshToken = currentRefreshToken,
                    deviceId = deviceId
                )
            )

            try {
                val refreshResult = refreshCall.execute()
                if (refreshResult.isSuccessful) {
                    val authData = refreshResult.body()
                    if (authData != null) {
                        preferenceManager.saveTokens(
                            accessToken = authData.accessToken,
                            refreshToken = authData.refreshToken,
                            mustChangePassword = authData.mustChangePassword
                        )

                        return response.request.newBuilder()
                            .header("Authorization", "Bearer ${authData.accessToken}")
                            .build()
                    }
                }
            } catch (_: Exception) {
                // Network or parsing failure
            }

            // Refresh also failed - remove session and return member to sign-in screen
            preferenceManager.clearSession()
            onSessionExpired()
            return null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
