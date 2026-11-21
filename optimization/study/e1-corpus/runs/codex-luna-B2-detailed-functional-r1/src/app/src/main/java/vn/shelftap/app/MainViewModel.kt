package vn.shelftap.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScreenState { data object Loading : ScreenState; data object SignedOut : ScreenState; data class SignedIn(val member: Member, val changingPassword: Boolean = false) : ScreenState }
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SessionStore(app); private val api = ApiClient(store)
    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading); val state = _state.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()
    init { if (store.tokens() == null) _state.value = ScreenState.SignedOut else loadMember() }
    private fun loadMember() = viewModelScope.launch {
        runCatching { api.authenticated { api.me() } }.onSuccess { member ->
            val change = store.tokens()?.mustChangePassword == true || member.mustChangePassword
            _state.value = ScreenState.SignedIn(member, change)
        }.onFailure { signOutLocal(it) }
    }
    fun login(email: String, password: String) = viewModelScope.launch {
        _message.value = null
        runCatching { api.login(email.trim(), password) }.onSuccess { t -> store.save(t); loadMember() }.onFailure { _message.value = errorText(it) }
    }
    fun changePassword(current: String, new: String) = viewModelScope.launch {
        _message.value = null
        runCatching { api.authenticated { api.password(current, new) } }.onSuccess { store.setMustChange(false); loadMember() }.onFailure { _message.value = errorText(it) }
    }
    fun signOut() = viewModelScope.launch { api.logout(); store.clear(); _state.value = ScreenState.SignedOut }
    private fun signOutLocal(t: Throwable) { if (t is ApiException && t.code == "PASSWORD_CHANGE_REQUIRED") return else { store.clear(); _state.value = ScreenState.SignedOut; _message.value = errorText(t) } }
    private fun errorText(t: Throwable): String = when ((t as? ApiException)?.code) { "INVALID_CREDENTIALS" -> "Email or password is incorrect."; "ACCOUNT_LOCKED" -> "Your account is locked. Contact the library."; "RATE_LIMITED" -> "Too many attempts. Try again in ${(t as ApiException).retryAfter ?: 60} seconds."; "PASSWORD_CHANGE_REQUIRED" -> "Please change your password to continue."; "TOKEN_INVALID" -> "Your session has expired. Please sign in again."; else -> t.message ?: "Something went wrong. Please try again." }
}
