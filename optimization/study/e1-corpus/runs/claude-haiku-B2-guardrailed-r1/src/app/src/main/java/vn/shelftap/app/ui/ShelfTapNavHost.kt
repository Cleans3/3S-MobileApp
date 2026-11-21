package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import vn.shelftap.app.data.session.AuthState
import vn.shelftap.app.data.session.SessionRepository
import vn.shelftap.app.ui.changepassword.ChangePasswordScreen
import vn.shelftap.app.ui.changepassword.ChangePasswordViewModel
import vn.shelftap.app.ui.profile.ProfileScreen
import vn.shelftap.app.ui.profile.ProfileViewModel
import vn.shelftap.app.ui.signin.SignInScreen
import vn.shelftap.app.ui.signin.SignInViewModel

private object Routes {
    const val SIGN_IN = "sign_in"
    const val PROFILE = "profile"
    const val CHANGE_PASSWORD = "change_password"
}

@Composable
internal fun ShelfTapRoot(session: SessionRepository) {
    val auth by session.authState.collectAsStateWithLifecycle()
    if (auth == AuthState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ShelfTapNavHost(session = session, auth = auth)
    }
}

@Composable
private fun ShelfTapNavHost(session: SessionRepository, auth: AuthState) {
    val nav = rememberNavController()
    val startRoute = remember { routeFor(auth) }

    // Follow every change of the session: sign-in, sign-out, a forced password change, and so on.
    LaunchedEffect(auth) {
        val target = routeFor(auth)
        if (nav.currentDestination?.route != target) {
            nav.navigate(target) {
                popUpTo(nav.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = nav, startDestination = startRoute) {
        composable(Routes.SIGN_IN) {
            val viewModel: SignInViewModel = viewModel(factory = SignInViewModel.factory(session))
            SignInScreen(viewModel = viewModel)
        }
        composable(Routes.PROFILE) {
            val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(session))
            ProfileScreen(
                viewModel = viewModel,
                onChangePassword = { nav.navigate(Routes.CHANGE_PASSWORD) },
            )
        }
        composable(Routes.CHANGE_PASSWORD) {
            val viewModel: ChangePasswordViewModel =
                viewModel(factory = ChangePasswordViewModel.factory(session))
            ChangePasswordScreen(
                viewModel = viewModel,
                onDone = { nav.popBackStack() },
            )
        }
    }
}

private fun routeFor(auth: AuthState): String = when (auth) {
    AuthState.Loading, AuthState.SignedOut -> Routes.SIGN_IN
    is AuthState.SignedIn -> if (auth.mustChangePassword) Routes.CHANGE_PASSWORD else Routes.PROFILE
}
