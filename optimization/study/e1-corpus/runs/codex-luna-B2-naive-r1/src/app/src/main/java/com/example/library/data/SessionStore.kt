package com.example.library.data

import android.content.Context

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("library_session", Context.MODE_PRIVATE)

    var token: String?
        get() = preferences.getString(KEY_TOKEN, null)
        private set(value) = preferences.edit().putString(KEY_TOKEN, value).apply()

    fun save(token: String) { this.token = token }
    fun clear() { preferences.edit().clear().apply() }

    private companion object { const val KEY_TOKEN = "access_token" }
}
