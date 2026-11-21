package vn.shelftap.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import vn.shelftap.app.data.ApiException

fun formatDate(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .format(instant.atZone(ZoneId.systemDefault()).toLocalDate())

/** "3 days left", "Due today", or "Past due by 2 days" relative to [now]. */
fun daysLeftText(due: Instant, now: Instant): String {
    val zone = ZoneId.systemDefault()
    val days = ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), due.atZone(zone).toLocalDate())
    return if (due.isBefore(now)) {
        val late = maxOf(1L, -days)
        if (late == 1L) "Past due by 1 day" else "Past due by $late days"
    } else {
        when (days) {
            0L -> "Due today"
            1L -> "1 day left"
            else -> "$days days left"
        }
    }
}

/** Message for an error that happened with no usable saved data. */
fun describeError(e: Throwable): String = when (e) {
    is IOException -> "No network connection."
    is ApiException -> e.message ?: "The request failed."
    else -> "Something went wrong."
}

@Composable
fun SavedDataNote(modifier: Modifier = Modifier) {
    Text(
        text = "Showing saved data",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
