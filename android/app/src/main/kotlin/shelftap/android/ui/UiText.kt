package shelftap.android.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

/**
 * A message that is resolved against the current locale when it is shown, not when it is created. ViewModels hold these
 * instead of strings, so a message made in one language still reads right after the language or font setting changes,
 * and ViewModels need no [Context]. [args] fill the `%1$s`-style placeholders in the resource.
 */
@Immutable
data class UiText(@StringRes val id: Int, val args: List<Any> = emptyList()) {
    @Composable
    fun asString(): String = stringResource(id, *args.toTypedArray())

    fun asString(context: Context): String = context.getString(id, *args.toTypedArray())
}

fun uiText(@StringRes id: Int, vararg args: Any) = UiText(id, args.toList())
