package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vn.shelftap.app.ui.navigation.AppNavGraph
import vn.shelftap.app.ui.theme.ShelfTapTheme

/**
 * Main Activity hosting the Jetpack Compose Material 3 UI for ShelfTap.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ShelfTapApplication
        val repository = app.repository

        setContent {
            ShelfTapTheme {
                AppNavGraph(repository = repository)
            }
        }
    }
}
