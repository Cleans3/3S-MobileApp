package vn.shelftap.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import vn.shelftap.app.R

@Composable
fun UiError.message(): String = when (this) {
    UiError.MissingFields -> stringResource(R.string.error_missing_fields)
    UiError.InvalidCredentials -> stringResource(R.string.error_invalid_credentials)
    UiError.AccountLocked -> stringResource(R.string.error_account_locked)
    is UiError.RateLimited -> stringResource(R.string.error_rate_limited, seconds)
    UiError.SessionEnded -> stringResource(R.string.error_session_ended)
    UiError.CurrentPasswordWrong -> stringResource(R.string.error_current_password_wrong)
    UiError.PasswordMismatch -> stringResource(R.string.error_passwords_do_not_match)
    UiError.Network -> stringResource(R.string.error_network)
    UiError.Generic -> stringResource(R.string.error_generic)
}
