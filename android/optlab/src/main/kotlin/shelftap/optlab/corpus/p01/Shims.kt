package shelftap.optlab.corpus.p01

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

// HistoryItem from vn/shelftap/app/data/Models.kt, parseInstant and formatDate from vn/shelftap/app/ui/Common.kt, copied unchanged.

data class HistoryItem(
    val titleId: String,
    val title: String,
    val outAt: String,
    val returnedAt: String? = null,
)

val localZone: ZoneId = ZoneId.systemDefault()

/** Parses an ISO-8601 timestamp (UTC "Z" or with an offset). Returns null when it is not valid. */
fun parseInstant(value: String?): Instant? =
    value?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }

fun formatDate(value: String?): String {
    val instant = parseInstant(value) ?: return "—"
    return instant.atZone(localZone).toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
}
