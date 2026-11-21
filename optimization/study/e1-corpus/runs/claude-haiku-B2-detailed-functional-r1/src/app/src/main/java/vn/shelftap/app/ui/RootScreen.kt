package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import vn.shelftap.app.data.AuthState
import vn.shelftap.app.data.SessionRepository

/**
 * Picks the top-level screen from the session state:
 * signed out -> sign in; must change password -> forced change; otherwise -> member screens.
 */
@Composable
fun ShelfTapRoot(repo: SessionRepository) {
    val state by repo.state.collectAsState()
    when (val s = state) {
        AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        AuthState.SignedOut -> SignInScreen()
        is AuthState.SignedIn -> {
            if (s.mustChangePassword) {
                // Nothing else is reachable until the password has been changed.
                ChangePasswordScreen(forced = true, onDone = {}, onBack = {})
            } else {
                MemberNavHost()
            }
        }
    }
}

@Composable
private fun MemberNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "profile") {
        composable("profile") {
            ProfileScreen(onChangePassword = { nav.navigate("change_password") })
        }
        composable("change_password") {
            ChangePasswordScreen(
                forced = false,
                onDone = { nav.popBackStack() },
                onBack = { nav.popBackStack() },
            )
        }
    }
}
