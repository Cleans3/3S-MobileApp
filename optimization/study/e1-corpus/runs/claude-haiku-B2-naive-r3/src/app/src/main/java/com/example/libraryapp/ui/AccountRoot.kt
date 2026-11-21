package com.example.libraryapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Chooses between the loading indicator, the sign-in form and the profile based on the session state. */
@Composable
fun AccountRoot(authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)) {
    val state by authViewModel.state.collectAsStateWithLifecycle()

    when (state.status) {
        SessionStatus.Restoring -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        SessionStatus.SignedOut -> LoginScreen(
            state = state,
            onEmailChange = authViewModel::onEmailChanged,
            onPasswordChange = authViewModel::onPasswordChanged,
            onSignIn = authViewModel::signIn,
        )

        SessionStatus.SignedIn -> ProfileScreen(
            state = state,
            onRefresh = authViewModel::refreshProfile,
            onSignOut = authViewModel::signOut,
        )
    }
}
