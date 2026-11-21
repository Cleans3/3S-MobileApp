package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vn.shelftap.app.ui.navigation.ShelfTapNavHost
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ShelfTapApplication

        setContent {
            ShelfTapTheme {
                ShelfTapNavHost(
                    repository = app.repository,
                    dispatchers = app.dispatchers
                )
            }
        }
    }
}
