package vn.shelftap.lab

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

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
            output.text = try {
                CatalogClient().fetchCatalog()
            } catch (e: java.io.IOException) {
                Log.e("ShelfTap", "catalog failed", e)
                e.toString()
            }
        }
    }
}
