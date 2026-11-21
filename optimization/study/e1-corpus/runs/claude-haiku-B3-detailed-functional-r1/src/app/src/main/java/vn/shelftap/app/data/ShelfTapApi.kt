package vn.shelftap.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import vn.shelftap.app.auth.AuthRepository

/** Thrown for a non-2xx response. [errorCode] is the server's "code" field, if any. */
class ApiException(val httpCode: Int, val errorCode: String?) :
    Exception("API error HTTP $httpCode ${errorCode.orEmpty()}")

class ShelfTapApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val auth: AuthRepository,
) {
    private val base: HttpUrl = BASE_URL.toHttpUrl()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun loans(): List<Loan> =
        call(Request.Builder().url(url("api", "me", "loans")).get(), ListSerializer(Loan.serializer()))

    suspend fun history(): List<HistoryItem> =
        call(Request.Builder().url(url("api", "me", "history")).get(), ListSerializer(HistoryItem.serializer()))

    suspend fun member(): Member =
        call(Request.Builder().url(url("api", "me")).get(), Member.serializer())

    suspend fun renewLoan(loanId: String): Loan =
        call(
            Request.Builder().url(url("api", "loans", loanId, "renew")).post("".toRequestBody(jsonMedia)),
            Loan.serializer(),
        )

    suspend fun setHistoryEnabled(enabled: Boolean): Member {
        val body = json.encodeToString(PrivacyUpdate.serializer(), PrivacyUpdate(enabled))
        return call(
            Request.Builder().url(url("api", "me", "privacy")).put(body.toRequestBody(jsonMedia)),
            Member.serializer(),
        )
    }

    suspend fun title(titleId: String): BookTitle =
        call(Request.Builder().url(url("api", "titles", titleId)).get(), BookTitle.serializer())

    private fun url(vararg segments: String): HttpUrl =
        base.newBuilder().apply { segments.forEach { addPathSegment(it) } }.build()

    private suspend fun <T> call(request: Request.Builder, serializer: KSerializer<T>): T =
        withContext(Dispatchers.IO) {
            val token = auth.accessToken()
            val httpRequest = request
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val code = runCatching {
                        json.decodeFromString(ApiErrorBody.serializer(), body).code
                    }.getOrNull()
                    throw ApiException(response.code, code)
                }
                json.decodeFromString(serializer, body)
            }
        }

    companion object {
        const val BASE_URL = "https://api.shelftap.example"
    }
}
