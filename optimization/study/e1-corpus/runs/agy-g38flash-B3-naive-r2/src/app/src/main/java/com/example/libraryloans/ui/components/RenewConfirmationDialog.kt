package com.example.libraryloans.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.libraryloans.model.Loan
import java.time.format.DateTimeFormatter

@Composable
fun RenewConfirmationDialog(
    loan: Loan,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val currentDue = loan.dueDate.format(formatter)
    val newDue = loan.dueDate.plusDays(14).format(formatter)
    val renewalsLeft = loan.maxRenewals - loan.renewalCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Renew Loan",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "Renew \"${loan.book.title}\" for 14 more days?\n\n" +
                        "• Current due date: $currentDue\n" +
                        "• New due date: $newDue\n" +
                        "• Remaining renewals after this: ${renewalsLeft - 1}",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(loan.id) }
            ) {
                Text("Confirm Renewal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
