package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.data.repository.AuthState
import vn.shelftap.app.ui.auth.SignInScreen
import vn.shelftap.app.ui.auth.SignInViewModel
import vn.shelftap.app.ui.password.ChangePasswordScreen
import vn.shelftap.app.ui.password.ChangePasswordViewModel
import vn.shelftap.app.ui.profile.ProfileScreen
import vn.shelftap.app.ui.profile.ProfileViewModel

@Composable
fun ShelfTapNavHost(
    modifier: Modifier = Modifier
) {
    val authRepository = ShelfTapApp.instance.authRepository
    val authState by authRepository.authState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (authState) {
            is AuthState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            is AuthState.Unauthenticated -> {
                val signInViewModel: SignInViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return SignInViewModel(authRepository) as T
                        }
                    }
                )
                SignInScreen(viewModel = signInViewModel)
            }

            is AuthState.RequirePasswordChange -> {
                val changePasswordViewModel: ChangePasswordViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return ChangePasswordViewModel(authRepository) as T
                        }
                    }
                )
                ChangePasswordScreen(viewModel = changePasswordViewModel)
            }

            is AuthState.Authenticated -> {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return ProfileViewModel(authRepository) as T
                        }
                    }
                )
                ProfileScreen(viewModel = profileViewModel)
            }
        }
    }
}
