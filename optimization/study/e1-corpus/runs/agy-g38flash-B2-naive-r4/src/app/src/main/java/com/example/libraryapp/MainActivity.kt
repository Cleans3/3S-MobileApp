package com.example.libraryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.libraryapp.ui.auth.LoginScreen
import com.example.libraryapp.ui.auth.LoginViewModel
import com.example.libraryapp.ui.navigation.Screen
import com.example.libraryapp.ui.profile.ProfileScreen
import com.example.libraryapp.ui.profile.ProfileViewModel
import com.example.libraryapp.ui.theme.LibraryAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LibraryApplication
        val authRepository = app.authRepository
        val startDestination = if (authRepository.isLoggedIn()) {
            Screen.Profile.route
        } else {
            Screen.Login.route
        }

        setContent {
            LibraryAppTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(Screen.Login.route) {
                        val loginViewModel: LoginViewModel = viewModel(
                            factory = LoginViewModel.Factory(authRepository)
                        )
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
                        val profileViewModel: ProfileViewModel = viewModel(
                            factory = ProfileViewModel.Factory(authRepository)
                        )
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onLogoutSuccess = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Profile.route) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
