package vn.shelftap.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

private const val MIN_PASSWORD_LENGTH = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    forced: Boolean,
    onSubmit: (currentPassword: String, newPassword: String, onComplete: (String?) -> Unit) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    // A forced change has no way back; the system back button just closes the app.
    BackHandler(enabled = !forced) { onBack() }

    val submit: () -> Unit = {
        val problem = when {
            current.isEmpty() -> "Enter your current password."
            newPassword.length < MIN_PASSWORD_LENGTH ->
                "The new password must be at least $MIN_PASSWORD_LENGTH characters."
            newPassword == current -> "The new password must be different from the current one."
            newPassword != confirm -> "The new passwords do not match."
            else -> null
        }
        error = problem
        if (problem == null && !busy) {
            busy = true
            onSubmit(current, newPassword) { result ->
                busy = false
                error = result
                if (result == null) {
                    current = ""
                    newPassword = ""
                    confirm = ""
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change password") },
                navigationIcon = {
                    if (!forced) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (forced) {
                Text(
                    text = "You must change your temporary password before you can continue.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            PasswordField(
                label = "Current password",
                value = current,
                onValueChange = { current = it },
                imeAction = ImeAction.Next,
            )
            PasswordField(
                label = "New password",
                value = newPassword,
                onValueChange = { newPassword = it },
                imeAction = ImeAction.Next,
            )
            PasswordField(
                label = "Confirm new password",
                value = confirm,
                onValueChange = { confirm = it },
                imeAction = ImeAction.Done,
                onDone = submit,
            )

            error?.let { ErrorText(it) }

            Button(
                onClick = submit,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(2.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Change password")
                }
            }

            if (forced) {
                TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign out")
                }
            }
        }
    }
}
