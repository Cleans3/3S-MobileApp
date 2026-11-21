package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ShelfTapTheme { ShelfTapApp() } } }
}

@Composable fun ShelfTapTheme(content: @Composable () -> Unit) { MaterialTheme(content = content) }

@Composable fun ShelfTapApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    Surface(Modifier.fillMaxSize()) { when (state.screen) { AppScreen.SignIn -> SignIn(state, vm); AppScreen.ChangePassword -> ChangePassword(state, vm); AppScreen.Profile -> Profile(state, vm) } }
}

@Composable private fun Page(title: String, state: UiState, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text(title, style = MaterialTheme.typography.headlineMedium); content(); if (state.busy) CircularProgressIndicator(); state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
}

@Composable private fun SignIn(state: UiState, vm: MainViewModel) { var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; Page("Welcome to ShelfTap", state) { Text("Sign in to manage your library membership."); OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true); Button({ vm.signIn(email, password) }, Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Sign in") } } }

@Composable private fun Profile(state: UiState, vm: MainViewModel) { val m = state.member ?: return; Page("My profile", state) { Text(m.fullName, style = MaterialTheme.typography.headlineSmall); HorizontalDivider(); Info("Email", m.email); Info("Member type", m.memberType); Info("Status", m.status); OutlinedButton({ vm.signOut() }, Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Sign out") } } }

@Composable private fun ChangePassword(state: UiState, vm: MainViewModel) { var current by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; Page("Change password", state) { Text("For your security, change your password before continuing."); PasswordField("Current password", current) { current = it }; PasswordField("New password", next) { next = it }; PasswordField("Confirm new password", confirm) { confirm = it }; Button({ vm.changePassword(current, next, confirm) }, Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Update password") } } }

@Composable private fun PasswordField(label: String, value: String, change: (String) -> Unit) = OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
@Composable private fun Info(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value) } }
