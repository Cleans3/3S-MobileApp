package com.example.libraryapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.libraryapp.data.model.MemberDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to standard private SharedPreferences if EncryptedSharedPreferences cannot be initialized
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _isLoggedInFlow = MutableStateFlow(getAuthToken() != null)
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        _isLoggedInFlow.value = true
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveMember(member: MemberDto) {
        val json = gson.toJson(member)
        prefs.edit().putString(KEY_CACHED_MEMBER, json).apply()
    }

    fun getMember(): MemberDto? {
        val json = prefs.getString(KEY_CACHED_MEMBER, null) ?: return null
        return try {
            gson.fromJson(json, MemberDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrBlank()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_CACHED_MEMBER)
            .apply()
        _isLoggedInFlow.value = false
    }

    companion object {
        private const val PREFS_NAME = "library_secure_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_CACHED_MEMBER = "cached_member"
    }
}
