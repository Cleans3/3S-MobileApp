package vn.shelftap.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.security.GeneralSecurityException
import java.util.UUID

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "shelftap_session")

data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val mustChangePassword: Boolean,
)

/**
 * Persists the session as one AES-256-GCM ciphertext blob in app-private DataStore.
 * The plaintext tokens never touch disk. The device id is a random UUID, not personal data.
 */
class SessionStore(
    private val context: Context,
    private val cipher: TokenCipher,
    private val dispatcher: CoroutineDispatcher,
) {
    private val dataStore: DataStore<Preferences>
        get() = context.sessionDataStore

    /** Returns the install's device id, creating it once on first use. */
    suspend fun deviceId(): String = withContext(dispatcher) {
        var result = ""
        dataStore.edit { prefs ->
            result = prefs[DEVICE_ID_KEY]
                ?: UUID.randomUUID().toString().also { prefs[DEVICE_ID_KEY] = it }
        }
        result
    }

    /** Returns the stored session, or null if none exists or it cannot be decrypted. */
    suspend fun loadSession(): StoredSession? = withContext(dispatcher) {
        val blob = dataStore.data.first()[SESSION_KEY] ?: return@withContext null
        val session = try {
            decode(cipher.decrypt(blob))
        } catch (e: GeneralSecurityException) {
            null // e.g. the Keystore key is gone: treat as signed out
        } catch (e: JSONException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
        if (session == null) {
            clearSession()
        }
        session
    }

    suspend fun saveSession(session: StoredSession) = withContext(dispatcher) {
        val json = JSONObject()
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("mustChangePassword", session.mustChangePassword)
        val blob = cipher.encrypt(json.toString().toByteArray(Charsets.UTF_8))
        dataStore.edit { prefs -> prefs[SESSION_KEY] = blob }
        Unit
    }

    suspend fun clearSession() = withContext(dispatcher) {
        dataStore.edit { prefs -> prefs.remove(SESSION_KEY) }
        Unit
    }

    private fun decode(bytes: ByteArray): StoredSession {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        return StoredSession(
            accessToken = json.getString("accessToken"),
            refreshToken = json.getString("refreshToken"),
            mustChangePassword = json.getBoolean("mustChangePassword"),
        )
    }

    private companion object {
        val SESSION_KEY = stringPreferencesKey("session_blob")
        val DEVICE_ID_KEY = stringPreferencesKey("device_id")
    }
}
