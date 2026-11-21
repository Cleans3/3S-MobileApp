package com.library.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.library.app.LibraryApplication
import com.library.app.ui.auth.AuthViewModel
import com.library.app.ui.auth.SignInScreen
import com.library.app.ui.profile.ProfileScreen
import com.library.app.ui.profile.ProfileViewModel

object Destinations {
    const val SIGN_IN = "sign_in"
    const val PROFILE = "profile"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val repository = LibraryApplication.instance.repository
    val startDestination = if (repository.isLoggedIn()) Destinations.PROFILE else Destinations.SIGN_IN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Destinations.SIGN_IN) {
            val authViewModel = viewModel {
                AuthViewModel(repository)
            }
            SignInScreen(
                viewModel = authViewModel,
                onSignInSuccess = {
                    navController.navigate(Destinations.PROFILE) {
                        popUpTo(Destinations.SIGN_IN) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.PROFILE) {
            val profileViewModel = viewModel {
                ProfileViewModel(repository)
            }
            ProfileScreen(
                viewModel = profileViewModel,
                onSignOut = {
                    navController.navigate(Destinations.SIGN_IN) {
                        popUpTo(Destinations.PROFILE) { inclusive = true }
                    }
                }
            )
        }
    }
}
