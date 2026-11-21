package vn.shelftap.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.AppContainer
import vn.shelftap.app.data.local.AuthState
import vn.shelftap.app.ui.screens.password.ChangePasswordScreen
import vn.shelftap.app.ui.screens.password.ChangePasswordViewModel
import vn.shelftap.app.ui.screens.profile.ProfileScreen
import vn.shelftap.app.ui.screens.profile.ProfileViewModel
import vn.shelftap.app.ui.screens.signin.SignInScreen
import vn.shelftap.app.ui.screens.signin.SignInViewModel

object Destinations {
    const val SIGN_IN = "sign_in"
    const val PROFILE = "profile"
    const val CHANGE_PASSWORD = "change_password/{mandatory}"

    fun changePasswordRoute(mandatory: Boolean): String = "change_password/$mandatory"
}

@Composable
fun AppNavigation(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val authState by container.sessionManager.authState.collectAsState()

    when (val state = authState) {
        is AuthState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is AuthState.Unauthenticated -> {
            val signInViewModel: SignInViewModel = viewModel(
                factory = SignInViewModel.Factory(container.authRepository)
            )
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Destinations.SIGN_IN,
                modifier = modifier
            ) {
                composable(Destinations.SIGN_IN) {
                    SignInScreen(
                        viewModel = signInViewModel,
                        onNavigateToProfile = {
                            // Handled by AuthState transition
                        },
                        onNavigateToChangePassword = {
                            // Handled by AuthState transition
                        }
                    )
                }
            }
        }

        is AuthState.Authenticated -> {
            val navController = rememberNavController()
            val startDestination = if (state.mustChangePassword) {
                Destinations.changePasswordRoute(mandatory = true)
            } else {
                Destinations.PROFILE
            }

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = modifier
            ) {
                composable(Destinations.PROFILE) {
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModel.Factory(
                            container.memberRepository,
                            container.authRepository
                        )
                    )
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onNavigateToSignIn = {
                            container.authRepository.clearLocalSession()
                        },
                        onNavigateToChangePassword = {
                            navController.navigate(Destinations.changePasswordRoute(mandatory = false))
                        }
                    )
                }

                composable(
                    route = Destinations.CHANGE_PASSWORD,
                    arguments = listOf(
                        navArgument("mandatory") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val isMandatory = backStackEntry.arguments?.getBoolean("mandatory") ?: false
                    val changePasswordViewModel: ChangePasswordViewModel = viewModel(
                        factory = ChangePasswordViewModel.Factory(
                            container.memberRepository,
                            isMandatory = isMandatory
                        )
                    )
                    ChangePasswordScreen(
                        viewModel = changePasswordViewModel,
                        onNavigateToProfile = {
                            if (isMandatory) {
                                // Once mandatory password change succeeds, session authState updates
                                // and user is at Profile
                                navController.navigate(Destinations.PROFILE) {
                                    popUpTo(Destinations.changePasswordRoute(true)) {
                                        inclusive = true
                                    }
                                }
                            } else {
                                navController.popBackStack()
                            }
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToSignIn = {
                            container.authRepository.clearLocalSession()
                        }
                    )
                }
            }
        }
    }
}
