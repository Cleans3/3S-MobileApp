package vn.shelftap.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScreenState {
    data object Loading : ScreenState
    data object SignIn : ScreenState
    data class Profile(val member: MemberProfile) : ScreenState
    data class ChangePassword(val member: MemberProfile) : ScreenState
}

class ShelfTapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MemberRepository(application)
    private val _screen = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val screen: StateFlow<ScreenState> = _screen.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { viewModelScope.launch { restore() } }

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _error.value = null
        try { showMember(repository.signIn(email.trim(), password)) } catch (e: Exception) { handleError(e) }
    }

    fun changePassword(current: String, new: String) = viewModelScope.launch {
        _error.value = null
        try { showMember(repository.changePassword(current, new)) } catch (e: Exception) { handleError(e) }
    }

    fun signOut() = viewModelScope.launch {
        repository.signOut()
        _error.value = null
        _screen.value = ScreenState.SignIn
    }

    fun clearError() { _error.value = null }

    private suspend fun restore() {
        try { repository.restore()?.let { showMember(it) } ?: run { _screen.value = ScreenState.SignIn } }
        catch (e: Exception) { _error.value = errorMessage(e); _screen.value = ScreenState.SignIn }
    }

    private fun showMember(member: MemberProfile) {
        _screen.value = if (member.mustChangePassword) ScreenState.ChangePassword(member) else ScreenState.Profile(member)
    }

    private fun handleError(error: Throwable) {
        if (error is SessionExpiredException) _screen.value = ScreenState.SignIn
        _error.value = errorMessage(error)
    }

    private fun errorMessage(error: Throwable): String = when (error) {
        is ApiException -> when (error.code) {
            "INVALID_CREDENTIALS" -> "INVALID_CREDENTIALS"
            "ACCOUNT_LOCKED" -> "ACCOUNT_LOCKED"
            "RATE_LIMITED" -> "RATE_LIMITED:${error.retryAfterSeconds ?: 0}"
            "PASSWORD_CHANGE_REQUIRED" -> "PASSWORD_CHANGE_REQUIRED"
            else -> "GENERIC"
        }
        is SessionExpiredException -> "SESSION_EXPIRED"
        else -> "NETWORK"
    }
}
