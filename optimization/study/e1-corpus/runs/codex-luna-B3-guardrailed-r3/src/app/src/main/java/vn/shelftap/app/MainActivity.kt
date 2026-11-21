package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import vn.shelftap.app.data.*
import vn.shelftap.app.ui.ShelfTapApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); val repo = ShelfTapRepository(applicationContext, AuthRepositoryProvider.repository); setContent { MaterialTheme { Surface { ShelfTapApp(repo) } } } }
}
