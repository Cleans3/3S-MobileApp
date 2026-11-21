package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import vn.shelftap.app.AppContainer
import vn.shelftap.app.data.SessionStatus
import vn.shelftap.app.ui.password.ChangePasswordScreen
import vn.shelftap.app.ui.password.ChangePasswordViewModel
import vn.shelftap.app.ui.profile.ProfileScreen
import vn.shelftap.app.ui.profile.ProfileViewModel
import vn.shelftap.app.ui.signin.SignInScreen
import vn.shelftap.app.ui.signin.SignInViewModel

object Routes {
    const val SIGN_IN = "sign_in"
    const val PROFILE = "profile"
    const val CHANGE_PASSWORD = "change_password"
    const val CHANGE_PASSWORD_REQUIRED = "change_password_required"
}

/**
 * Root of the app. The session status decides which screen is shown, so a sign-in, a sign-out,
 * an expired session or a required password change always moves the member to the right place.
 */
@Composable
fun ShelfTapRoot(container: AppContainer) {
    val status by container.sessions.status.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        container.sessions.restore()
    }

    if (status == SessionStatus.Unknown) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()

    LaunchedEffect(status) {
        val target = status.toRoute() ?: return@LaunchedEffect
        if (navController.currentBackStackEntry?.destination?.route != target) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = status.toRoute() ?: Routes.SIGN_IN,
    ) {
        composable(Routes.SIGN_IN) {
            val viewModel: SignInViewModel = viewModel(
                factory = SignInViewModel.factory(container.accounts),
            )
            SignInScreen(viewModel = viewModel)
        }
        composable(Routes.PROFILE) {
            val viewModel: ProfileViewModel = viewModel(
                factory = ProfileViewModel.factory(container.accounts),
            )
            ProfileScreen(
                viewModel = viewModel,
                onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
            )
        }
        composable(Routes.CHANGE_PASSWORD) {
            val viewModel: ChangePasswordViewModel = viewModel(
                factory = ChangePasswordViewModel.factory(container.accounts, required = false),
            )
            ChangePasswordScreen(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() },
            )
        }
        composable(Routes.CHANGE_PASSWORD_REQUIRED) {
            val viewModel: ChangePasswordViewModel = viewModel(
                factory = ChangePasswordViewModel.factory(container.accounts, required = true),
            )
            // Finishing a required change moves on through the session status, not through back navigation.
            ChangePasswordScreen(viewModel = viewModel, onFinished = {})
        }
    }
}

private fun SessionStatus.toRoute(): String? = when (this) {
    SessionStatus.Unknown -> null
    SessionStatus.SignedOut -> Routes.SIGN_IN
    is SessionStatus.SignedIn -> if (mustChangePassword) {
        Routes.CHANGE_PASSWORD_REQUIRED
    } else {
        Routes.PROFILE
    }
}
