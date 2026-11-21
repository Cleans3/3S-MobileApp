package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Picks the screen to show from the view model's state. */
@Composable
fun ShelfTapRoot(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state.destination) {
        Destination.LOADING -> LoadingScreen()

        Destination.SIGN_IN -> SignInScreen(
            error = state.signInError,
            busy = state.busy,
            onSignIn = viewModel::signIn,
        )

        Destination.PROFILE -> {
            val member = state.member
            if (member == null) {
                LoadingScreen()
            } else {
                ProfileScreen(
                    member = member,
                    notice = state.profileNotice,
                    error = state.profileError,
                    busy = state.busy,
                    onChangePassword = viewModel::openChangePassword,
                    onSignOut = viewModel::signOut,
                )
            }
        }

        Destination.CHANGE_PASSWORD -> ChangePasswordScreen(
            forced = state.forcedPasswordChange,
            error = state.changePasswordError,
            busy = state.busy,
            onSubmit = viewModel::changePassword,
            onCancel = viewModel::cancelChangePassword,
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
