package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.shelftap.app.data.ApiClient
import vn.shelftap.app.data.Repository
import vn.shelftap.app.data.SessionStore
import vn.shelftap.app.ui.AppScreen
import vn.shelftap.app.ui.ShelfTapViewModel
import vn.shelftap.app.ui.ShelfTapViewModelFactory
import vn.shelftap.app.ui.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SessionStore(applicationContext)
        val repository = Repository(ApiClient.create(store), store)
        setContent { ShelfTapApp(repository) }
    }
}

@Composable
private fun ShelfTapApp(repository: Repository) {
    val vm: ShelfTapViewModel = viewModel(factory = ShelfTapViewModelFactory(repository))
    val state by vm.state.collectAsStateWithLifecycle()
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when (state.screen) {
                AppScreen.SignIn -> SignInScreen(state, vm::signIn)
                AppScreen.ChangePassword -> ChangePasswordScreen(state, vm::changePassword)
                AppScreen.Profile -> ProfileScreen(state, vm::signOut)
            }
        }
    }
}

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("ShelfTap", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
private fun SignInScreen(state: UiState, onSignIn: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Page {
        Text("Member sign in", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        ErrorText(state.error)
        Spacer(Modifier.height(16.dp))
        Button({ onSignIn(email, password) }, Modifier.fillMaxWidth(), enabled = !state.loading) {
            if (state.loading) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Sign in")
        }
    }
}

@Composable
private fun ChangePasswordScreen(state: UiState, onChange: (String, String, String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Page {
        Text("Change your password", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("A password change is required before you can continue.")
        Spacer(Modifier.height(16.dp))
        PasswordField("Current password", current) { current = it }
        Spacer(Modifier.height(12.dp))
        PasswordField("New password", newPassword) { newPassword = it }
        Spacer(Modifier.height(12.dp))
        PasswordField("Confirm new password", confirm) { confirm = it }
        ErrorText(state.error)
        Spacer(Modifier.height(16.dp))
        Button({ onChange(current, newPassword, confirm) }, Modifier.fillMaxWidth(), enabled = !state.loading) {
            if (state.loading) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Save password")
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
}

@Composable
private fun ProfileScreen(state: UiState, onSignOut: () -> Unit) {
    val member = state.member ?: return
    Page {
        Text("Profile", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))
        ProfileRow("Name", member.fullName)
        ProfileRow("Email", member.email)
        ProfileRow("Member type", member.memberType)
        ProfileRow("Status", member.status)
        Spacer(Modifier.height(28.dp))
        OutlinedButton(onSignOut, Modifier.fillMaxWidth(), enabled = !state.loading) { Text("Sign out") }
        ErrorText(state.error)
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text("$label: ", style = MaterialTheme.typography.labelLarge)
        Text(value)
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (!error.isNullOrBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(error, color = MaterialTheme.colorScheme.error)
    }
}
