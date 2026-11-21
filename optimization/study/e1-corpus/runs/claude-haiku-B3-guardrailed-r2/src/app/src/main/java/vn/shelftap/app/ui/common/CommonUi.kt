package vn.shelftap.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.shelftap.app.R
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** User-facing messages. ViewModels pick one; screens resolve it to text. */
enum class UiMessage(@StringRes val resId: Int) {
    NO_CONNECTION(R.string.error_no_connection),
    LOAD_FAILED(R.string.error_load_failed),
    RENEWAL_NOT_ALLOWED(R.string.error_renewal_not_allowed),
    RENEW_FAILED(R.string.error_renew_failed),
    PRIVACY_FAILED(R.string.error_privacy_failed),
}

/** A network failure means no connection; any other failure gets [fallback]. */
fun Throwable.toUiMessage(fallback: UiMessage): UiMessage =
    if (this is IOException) UiMessage.NO_CONNECTION else fallback

/** Medium-style local date in the device time zone, for example "Mar 4, 2026". */
fun Instant.toDisplayDate(): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(this)

@Composable
fun SavedDataNote(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = stringResource(R.string.showing_saved_data),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun MessageNote(message: UiMessage, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(message.resId),
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}
