package vn.shelftap.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ShelfTapApp(viewModel: ShelfTapViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        ScreenState.Loading -> Centered { CircularProgressIndicator() }
        is ScreenState.SignIn -> SignInScreen(current, viewModel::signIn)
        is ScreenState.ChangePassword -> ChangePasswordScreen(current, viewModel::savePassword)
        is ScreenState.Profile -> ProfileScreen(current, viewModel::signOut)
    }
}

@Composable
private fun SignInScreen(state: ScreenState.SignIn, onSignIn: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    FormColumn {
        Text(text = stringResource(R.string.app_name), style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.email)) }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.password)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        state.message?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onSignIn(email, password) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            if (state.busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.width(20.dp).height(20.dp))
            else Text(stringResource(R.string.sign_in))
        }
    }
}

@Composable
private fun ChangePasswordScreen(state: ScreenState.ChangePassword, onSave: (String, String, String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    FormColumn {
        Text(stringResource(R.string.change_password), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        PasswordField(stringResource(R.string.current_password), current) { current = it }
        Spacer(Modifier.height(12.dp))
        PasswordField(stringResource(R.string.new_password), newPassword) { newPassword = it }
        Spacer(Modifier.height(12.dp))
        PasswordField(stringResource(R.string.confirm_new_password), confirm) { confirm = it }
        state.message?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onSave(current, newPassword, confirm) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            if (state.busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.width(20.dp).height(20.dp))
            else Text(stringResource(R.string.save_password))
        }
    }
}

@Composable
private fun ProfileScreen(state: ScreenState.Profile, onSignOut: () -> Unit) {
    val member = state.member
    FormColumn {
        Text(stringResource(R.string.profile), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(28.dp))
        Text(member.fullName, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.email_value, member.email))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.member_type_value, member.memberType))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.status_value, member.status))
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onSignOut, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            if (state.busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.width(20.dp).height(20.dp))
            else Text(stringResource(R.string.sign_out))
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
}

@Composable
private fun FormColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center, content = content)
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
}
