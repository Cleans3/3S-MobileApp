package vn.shelftap.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.sessionDataStore by preferencesDataStore(name = "shelftap_session")

private val DEVICE_ID = stringPreferencesKey("device_id")
private val ACCESS_TOKEN = stringPreferencesKey("access_token")
private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")

data class Tokens(val accessToken: String, val refreshToken: String)

/**
 * Persists the device id and the session tokens in DataStore.
 *
 * Note: values are stored unencrypted in app-private storage. Backups are disabled in the manifest.
 */
class SessionStore(context: Context) {
    private val store = context.applicationContext.sessionDataStore

    /** The installation's device id. Created once, on first use, and kept for the life of the install. */
    suspend fun deviceId(): String {
        val existing = store.data.first()[DEVICE_ID]
        if (existing != null) return existing
        val updated = store.edit { prefs ->
            if (prefs[DEVICE_ID] == null) {
                prefs[DEVICE_ID] = UUID.randomUUID().toString()
            }
        }
        return updated[DEVICE_ID]!!
    }

    /** Returns the stored tokens, or null when no member is signed in. */
    suspend fun readTokens(): Tokens? {
        val prefs = store.data.first()
        val access = prefs[ACCESS_TOKEN]
        val refresh = prefs[REFRESH_TOKEN]
        return if (access != null && refresh != null) Tokens(access, refresh) else null
    }

    suspend fun saveTokens(tokens: Tokens) {
        store.edit { prefs ->
            prefs[ACCESS_TOKEN] = tokens.accessToken
            prefs[REFRESH_TOKEN] = tokens.refreshToken
        }
    }

    /** Removes the session. The device id is kept. */
    suspend fun clearSession() {
        store.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
        }
    }
}
