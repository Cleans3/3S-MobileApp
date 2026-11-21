package vn.shelftap.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import vn.shelftap.app.ShelfTapApplication
import vn.shelftap.app.ui.screens.password.ChangePasswordScreen
import vn.shelftap.app.ui.screens.password.ChangePasswordViewModel
import vn.shelftap.app.ui.screens.profile.ProfileScreen
import vn.shelftap.app.ui.screens.profile.ProfileViewModel
import vn.shelftap.app.ui.screens.signin.SignInScreen
import vn.shelftap.app.ui.screens.signin.SignInViewModel

@Composable
fun ShelfTapNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val authRepository = ShelfTapApplication.instance.authRepository

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.SignIn.route) {
            val signInViewModel: SignInViewModel = viewModel(
                factory = SignInViewModel.Factory(authRepository)
            )
            SignInScreen(
                viewModel = signInViewModel,
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    navController.navigate(Screen.ChangePassword.createRoute(forced = true)) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
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
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    navController.navigate(Screen.ChangePassword.createRoute(forced = false))
                }
            )
        }

        composable(
            route = Screen.ChangePassword.route,
            arguments = listOf(
                navArgument("forced") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val forced = backStackEntry.arguments?.getBoolean("forced") ?: false
            val changePasswordViewModel: ChangePasswordViewModel = viewModel(
                factory = ChangePasswordViewModel.Factory(authRepository, forced)
            )
            ChangePasswordScreen(
                viewModel = changePasswordViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
