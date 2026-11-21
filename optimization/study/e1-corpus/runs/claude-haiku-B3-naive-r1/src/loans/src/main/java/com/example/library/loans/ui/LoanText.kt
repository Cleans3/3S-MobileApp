package com.example.library.loans.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.example.library.loans.R
import com.example.library.loans.domain.RejectReason
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

internal fun LocalDate.display(): String = format(dateFormat)

@Composable
internal fun messageText(message: LoanMessage): String = when (message) {
    is LoanMessage.Renewed -> stringResource(
        R.string.renewed_message,
        message.title,
        message.newDue.display(),
    )
    is LoanMessage.Failed -> stringResource(
        when (message.reason) {
            RejectReason.NOT_FOUND -> R.string.reject_not_found
            RejectReason.ALREADY_RETURNED -> R.string.reject_returned
            RejectReason.OVERDUE -> R.string.reject_overdue
            RejectReason.RENEWAL_LIMIT_REACHED -> R.string.reject_limit
        },
    )
}

/** Shows [message] in the snackbar and then clears it via [onShown]. */
@Composable
internal fun ShowLoanMessage(
    message: LoanMessage?,
    snackbar: SnackbarHostState,
    onShown: () -> Unit,
) {
    val text = message?.let { messageText(it) }
    LaunchedEffect(message) {
        if (text != null) {
            snackbar.showSnackbar(text)
            onShown()
        }
    }
}
