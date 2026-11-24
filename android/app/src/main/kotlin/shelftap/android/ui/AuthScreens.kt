package shelftap.android.ui

import android.nfc.Tag
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import shelftap.android.R
import shelftap.android.api.ApiException
import shelftap.android.data.Repository
import shelftap.android.nfc.CardException
import shelftap.android.nfc.TagReader
import shelftap.android.security.BiometricLock
import shelftap.android.session.LogoutReason
import shelftap.android.session.SessionManager
import shelftap.android.ui.theme.Spacing

class AuthViewModel(private val session: SessionManager) : ViewModel() {
    data class UiState(val busy: Boolean = false, val error: UiText? = null)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun login(email: String, password: String) = launchOp { session.login(email.trim(), password) }

    /** Reader-mode callback: one card exchange at a time. */
    fun cardTapped(tag: Tag) {
        if (_state.value.busy) return
        val iso = TagReader.isoDep(tag) ?: return fail(uiText(R.string.err_not_library_card))
        launchOp {
            try {
                withContext(Dispatchers.IO) { iso.connect() }
                session.cardLogin { iso.transceive(it) }
            } catch (e: IOException) {
                throw CardException.Lost(e)
            } finally {
                withContext(Dispatchers.IO) { runCatching { iso.close() } }
            }
        }
    }

    private fun fail(message: UiText) {
        _state.value = UiState(error = message)
    }

    private fun launchOp(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.value = UiState(busy = true)
        viewModelScope.launch {
            try {
                block()
                _state.value = UiState()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.value = UiState(error = e.userMessage())
            }
        }
    }
}

@StringRes
private fun LogoutReason.message() = when (this) {
    LogoutReason.SESSION_EXPIRED -> R.string.logout_session_expired
    LogoutReason.TOKEN_REUSED -> R.string.logout_token_reused
    LogoutReason.INACTIVITY -> R.string.logout_inactivity
    LogoutReason.LOCK_KEY_INVALIDATED -> R.string.logout_lock_key_invalidated
}

@Composable
fun LoginScreen(reason: LogoutReason?, onCard: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // never saved to instance state

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pagePadding()).padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        IntegrityBanner()
        reason?.let { Text(stringResource(it.message())) }
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.login_email)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        PasswordField(password, { password = it }, stringResource(R.string.login_password), Modifier.fillMaxWidth())
        ErrorText(state.error)
        PrimaryButton(
            onClick = { vm.login(email, password) },
            enabled = !state.busy && email.isNotBlank() && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(if (state.busy) R.string.login_busy else R.string.login_action)) }
        SecondaryButton(onClick = onCard, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.login_with_card)) }
    }
}

@Composable
fun CardLoginScreen(onBack: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    NfcGate {
        NfcReaderEffect(TagReader.CARD_FLAGS) { vm.cardTapped(it) }
        Column(
            Modifier.fillMaxSize().padding(pagePadding()).padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.card_hold), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.card_explain))
            if (state.busy) Spinner()
            ErrorText(state.error)
            TextButton(onClick = onBack, enabled = !state.busy) { Text(stringResource(R.string.card_use_password)) }
        }
    }
}

@Composable
fun OfflineScreen() {
    val session = LocalContainer.current.session
    val scope = rememberCoroutineScope()
    Centered {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text(stringResource(R.string.offline_message))
            PrimaryButton(onClick = { scope.launch { session.restore() } }) { Text(stringResource(R.string.action_retry)) }
            TextButton(onClick = { scope.launch { session.logout() } }) { Text(stringResource(R.string.action_logout)) }
        }
    }
}

@Composable
fun LockScreen() {
    val session = LocalContainer.current.session
    val activity = LocalActivity.current as FragmentActivity
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.lock_title)
    val cancelLabel = stringResource(R.string.action_cancel)
    val unlock: () -> Unit = {
        scope.launch {
            session.unlock { cipher ->
                BiometricLock.authenticate(activity, cipher, title = title, cancelLabel = cancelLabel)
            }
        }
    }
    LaunchedEffect(Unit) { unlock() }
    Centered {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text(stringResource(R.string.lock_locked), style = MaterialTheme.typography.titleMedium)
            PrimaryButton(onClick = unlock) { Text(stringResource(R.string.lock_unlock)) }
            TextButton(onClick = { scope.launch { session.logout() } }) { Text(stringResource(R.string.action_logout)) }
        }
    }
}

class PasswordViewModel(private val repo: Repository) : ViewModel() {
    data class UiState(val busy: Boolean = false, val error: UiText? = null, val done: Boolean = false)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun change(current: String, new: String) {
        if (_state.value.busy) return
        _state.value = UiState(busy = true)
        viewModelScope.launch {
            _state.value = try {
                repo.changePassword(current, new)
                UiState(done = true)
            } catch (e: shelftap.android.api.ApiException) {
                UiState(error = e.userMessage())
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(forced: Boolean, onDone: () -> Unit) {
    val vm = appViewModel { PasswordViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val session = LocalContainer.current.session
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.pw_title), style = MaterialTheme.typography.headlineSmall)
        if (forced) Text(stringResource(R.string.pw_forced))
        listOf(
            Triple(R.string.pw_current, current) { v: String -> current = v },
            Triple(R.string.pw_new, new) { v: String -> new = v },
            Triple(R.string.pw_again, again) { v: String -> again = v },
        ).forEach { (label, value, set) ->
            PasswordField(value, set, stringResource(label), Modifier.fillMaxWidth())
        }
        if (new.isNotEmpty() && again.isNotEmpty() && new != again) Text(stringResource(R.string.pw_mismatch), color = MaterialTheme.colorScheme.error)
        ErrorText(state.error)
        Spacer(Modifier.height(Spacing.xs))
        PrimaryButton(
            onClick = { vm.change(current, new) },
            enabled = !state.busy && current.isNotEmpty() && new.isNotEmpty() && new == again,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(if (state.busy) R.string.pw_saving else R.string.pw_title)) }
        if (forced) {
            TextButton(onClick = { scope.launch { session.logout() } }) { Text(stringResource(R.string.action_logout)) }
        }
    }
}

/**
 * A card login proves possession of the card only, so history and privacy answer 403 STEP_UP_REQUIRED.
 * This asks for the password and swaps the card session for a password session; cancelling keeps the card session.
 * Dialogs are separate windows, so FLAG_SECURE is set on this one explicitly.
 */
@Composable
fun StepUpDialog(defaultEmail: String = "", onDismiss: () -> Unit, onDone: () -> Unit) {
    val session = LocalContainer.current.session
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf(defaultEmail) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        title = { Text(stringResource(R.string.stepup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(stringResource(R.string.stepup_text))
                OutlinedTextField(
                    value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.login_email)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
                )
                PasswordField(password, { password = it }, stringResource(R.string.login_password), Modifier.fillMaxWidth())
                ErrorText(error)
            }
        },
        confirmButton = {
            PrimaryButton(
                enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            session.stepUpLogin(email.trim(), password)
                            onDone()
                        } catch (e: ApiException) {
                            error = e.userMessage()
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text(stringResource(if (busy) R.string.login_busy else R.string.login_action)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.action_cancel)) } },
    )
}
