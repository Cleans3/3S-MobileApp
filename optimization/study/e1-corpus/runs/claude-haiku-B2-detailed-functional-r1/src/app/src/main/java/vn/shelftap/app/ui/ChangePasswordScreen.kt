package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.NETWORK_ERROR_MESSAGE
import vn.shelftap.app.data.SessionEndedException
import vn.shelftap.app.data.SessionRepository
import vn.shelftap.app.data.userMessage
import java.io.IOException

data class ChangePasswordUiState(
    val current: String = "",
    val newPassword: String = "",
    val confirm: String = "",
    val saving: Boolean = false,
    val error: String? = null,
) {
    val mismatch: Boolean get() = confirm.isNotEmpty() && newPassword != confirm
}

class ChangePasswordViewModel(private val repo: SessionRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ChangePasswordUiState())
    val ui: StateFlow<ChangePasswordUiState> = _ui.asStateFlow()

    fun onCurrentChange(value: String) = _ui.update { it.copy(current = value, error = null) }

    fun onNewChange(value: String) = _ui.update { it.copy(newPassword = value, error = null) }

    fun onConfirmChange(value: String) = _ui.update { it.copy(confirm = value, error = null) }

    fun submit(onDone: () -> Unit) {
        val s = _ui.value
        if (s.saving) return
        if (s.current.isEmpty() || s.newPassword.isEmpty() || s.confirm.isEmpty()) {
            _ui.update { it.copy(error = "Fill in all three fields.") }
            return
        }
        if (s.newPassword != s.confirm) {
            _ui.update { it.copy(error = "The new passwords do not match.") }
            return
        }
        _ui.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repo.changePassword(s.current, s.newPassword)
                _ui.value = ChangePasswordUiState()
                onDone()
            } catch (e: SessionEndedException) {
                // The root screen is already moving to sign-in.
                _ui.update { it.copy(saving = false) }
            } catch (e: ApiException) {
                val message = if (e.code == "INVALID_CREDENTIALS") {
                    "Current password is incorrect."
                } else {
                    e.userMessage()
                }
                _ui.update { it.copy(saving = false, error = message) }
            } catch (e: IOException) {
                _ui.update { it.copy(saving = false, error = NETWORK_ERROR_MESSAGE) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { repo.signOut() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    forced: Boolean,
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: ChangePasswordViewModel = viewModel<ChangePasswordViewModel>(
        factory = AppViewModelFactory { ChangePasswordViewModel(it) },
    ),
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change password") },
                navigationIcon = {
                    if (!forced) {
                        TextButton(onClick = onBack) { Text("Back") }
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (forced) {
                Text(
                    text = "Your password must be changed before you can use ShelfTap.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            OutlinedTextField(
                value = ui.current,
                onValueChange = vm::onCurrentChange,
                label = { Text("Current password") },
                singleLine = true,
                enabled = !ui.saving,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.newPassword,
                onValueChange = vm::onNewChange,
                label = { Text("New password") },
                singleLine = true,
                enabled = !ui.saving,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.confirm,
                onValueChange = vm::onConfirmChange,
                label = { Text("Confirm new password") },
                singleLine = true,
                enabled = !ui.saving,
                isError = ui.mismatch,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { vm.submit(onDone) }),
                modifier = Modifier.fillMaxWidth(),
            )

            ui.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { vm.submit(onDone) },
                enabled = !ui.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (ui.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save new password")
                }
            }

            if (forced) {
                TextButton(
                    onClick = vm::signOut,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign out")
                }
            }
        }
    }
}
