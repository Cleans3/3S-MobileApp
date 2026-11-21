package com.example.libcatalogue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.libcatalogue.ui.CatalogueScreen
import com.example.libcatalogue.ui.CatalogueViewModel
import com.example.libcatalogue.ui.theme.LibraryTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CatalogueViewModel by viewModels { CatalogueViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibraryTheme {
                CatalogueScreen(viewModel = viewModel)
            }
        }
    }
}
