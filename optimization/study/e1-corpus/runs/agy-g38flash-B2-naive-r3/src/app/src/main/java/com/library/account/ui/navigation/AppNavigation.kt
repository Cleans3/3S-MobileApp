package com.library.account.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.library.account.LibraryApplication
import com.library.account.ui.auth.AuthViewModel
import com.library.account.ui.auth.LoginScreen
import com.library.account.ui.profile.ProfileScreen
import com.library.account.ui.profile.ProfileViewModel

/**
 * Handles seamless navigation between Member Sign In and Profile based on authentication state.
 */
@Composable
fun AppNavigation() {
    val app = LibraryApplication.instance
    val authRepository = app.authRepository
    val isLoggedIn by authRepository.isLoggedIn.collectAsState()

    Crossfade(
        targetState = isLoggedIn,
        animationSpec = tween(durationMillis = 350),
        label = "AuthNavigationCrossfade"
    ) { loggedIn ->
        if (loggedIn) {
            val profileViewModel: ProfileViewModel = viewModel(
                factory = ProfileViewModel.Factory(authRepository)
            )
            ProfileScreen(viewModel = profileViewModel)
        } else {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(authRepository)
            )
            LoginScreen(viewModel = authViewModel)
        }
    }
}
