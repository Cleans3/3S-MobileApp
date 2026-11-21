package vn.shelftap.app.data

import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

data class Loan(
    val loanId: String,
    val copyId: String,
    val titleId: String,
    val title: String,
    val barcode: String,
    val outAt: Instant,
    val dueAt: Instant,
    val returnedAt: Instant?,
    val renewals: Int,
    val renewable: Boolean,
)

data class HistoryItem(
    val titleId: String,
    val title: String,
    val outAt: Instant,
    val returnedAt: Instant?,
)

data class Member(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean,
)

data class CatalogTitle(
    val titleId: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int,
    val copiesTotal: Int,
)

internal fun loanFromJson(json: JSONObject): Loan = Loan(
    loanId = json.idOf("loanId"),
    copyId = json.idOf("copyId"),
    titleId = json.idOf("titleId"),
    title = json.getString("title"),
    barcode = json.optString("barcode"),
    outAt = json.getInstant("outAt"),
    dueAt = json.getInstant("dueAt"),
    returnedAt = json.optInstant("returnedAt"),
    renewals = json.getInt("renewals"),
    renewable = json.getBoolean("renewable"),
)

internal fun Loan.toJson(): JSONObject = JSONObject()
    .put("loanId", loanId)
    .put("copyId", copyId)
    .put("titleId", titleId)
    .put("title", title)
    .put("barcode", barcode)
    .put("outAt", outAt.toString())
    .put("dueAt", dueAt.toString())
    .put("returnedAt", returnedAt?.toString() ?: JSONObject.NULL)
    .put("renewals", renewals)
    .put("renewable", renewable)

internal fun historyItemFromJson(json: JSONObject): HistoryItem = HistoryItem(
    titleId = json.idOf("titleId"),
    title = json.getString("title"),
    outAt = json.getInstant("outAt"),
    returnedAt = json.optInstant("returnedAt"),
)

internal fun HistoryItem.toJson(): JSONObject = JSONObject()
    .put("titleId", titleId)
    .put("title", title)
    .put("outAt", outAt.toString())
    .put("returnedAt", returnedAt?.toString() ?: JSONObject.NULL)

internal fun memberFromJson(json: JSONObject): Member = Member(
    memberId = json.idOf("memberId"),
    fullName = json.getString("fullName"),
    email = json.getString("email"),
    memberType = json.getString("memberType"),
    status = json.getString("status"),
    historyEnabled = json.getBoolean("historyEnabled"),
    mustChangePassword = json.optBoolean("mustChangePassword", false),
)

internal fun catalogTitleFromJson(json: JSONObject): CatalogTitle = CatalogTitle(
    titleId = json.idOf("titleId"),
    isbn = json.optString("isbn"),
    title = json.getString("title"),
    authors = json.optStringList("authors"),
    subjects = json.optStringList("subjects"),
    coverUrl = json.optStringOrNull("coverUrl"),
    copiesAvailable = json.getInt("copiesAvailable"),
    copiesTotal = json.getInt("copiesTotal"),
)

internal fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    List(length()) { index -> transform(getJSONObject(index)) }

private fun JSONObject.idOf(name: String): String = get(name).toString()

private fun JSONObject.getInstant(name: String): Instant = Instant.parse(getString(name))

private fun JSONObject.optInstant(name: String): Instant? =
    optStringOrNull(name)?.let { Instant.parse(it) }

private fun JSONObject.optStringOrNull(name: String): String? =
    if (isNull(name)) null else optString(name).ifEmpty { null }

private fun JSONObject.optStringList(name: String): List<String> =
    optJSONArray(name)?.let { array -> List(array.length()) { array.getString(it) } }.orEmpty()
