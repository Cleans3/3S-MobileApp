package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
)

class SignInViewModel(private val repo: SessionRepository) : ViewModel() {
    private val _ui = MutableStateFlow(SignInUiState())
    val ui: StateFlow<SignInUiState> = _ui.asStateFlow()

    fun onEmailChange(value: String) = _ui.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _ui.update { it.copy(password = value, error = null) }

    fun submit() {
        val s = _ui.value
        if (s.submitting) return
        if (s.email.isBlank() || s.password.isEmpty()) {
            _ui.update { it.copy(error = "Enter your email and password.") }
            return
        }
        _ui.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                repo.signIn(s.email.trim(), s.password)
                // On success the root screen swaps this screen out; clear the password first.
                _ui.update { it.copy(password = "", submitting = false) }
            } catch (e: ApiException) {
                _ui.update { it.copy(submitting = false, error = e.userMessage()) }
            } catch (e: SessionEndedException) {
                _ui.update { it.copy(submitting = false) }
            } catch (e: IOException) {
                _ui.update { it.copy(submitting = false, error = NETWORK_ERROR_MESSAGE) }
            }
        }
    }
}

@Composable
fun SignInScreen(
    vm: SignInViewModel = viewModel<SignInViewModel>(
        factory = AppViewModelFactory { SignInViewModel(it) },
    ),
) {
    val ui by vm.ui.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "ShelfTap",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Sign in to your member account",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer12()

        OutlinedTextField(
            value = ui.email,
            onValueChange = vm::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            enabled = !ui.submitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer12()
        OutlinedTextField(
            value = ui.password,
            onValueChange = vm::onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            enabled = !ui.submitting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { vm.submit() }),
            modifier = Modifier.fillMaxWidth(),
        )

        ui.error?.let { message ->
            Spacer12()
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer12()
        Button(
            onClick = vm::submit,
            enabled = !ui.submitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (ui.submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Sign in")
            }
        }
    }
}

@Composable
private fun Spacer12() {
    Spacer(Modifier.size(12.dp))
}
