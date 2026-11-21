package com.example.libraryapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.libraryapp.di.ServiceLocator
import com.example.libraryapp.ui.screens.login.LoginScreen
import com.example.libraryapp.ui.screens.login.LoginViewModel
import com.example.libraryapp.ui.screens.profile.ProfileScreen
import com.example.libraryapp.ui.screens.profile.ProfileViewModel

@Composable
fun LibraryNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    val factory = ServiceLocator.provideViewModelFactory()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = viewModel(factory = factory)
            ProfileScreen(
                viewModel = profileViewModel,
                onSignOutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
