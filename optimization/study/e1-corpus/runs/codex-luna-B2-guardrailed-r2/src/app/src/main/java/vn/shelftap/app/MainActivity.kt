package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShelfTapApp() }
    }
}

@Composable
private fun ShelfTapApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val current = state) {
                AppState.Loading -> LoadingView()
                AppState.SignedOut -> SignInView(busy, error, vm::signIn)
                is AppState.Profile -> ProfileView(current.member, busy, error, vm::showPasswordChange, vm::signOut)
                is AppState.PasswordChange -> PasswordChangeView(busy, error, current.member, vm::savePassword)
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp))
    }
}

@Composable
private fun SignInView(busy: Boolean, error: String?, onSignIn: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    FormColumn("Welcome to ShelfTap") {
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { androidx.compose.material3.TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide" else "Show") } })
        ErrorText(error)
        Button(onClick = { onSignIn(email, password) }, enabled = !busy && email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) {
            if (busy) CircularProgressIndicator(Modifier.sizeIn(maxWidth = 20.dp, maxHeight = 20.dp)) else Text("Sign in")
        }
    }
}

@Composable
private fun ProfileView(member: Member, busy: Boolean, error: String?, onChange: () -> Unit, onSignOut: () -> Unit) {
    FormColumn("Your profile") {
        ProfileRow("Full name", member.fullName)
        ProfileRow("Email", member.email)
        ProfileRow("Member type", member.memberType)
        ProfileRow("Status", member.status)
        ErrorText(error)
        Button(onClick = onChange, enabled = !busy, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Change password") }
        OutlinedButton(onClick = onSignOut, enabled = !busy, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Sign out") }
    }
}

@Composable
private fun PasswordChangeView(busy: Boolean, error: String?, member: Member, onSave: (String, String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val mismatch = confirmation.isNotEmpty() && newPassword != confirmation
    FormColumn("Change password") {
        Text("Hi ${member.fullName}, choose a new password to continue.", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(current, { current = it }, Modifier.fillMaxWidth(), label = { Text("Current password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        OutlinedTextField(newPassword, { newPassword = it }, Modifier.fillMaxWidth(), label = { Text("New password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        OutlinedTextField(confirmation, { confirmation = it }, Modifier.fillMaxWidth(), label = { Text("Confirm new password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        ErrorText(error ?: if (mismatch) "The new passwords do not match." else null)
        Button(onClick = { onSave(current, newPassword) }, enabled = !busy && current.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmation, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text("Save password") }
    }
}

@Composable
private fun FormColumn(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        content()
    }
}

@Composable private fun ProfileRow(label: String, value: String) {
    Column { Text(label, style = MaterialTheme.typography.labelLarge); Text(value, style = MaterialTheme.typography.bodyLarge) }
}

@Composable private fun ErrorText(error: String?) { if (error != null) Text(error, color = MaterialTheme.colorScheme.error) }
