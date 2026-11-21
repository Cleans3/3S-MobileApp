package vn.shelftap.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import vn.shelftap.app.R
import vn.shelftap.app.data.ApiException
import java.io.IOException

enum class ErrorKind {
    MISSING_FIELDS,
    PASSWORDS_DO_NOT_MATCH,
    INVALID_CREDENTIALS,
    CURRENT_PASSWORD_WRONG,
    ACCOUNT_LOCKED,
    RATE_LIMITED,
    NETWORK,
    UNKNOWN,
}

/** A user-facing error. Text is resolved from string resources in the UI layer. */
data class UiError(
    val kind: ErrorKind,
    val retryAfterSeconds: Int? = null,
)

/** Maps an API or network failure to a user-facing error. Branches on the API error code. */
fun Throwable.toUiError(forPasswordChange: Boolean = false): UiError = when (this) {
    is ApiException -> when (code) {
        "INVALID_CREDENTIALS" -> UiError(
            if (forPasswordChange) ErrorKind.CURRENT_PASSWORD_WRONG else ErrorKind.INVALID_CREDENTIALS,
        )
        "ACCOUNT_LOCKED" -> UiError(ErrorKind.ACCOUNT_LOCKED)
        "RATE_LIMITED" -> UiError(ErrorKind.RATE_LIMITED, retryAfterSeconds)
        else -> UiError(ErrorKind.UNKNOWN)
    }
    is IOException -> UiError(ErrorKind.NETWORK)
    else -> UiError(ErrorKind.UNKNOWN)
}

@Composable
fun UiError.message(): String = when (kind) {
    ErrorKind.MISSING_FIELDS -> stringResource(R.string.error_missing_fields)
    ErrorKind.PASSWORDS_DO_NOT_MATCH -> stringResource(R.string.error_passwords_do_not_match)
    ErrorKind.INVALID_CREDENTIALS -> stringResource(R.string.error_invalid_credentials)
    ErrorKind.CURRENT_PASSWORD_WRONG -> stringResource(R.string.error_current_password_wrong)
    ErrorKind.ACCOUNT_LOCKED -> stringResource(R.string.error_account_locked)
    ErrorKind.RATE_LIMITED -> {
        val seconds = retryAfterSeconds
        if (seconds != null) {
            stringResource(R.string.error_rate_limited_seconds, seconds)
        } else {
            stringResource(R.string.error_rate_limited)
        }
    }
    ErrorKind.NETWORK -> stringResource(R.string.error_network)
    ErrorKind.UNKNOWN -> stringResource(R.string.error_unknown)
}
