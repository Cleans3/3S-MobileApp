package shelftap.android.ui

import android.nfc.Tag
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
import shelftap.android.api.ApiException
import shelftap.android.data.Repository
import shelftap.android.nfc.CardException
import shelftap.android.nfc.TagReader
import shelftap.android.security.BiometricLock
import shelftap.android.session.LogoutReason
import shelftap.android.session.SessionManager

class AuthViewModel(private val session: SessionManager) : ViewModel() {
    data class UiState(val busy: Boolean = false, val error: String? = null)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun login(email: String, password: String) = launchOp { session.login(email.trim(), password) }

    /** Reader-mode callback: one card exchange at a time. */
    fun cardTapped(tag: Tag) {
        if (_state.value.busy) return
        val iso = TagReader.isoDep(tag) ?: return fail("That is not a library card.")
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

    private fun fail(message: String) {
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

private fun LogoutReason.message() = when (this) {
    LogoutReason.SESSION_EXPIRED -> "Your session expired. Log in again."
    LogoutReason.TOKEN_REUSED -> "This session was revoked because the account was used elsewhere. Log in again."
    LogoutReason.INACTIVITY -> "Logged out after a period of inactivity."
    LogoutReason.LOCK_KEY_INVALIDATED -> "Biometrics changed on this phone, so the biometric lock was reset. Log in again."
}

@Composable
fun LoginScreen(reason: LogoutReason?, onCard: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // never saved to instance state

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("ShelfTap", style = MaterialTheme.typography.headlineMedium)
        IntegrityBanner()
        reason?.let { Text(it.message()) }
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(state.error)
        Button(
            onClick = { vm.login(email, password) },
            enabled = !state.busy && email.isNotBlank() && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.busy) "Logging in..." else "Log in") }
        OutlinedButton(onClick = onCard, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Log in with library card") }
    }
}

@Composable
fun CardLoginScreen(onBack: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    NfcGate {
        NfcReaderEffect(TagReader.CARD_FLAGS) { vm.cardTapped(it) }
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Hold your library card against the back of the phone.", style = MaterialTheme.typography.titleMedium)
            Text("The card proves it is yours with a signature. You do not type a PIN, and nothing on the card is read.")
            if (state.busy) Spinner()
            ErrorText(state.error)
            TextButton(onClick = onBack, enabled = !state.busy) { Text("Use email and password instead") }
        }
    }
}

@Composable
fun OfflineScreen() {
    val session = LocalContainer.current.session
    val scope = rememberCoroutineScope()
    Centered {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Can't reach the server.")
            Button(onClick = { scope.launch { session.restore() } }) { Text("Retry") }
            TextButton(onClick = { scope.launch { session.logout() } }) { Text("Log out") }
        }
    }
}

@Composable
fun LockScreen() {
    val session = LocalContainer.current.session
    val activity = LocalActivity.current as FragmentActivity
    val scope = rememberCoroutineScope()
    val unlock: () -> Unit = {
        scope.launch {
            session.unlock { cipher ->
                BiometricLock.authenticate(activity, cipher, title = "Unlock ShelfTap", cancelLabel = "Cancel")
            }
        }
    }
    LaunchedEffect(Unit) { unlock() }
    Centered {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ShelfTap is locked.", style = MaterialTheme.typography.titleMedium)
            Button(onClick = unlock) { Text("Unlock") }
            TextButton(onClick = { scope.launch { session.logout() } }) { Text("Log out") }
        }
    }
}

class PasswordViewModel(private val repo: Repository) : ViewModel() {
    data class UiState(val busy: Boolean = false, val error: String? = null, val done: Boolean = false)

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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Change password", style = MaterialTheme.typography.headlineSmall)
        if (forced) Text("Your account needs a new password before you can use the library.")
        listOf(
            Triple("Current password", current) { v: String -> current = v },
            Triple("New password", new) { v: String -> new = v },
            Triple("New password again", again) { v: String -> again = v },
        ).forEach { (label, value, set) ->
            OutlinedTextField(
                value = value, onValueChange = set, label = { Text(label) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
            )
        }
        if (new.isNotEmpty() && again.isNotEmpty() && new != again) Text("The new passwords do not match.", color = MaterialTheme.colorScheme.error)
        ErrorText(state.error)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { vm.change(current, new) },
            enabled = !state.busy && current.isNotEmpty() && new.isNotEmpty() && new == again,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.busy) "Saving..." else "Change password") }
        if (forced) {
            TextButton(onClick = { scope.launch { session.logout() } }) { Text("Log out") }
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
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        title = { Text("Password needed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You logged in with your card. Your reading history is personal data, so this needs your password.")
                OutlinedTextField(
                    value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
                )
                ErrorText(error)
            }
        },
        confirmButton = {
            Button(
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
            ) { Text(if (busy) "Logging in..." else "Log in") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}
