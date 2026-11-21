package vn.shelftap.slop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CatalogScreen() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // ask before leaving the catalog
        super.onBackPressed()
    }
}
