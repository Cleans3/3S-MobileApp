package com.example.library.data

import com.example.library.BuildConfig
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class MemberProfile(
    val name: String,
    val email: String,
    val membershipNumber: String,
    val memberSince: String
)

class ApiException(val statusCode: Int) : IOException()

class LibraryApi {
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

    fun signIn(email: String, password: String): String {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        val response = request("/auth/login", "POST", body.toString())
        val json = JSONObject(response)
        return json.optString("token").ifBlank {
            json.optString("access_token").ifBlank { throw IOException("Missing access token") }
        }
    }

    fun profile(token: String): MemberProfile {
        val json = JSONObject(request("/me", "GET", null, token))
        val member = json.optJSONObject("member") ?: json
        return MemberProfile(
            name = member.optString("name").ifBlank {
                listOf(member.optString("firstName"), member.optString("lastName"))
                    .filter(String::isNotBlank).joinToString(" ")
            }.ifBlank { "Library member" },
            email = member.optString("email"),
            membershipNumber = member.optString("membershipNumber").ifBlank {
                member.optString("member_number")
            },
            memberSince = member.optString("memberSince").ifBlank {
                member.optString("member_since")
            }
        )
    }

    private fun request(path: String, method: String, body: String?, token: String? = null): String {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray()) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw ApiException(status)
            text
        } finally {
            connection.disconnect()
        }
    }
}
