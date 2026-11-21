package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.MeResponse
import vn.shelftap.app.data.NETWORK_ERROR_MESSAGE
import vn.shelftap.app.data.SessionEndedException
import vn.shelftap.app.data.SessionRepository
import vn.shelftap.app.data.userMessage
import java.io.IOException

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Loaded(val me: MeResponse) : ProfileUiState
    data class Failed(val message: String) : ProfileUiState
}

class ProfileViewModel(private val repo: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                _state.value = ProfileUiState.Loaded(repo.loadProfile())
            } catch (e: SessionEndedException) {
                // The session ended; the root screen is already showing sign-in.
            } catch (e: ApiException) {
                _state.value = ProfileUiState.Failed(e.userMessage())
            } catch (e: IOException) {
                _state.value = ProfileUiState.Failed(NETWORK_ERROR_MESSAGE)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { repo.signOut() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onChangePassword: () -> Unit,
    vm: ProfileViewModel = viewModel<ProfileViewModel>(
        factory = AppViewModelFactory { ProfileViewModel(it) },
    ),
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val s = state) {
                ProfileUiState.Loading -> CircularProgressIndicator()
                is ProfileUiState.Failed -> {
                    Text(
                        text = s.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = vm::load) { Text("Try again") }
                }
                is ProfileUiState.Loaded -> {
                    InfoRow(label = "Full name", value = s.me.fullName)
                    InfoRow(label = "Email", value = s.me.email)
                    InfoRow(label = "Member type", value = s.me.memberType)
                    InfoRow(label = "Status", value = s.me.status)
                }
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = onChangePassword,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Change password")
            }
            Button(
                onClick = vm::signOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
