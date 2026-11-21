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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ShelfTapApp() } }
}

@Composable
fun ShelfTapApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle(); val message by vm.message.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF8FAF4)) {
        when (val s = state) {
            ScreenState.Loading -> LoadingView()
            ScreenState.SignedOut -> SignInScreen(message, vm::login)
            is ScreenState.SignedIn -> if (s.changingPassword) ChangePasswordScreen(message, vm::changePassword) else ProfileScreen(s.member, vm::signOut)
        }
    }
}

@Composable private fun LoadingView() { Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator(color = Color(0xFF365E3D)) } }

@Composable
private fun SignInScreen(error: String?, onLogin: (String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }; var password by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.LibraryBooks, null, tint = Color(0xFF365E3D), modifier = Modifier.height(54.dp))
        Text("ShelfTap", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color(0xFF193C24))
        Text("Your library account, at your fingertips", color = Color(0xFF536158), modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onLogin(email, password) }, enabled = email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Sign in") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(member: Member, onSignOut: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("My profile") }) }) { pad ->
        Column(Modifier.padding(pad).padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Member details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            ProfileRow("Full name", member.fullName); ProfileRow("Email", member.email); ProfileRow("Member type", member.memberType); ProfileRow("Status", member.status)
            Spacer(Modifier.height(14.dp)); OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Sign out") }
        }
    }
}

@Composable private fun ProfileRow(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelLarge, color = Color(0xFF536158)); Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 3.dp)) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordScreen(error: String?, onChange: (String, String) -> Unit) {
    var current by rememberSaveable { mutableStateOf("") }; var next by rememberSaveable { mutableStateOf("") }; var confirm by rememberSaveable { mutableStateOf("") }; var localError by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Change password") }) }) { pad ->
        Column(Modifier.padding(pad).padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Update your password to continue using ShelfTap.", style = MaterialTheme.typography.bodyLarge)
            PasswordField("Current password", current) { current = it }; PasswordField("New password", next) { next = it }; PasswordField("Confirm new password", confirm) { confirm = it }
            val shownError = localError ?: error; if (shownError != null) Text(shownError, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(4.dp)); Button(onClick = { if (next != confirm) localError = "The new passwords do not match." else { localError = null; onChange(current, next) } }, enabled = current.isNotBlank() && next.isNotBlank() && confirm.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Save password") }
        }
    }
}

@Composable private fun PasswordField(label: String, value: String, onValue: (String) -> Unit) { OutlinedTextField(value, onValue, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()) }
