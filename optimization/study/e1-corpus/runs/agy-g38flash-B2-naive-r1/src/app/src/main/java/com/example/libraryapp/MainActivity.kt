package com.example.libraryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.libraryapp.di.ServiceLocator
import com.example.libraryapp.ui.navigation.LibraryNavGraph
import com.example.libraryapp.ui.navigation.Screen
import com.example.libraryapp.ui.theme.LibraryAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = ServiceLocator.provideLibraryRepository()
        val startDestination = if (repository.isLoggedIn()) {
            Screen.Profile.route
        } else {
            Screen.Login.route
        }

        setContent {
            LibraryAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LibraryNavGraph(startDestination = startDestination)
                }
            }
        }
    }
}
