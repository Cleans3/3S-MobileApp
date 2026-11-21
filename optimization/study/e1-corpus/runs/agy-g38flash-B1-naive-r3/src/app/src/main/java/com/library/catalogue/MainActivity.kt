package com.library.catalogue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.library.catalogue.ui.catalog.CatalogScreen
import com.library.catalogue.ui.catalog.CatalogViewModel
import com.library.catalogue.ui.theme.LibraryCatalogueTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CatalogViewModel by viewModels {
        val app = application as LibraryApplication
        CatalogViewModel.provideFactory(app.bookRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibraryCatalogueTheme {
                CatalogScreen(viewModel = viewModel)
            }
        }
    }
}
