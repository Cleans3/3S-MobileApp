package com.example.librarycatalogue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.librarycatalogue.ui.BookListScreen
import com.example.librarycatalogue.ui.BookListViewModel
import com.example.librarycatalogue.ui.theme.LibraryTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BookListViewModel by viewModels {
        BookListViewModel.Factory((application as LibraryApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibraryTheme {
                BookListScreen(viewModel = viewModel)
            }
        }
    }
}
