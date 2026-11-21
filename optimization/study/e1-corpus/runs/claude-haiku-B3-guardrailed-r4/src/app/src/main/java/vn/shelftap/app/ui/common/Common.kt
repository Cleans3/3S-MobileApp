package vn.shelftap.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vn.shelftap.app.R

/** Errors that the screens show to the member. Each maps to a string resource. */
enum class UiError {
    /** Offline and no saved copy exists yet. */
    NO_SAVED_DATA,

    /** Offline while performing an action that needs the network. */
    OFFLINE,

    /** The server refused a renewal (RENEWAL_NOT_ALLOWED, HTTP 409). */
    RENEWAL_NOT_ALLOWED,

    GENERIC,
}

@StringRes
fun UiError.messageRes(): Int = when (this) {
    UiError.NO_SAVED_DATA -> R.string.error_no_saved_data
    UiError.OFFLINE -> R.string.error_offline
    UiError.RENEWAL_NOT_ALLOWED -> R.string.renewal_not_allowed
    UiError.GENERIC -> R.string.generic_error
}

@Composable
fun SavedDataNote(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.showing_saved_data),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun CenteredProgress(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MessageWithRetry(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}
