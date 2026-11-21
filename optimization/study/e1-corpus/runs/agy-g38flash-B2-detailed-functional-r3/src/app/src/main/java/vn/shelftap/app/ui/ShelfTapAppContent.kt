package vn.shelftap.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.shelftap.app.data.repository.AuthRepository
import vn.shelftap.app.data.repository.AuthState
import vn.shelftap.app.ui.navigation.Screen
import vn.shelftap.app.ui.password.ChangePasswordScreen
import vn.shelftap.app.ui.password.ChangePasswordViewModel
import vn.shelftap.app.ui.profile.ProfileScreen
import vn.shelftap.app.ui.profile.ProfileViewModel
import vn.shelftap.app.ui.signin.SignInScreen
import vn.shelftap.app.ui.signin.SignInViewModel

@Composable
fun ShelfTapAppContent(
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val authState by authRepository.authState.collectAsState()
    var userNavigatedToChangePassword by remember { mutableStateOf(false) }

    Crossfade(
        targetState = authState,
        label = "AuthStateCrossfade",
        modifier = modifier.fillMaxSize()
    ) { state ->
        when (state) {
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
                userNavigatedToChangePassword = false
                val signInViewModel: SignInViewModel = viewModel(
                    factory = SignInViewModel.provideFactory(authRepository)
                )
                SignInScreen(viewModel = signInViewModel)
            }

            is AuthState.MustChangePassword -> {
                userNavigatedToChangePassword = false
                val changePasswordViewModel: ChangePasswordViewModel = viewModel(
                    key = "mandatory_password_change",
                    factory = ChangePasswordViewModel.provideFactory(authRepository, isMandatory = true)
                )
                ChangePasswordScreen(
                    viewModel = changePasswordViewModel,
                    onPasswordChangedSuccessfully = {
                        // Repository handles moving to Authenticated state
                    },
                    onBackClick = null // No back navigation when password change is mandatory
                )
            }

            is AuthState.Authenticated -> {
                if (userNavigatedToChangePassword) {
                    val changePasswordViewModel: ChangePasswordViewModel = viewModel(
                        key = "voluntary_password_change",
                        factory = ChangePasswordViewModel.provideFactory(authRepository, isMandatory = false)
                    )
                    ChangePasswordScreen(
                        viewModel = changePasswordViewModel,
                        onPasswordChangedSuccessfully = {
                            userNavigatedToChangePassword = false
                        },
                        onBackClick = {
                            userNavigatedToChangePassword = false
                        }
                    )
                } else {
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModel.provideFactory(authRepository)
                    )
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onChangePasswordClick = {
                            userNavigatedToChangePassword = true
                        }
                    )
                }
            }
        }
    }
}
