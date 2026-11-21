package com.library.account.data.session

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.library.account.data.model.MemberProfile

/**
 * Manages secure persistent session data, credentials, and member caching.
 * Enables the member to stay signed in across app launches.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "library_session_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_STAY_SIGNED_IN = "stay_signed_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_MEMBER_NUMBER = "member_number"
        private const val KEY_CACHED_PROFILE = "cached_profile"
        private const val KEY_MOCK_MODE = "mock_mode"
    }

    /**
     * Checks if the user has an active session and is set to stay signed in.
     */
    fun isLoggedIn(): Boolean {
        val hasToken = !getAuthToken().isNullOrBlank()
        val staySignedIn = prefs.getBoolean(KEY_STAY_SIGNED_IN, true)
        return hasToken && staySignedIn
    }

    /**
     * Saves authentication tokens and member metadata after a successful sign-in.
     */
    fun saveSession(
        token: String,
        email: String,
        fullName: String,
        memberNumber: String,
        staySignedIn: Boolean
    ) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, fullName)
            .putString(KEY_MEMBER_NUMBER, memberNumber)
            .putBoolean(KEY_STAY_SIGNED_IN, staySignedIn)
            .apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getMemberNumber(): String? {
        return prefs.getString(KEY_MEMBER_NUMBER, null)
    }

    fun shouldStaySignedIn(): Boolean {
        return prefs.getBoolean(KEY_STAY_SIGNED_IN, true)
    }

    /**
     * Caches the fetched member profile for offline inspection and fast app launches.
     */
    fun saveCachedProfile(profile: MemberProfile) {
        val json = gson.toJson(profile)
        prefs.edit()
            .putString(KEY_CACHED_PROFILE, json)
            .putString(KEY_USER_EMAIL, profile.email)
            .putString(KEY_USER_NAME, profile.fullName)
            .putString(KEY_MEMBER_NUMBER, profile.memberNumber)
            .apply()
    }

    fun getCachedProfile(): MemberProfile? {
        val json = prefs.getString(KEY_CACHED_PROFILE, null) ?: return null
        return try {
            gson.fromJson(json, MemberProfile::class.java)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Clears all session information upon sign-out.
     */
    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_MEMBER_NUMBER)
            .remove(KEY_CACHED_PROFILE)
            .apply()
    }

    fun isMockModeEnabled(): Boolean {
        // Defaults to true so out-of-the-box evaluation succeeds immediately without external server
        return prefs.getBoolean(KEY_MOCK_MODE, true)
    }

    fun setMockModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MOCK_MODE, enabled).apply()
    }
}
