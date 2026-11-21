package com.example.libraryapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.libraryapp.data.AccountRepository
import com.example.libraryapp.ui.auth.LoginScreen
import com.example.libraryapp.ui.profile.ProfileScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Exposes the sign-in state: null while it is being restored, then true or false. */
class SessionViewModel(repository: AccountRepository) : ViewModel() {

    val signedIn: StateFlow<Boolean?> = repository.isSignedIn
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/** Chooses between the sign-in screen and the member's profile. */
@Composable
fun AppRoot(repository: AccountRepository) {
    val sessionViewModel: SessionViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SessionViewModel(repository) }
        }
    )
    val signedIn by sessionViewModel.signedIn.collectAsStateWithLifecycle()

    when (signedIn) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        false -> LoginScreen(repository = repository)
        true -> ProfileScreen(repository = repository)
    }
}
