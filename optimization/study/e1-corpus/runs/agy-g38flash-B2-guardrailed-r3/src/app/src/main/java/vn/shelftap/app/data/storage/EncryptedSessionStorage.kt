package vn.shelftap.app.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.security.KeyStoreManager
import java.util.UUID

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "shelftap_session")

/**
 * Manages encrypted session storage using Android Keystore AES-256-GCM encryption
 * and Preferences DataStore.
 */
class EncryptedSessionStorage(
    private val context: Context,
    private val keyStoreManager: KeyStoreManager = KeyStoreManager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private val KEY_ENCRYPTED_ACCESS_TOKEN = stringPreferencesKey("enc_access_token")
        private val KEY_ENCRYPTED_REFRESH_TOKEN = stringPreferencesKey("enc_refresh_token")
        private val KEY_ENCRYPTED_PROFILE = stringPreferencesKey("enc_profile")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_MUST_CHANGE_PASSWORD = booleanPreferencesKey("must_change_password")
    }

    /**
     * deviceId is a UUID that the app creates once, on first launch,
     * and keeps for the life of the installation.
     */
    suspend fun getOrCreateDeviceId(): String = withContext(ioDispatcher) {
        val existing = context.sessionDataStore.data.map { it[KEY_DEVICE_ID] }.firstOrNull()
        if (!existing.isNullOrBlank()) {
            existing
        } else {
            val newId = UUID.randomUUID().toString()
            context.sessionDataStore.edit { prefs ->
                if (prefs[KEY_DEVICE_ID].isNullOrBlank()) {
                    prefs[KEY_DEVICE_ID] = newId
                }
            }
            context.sessionDataStore.data.map { it[KEY_DEVICE_ID] }.firstOrNull() ?: newId
        }
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        mustChangePassword: Boolean
    ) = withContext(ioDispatcher) {
        val encAccess = keyStoreManager.encrypt(accessToken)
        val encRefresh = keyStoreManager.encrypt(refreshToken)
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_ENCRYPTED_ACCESS_TOKEN] = encAccess
            prefs[KEY_ENCRYPTED_REFRESH_TOKEN] = encRefresh
            prefs[KEY_MUST_CHANGE_PASSWORD] = mustChangePassword
        }
    }

    suspend fun getAccessToken(): String? = withContext(ioDispatcher) {
        val encAccess = context.sessionDataStore.data.map { it[KEY_ENCRYPTED_ACCESS_TOKEN] }.firstOrNull()
            ?: return@withContext null
        keyStoreManager.decrypt(encAccess)
    }

    suspend fun getRefreshToken(): String? = withContext(ioDispatcher) {
        val encRefresh = context.sessionDataStore.data.map { it[KEY_ENCRYPTED_REFRESH_TOKEN] }.firstOrNull()
            ?: return@withContext null
        keyStoreManager.decrypt(encRefresh)
    }

    suspend fun saveProfile(profile: MemberProfile) = withContext(ioDispatcher) {
        val profileJson = json.encodeToString(MemberProfile.serializer(), profile)
        val encProfile = keyStoreManager.encrypt(profileJson)
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_ENCRYPTED_PROFILE] = encProfile
            prefs[KEY_MUST_CHANGE_PASSWORD] = profile.mustChangePassword
        }
    }

    suspend fun getProfile(): MemberProfile? = withContext(ioDispatcher) {
        val encProfile = context.sessionDataStore.data.map { it[KEY_ENCRYPTED_PROFILE] }.firstOrNull()
            ?: return@withContext null
        val profileJson = keyStoreManager.decrypt(encProfile) ?: return@withContext null
        try {
            json.decodeFromString(MemberProfile.serializer(), profileJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isMustChangePassword(): Boolean = withContext(ioDispatcher) {
        context.sessionDataStore.data.map { it[KEY_MUST_CHANGE_PASSWORD] ?: false }.firstOrNull() ?: false
    }

    suspend fun setMustChangePassword(mustChange: Boolean) = withContext(ioDispatcher) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_MUST_CHANGE_PASSWORD] = mustChange
        }
    }

    suspend fun hasSession(): Boolean = withContext(ioDispatcher) {
        val token = getRefreshToken()
        !token.isNullOrBlank()
    }

    suspend fun clearSession() = withContext(ioDispatcher) {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(KEY_ENCRYPTED_ACCESS_TOKEN)
            prefs.remove(KEY_ENCRYPTED_REFRESH_TOKEN)
            prefs.remove(KEY_ENCRYPTED_PROFILE)
            prefs.remove(KEY_MUST_CHANGE_PASSWORD)
        }
    }
}
