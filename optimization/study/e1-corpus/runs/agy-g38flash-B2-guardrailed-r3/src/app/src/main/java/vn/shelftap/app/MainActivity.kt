package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import vn.shelftap.app.ui.navigation.Screen
import vn.shelftap.app.ui.screens.password.ChangePasswordScreen
import vn.shelftap.app.ui.screens.profile.ProfileScreen
import vn.shelftap.app.ui.screens.signin.SignInScreen
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ShelfTapApp
        val repository = app.repository

        setContent {
            ShelfTapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShelfTapAppContent(repository = repository)
                }
            }
        }
    }
}

@Composable
fun ShelfTapAppContent(
    repository: vn.shelftap.app.data.repository.ShelfTapRepository
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }

    LaunchedEffect(Unit) {
        val hasSession = repository.hasSession()
        if (hasSession) {
            val mustChange = repository.isMustChangePassword()
            if (mustChange) {
                currentScreen = Screen.ChangePassword
            } else {
                currentScreen = Screen.Profile
            }
        } else {
            currentScreen = Screen.SignIn
        }
    }

    when (currentScreen) {
        Screen.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        Screen.SignIn -> {
            SignInScreen(
                repository = repository,
                onNavigateToProfile = {
                    currentScreen = Screen.Profile
                },
                onNavigateToChangePassword = {
                    currentScreen = Screen.ChangePassword
                }
            )
        }
        Screen.Profile -> {
            ProfileScreen(
                repository = repository,
                onSignOutComplete = {
                    currentScreen = Screen.SignIn
                },
                onPasswordChangeRequired = {
                    currentScreen = Screen.ChangePassword
                }
            )
        }
        Screen.ChangePassword -> {
            ChangePasswordScreen(
                repository = repository,
                onPasswordChanged = {
                    currentScreen = Screen.Profile
                },
                onSessionExpired = {
                    currentScreen = Screen.SignIn
                }
            )
        }
    }
}
