package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vn.shelftap.app.ui.ShelfTapApp
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ShelfTapApplication
        val sessionRepository = app.container.sessionRepository

        lifecycleScope.launch {
            sessionRepository.initialize()
        }

        setContent {
            ShelfTapTheme {
                ShelfTapApp(sessionRepository = sessionRepository)
            }
        }
    }
}
