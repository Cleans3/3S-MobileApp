package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.AppError
import vn.shelftap.app.data.AuthRepository
import vn.shelftap.app.data.ErrorCodes
import vn.shelftap.app.data.RemoteException
import vn.shelftap.app.data.SessionEndedException
import vn.shelftap.app.data.toAppError

class AppViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (repo.hasSession()) {
                loadProfile()
            } else {
                _state.update { it.copy(stage = Stage.SignedOut) }
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isEmpty()) {
            _state.update { it.copy(signInError = AppError(ErrorCodes.FIELDS_REQUIRED)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(signInLoading = true, signInError = null) }
            try {
                val mustChangePassword = repo.signIn(email.trim(), password)
                _state.update { it.copy(signInLoading = false) }
                if (mustChangePassword) {
                    enterForcedPasswordChange()
                } else {
                    loadProfile()
                }
            } catch (e: RemoteException) {
                _state.update { it.copy(signInLoading = false, signInError = e.toAppError()) }
            }
        }
    }

    fun retryProfile() {
        viewModelScope.launch { loadProfile() }
    }

    fun openChangePassword() {
        _state.update {
            it.copy(stage = Stage.ChangePassword, passwordForced = false, passwordError = null)
        }
    }

    fun cancelChangePassword() {
        _state.update { it.copy(stage = Stage.Profile, passwordError = null) }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            _state.update { it.copy(passwordError = AppError(ErrorCodes.FIELDS_REQUIRED)) }
            return
        }
        if (newPassword != confirmPassword) {
            _state.update { it.copy(passwordError = AppError(ErrorCodes.PASSWORD_MISMATCH)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(passwordLoading = true, passwordError = null) }
            try {
                repo.changePassword(currentPassword, newPassword)
                _state.update { it.copy(passwordLoading = false, passwordForced = false) }
                loadProfile()
            } catch (e: SessionEndedException) {
                sessionEnded()
            } catch (e: RemoteException) {
                _state.update { it.copy(passwordLoading = false, passwordError = e.toAppError()) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repo.signOut()
            _state.value = AppUiState(stage = Stage.SignedOut)
        }
    }

    private suspend fun loadProfile() {
        _state.update { it.copy(stage = Stage.Profile, profileLoading = true, profileError = null) }
        try {
            val member = repo.fetchMe()
            if (member.mustChangePassword) {
                enterForcedPasswordChange()
            } else {
                _state.update { it.copy(member = member, profileLoading = false) }
            }
        } catch (e: SessionEndedException) {
            sessionEnded()
        } catch (e: RemoteException) {
            if (e is ApiException && e.code == ErrorCodes.PASSWORD_CHANGE_REQUIRED) {
                enterForcedPasswordChange()
            } else {
                _state.update { it.copy(profileLoading = false, profileError = e.toAppError()) }
            }
        }
    }

    private fun enterForcedPasswordChange() {
        _state.update {
            it.copy(
                stage = Stage.ChangePassword,
                passwordForced = true,
                passwordLoading = false,
                passwordError = null,
                profileLoading = false,
            )
        }
    }

    private fun sessionEnded() {
        _state.value = AppUiState(
            stage = Stage.SignedOut,
            signInError = AppError(ErrorCodes.TOKEN_INVALID),
        )
    }

    companion object {
        fun factory(repo: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(repo) as T
            }
    }
}
