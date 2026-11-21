package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.shelftap.app.data.AuthRepository
import vn.shelftap.app.data.AuthState
import vn.shelftap.app.ui.ChangePasswordScreen
import vn.shelftap.app.ui.ChangePasswordViewModel
import vn.shelftap.app.ui.ProfileScreen
import vn.shelftap.app.ui.ProfileViewModel
import vn.shelftap.app.ui.SignInScreen
import vn.shelftap.app.ui.SignInViewModel
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val auth = (application as ShelfTapApp).container.authRepository
        setContent {
            ShelfTapTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(auth)
                }
            }
        }
    }
}

/** Chooses the screen from the auth state. There is no navigation graph; the state is the navigation. */
@Composable
fun AppRoot(auth: AuthRepository) {
    val state by auth.authState.collectAsState()
    LaunchedEffect(Unit) { auth.restore() }

    when (val current = state) {
        AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        AuthState.SignedOut -> {
            val signInVm: SignInViewModel = viewModel(
                factory = viewModelFactory { initializer { SignInViewModel(auth) } },
            )
            SignInScreen(signInVm)
        }

        is AuthState.SignedIn -> if (current.mustChangePassword) {
            // Nothing else is reachable until the password has been changed.
            val changeVm: ChangePasswordViewModel = viewModel(
                factory = viewModelFactory { initializer { ChangePasswordViewModel(auth) } },
            )
            ChangePasswordScreen(viewModel = changeVm, forced = true, onDone = {})
        } else {
            MemberArea(auth)
        }
    }
}

@Composable
private fun MemberArea(auth: AuthRepository) {
    var changingPassword by rememberSaveable { mutableStateOf(false) }
    if (changingPassword) {
        BackHandler { changingPassword = false }
        val changeVm: ChangePasswordViewModel = viewModel(
            factory = viewModelFactory { initializer { ChangePasswordViewModel(auth) } },
        )
        ChangePasswordScreen(
            viewModel = changeVm,
            forced = false,
            onDone = { changingPassword = false },
        )
    } else {
        val profileVm: ProfileViewModel = viewModel(
            factory = viewModelFactory { initializer { ProfileViewModel(auth) } },
        )
        ProfileScreen(
            viewModel = profileVm,
            onChangePassword = { changingPassword = true },
        )
    }
}
