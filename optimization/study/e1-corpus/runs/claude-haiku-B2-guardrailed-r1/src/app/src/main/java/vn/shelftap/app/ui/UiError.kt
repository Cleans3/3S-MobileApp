package vn.shelftap.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import vn.shelftap.app.R
import vn.shelftap.app.data.api.ApiException
import vn.shelftap.app.data.api.SessionEndedException
import java.io.IOException

/** What went wrong, in terms the screens can show. Texts live in strings.xml. */
sealed interface UiError {
    data object FieldsRequired : UiError
    data object InvalidCredentials : UiError
    data object AccountLocked : UiError
    data class RateLimited(val retryAfterSeconds: Int?) : UiError
    data object WrongCurrentPassword : UiError
    data object PasswordsDoNotMatch : UiError
    data object SessionEnded : UiError
    data object Network : UiError
    data object Unknown : UiError
}

/** [duringPasswordChange] makes INVALID_CREDENTIALS mean "current password is wrong". */
internal fun Throwable.toUiError(duringPasswordChange: Boolean = false): UiError = when (this) {
    is SessionEndedException -> UiError.SessionEnded
    is ApiException -> when (code) {
        "INVALID_CREDENTIALS" ->
            if (duringPasswordChange) UiError.WrongCurrentPassword else UiError.InvalidCredentials
        "ACCOUNT_LOCKED" -> UiError.AccountLocked
        "RATE_LIMITED" -> UiError.RateLimited(retryAfterSeconds)
        "TOKEN_INVALID" -> UiError.SessionEnded
        else -> UiError.Unknown
    }
    is IOException -> UiError.Network
    else -> UiError.Unknown
}

@Composable
internal fun UiError.text(): String = when (this) {
    UiError.FieldsRequired -> stringResource(R.string.error_fields_required)
    UiError.InvalidCredentials -> stringResource(R.string.error_invalid_credentials)
    UiError.AccountLocked -> stringResource(R.string.error_account_locked)
    is UiError.RateLimited -> {
        val seconds = retryAfterSeconds
        if (seconds != null) {
            stringResource(R.string.error_rate_limited_seconds, seconds)
        } else {
            stringResource(R.string.error_rate_limited)
        }
    }
    UiError.WrongCurrentPassword -> stringResource(R.string.error_wrong_current_password)
    UiError.PasswordsDoNotMatch -> stringResource(R.string.error_passwords_do_not_match)
    UiError.SessionEnded -> stringResource(R.string.error_session_ended)
    UiError.Network -> stringResource(R.string.error_network)
    UiError.Unknown -> stringResource(R.string.error_unknown)
}
