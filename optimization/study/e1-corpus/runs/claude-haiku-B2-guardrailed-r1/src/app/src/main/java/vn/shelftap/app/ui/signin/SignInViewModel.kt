package vn.shelftap.app.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.session.SessionRepository
import vn.shelftap.app.ui.UiError
import vn.shelftap.app.ui.toUiError

internal class SignInViewModel(private val session: SessionRepository) : ViewModel() {

    data class UiState(
        val isSubmitting: Boolean = false,
        val error: UiError? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun signIn(email: String, password: String) {
        if (_state.value.isSubmitting) return
        if (email.isBlank() || password.isEmpty()) {
            _state.update { it.copy(error = UiError.FieldsRequired) }
            return
        }
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                session.signIn(email.trim(), password)
                // On success the root navigation leaves this screen.
                _state.update { it.copy(isSubmitting = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false, error = e.toUiError()) }
            }
        }
    }

    companion object {
        fun factory(session: SessionRepository) = viewModelFactory {
            initializer { SignInViewModel(session) }
        }
    }
}
