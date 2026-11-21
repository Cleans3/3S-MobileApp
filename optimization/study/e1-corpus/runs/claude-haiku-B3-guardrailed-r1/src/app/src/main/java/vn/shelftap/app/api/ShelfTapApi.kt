package vn.shelftap.app.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import vn.shelftap.app.auth.AuthRepository

/** An HTTP error answered by the server. [errorCode] is the API error code, if any. */
class ApiException(val httpCode: Int, val errorCode: String?) :
    Exception("Request failed with HTTP $httpCode")

class ShelfTapApi(
    private val auth: AuthRepository,
    private val client: OkHttpClient,
    private val json: Json,
    private val io: CoroutineDispatcher,
) {

    suspend fun loans(): List<Loan> =
        send("GET", ListSerializer(Loan.serializer()), null, "api", "me", "loans")

    suspend fun history(): List<HistoryItem> =
        send("GET", ListSerializer(HistoryItem.serializer()), null, "api", "me", "history")

    suspend fun renew(loanId: String): Loan =
        send("POST", Loan.serializer(), null, "api", "loans", loanId, "renew")

    suspend fun me(): Me =
        send("GET", Me.serializer(), null, "api", "me")

    suspend fun updatePrivacy(enabled: Boolean): Me {
        val body = json.encodeToString(PrivacyUpdate.serializer(), PrivacyUpdate(enabled))
        return send("PUT", Me.serializer(), body, "api", "me", "privacy")
    }

    suspend fun title(titleId: String): TitleDetail =
        send("GET", TitleDetail.serializer(), null, "api", "titles", titleId)

    private suspend fun <T> send(
        method: String,
        serializer: KSerializer<T>,
        body: String?,
        vararg segments: String,
    ): T = withContext(io) {
        val token = auth.accessToken()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host(API_HOST)
            .apply { segments.forEach { addPathSegment(it) } }
            .build()
        val requestBody = if (method == "GET") null else (body ?: "").toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .method(method, requestBody)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException(response.code, parseErrorCode(text))
            }
            json.decodeFromString(serializer, text)
        }
    }

    private fun parseErrorCode(text: String): String? = try {
        val root = json.parseToJsonElement(text).jsonObject
        root["code"]?.jsonPrimitive?.contentOrNull
            ?: root["error"]?.jsonObject?.get("code")?.jsonPrimitive?.contentOrNull
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: kotlinx.serialization.SerializationException) {
        null
    }

    private companion object {
        const val API_HOST = "api.shelftap.example"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
