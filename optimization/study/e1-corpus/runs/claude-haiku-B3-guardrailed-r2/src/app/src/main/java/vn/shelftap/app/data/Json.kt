package vn.shelftap.app.data

import org.json.JSONArray
import org.json.JSONObject
import vn.shelftap.app.model.HistoryItem
import vn.shelftap.app.model.Loan
import vn.shelftap.app.model.Member
import vn.shelftap.app.model.TitleDetail
import java.time.Instant
import java.time.OffsetDateTime

// Parsing and encoding for the REST payloads. The same JSON shapes are used for
// the encrypted local copy, so the cache and the API share one format.

internal fun parseInstant(text: String): Instant = OffsetDateTime.parse(text).toInstant()

internal fun <T> parseArray(text: String, parse: (JSONObject) -> T): List<T> {
    val array = JSONArray(text)
    return List(array.length()) { parse(array.getJSONObject(it)) }
}

private fun JSONObject.optNullableString(name: String): String? =
    if (isNull(name)) null else getString(name)

private fun JSONArray?.toStrings(): List<String> =
    if (this == null) emptyList() else List(length()) { getString(it) }

internal fun parseLoan(o: JSONObject): Loan = Loan(
    loanId = o.getString("loanId"),
    copyId = o.getString("copyId"),
    titleId = o.getString("titleId"),
    title = o.getString("title"),
    barcode = o.getString("barcode"),
    outAt = parseInstant(o.getString("outAt")),
    dueAt = parseInstant(o.getString("dueAt")),
    returnedAt = o.optNullableString("returnedAt")?.let(::parseInstant),
    renewals = o.getInt("renewals"),
    renewable = o.getBoolean("renewable"),
)

internal fun loanToJson(loan: Loan): JSONObject = JSONObject()
    .put("loanId", loan.loanId)
    .put("copyId", loan.copyId)
    .put("titleId", loan.titleId)
    .put("title", loan.title)
    .put("barcode", loan.barcode)
    .put("outAt", loan.outAt.toString())
    .put("dueAt", loan.dueAt.toString())
    .put("returnedAt", loan.returnedAt?.toString() ?: JSONObject.NULL)
    .put("renewals", loan.renewals)
    .put("renewable", loan.renewable)

internal fun parseHistoryItem(o: JSONObject): HistoryItem = HistoryItem(
    titleId = o.getString("titleId"),
    title = o.getString("title"),
    outAt = parseInstant(o.getString("outAt")),
    returnedAt = o.optNullableString("returnedAt")?.let(::parseInstant),
)

internal fun historyToJson(item: HistoryItem): JSONObject = JSONObject()
    .put("titleId", item.titleId)
    .put("title", item.title)
    .put("outAt", item.outAt.toString())
    .put("returnedAt", item.returnedAt?.toString() ?: JSONObject.NULL)

internal fun parseMember(o: JSONObject): Member = Member(
    memberId = o.getString("memberId"),
    fullName = o.getString("fullName"),
    email = o.getString("email"),
    memberType = o.getString("memberType"),
    status = o.getString("status"),
    historyEnabled = o.getBoolean("historyEnabled"),
    mustChangePassword = o.getBoolean("mustChangePassword"),
)

internal fun parseTitle(o: JSONObject): TitleDetail = TitleDetail(
    titleId = o.getString("titleId"),
    isbn = o.getString("isbn"),
    title = o.getString("title"),
    authors = o.optJSONArray("authors").toStrings(),
    subjects = o.optJSONArray("subjects").toStrings(),
    coverUrl = o.optNullableString("coverUrl"),
    copiesAvailable = o.getInt("copiesAvailable"),
    copiesTotal = o.getInt("copiesTotal"),
)
