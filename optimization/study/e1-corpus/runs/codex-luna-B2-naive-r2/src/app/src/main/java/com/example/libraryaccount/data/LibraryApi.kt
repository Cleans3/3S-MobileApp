package com.example.libraryaccount.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class LibraryApi(private val baseUrl: String) {
    fun login(email: String, password: String): String {
        val body = JSONObject().put("email", email).put("password", password).toString()
        val response = request("/auth/login", "POST", body, null)
        return response.optString("token").takeIf { it.isNotBlank() } ?: throw IOException("No session token returned")
    }

    fun getProfile(token: String): Profile {
        val json = request("/me", "GET", null, token)
        return Profile(
            memberId = json.optString("id", json.optString("memberId", "—")),
            name = json.optString("name", ""),
            email = json.optString("email", ""),
            memberSince = json.optString("memberSince").ifBlank { null }
        )
    }

    private fun request(path: String, method: String, body: String?, token: String?): JSONObject {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") }
        }
        try {
            body?.let { connection.outputStream.use { stream -> stream.write(it.toByteArray(StandardCharsets.UTF_8)) } }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull()
                throw IOException(message?.takeIf { it.isNotBlank() } ?: "Request failed ($status)")
            }
            return JSONObject(text)
        } finally { connection.disconnect() }
    }
}

data class Profile(val memberId: String, val name: String, val email: String, val memberSince: String?)
