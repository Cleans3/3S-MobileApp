package com.example.libraryaccount.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(context: Context, private val api: LibraryApi) {
    private val preferences = context.getSharedPreferences("library_account", Context.MODE_PRIVATE)
    val isSignedIn: Boolean get() = !preferences.getString(KEY_TOKEN, null).isNullOrBlank()

    suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        val token = api.login(email, password)
        preferences.edit().putString(KEY_TOKEN, token).apply()
    }

    suspend fun profile(): Profile = withContext(Dispatchers.IO) {
        api.getProfile(preferences.getString(KEY_TOKEN, null) ?: error("Not signed in"))
    }

    fun signOut() { preferences.edit().remove(KEY_TOKEN).apply() }
    private companion object { const val KEY_TOKEN = "session_token" }
}
