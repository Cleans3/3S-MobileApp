package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state.stage) {
        Stage.Starting -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        Stage.SignedOut -> SignInScreen(
            state = state,
            onSignIn = viewModel::signIn,
        )

        Stage.ChangePassword -> ChangePasswordScreen(
            state = state,
            onSubmit = viewModel::changePassword,
            onCancel = viewModel::cancelChangePassword,
            onSignOut = viewModel::signOut,
        )

        Stage.Profile -> ProfileScreen(
            state = state,
            onChangePassword = viewModel::openChangePassword,
            onSignOut = viewModel::signOut,
            onRetry = viewModel::retryProfile,
        )
    }
}
