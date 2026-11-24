package shelftap.android.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
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
import shelftap.android.R
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.api.Me
import shelftap.android.data.Repository
import shelftap.android.data.ThemeMode
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.shelfTapSwitchColors
import shelftap.android.security.BiometricLock
import shelftap.android.security.RefreshTokenVault
import shelftap.android.session.SessionManager
import shelftap.android.ui.theme.Spacing

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
        val error: UiText? = null,
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
            val me = try { Load.Ready(repo.me()) } catch (e: ApiException) { e.toFailed() }
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
                _state.update { it.copy(busy = false, error = if (e is ApiException) e.userMessage() else uiText(R.string.err_op_failed)) }
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
    val themeStore = LocalContainer.current.themeStore
    val themeMode by themeStore.mode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val bioTurnOn = stringResource(R.string.account_bio_turn_on)
    val bioConfirm = stringResource(R.string.account_bio_confirm)
    val cancelLabel = stringResource(R.string.action_cancel)

    LoadView(state.me, onRetry = vm::load) { me ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text(me.fullName, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.account_line, me.email, me.memberType.lowercase(), me.status.lowercase()))
            IntegrityBanner()
            ErrorText(state.error)
            if (state.stepUpFor != null) {
                StepUpDialog(defaultEmail = me.email, onDismiss = vm::stepUpDismissed, onDone = vm::stepUpDone)
            }
            HorizontalDivider()

            Text(stringResource(R.string.account_privacy), style = MaterialTheme.typography.titleMedium)
            SwitchRow(
                stringResource(R.string.account_history_keep),
                stringResource(R.string.account_history_keep_caption),
                checked = me.historyEnabled, enabled = !state.busy, onChange = vm::setHistoryEnabled,
            )
            SecondaryButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.account_history)) }
            HorizontalDivider()

            Text(stringResource(R.string.account_theme), style = MaterialTheme.typography.titleMedium)
            ThemeChoice(themeMode, onChange = { scope.launch { themeStore.set(it) } })
            HorizontalDivider()

            if (BuildConfig.DEBUG) {
                DebugEffectChoice(onChange = { name -> scope.launch { themeStore.setForcedEffect(name) } })
                HorizontalDivider()
            }

            Text(stringResource(R.string.account_security), style = MaterialTheme.typography.titleMedium)
            SwitchRow(
                stringResource(R.string.account_bio),
                stringResource(if (biometricAvailable) R.string.account_bio_caption else R.string.account_bio_none),
                checked = state.lockEnabled, enabled = !state.busy && (biometricAvailable || state.lockEnabled),
                onChange = { enable ->
                    vm.setBiometricLock(enable) { cipher ->
                        BiometricLock.authenticate(
                            activity, cipher,
                            title = if (enable) bioTurnOn else bioConfirm,
                            cancelLabel = cancelLabel,
                        )
                    }
                },
            )
            Text(stringResource(R.string.account_session_key, state.keyProtection), style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(R.string.account_auto_logout, BuildConfig.INACTIVITY_LIMIT_MS / 60_000),
                style = MaterialTheme.typography.bodySmall,
            )
            SecondaryButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.account_change_password)) }
            SecondaryButton(onClick = { scope.launch { vm.logout() } }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_logout)) }
        }
    }
}

/** Debug builds only: force one effect tier to compare A/B on the same phone (plan §6.3). Never in a release build. */
@Composable
private fun DebugEffectChoice(onChange: (String?) -> Unit) {
    val current = LocalEffectTier.current
    val forced by LocalContainer.current.themeStore.forcedEffect.collectAsStateWithLifecycle(null)
    Text(stringResource(R.string.debug_effect_title, current.name), style = MaterialTheme.typography.titleMedium)
    Column(Modifier.selectableGroup()) {
        listOf(null to R.string.debug_effect_auto, "FULL" to R.string.debug_effect_full, "MEDIUM" to R.string.debug_effect_medium, "LIGHT" to R.string.debug_effect_light)
            .forEach { (name, label) ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Spacing.touch).selectable(selected = forced == name, role = Role.RadioButton, onClick = { onChange(name) }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    RadioButton(selected = forced == name, onClick = null)
                    Text(stringResource(label))
                }
            }
    }
}

/** Light, dark or follow the phone. Radio rows, not a segmented bar: they keep their labels whole at large font sizes. */
@Composable
private fun ThemeChoice(selected: ThemeMode, onChange: (ThemeMode) -> Unit) {
    Column(Modifier.selectableGroup()) {
        listOf(
            ThemeMode.SYSTEM to R.string.theme_system,
            ThemeMode.LIGHT to R.string.theme_light,
            ThemeMode.DARK to R.string.theme_dark,
        ).forEach { (mode, label) ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = Spacing.touch).selectable(selected = selected == mode, role = Role.RadioButton, onClick = { onChange(mode) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                RadioButton(selected = selected == mode, onClick = null)
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, caption: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(caption, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled, colors = shelfTapSwitchColors())
    }
}
