package vn.shelftap.app.ui.common

import android.content.Context
import vn.shelftap.app.R
import vn.shelftap.app.data.api.models.ApiErrorCodes
import vn.shelftap.app.data.api.models.ApiException

object ErrorFormatter {
    fun format(context: Context, throwable: Throwable): String {
        if (throwable is ApiException) {
            val error = throwable.error
            return when (error.code) {
                ApiErrorCodes.INVALID_CREDENTIALS ->
                    context.getString(R.string.error_invalid_credentials)

                ApiErrorCodes.ACCOUNT_LOCKED ->
                    context.getString(R.string.error_account_locked)

                ApiErrorCodes.RATE_LIMITED -> {
                    val seconds = throwable.retryAfterSeconds
                    if (seconds != null) {
                        context.getString(R.string.error_rate_limited_seconds, seconds)
                    } else {
                        context.getString(R.string.error_rate_limited)
                    }
                }

                ApiErrorCodes.TOKEN_INVALID ->
                    context.getString(R.string.error_token_invalid)

                ApiErrorCodes.PASSWORD_CHANGE_REQUIRED ->
                    context.getString(R.string.error_password_change_required)

                else ->
                    error.detail ?: error.title ?: context.getString(R.string.error_generic)
            }
        }
        return throwable.localizedMessage ?: context.getString(R.string.error_generic)
    }
}
