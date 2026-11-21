package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShelfTapTheme { ShelfTapApp() } }
    }
}

@Composable
private fun ShelfTapApp(viewModel: ShelfTapViewModel = viewModel()) {
    val state by viewModel.screen.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val current = state) {
            ScreenState.Loading -> LoadingScreen()
            ScreenState.SignIn -> SignInScreen(error, viewModel::signIn)
            is ScreenState.Profile -> ProfileScreen(current.member, error, viewModel::signOut)
            is ScreenState.ChangePassword -> ChangePasswordScreen(current.member, error, viewModel::changePassword)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SignInScreen(error: String?, onSignIn: (String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("ShelfTap", style = MaterialTheme.typography.headlineLarge)
        Text("Member account", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.email)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(12.dp))
        PasswordField(password, { password = it }, showPassword, { showPassword = !showPassword }, stringResource(R.string.password))
        ErrorText(error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onSignIn(email, password) }, enabled = email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sign_in)) }
    }
}

@Composable
private fun ProfileScreen(member: MemberProfile, error: String?, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(stringResource(R.string.profile), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        ProfileRow(stringResource(R.string.full_name), member.fullName)
        ProfileRow(stringResource(R.string.email), member.email)
        ProfileRow(stringResource(R.string.member_type), member.memberType)
        ProfileRow(stringResource(R.string.status), member.status)
        ErrorText(error)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sign_out)) }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(10.dp))
        HorizontalDivider()
    }
}

@Composable
private fun ChangePasswordScreen(member: MemberProfile, error: String?, onChange: (String, String) -> Unit) {
    var current by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showCurrent by rememberSaveable { mutableStateOf(false) }
    var showNew by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.change_password), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.password_required), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        PasswordField(current, { current = it }, showCurrent, { showCurrent = !showCurrent }, stringResource(R.string.current_password))
        Spacer(Modifier.height(12.dp))
        PasswordField(newPassword, { newPassword = it }, showNew, { showNew = !showNew }, stringResource(R.string.new_password))
        Spacer(Modifier.height(12.dp))
        PasswordField(confirm, { confirm = it }, showNew, { showNew = !showNew }, stringResource(R.string.confirm_new_password))
        ErrorText(error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { if (newPassword == confirm) onChange(current, newPassword) }, enabled = current.isNotBlank() && newPassword.isNotBlank() && confirm.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_password)) }
        if (newPassword.isNotEmpty() && confirm.isNotEmpty() && newPassword != confirm) Text(stringResource(R.string.passwords_do_not_match), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, visible: Boolean, toggle: () -> Unit, label: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value, onValueChange, Modifier.weight(1f), label = { Text(label) }, singleLine = true, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Spacer(Modifier.width(4.dp))
        TextButton(onClick = toggle) { Text(if (visible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)) }
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error == null) return
    val message = when {
        error == "INVALID_CREDENTIALS" -> stringResource(R.string.invalid_credentials)
        error == "ACCOUNT_LOCKED" -> stringResource(R.string.account_locked)
        error.startsWith("RATE_LIMITED:") -> stringResource(R.string.rate_limited, error.substringAfter(':').toIntOrNull() ?: 0)
        error == "PASSWORD_CHANGE_REQUIRED" -> stringResource(R.string.password_required)
        error == "NETWORK" -> stringResource(R.string.network_error)
        else -> stringResource(R.string.generic_error)
    }
    Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
}
