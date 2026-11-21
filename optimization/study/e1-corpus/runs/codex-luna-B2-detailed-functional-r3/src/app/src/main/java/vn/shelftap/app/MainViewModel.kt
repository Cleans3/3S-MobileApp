package vn.shelftap.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiFailure
import vn.shelftap.app.data.Member
import vn.shelftap.app.data.SessionStore
import vn.shelftap.app.data.ShelfTapApi

sealed interface AppScreen { data object SignIn : AppScreen; data object Profile : AppScreen; data object ChangePassword : AppScreen }
data class UiState(val screen: AppScreen = AppScreen.SignIn, val member: Member? = null, val busy: Boolean = false, val error: String? = null, val retryAfter: Long? = null)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SessionStore(app)
    private val api = ShelfTapApi(store)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { if (store.session() != null) { if (store.session()!!.mustChangePassword) _state.value = UiState(AppScreen.ChangePassword) else loadProfile() } }
    fun signIn(email: String, password: String) { if (email.isBlank() || password.isBlank()) { error("Enter your email and password."); return }; run { val action = suspend { val session = api.login(email.trim(), password); store.save(session); if (session.mustChangePassword) _state.value = UiState(AppScreen.ChangePassword) else loadProfile() }; perform(action) } }
    fun changePassword(current: String, newPassword: String, confirm: String) { when { current.isBlank() || newPassword.isBlank() -> error("Complete all password fields."); newPassword != confirm -> error("The new passwords do not match."); newPassword.length < 8 -> error("The new password must be at least 8 characters."); else -> perform { api.changePassword(current, newPassword); loadProfile() } } }
    fun signOut() { perform { api.logout(); _state.value = UiState(AppScreen.SignIn) } }
    fun clearError() { _state.value = _state.value.copy(error = null, retryAfter = null) }
    private fun loadProfile() { perform { _state.value = _state.value.copy(busy = true); _state.value = UiState(AppScreen.Profile, api.me()) } }
    private fun perform(action: suspend () -> Unit) { viewModelScope.launch(Dispatchers.IO) { _state.value = _state.value.copy(busy = true, error = null); try { action() } catch (e: ApiFailure) { if (e.code == "TOKEN_INVALID" || e.code == "PASSWORD_CHANGE_REQUIRED") { store.clear(); _state.value = UiState(AppScreen.SignIn, error = if (e.code == "PASSWORD_CHANGE_REQUIRED") "Please change your password to continue." else "Your session has expired. Please sign in again.") } else _state.value = _state.value.copy(busy = false, error = message(e), retryAfter = e.retryAfterSeconds) } catch (_: Exception) { _state.value = _state.value.copy(busy = false, error = "Something went wrong. Please try again.") } } }
    private fun error(text: String) { _state.value = _state.value.copy(error = text) }
    private fun message(e: ApiFailure) = when (e.code) { "INVALID_CREDENTIALS" -> "Incorrect email or password."; "ACCOUNT_LOCKED" -> "This account is locked. Contact the library."; "RATE_LIMITED" -> "Too many attempts. Try again later."; else -> e.message ?: "Something went wrong." }
}
