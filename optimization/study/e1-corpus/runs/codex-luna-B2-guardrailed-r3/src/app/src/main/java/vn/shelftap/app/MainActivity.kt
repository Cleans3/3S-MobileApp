package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ShelfTapApp() } }
}

@Composable
fun ShelfTapApp(vm: ShelfTapViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    MaterialTheme { Surface(Modifier.fillMaxSize()) { when (val current = state) {
        ScreenState.Loading -> CircularProgressIndicator(Modifier.padding(32.dp))
        is ScreenState.SignedOut -> SignInScreen(current.message, vm)
        is ScreenState.SignedIn -> if (current.changingPassword) ChangePasswordScreen(vm) else ProfileScreen(current.member, vm)
        is ScreenState.Error -> ErrorScreen(current.message, vm)
    } } }
}

@Composable private fun SignInScreen(message: String?, vm: ShelfTapViewModel) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("ShelfTap", style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(8.dp)); Text("Member account", style = MaterialTheme.typography.bodyLarge); Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
        Spacer(Modifier.height(12.dp)); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        if (message != null) { Spacer(Modifier.height(8.dp)); Text(message, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(20.dp)); Button({ vm.signIn(email, password) }, Modifier.fillMaxWidth()) { Text("Sign in") }
    }
}

@Composable private fun ProfileScreen(member: Member, vm: ShelfTapViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium); Text(member.fullName, style = MaterialTheme.typography.titleLarge)
        Detail("Email", member.email); Detail("Member type", member.memberType); Detail("Status", member.status)
        Spacer(Modifier.height(8.dp)); Button({ vm.beginChangePassword() }, Modifier.fillMaxWidth()) { Text("Change password") }
        OutlinedButton({ vm.signOut() }, Modifier.fillMaxWidth()) { Text("Sign out") }
    }
}

@Composable private fun Detail(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Text(value, style = MaterialTheme.typography.bodyLarge) } }

@Composable private fun ChangePasswordScreen(vm: ShelfTapViewModel) {
    var current by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        Text("Change password", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(8.dp)); Text("Choose a new password to continue."); Spacer(Modifier.height(20.dp))
        PasswordField("Current password", current) { current = it }; Spacer(Modifier.height(10.dp)); PasswordField("New password", next) { next = it }; Spacer(Modifier.height(10.dp)); PasswordField("Confirm new password", confirm) { confirm = it }
        Spacer(Modifier.height(20.dp)); Button({ vm.changePassword(current, next, confirm) }, Modifier.fillMaxWidth()) { Text("Change password") }
    }
}

@Composable private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) { OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), singleLine = true) }

@Composable private fun ErrorScreen(message: String, vm: ShelfTapViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text(message, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(16.dp)); Button({ vm.dismissError() }, Modifier.fillMaxWidth()) { Text("Continue") } }
}
