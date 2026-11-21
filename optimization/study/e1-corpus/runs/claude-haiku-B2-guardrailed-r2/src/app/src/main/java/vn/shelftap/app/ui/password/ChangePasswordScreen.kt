package vn.shelftap.app.ui.password

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.shelftap.app.R
import vn.shelftap.app.ui.message

@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel,
    onFinished: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val required = viewModel.required

    // A required change cannot be left with the back gesture or button.
    BackHandler(enabled = required) { }

    LaunchedEffect(state.saved) {
        if (state.saved && !required) {
            onFinished()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.change_password_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            if (required) {
                Text(
                    text = stringResource(R.string.change_password_required_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            PasswordField(
                value = state.current,
                onValueChange = viewModel::onCurrentChange,
                label = stringResource(R.string.label_current_password),
                imeAction = ImeAction.Next,
                enabled = !state.submitting,
            )
            PasswordField(
                value = state.newPassword,
                onValueChange = viewModel::onNewChange,
                label = stringResource(R.string.label_new_password),
                imeAction = ImeAction.Next,
                enabled = !state.submitting,
            )
            PasswordField(
                value = state.confirm,
                onValueChange = viewModel::onConfirmChange,
                label = stringResource(R.string.label_confirm_password),
                imeAction = ImeAction.Done,
                enabled = !state.submitting,
                onDone = viewModel::submit,
            )

            state.error?.let { error ->
                Text(
                    text = error.message(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = viewModel::submit,
                enabled = !state.submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.save_password_button))
                }
            }

            if (required) {
                TextButton(
                    onClick = viewModel::signOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.sign_out_button))
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    enabled: Boolean,
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = Modifier.fillMaxWidth(),
    )
}
