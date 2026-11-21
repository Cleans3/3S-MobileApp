package com.example.libraryloans

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.libraryloans.ui.screens.MainLoansScreen
import com.example.libraryloans.ui.theme.LibraryLoansTheme
import com.example.libraryloans.ui.viewmodel.LoansViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LoansViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibraryLoansTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLoansScreen(viewModel = viewModel)
                }
            }
        }
    }
}
