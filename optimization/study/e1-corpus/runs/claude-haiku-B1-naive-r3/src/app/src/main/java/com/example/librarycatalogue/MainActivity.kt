package com.example.librarycatalogue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.librarycatalogue.ui.CatalogueScreen
import com.example.librarycatalogue.ui.theme.LibraryCatalogueTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibraryCatalogueTheme {
                CatalogueScreen()
            }
        }
    }
}
