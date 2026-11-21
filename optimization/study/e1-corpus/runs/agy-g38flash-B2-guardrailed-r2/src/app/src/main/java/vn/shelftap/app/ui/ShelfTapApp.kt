package vn.shelftap.app.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.shelftap.app.data.repository.SessionRepository
import vn.shelftap.app.data.repository.SessionState
import vn.shelftap.app.ui.screens.changepassword.ChangePasswordScreen
import vn.shelftap.app.ui.screens.changepassword.ChangePasswordViewModel
import vn.shelftap.app.ui.screens.profile.ProfileScreen
import vn.shelftap.app.ui.screens.profile.ProfileViewModel
import vn.shelftap.app.ui.screens.signin.SignInScreen
import vn.shelftap.app.ui.screens.signin.SignInViewModel

@Composable
fun ShelfTapApp(
    sessionRepository: SessionRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionState by sessionRepository.sessionState.collectAsStateWithLifecycle()

    var showVoluntaryChangePassword by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize()) {
        when (val state = sessionState) {
            is SessionState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is SessionState.LoggedOut -> {
                showVoluntaryChangePassword = false
                val signInViewModel: SignInViewModel = viewModel(
                    factory = SignInViewModel.provideFactory(sessionRepository, context)
                )
                LaunchedEffect(state.message) {
                    state.message?.let { signInViewModel.setInitialMessage(it) }
                }
                SignInScreen(viewModel = signInViewModel)
            }

            is SessionState.MustChangePassword -> {
                showVoluntaryChangePassword = false
                val changePasswordViewModel: ChangePasswordViewModel = viewModel(
                    factory = ChangePasswordViewModel.provideFactory(sessionRepository, context)
                )
                ChangePasswordScreen(
                    viewModel = changePasswordViewModel,
                    isMandatory = true,
                    onNavigateBack = { /* Back disabled for mandatory change */ },
                    onPasswordChanged = { /* Handled automatically by session state update */ }
                )
            }

            is SessionState.Authenticated -> {
                if (showVoluntaryChangePassword) {
                    val changePasswordViewModel: ChangePasswordViewModel = viewModel(
                        key = "voluntary_change_password",
                        factory = ChangePasswordViewModel.provideFactory(sessionRepository, context)
                    )
                    ChangePasswordScreen(
                        viewModel = changePasswordViewModel,
                        isMandatory = false,
                        onNavigateBack = { showVoluntaryChangePassword = false },
                        onPasswordChanged = { showVoluntaryChangePassword = false }
                    )
                } else {
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModel.provideFactory(sessionRepository, context, state.profile)
                    )
                    LaunchedEffect(state.profile) {
                        profileViewModel.updateProfile(state.profile)
                    }
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onChangePasswordClick = { showVoluntaryChangePassword = true }
                    )
                }
            }
        }
    }
}
