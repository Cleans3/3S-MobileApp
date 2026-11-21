package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.R
import vn.shelftap.app.data.AuthRepository
import vn.shelftap.app.data.Failure
import vn.shelftap.app.data.toFailure

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val submitting: Boolean = false,
    val failure: Failure? = null,
) {
    val passwordsMatch: Boolean get() = newPassword == confirmPassword
    val canSubmit: Boolean
        get() = !submitting &&
            currentPassword.isNotEmpty() &&
            newPassword.isNotEmpty() &&
            passwordsMatch
}

class ChangePasswordViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onCurrentChange(value: String) = _state.update { it.copy(currentPassword = value, failure = null) }

    fun onNewChange(value: String) = _state.update { it.copy(newPassword = value, failure = null) }

    fun onConfirmChange(value: String) = _state.update { it.copy(confirmPassword = value, failure = null) }

    /** [onSuccess] runs once, after the server accepted the new password. */
    fun submit(onSuccess: () -> Unit) {
        val snapshot = _state.value
        if (!snapshot.canSubmit) return
        _state.update { it.copy(submitting = true, failure = null) }
        viewModelScope.launch {
            try {
                auth.changePassword(snapshot.currentPassword, snapshot.newPassword)
                _state.update {
                    it.copy(
                        submitting = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                    )
                }
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(submitting = false, failure = e.toFailure(duringPasswordChange = true))
                }
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel,
    forced: Boolean,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val focus = LocalFocusManager.current
    val mismatchText: @Composable () -> Unit = {
        Text(stringResource(R.string.passwords_do_not_match))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.change_password_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (forced) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.must_change_password_notice),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.currentPassword,
            onValueChange = viewModel::onCurrentChange,
            label = { Text(stringResource(R.string.current_password_label)) },
            singleLine = true,
            enabled = !state.submitting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focus.moveFocus(FocusDirection.Down) },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.newPassword,
            onValueChange = viewModel::onNewChange,
            label = { Text(stringResource(R.string.new_password_label)) },
            singleLine = true,
            enabled = !state.submitting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focus.moveFocus(FocusDirection.Down) },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        val mismatch = state.confirmPassword.isNotEmpty() && !state.passwordsMatch
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmChange,
            label = { Text(stringResource(R.string.confirm_password_label)) },
            singleLine = true,
            enabled = !state.submitting,
            isError = mismatch,
            supportingText = if (mismatch) mismatchText else null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focus.clearFocus()
                    viewModel.submit(onDone)
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        state.failure?.let { failure ->
            Text(
                text = failureMessage(failure),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                focus.clearFocus()
                viewModel.submit(onDone)
            },
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.save_password_button))
        }

        if (!forced) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDone,
                enabled = !state.submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    }
}
