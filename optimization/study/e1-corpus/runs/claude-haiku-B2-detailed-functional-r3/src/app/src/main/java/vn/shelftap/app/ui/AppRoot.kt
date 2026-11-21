package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import vn.shelftap.app.R

private const val ROUTE_LOADING = "loading"
private const val ROUTE_UNAVAILABLE = "unavailable"
private const val ROUTE_SIGN_IN = "sign_in"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_CHANGE_PASSWORD = "change_password"
private const val ROUTE_FORCED_PASSWORD = "forced_password"

/**
 * Hosts the navigation graph. Whenever the session kind changes, the graph is reset to the
 * matching root screen, so a forced password change cannot be bypassed.
 */
@Composable
fun AppRoot(session: SessionViewModel) {
    val state by session.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(state::class) {
        val target = when (state) {
            SessionState.Loading -> ROUTE_LOADING
            SessionState.SignedOut -> ROUTE_SIGN_IN
            SessionState.PasswordChangeRequired -> ROUTE_FORCED_PASSWORD
            is SessionState.SignedIn -> ROUTE_PROFILE
            is SessionState.Unavailable -> ROUTE_UNAVAILABLE
        }
        if (navController.currentBackStackEntry?.destination?.route != target) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_LOADING) {
        composable(ROUTE_LOADING) {
            CenteredContent {
                CircularProgressIndicator()
            }
        }
        composable(ROUTE_UNAVAILABLE) {
            val message = (state as? SessionState.Unavailable)?.message.orEmpty()
            CenteredContent {
                Text(text = message, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { session.restore() }, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.button_retry))
                }
            }
        }
        composable(ROUTE_SIGN_IN) {
            SignInScreen(session)
        }
        composable(ROUTE_PROFILE) {
            (state as? SessionState.SignedIn)?.let { signedIn ->
                ProfileScreen(
                    member = signedIn.member,
                    onChangePassword = { navController.navigate(ROUTE_CHANGE_PASSWORD) },
                    onSignOut = { session.signOut() },
                )
            }
        }
        composable(ROUTE_CHANGE_PASSWORD) {
            ChangePasswordScreen(
                session = session,
                forced = false,
                onDone = { navController.popBackStack() },
            )
        }
        composable(ROUTE_FORCED_PASSWORD) {
            ChangePasswordScreen(
                session = session,
                forced = true,
                onDone = {},
            )
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}
