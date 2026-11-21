package com.example.libraryapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.libraryapp.data.model.MemberProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages persisting authentication tokens and member profile data.
 * Employs EncryptedSharedPreferences for secure token persistence,
 * falling back to standard SharedPreferences if Keystore is unavailable.
 */
class SessionManager(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for environments / unit tests without Android KeyStore
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val ENCRYPTED_PREFS_NAME = "library_secure_session"
        private const val PREFS_NAME = "library_session"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_PROFILE = "user_profile"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveAuthToken(token: String) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveRefreshToken(token: String) {
        prefs.edit()
            .putString(KEY_REFRESH_TOKEN, token)
            .apply()
    }

    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveUserProfile(profile: MemberProfile) {
        val jsonString = json.encodeToString(profile)
        prefs.edit()
            .putString(KEY_USER_PROFILE, jsonString)
            .apply()
    }

    fun getUserProfile(): MemberProfile? {
        val jsonString = prefs.getString(KEY_USER_PROFILE, null) ?: return null
        return try {
            json.decodeFromString<MemberProfile>(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        val token = getAuthToken()
        return !token.isNullOrEmpty() && prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_PROFILE)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
}
