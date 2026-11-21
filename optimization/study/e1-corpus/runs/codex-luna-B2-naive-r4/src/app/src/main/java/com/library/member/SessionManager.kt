package com.library.member

import android.content.Context

class SessionManager(context: Context) {
    private val preferences = context.getSharedPreferences("member_session", Context.MODE_PRIVATE)
    val token: String? get() = preferences.getString("token", null)
    fun saveToken(value: String) = preferences.edit().putString("token", value).apply()
    fun clear() = preferences.edit().clear().apply()
}
