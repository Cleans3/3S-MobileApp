package com.example.libraryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.libraryapp.ui.AppRoot
import com.example.libraryapp.ui.theme.LibraryTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as LibraryApplication).accountRepository
        setContent {
            LibraryTheme {
                AppRoot(repository = repository)
            }
        }
    }
}
