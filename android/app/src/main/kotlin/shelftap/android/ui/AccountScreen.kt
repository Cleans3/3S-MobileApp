package shelftap.android.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.SecureFlagPolicy
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
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.AccountSize
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.bannerBackground
import shelftap.android.ui.theme.dangerSolid
import shelftap.android.ui.theme.pressScale
import shelftap.android.ui.theme.shelfTap
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    LoadView(state.me, onRetry = vm::load) { me ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = GlassSize.page, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            ProfileCard(me.fullName, me.email)
            IntegrityBanner()
            ErrorText(state.error)
            if (state.stepUpFor != null) {
                StepUpDialog(defaultEmail = me.email, onDismiss = vm::stepUpDismissed, onDone = vm::stepUpDone)
            }

            SectionLabel(stringResource(R.string.account_privacy))
            GlassGroup {
                SwitchItem(
                    stringResource(R.string.account_history_keep), stringResource(R.string.account_history_keep_caption),
                    checked = me.historyEnabled, enabled = !state.busy, onChange = vm::setHistoryEnabled,
                )
                GroupDivider()
                LinkItem(stringResource(R.string.account_history), onClick = onHistory)
            }

            SectionLabel(stringResource(R.string.account_security))
            GlassGroup {
                SwitchItem(
                    stringResource(R.string.account_bio), stringResource(if (biometricAvailable) R.string.account_bio_caption else R.string.account_bio_none),
                    checked = state.lockEnabled, enabled = !state.busy && (biometricAvailable || state.lockEnabled),
                    onChange = { enable ->
                        vm.setBiometricLock(enable) { cipher ->
                            BiometricLock.authenticate(activity, cipher, title = if (enable) bioTurnOn else bioConfirm, cancelLabel = cancelLabel)
                        }
                    },
                )
                GroupDivider()
                LinkItem(stringResource(R.string.account_change_password), onClick = onChangePassword)
                GroupDivider()
                InfoItem(stringResource(R.string.account_auto_logout, BuildConfig.INACTIVITY_LIMIT_MS / 60_000))
                InfoItem(stringResource(R.string.account_session_key, state.keyProtection))
            }

            SectionLabel(stringResource(R.string.account_theme))
            ThemeSegments(themeMode, onChange = { scope.launch { themeStore.set(it) } })

            if (BuildConfig.DEBUG) {
                GlassGroup { DebugEffectChoice(onChange = { name -> scope.launch { themeStore.setForcedEffect(name) } }) }
            }

            DangerOutlineButton(stringResource(R.string.action_logout), onClick = { confirmLogout = true })
        }
    }

    if (confirmLogout) {
        // A bottom sheet in its own window: FLAG_SECURE is set on it explicitly, like the other dialogs.
        ModalBottomSheet(
            onDismissRequest = { confirmLogout = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
            containerColor = MaterialTheme.shelfTap.dialog,
            properties = ModalBottomSheetProperties(securePolicy = SecureFlagPolicy.SecureOn),
        ) {
            Column(Modifier.padding(horizontal = Spacing.xl).padding(bottom = Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(stringResource(R.string.logout_confirm_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.logout_confirm_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.shelfTap.ink2)
                Box(
                    Modifier.fillMaxWidth().height(AccountSize.action).clip(CircleShape).background(dangerSolid)
                        .clickable(role = Role.Button) { confirmLogout = false; scope.launch { vm.logout() } },
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.action_logout), style = MaterialTheme.typography.titleMedium, color = Color.White) }
                Box(
                    Modifier.fillMaxWidth().height(AccountSize.segment).clip(CircleShape).clickable(role = Role.Button) { confirmLogout = false },
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.logout_stay), style = MaterialTheme.typography.titleSmall) }
            }
        }
    }
}

/** The member's card: the banner colour, a round glass avatar, the name and the email. */
@Composable
private fun ProfileCard(name: String, email: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.banner)).background(bannerBackground).padding(Spacing.l),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        GlassSurface(Modifier.size(AccountSize.avatar), shape = CircleShape, fill = AVATAR_FILL) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LucideIcon(LucideIcons.User, tint = Color.White, size = Spacing.xl) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs / 2)) {
            Text(name, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(email, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = ON_BANNER_SOFT))
        }
    }
}

private const val AVATAR_FILL = 0.2f
private const val ON_BANNER_SOFT = 0.9f
private const val GROUP_FILL = 0.8f
private const val DIVIDER_ALPHA = 0.08f

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.shelfTap.ink2, modifier = Modifier.padding(start = Spacing.xs + Spacing.xs / 2, top = Spacing.xs))
}

/** A rounded glass card that holds rows. */
@Composable
private fun GlassGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.group)).background(MaterialTheme.shelfTap.glass.copy(alpha = GROUP_FILL))
            .padding(horizontal = Spacing.l, vertical = Spacing.xs + Spacing.xs / 2),
        content = content,
    )
}

@Composable
private fun GroupDivider() = HorizontalDivider(color = MaterialTheme.shelfTap.ink.copy(alpha = DIVIDER_ALPHA))

/** A row with a switch; the whole row toggles it, and TalkBack reads one switch with its caption. */
@Composable
private fun SwitchItem(title: String, caption: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = AccountSize.rowTall).toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs / 2)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.shelfTap.ink2) }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = shelfTapSwitchColors())
    }
}

/** A row that opens another screen, marked by a chevron. */
@Composable
private fun LinkItem(title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = AccountSize.row).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        LucideIcon(LucideIcons.ChevronRight, tint = MaterialTheme.shelfTap.ink3)
    }
}

@Composable
private fun InfoItem(text: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = AccountSize.row), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.shelfTap.ink2)
    }
}

/** Light, dark or follow the phone, as three pills in one glass capsule; the chosen one is filled. They stay radio buttons for TalkBack. */
@Composable
private fun ThemeSegments(selected: ThemeMode, onChange: (ThemeMode) -> Unit) {
    val c = MaterialTheme.shelfTap
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(c.glass.copy(alpha = GROUP_FILL)).padding(Spacing.xs).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark).forEach { (mode, label) ->
            val on = selected == mode
            Box(
                Modifier.weight(1f).heightIn(min = AccountSize.segment).clip(CircleShape).background(if (on) c.primary else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onChange(mode) }).padding(horizontal = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label), style = if (on) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    color = if (on) c.onPrimary else c.ink2, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private const val DANGER_EDGE = 0.35f

/** The sign-out button: outlined in the danger colour; it opens a confirmation, it does not sign out. */
@Composable
private fun DangerOutlineButton(text: String, onClick: () -> Unit) {
    val danger = MaterialTheme.shelfTap.danger
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxWidth().padding(top = Spacing.xs).height(AccountSize.action).pressScale(source).clip(CircleShape)
            .border(BorderStroke(Radius.hairline, danger.copy(alpha = DANGER_EDGE)), CircleShape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.titleMedium, color = danger) }
}

/** Debug builds only: force one effect tier to compare A/B on the same phone (plan section 6.3). Never in a release build. */
@Composable
private fun DebugEffectChoice(onChange: (String?) -> Unit) {
    val current = LocalEffectTier.current
    val forced by LocalContainer.current.themeStore.forcedEffect.collectAsStateWithLifecycle(null)
    Text(stringResource(R.string.debug_effect_title, current.name), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = Spacing.s))
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
