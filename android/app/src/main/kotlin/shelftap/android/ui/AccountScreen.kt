package shelftap.android.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import javax.crypto.Cipher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shelftap.android.BuildConfig
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.api.Me
import shelftap.android.data.Repository
import shelftap.android.security.BiometricLock
import shelftap.android.security.RefreshTokenVault
import shelftap.android.session.SessionManager

class AccountViewModel(
    private val repo: Repository,
    private val session: SessionManager,
    private val vault: RefreshTokenVault,
) : ViewModel() {
    data class UiState(
        val me: Load<Me> = Load.Loading,
        val lockEnabled: Boolean = false,
        val keyProtection: String = "",
        val busy: Boolean = false,
        val error: String? = null,
        /** The history setting the member asked for while on a card session; applied after the password login. */
        val stepUpFor: Boolean? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(me = Load.Loading) }
        viewModelScope.launch {
            val me = try { Load.Ready(repo.me()) } catch (e: ApiException) { Load.Failed(e.userMessage()) }
            _state.update { it.copy(me = me) }
            refreshSecurity()
        }
    }

    fun setHistoryEnabled(enabled: Boolean) = op {
        try {
            val me = repo.setHistoryEnabled(enabled)
            _state.update { it.copy(me = Load.Ready(me)) }
        } catch (e: ApiException.Http) {
            if (e.code != ErrorCode.STEP_UP_REQUIRED) throw e
            _state.update { it.copy(stepUpFor = enabled) } // card session: ask for the password, then apply
        }
    }

    fun stepUpDone() {
        val wanted = _state.value.stepUpFor
        _state.update { it.copy(stepUpFor = null) }
        if (wanted != null) setHistoryEnabled(wanted)
    }

    fun stepUpDismissed() = _state.update { it.copy(stepUpFor = null) }

    /** [authenticate] shows BiometricPrompt on the cipher and returns it authenticated, or null if cancelled. */
    fun setBiometricLock(enable: Boolean, authenticate: suspend (Cipher) -> Cipher?) = op {
        if (enable) authenticate(vault.beginEnableLock())?.let { vault.finishEnableLock(it) } else vault.disableLock()
        refreshSecurity()
    }

    fun logout() {
        viewModelScope.launch { session.logout() }
    }

    private suspend fun refreshSecurity() {
        _state.update { it.copy(lockEnabled = vault.isLockEnabled(), keyProtection = vault.keyProtection()) }
    }

    private fun op(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(busy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = if (e is ApiException) e.userMessage() else "That did not work.") }
                refreshSecurity()
            }
        }
    }
}

@Composable
fun AccountScreen(onHistory: () -> Unit, onChangePassword: () -> Unit) {
    val vm = appViewModel { AccountViewModel(it.repository, it.session, it.vault) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current as FragmentActivity
    val scope = rememberCoroutineScope()
    val biometricAvailable = BiometricLock.available(context)

    LoadView(state.me, onRetry = vm::load) { me ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(me.fullName, style = MaterialTheme.typography.headlineSmall)
            Text("${me.email} · ${me.memberType.lowercase()} · ${me.status.lowercase()}")
            IntegrityBanner()
            ErrorText(state.error)
            if (state.stepUpFor != null) {
                StepUpDialog(defaultEmail = me.email, onDismiss = vm::stepUpDismissed, onDone = vm::stepUpDone)
            }
            HorizontalDivider()

            Text("Privacy", style = MaterialTheme.typography.titleMedium)
            SwitchRow(
                "Keep my reading history",
                "Off: finished loans are not recorded in your history (Decree 13/2023).",
                checked = me.historyEnabled, enabled = !state.busy, onChange = vm::setHistoryEnabled,
            )
            OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("Reading history") }
            HorizontalDivider()

            Text("Security", style = MaterialTheme.typography.titleMedium)
            SwitchRow(
                "Biometric lock",
                if (biometricAvailable) "Fingerprint or face is needed to open the app after it was in the background."
                else "No strong biometric is enrolled on this phone.",
                checked = state.lockEnabled, enabled = !state.busy && (biometricAvailable || state.lockEnabled),
                onChange = { enable ->
                    vm.setBiometricLock(enable) { cipher ->
                        BiometricLock.authenticate(
                            activity, cipher,
                            title = if (enable) "Turn on biometric lock" else "Confirm",
                            cancelLabel = "Cancel",
                        )
                    }
                },
            )
            Text("Session key: ${state.keyProtection}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Auto-logout after ${BuildConfig.INACTIVITY_LIMIT_MS / 60_000} min idle",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) { Text("Change password") }
            OutlinedButton(onClick = { scope.launch { vm.logout() } }, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
        }
    }
}

@Composable
private fun SwitchRow(title: String, caption: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(caption, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
