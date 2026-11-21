package vn.shelftap.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.sessionDataStore by preferencesDataStore(name = "shelftap_session")

data class SessionSnapshot(
    val deviceId: String?,
    val accessToken: String?,
    val refreshToken: String?,
)

/**
 * Persists the device id and the current token pair in app-private DataStore.
 * The device id is created once and kept for the life of the installation.
 */
class SessionStore(private val context: Context) {

    private val deviceIdKey = stringPreferencesKey("device_id")
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    suspend fun read(): SessionSnapshot {
        val prefs = context.sessionDataStore.data.first()
        return SessionSnapshot(
            deviceId = prefs[deviceIdKey],
            accessToken = prefs[accessTokenKey],
            refreshToken = prefs[refreshTokenKey],
        )
    }

    suspend fun ensureDeviceId(): String {
        val prefs = context.sessionDataStore.edit { p ->
            if (p[deviceIdKey] == null) {
                p[deviceIdKey] = UUID.randomUUID().toString()
            }
        }
        return checkNotNull(prefs[deviceIdKey])
    }

    /** Refresh tokens are single use, so the new pair always replaces the old one in one write. */
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.sessionDataStore.edit { p ->
            p[accessTokenKey] = accessToken
            p[refreshTokenKey] = refreshToken
        }
    }

    /** Removes the tokens but keeps the device id. */
    suspend fun clearSession() {
        context.sessionDataStore.edit { p ->
            p.remove(accessTokenKey)
            p.remove(refreshTokenKey)
        }
    }
}
