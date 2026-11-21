package vn.shelftap.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import vn.shelftap.app.R
import vn.shelftap.app.data.AppError
import vn.shelftap.app.data.ErrorCodes

/** Maps an API error code to a localised message. Unknown codes get a generic message. */
@Composable
fun AppError.message(): String = when (code) {
    ErrorCodes.INVALID_CREDENTIALS -> stringResource(R.string.error_invalid_credentials)
    ErrorCodes.ACCOUNT_LOCKED -> stringResource(R.string.error_account_locked)
    ErrorCodes.RATE_LIMITED -> retryAfterSeconds?.let { stringResource(R.string.error_rate_limited_seconds, it) } ?: stringResource(R.string.error_rate_limited)
    ErrorCodes.TOKEN_INVALID -> stringResource(R.string.error_session_ended)
    ErrorCodes.PASSWORD_CHANGE_REQUIRED -> stringResource(R.string.error_password_change_required)
    ErrorCodes.NETWORK -> stringResource(R.string.error_network)
    ErrorCodes.FIELDS_REQUIRED -> stringResource(R.string.error_fields_required)
    ErrorCodes.PASSWORD_MISMATCH -> stringResource(R.string.error_password_mismatch)
    else -> stringResource(R.string.error_generic)
}
