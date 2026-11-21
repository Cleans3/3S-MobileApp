package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import vn.shelftap.app.R

@Composable
fun ChangePasswordScreen(
    state: AppUiState,
    onSubmit: (String, String, String) -> Unit,
    onCancel: () -> Unit,
    onSignOut: () -> Unit,
) {
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.change_password_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            if (state.passwordForced) {
                Text(
                    text = stringResource(R.string.change_password_forced_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            PasswordField(
                label = stringResource(R.string.current_password_label),
                value = currentPassword,
                onValueChange = { currentPassword = it },
                enabled = !state.passwordLoading,
                imeAction = ImeAction.Next,
            )
            PasswordField(
                label = stringResource(R.string.new_password_label),
                value = newPassword,
                onValueChange = { newPassword = it },
                enabled = !state.passwordLoading,
                imeAction = ImeAction.Next,
            )
            PasswordField(
                label = stringResource(R.string.confirm_password_label),
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                enabled = !state.passwordLoading,
                imeAction = ImeAction.Done,
                onDone = { onSubmit(currentPassword, newPassword, confirmPassword) },
            )

            state.passwordError?.let { err ->
                Text(
                    text = err.message(),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = { onSubmit(currentPassword, newPassword, confirmPassword) },
                enabled = !state.passwordLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                if (state.passwordLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.save_password_button))
                }
            }

            // A forced change has no way back into the app, so offer sign-out instead of cancel.
            if (state.passwordForced) {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.sign_out_button))
                }
            } else {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        }
    }
}
