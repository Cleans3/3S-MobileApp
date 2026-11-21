package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import vn.shelftap.app.ui.ShelfTapAppContent
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ShelfTapApp
        val authRepository = app.authRepository

        setContent {
            ShelfTapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ShelfTapAppContent(authRepository = authRepository)
                }
            }
        }
    }
}
