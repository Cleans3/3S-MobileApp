package com.example.libraryaccount.data

import android.content.Context

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("library_session", Context.MODE_PRIVATE)
    var token: String?
        get() = preferences.getString("token", null)
        set(value) = preferences.edit().putString("token", value).apply()

    fun clear() = preferences.edit().clear().apply()
}
