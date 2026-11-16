package shelftap.android.ui

import android.content.Intent
import android.nfc.Tag
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shelftap.android.AppContainer
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.nfc.CardException
import shelftap.android.nfc.NfcAvailability
import shelftap.android.nfc.TagReader
import shelftap.android.security.IntegrityFlag

val LocalContainer = compositionLocalOf<AppContainer> { error("AppContainer not provided") }

@Composable
fun ShelfTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
    }
}

/** ViewModel built from the app container; [key] separates instances that differ by an argument. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = LocalContainer.current
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val message: String) : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
}

/** What the member sees for a failure. Branches on the error `code`, never on the server's text. */
fun Throwable.userMessage(): String = when (this) {
    is ApiException.Http -> when (code) {
        ErrorCode.VALIDATION_FAILED -> "Check the details you entered."
        ErrorCode.INVALID_CREDENTIALS -> "Wrong email or password."
        ErrorCode.ACCOUNT_LOCKED -> "Too many failed logins. Try again later."
        ErrorCode.TOKEN_INVALID -> "Your session expired. Try again."
        ErrorCode.TOKEN_REUSED -> "This session was revoked. Log in again."
        ErrorCode.PASSWORD_CHANGE_REQUIRED -> "You must change your password first."
        ErrorCode.FORBIDDEN -> "You are not allowed to do that."
        ErrorCode.NOT_FOUND -> "Not found."
        ErrorCode.CARD_NOT_ACTIVE -> "This card is blocked, lost or revoked. See a librarian."
        ErrorCode.CARD_AUTH_FAILED -> "The card could not be verified."
        ErrorCode.CHALLENGE_EXPIRED -> "Took too long. Hold the card on the phone and try again."
        ErrorCode.COPY_NOT_AVAILABLE -> "This copy is already on loan or cannot be lent."
        ErrorCode.LOAN_LIMIT_REACHED -> "You have reached the maximum number of loans."
        ErrorCode.UNPAID_FINE -> "You have an unpaid fine. Pay it at the desk first."
        ErrorCode.RENEWAL_NOT_ALLOWED -> "This loan cannot be renewed (already renewed, overdue or reserved)."
        ErrorCode.RESERVED_BY_OTHER -> "This copy is held for another member."
        ErrorCode.ALREADY_RESERVED -> "You already have a reservation or loan for this title."
        ErrorCode.RATE_LIMITED -> "Too many requests." + (retryAfterSeconds?.let { " Try again in $it s." } ?: "")
        ErrorCode.STEP_UP_REQUIRED -> "This needs your password. Log in with it first."
        ErrorCode.KEY_ALREADY_REGISTERED, ErrorCode.UNKNOWN -> "The server could not do that (HTTP $status)." // key registration is staff-only
    }
    is ApiException.Network -> "Can't reach the server. Check your connection."
    is CardException.Lost -> "Lost the card. Hold it still against the back of the phone."
    is CardException.Status -> if (sw == 0x6986) "This card refuses that over NFC." else "The card answered with an error (%04X).".format(sw)
    is CardException.Protocol -> "That is not a ShelfTap library card, or it has not been issued."
    else -> "Something went wrong."
}

fun formatDate(iso: String): String =
    try {
        Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    } catch (_: DateTimeParseException) {
        iso
    }

@Composable
fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun Spinner() = Centered { CircularProgressIndicator() }

@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message != null) Text(message, color = MaterialTheme.colorScheme.error, modifier = modifier)
}

/** Loading / failed / content for a [Load]. */
@Composable
fun <T> LoadView(load: Load<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    when (load) {
        Load.Loading -> Spinner()
        is Load.Failed -> Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ErrorText(load.message)
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
        is Load.Ready -> content(load.value)
    }
}

/** Warns, never blocks: the server is told the same flags in `X-Device-Integrity`. */
@Composable
fun IntegrityBanner(modifier: Modifier = Modifier) {
    val integrity = LocalContainer.current.integrity
    val flags by produceState(emptySet<IntegrityFlag>()) { value = withContext(Dispatchers.Default) { integrity.flags() } }
    if (flags.isNotEmpty()) {
        Text(
            "Device warning: ${flags.joinToString(", ") { it.wire }}. ShelfTap still works, and the library is told.",
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

/** Reader mode while this screen is resumed; released on pause, so the system never sees a stale foreground reader. */
@Composable
fun NfcReaderEffect(flags: Int, onTag: (Tag) -> Unit) {
    val activity = LocalActivity.current ?: return
    val current by rememberUpdatedState(onTag)
    LifecycleResumeEffect(flags) {
        TagReader.enable(activity, flags) { current(it) }
        onPauseOrDispose { TagReader.disable(activity) }
    }
}

/** Shows [content] when NFC is on; otherwise says what is missing. Re-checks on resume (the user may come back from settings). */
@Composable
fun NfcGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var availability by remember { mutableStateOf(TagReader.availability(context)) }
    LifecycleResumeEffect(Unit) {
        availability = TagReader.availability(context)
        onPauseOrDispose { }
    }
    when (availability) {
        NfcAvailability.READY -> content()
        NfcAvailability.MISSING -> Centered { Text("This phone has no NFC.") }
        NfcAvailability.DISABLED -> Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("NFC is switched off.")
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }) { Text("Open NFC settings") }
            }
        }
    }
}
