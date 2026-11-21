package vn.shelftap.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import vn.shelftap.app.R
import vn.shelftap.app.data.Failure

@Composable
fun failureMessage(failure: Failure): String = when (failure) {
    Failure.MissingFields -> stringResource(R.string.error_missing_fields)
    Failure.InvalidCredentials -> stringResource(R.string.error_invalid_credentials)
    Failure.CurrentPasswordWrong -> stringResource(R.string.error_current_password_wrong)
    Failure.AccountLocked -> stringResource(R.string.error_account_locked)
    is Failure.RateLimited -> failure.retryAfterSeconds
        ?.let { stringResource(R.string.error_rate_limited, it) }
        ?: stringResource(R.string.error_rate_limited_unknown)
    Failure.Network -> stringResource(R.string.error_network)
    Failure.SessionEnded -> stringResource(R.string.error_session_ended)
    Failure.Unknown -> stringResource(R.string.error_unknown)
}
