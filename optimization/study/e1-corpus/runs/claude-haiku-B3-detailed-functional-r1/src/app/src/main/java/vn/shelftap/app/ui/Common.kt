package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import vn.shelftap.app.data.ApiException

val localZone: ZoneId = ZoneId.systemDefault()

/** Parses an ISO-8601 timestamp (UTC "Z" or with an offset). Returns null when it is not valid. */
fun parseInstant(value: String?): Instant? =
    value?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }

fun formatDate(value: String?): String {
    val instant = parseInstant(value) ?: return "—"
    return instant.atZone(localZone).toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
}

/** Whole days from today (local date) until the date of [dueAt]. Negative when already past. */
fun daysUntilDue(dueAt: Instant): Long =
    ChronoUnit.DAYS.between(LocalDate.now(localZone), dueAt.atZone(localZone).toLocalDate())

fun Throwable.toUserMessage(): String = when (this) {
    is IOException -> "No network connection"
    is ApiException -> "The server returned an error (HTTP $httpCode)"
    else -> "Something went wrong"
}

@Composable
fun SavedDataNote(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = "Showing saved data",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}
