package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiError
import vn.shelftap.app.data.Member
import vn.shelftap.app.data.Repository
import vn.shelftap.app.data.ShelfTapException

sealed interface AppScreen { data object SignIn : AppScreen; data object ChangePassword : AppScreen; data object Profile : AppScreen }
data class UiState(
    val screen: AppScreen = AppScreen.SignIn,
    val member: Member? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val retryAfter: String? = null
)

class ShelfTapViewModel(private val repository: Repository) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        if (repository.hasSession()) restore() 
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) { message("Enter your email and password"); return }
        work {
            val member = repository.login(email.trim(), password)
            if (repository.mustChangePassword() || member.mustChangePassword) {
                _state.value = UiState(AppScreen.ChangePassword, member = member)
            } else _state.value = UiState(AppScreen.Profile, member = member)
        }
    }

    private fun restore() = work {
        val member = repository.currentMember()
        _state.value = UiState(if (repository.mustChangePassword() || member.mustChangePassword) AppScreen.ChangePassword else AppScreen.Profile, member = member)
    }

    fun changePassword(current: String, new: String, confirm: String) {
        when {
            current.isBlank() || new.isBlank() -> message("Enter all password fields")
            new != confirm -> message("The new passwords do not match")
            new.length < 8 -> message("New password must be at least 8 characters")
            else -> work {
                repository.changePassword(current, new)
                val member = repository.currentMember()
                _state.value = UiState(AppScreen.Profile, member = member)
            }
        }
    }

    fun signOut() = work {
        repository.logout()
        _state.value = UiState(AppScreen.SignIn)
    }

    private fun message(text: String) { _state.value = _state.value.copy(error = text, retryAfter = null) }
    private fun work(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, retryAfter = null)
            try { block() } catch (e: Exception) {
                val api = (e as? ShelfTapException)?.error ?: ApiError(detail = e.message)
                if (api.code == "TOKEN_INVALID") repository.clearSession()
                val text = when (api.code) {
                    "INVALID_CREDENTIALS" -> "Email or password is incorrect"
                    "ACCOUNT_LOCKED" -> "This account is locked"
                    "RATE_LIMITED" -> "Too many attempts. Try again later"
                    "TOKEN_INVALID" -> "Your session has expired. Please sign in again"
                    "PASSWORD_CHANGE_REQUIRED" -> "You must change your password"
                    else -> api.detail ?: "Something went wrong"
                }
                val target = when (api.code) {
                    "TOKEN_INVALID" -> AppScreen.SignIn
                    "PASSWORD_CHANGE_REQUIRED" -> AppScreen.ChangePassword
                    else -> _state.value.screen
                }
                _state.value = UiState(target, _state.value.member, error = text, retryAfter = null)
            }
            if (_state.value.loading) _state.value = _state.value.copy(loading = false)
        }
    }
}

class ShelfTapViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ShelfTapViewModel(repository) as T
}
