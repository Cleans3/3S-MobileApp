package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.shelftap.app.session.AppUiState
import vn.shelftap.app.session.AppViewModel

/** Chooses the screen for the current app state. */
@Composable
fun ShelfTapRoot(app: AppViewModel) {
    val state by app.state.collectAsStateWithLifecycle()
    when (val current = state) {
        AppUiState.Loading -> FullScreenProgress()

        is AppUiState.SignedOut -> SignInScreen(
            notice = current.notice,
            onSignIn = app::signIn,
        )

        is AppUiState.Unavailable -> UnavailableScreen(
            message = current.message,
            onRetry = app::retry,
            onSignOut = app::signOut,
        )

        is AppUiState.SignedIn -> ProfileScreen(
            me = current.me,
            onChangePassword = app::openChangePassword,
            onSignOut = app::signOut,
        )

        is AppUiState.ChangePassword -> ChangePasswordScreen(
            forced = current.forced,
            onSubmit = app::changePassword,
            onBack = app::closeChangePassword,
            onSignOut = app::signOut,
        )
    }
}

@Composable
private fun UnavailableScreen(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Try again")
        }
        TextButton(onClick = onSignOut) {
            Text("Sign out")
        }
    }
}
