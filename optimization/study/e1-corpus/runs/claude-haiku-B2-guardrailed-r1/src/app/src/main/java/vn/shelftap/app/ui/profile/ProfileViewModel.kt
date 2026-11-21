package vn.shelftap.app.ui.profile

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
import vn.shelftap.app.data.api.MeResponse
import vn.shelftap.app.data.session.SessionRepository
import vn.shelftap.app.ui.UiError
import vn.shelftap.app.ui.toUiError

internal class ProfileViewModel(private val session: SessionRepository) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val profile: MeResponse? = null,
        val error: UiError? = null,
        val isSigningOut: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val me = session.loadProfile()
                _state.update { it.copy(isLoading = false, profile = me) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.toUiError()) }
            }
        }
    }

    fun signOut() {
        if (_state.value.isSigningOut) return
        _state.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            session.signOut()
        }
    }

    companion object {
        fun factory(session: SessionRepository) = viewModelFactory {
            initializer { ProfileViewModel(session) }
        }
    }
}
