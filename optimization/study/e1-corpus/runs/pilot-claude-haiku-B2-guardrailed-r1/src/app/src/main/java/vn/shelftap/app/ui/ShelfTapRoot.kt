package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Chooses which screen to show from the view model state. */
@Composable
fun ShelfTapRoot(viewModel: AccountViewModel) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val screen = state.screen) {
            Screen.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            Screen.SignIn -> SignInScreen(
                state = state,
                onSignIn = viewModel::signIn,
            )

            Screen.Profile -> ProfileScreen(
                state = state,
                onSignOut = viewModel::signOut,
                onChangePassword = viewModel::openChangePassword,
                onRetry = viewModel::retryProfile,
            )

            is Screen.ChangePassword -> ChangePasswordScreen(
                forced = screen.forced,
                state = state,
                onSubmit = viewModel::changePassword,
                onBack = viewModel::closeChangePassword,
            )
        }
    }
}
