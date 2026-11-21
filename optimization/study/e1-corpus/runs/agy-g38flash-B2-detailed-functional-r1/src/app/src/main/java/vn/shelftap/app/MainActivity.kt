package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import vn.shelftap.app.ui.navigation.Screen
import vn.shelftap.app.ui.navigation.ShelfTapNavGraph
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = ShelfTapApplication.instance.sessionManager
        val authRepository = ShelfTapApplication.instance.authRepository

        // Determine initial destination based on stored session:
        // A member who signed in stays signed in when the app is closed and reopened.
        // If mustChangePassword is true after sign-in, member must change password before anything else.
        val startDestination = when {
            !authRepository.isLoggedIn() -> Screen.SignIn.route
            authRepository.mustChangePassword() -> Screen.ChangePassword.createRoute(forced = true)
            else -> Screen.Profile.route
        }

        setContent {
            ShelfTapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // React to global session expiration events (e.g., refresh token failure)
                    LaunchedEffect(sessionManager) {
                        sessionManager.sessionExpiredEvent.collect {
                            navController.navigate(Screen.SignIn.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    ShelfTapNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
