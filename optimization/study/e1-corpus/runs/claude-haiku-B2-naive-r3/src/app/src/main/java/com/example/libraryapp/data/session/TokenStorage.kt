package com.example.libraryapp.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** Persists the access token so the member stays signed in across app restarts. */
class TokenStorage(context: Context) {

    private val store = context.applicationContext.sessionDataStore
    private val tokenKey = stringPreferencesKey("access_token")

    suspend fun currentToken(): String? = store.data.first()[tokenKey]

    suspend fun save(token: String) {
        store.edit { prefs -> prefs[tokenKey] = token }
    }

    suspend fun clear() {
        store.edit { prefs -> prefs.remove(tokenKey) }
    }
}
