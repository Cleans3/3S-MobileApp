package com.example.library

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.library.ui.AccountViewModel
import com.example.library.ui.LoginScreen
import com.example.library.ui.ProfileScreen
import com.example.library.ui.theme.LibraryTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LibraryTheme {
                val app = application as LibraryApplication
                val viewModel: AccountViewModel = viewModel(
                    factory = AccountViewModel.factory(app.accountRepository)
                )
                val state by viewModel.state.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (state.isSignedIn) {
                            ProfileScreen(
                                state = state,
                                onRefresh = viewModel::refreshProfile,
                                onSignOut = viewModel::signOut
                            )
                        } else {
                            LoginScreen(
                                state = state,
                                onSignIn = viewModel::signIn
                            )
                        }
                    }
                }
            }
        }
    }
}
