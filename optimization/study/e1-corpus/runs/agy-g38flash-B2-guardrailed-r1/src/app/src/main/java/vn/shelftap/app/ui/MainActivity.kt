package vn.shelftap.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Validate session on app launch in background coroutine (Rule 16)
        lifecycleScope.launch {
            ShelfTapApp.instance.authRepository.checkInitialSession()
        }

        setContent {
            ShelfTapTheme {
                ShelfTapNavHost()
            }
        }
    }
}
