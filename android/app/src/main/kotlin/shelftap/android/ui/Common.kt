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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import shelftap.android.R
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.nfc.CardException
import shelftap.android.nfc.NfcAvailability
import shelftap.android.nfc.TagReader
import shelftap.android.security.IntegrityFlag
import shelftap.android.ui.theme.Spacing

val LocalContainer = compositionLocalOf<AppContainer> { error("AppContainer not provided") }

/** ViewModel built from the app container; [key] separates instances that differ by an argument. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = LocalContainer.current
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val message: UiText, val offline: Boolean = false) : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
}

/** What the member sees for a failure. Branches on the error `code`, never on the server's text. */
fun Throwable.userMessage(): UiText = when (this) {
    is ApiException.Http -> when (code) {
        ErrorCode.VALIDATION_FAILED -> uiText(R.string.err_validation)
        ErrorCode.INVALID_CREDENTIALS -> uiText(R.string.err_invalid_credentials)
        ErrorCode.ACCOUNT_LOCKED -> uiText(R.string.err_account_locked)
        ErrorCode.TOKEN_INVALID -> uiText(R.string.err_token_invalid)
        ErrorCode.TOKEN_REUSED -> uiText(R.string.err_token_reused)
        ErrorCode.PASSWORD_CHANGE_REQUIRED -> uiText(R.string.err_password_change_required)
        ErrorCode.FORBIDDEN -> uiText(R.string.err_forbidden)
        ErrorCode.NOT_FOUND -> uiText(R.string.err_not_found)
        ErrorCode.CARD_NOT_ACTIVE -> uiText(R.string.err_card_not_active)
        ErrorCode.CARD_AUTH_FAILED -> uiText(R.string.err_card_auth_failed)
        ErrorCode.CHALLENGE_EXPIRED -> uiText(R.string.err_challenge_expired)
        ErrorCode.COPY_NOT_AVAILABLE -> uiText(R.string.err_copy_not_available)
        ErrorCode.LOAN_LIMIT_REACHED -> uiText(R.string.err_loan_limit)
        ErrorCode.UNPAID_FINE -> uiText(R.string.err_unpaid_fine)
        ErrorCode.RENEWAL_NOT_ALLOWED -> uiText(R.string.err_renewal_not_allowed)
        ErrorCode.RESERVED_BY_OTHER -> uiText(R.string.err_reserved_by_other)
        ErrorCode.ALREADY_RESERVED -> uiText(R.string.err_already_reserved)
        ErrorCode.RATE_LIMITED -> retryAfterSeconds?.let { uiText(R.string.err_rate_limited_retry, it) } ?: uiText(R.string.err_rate_limited)
        ErrorCode.STEP_UP_REQUIRED -> uiText(R.string.err_step_up)
        ErrorCode.KEY_ALREADY_REGISTERED, ErrorCode.UNKNOWN -> uiText(R.string.err_server, status) // key registration is staff-only
    }
    is ApiException.Network -> uiText(R.string.err_network)
    is CardException.Lost -> uiText(R.string.err_card_lost)
    is CardException.Status -> if (sw == 0x6986) uiText(R.string.err_card_refused) else uiText(R.string.err_card_status, sw)
    is CardException.Protocol -> uiText(R.string.err_card_protocol)
    else -> uiText(R.string.err_generic)
}

fun formatDate(iso: String): String =
    try {
        Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    } catch (_: DateTimeParseException) {
        iso
    }

@Composable
fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun Spinner() = Centered { CircularProgressIndicator() }

@Composable
fun ErrorText(message: UiText?, modifier: Modifier = Modifier) {
    if (message != null) Text(message.asString(), color = MaterialTheme.colorScheme.error, modifier = modifier)
}

/** A failed [Load] for [Throwable]: no connection is flagged, so the screen can say so. */
fun Throwable.toFailed(): Load.Failed = Load.Failed(userMessage(), offline = this is ApiException.Network)

/** Loading / failed / content for a [Load]. [loading] is what shows while waiting: a skeleton for lists, a spinner by default. */
@Composable
fun <T> LoadView(load: Load<T>, onRetry: () -> Unit, loading: @Composable () -> Unit = { Spinner() }, content: @Composable (T) -> Unit) {
    when (load) {
        Load.Loading -> loading()
        is Load.Failed -> ErrorState(load.message, load.offline, onRetry)
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
            stringResource(R.string.integrity_warning, flags.joinToString(", ") { it.wire }),
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
        NfcAvailability.MISSING -> Centered { Text(stringResource(R.string.nfc_missing)) }
        NfcAvailability.DISABLED -> Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(stringResource(R.string.nfc_off))
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }) { Text(stringResource(R.string.nfc_open_settings)) }
            }
        }
    }
}
