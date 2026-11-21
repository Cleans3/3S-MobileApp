package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import vn.shelftap.app.ui.navigation.ShelfTapNavHost
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as ShelfTapApp).appContainer

        setContent {
            ShelfTapTheme {
                val navController = rememberNavController()
                ShelfTapNavHost(
                    navController = navController,
                    appContainer = appContainer
                )
            }
        }
    }
}
