package vn.shelftap.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.repository.HistoryRepository
import vn.shelftap.app.data.repository.MemberRepository

class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Load member profile to check historyEnabled state
            var isSavedDataCombined = false
            val profileResult = memberRepository.getProfile()
            profileResult.onSuccess { (profile, isSaved) ->
                isSavedDataCombined = isSavedDataCombined || isSaved
                _uiState.update { it.copy(historyEnabled = profile.historyEnabled) }
            }

            // 2. Load history items
            val historyResult = historyRepository.getHistory()
            historyResult.fold(
                onSuccess = { (items, isSaved) ->
                    isSavedDataCombined = isSavedDataCombined || isSaved
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            historyItems = items,
                            isSavedData = isSavedDataCombined,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSavedData = isSavedDataCombined,
                            errorMessage = error.localizedMessage ?: "Failed to load history"
                        )
                    }
                }
            )
        }
    }

    fun onHistorySwitchToggled(checked: Boolean) {
        if (!checked) {
            // Turning it off requires confirmation first
            _uiState.update { it.copy(showTurnOffConfirmation = true) }
        } else {
            // Turning it on can proceed directly
            updatePrivacy(true)
        }
    }

    fun dismissTurnOffConfirmation() {
        _uiState.update { it.copy(showTurnOffConfirmation = false) }
    }

    fun confirmTurnOffHistory() {
        _uiState.update { it.copy(showTurnOffConfirmation = false) }
        updatePrivacy(false)
    }

    private fun updatePrivacy(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPrivacy = true) }
            val result = memberRepository.updatePrivacy(enabled)
            result.fold(
                onSuccess = { updatedProfile ->
                    _uiState.update {
                        it.copy(
                            isUpdatingPrivacy = false,
                            historyEnabled = updatedProfile.historyEnabled
                        )
                    }
                    // Refresh history items to reflect new setting
                    loadData()
                },
                onFailure = {
                    _uiState.update { state ->
                        state.copy(
                            isUpdatingPrivacy = false,
                            historyEnabled = enabled // Optimistic fallback to local
                        )
                    }
                }
            )
        }
    }

    companion object {
        fun provideFactory(
            historyRepository: HistoryRepository,
            memberRepository: MemberRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(historyRepository, memberRepository) as T
            }
        }
    }
}
