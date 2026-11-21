package vn.shelftap.app.data

import android.content.Context
import android.util.Base64
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

internal val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** Everything the app keeps about the installation and the signed-in member. Stored only as ciphertext. */
data class Session(
    val deviceId: String,
    val accessToken: String? = null,
    val accessExpiresAtMillis: Long = 0L,
    val refreshToken: String? = null,
    val mustChangePassword: Boolean = false,
)

/**
 * Persists the [Session] as a single AES-256-GCM blob (Android Keystore key) in app-private DataStore.
 * Plain tokens or personal data are never written to disk.
 */
class SessionStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher,
    private val io: CoroutineDispatcher,
) {
    /** Returns null when nothing is stored, or when the stored blob can no longer be decrypted. */
    suspend fun load(): Session? = withContext(io) {
        val blob = dataStore.data.first()[SESSION_BLOB] ?: return@withContext null
        try {
            decode(cipher.decrypt(Base64.decode(blob, Base64.NO_WRAP)))
        } catch (e: GeneralSecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: JSONException) {
            null
        }
    }

    suspend fun save(session: Session) {
        withContext(io) {
            val ciphertext = cipher.encrypt(encode(session).toByteArray(Charsets.UTF_8))
            dataStore.edit { it[SESSION_BLOB] = Base64.encodeToString(ciphertext, Base64.NO_WRAP) }
        }
    }

    private fun encode(session: Session): String = JSONObject().apply {
        put("deviceId", session.deviceId)
        session.accessToken?.let { put("accessToken", it) }
        put("accessExpiresAt", session.accessExpiresAtMillis)
        session.refreshToken?.let { put("refreshToken", it) }
        put("mustChangePassword", session.mustChangePassword)
    }.toString()

    private fun decode(bytes: ByteArray): Session {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        return Session(
            deviceId = json.getString("deviceId"),
            accessToken = json.optString("accessToken").ifEmpty { null },
            accessExpiresAtMillis = json.optLong("accessExpiresAt"),
            refreshToken = json.optString("refreshToken").ifEmpty { null },
            mustChangePassword = json.optBoolean("mustChangePassword", false),
        )
    }

    private companion object {
        val SESSION_BLOB = stringPreferencesKey("session_blob")
    }
}
