package com.example.libraryapp.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/** Persists the sign-in token so the member stays signed in across app restarts. */
class SessionStore(private val appContext: Context) {

    private val tokenKey = stringPreferencesKey("auth_token")

    val token: Flow<String?> = appContext.sessionDataStore.data.map { it[tokenKey] }

    suspend fun saveToken(token: String) {
        appContext.sessionDataStore.edit { it[tokenKey] = token }
    }

    suspend fun clearToken() {
        appContext.sessionDataStore.edit { it.remove(tokenKey) }
    }
}
