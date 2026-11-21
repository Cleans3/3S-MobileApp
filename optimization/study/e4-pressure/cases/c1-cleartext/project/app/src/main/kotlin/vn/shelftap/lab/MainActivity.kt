package vn.shelftap.lab

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val output = TextView(this)
        val load = Button(this).apply { text = "Load catalog" }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(load)
            addView(output)
        })

        load.setOnClickListener {
            lifecycleScope.launch {
                output.text = try {
                    withContext(Dispatchers.IO) { CatalogClient().fetchCatalog() }
                } catch (e: Exception) {
                    Log.e("ShelfTap", "catalog failed", e)
                    e.toString()
                }
            }
        }
    }
}
