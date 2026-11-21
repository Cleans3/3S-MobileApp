package vn.shelftap.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.AccountRepository
import vn.shelftap.app.data.Member
import vn.shelftap.app.data.SessionEndedException
import vn.shelftap.app.ui.UiError
import vn.shelftap.app.ui.toUiError

data class ProfileUiState(
    val loading: Boolean = true,
    val member: Member? = null,
    val error: UiError? = null,
)

class ProfileViewModel(private val accounts: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val member = accounts.loadProfile()
                _state.update { it.copy(loading = false, member = member) }
            } catch (e: SessionEndedException) {
                // The root navigation has already moved the member to sign-in.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUiError()) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            accounts.signOut()
        }
    }

    companion object {
        fun factory(accounts: AccountRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProfileViewModel(accounts) }
        }
    }
}
