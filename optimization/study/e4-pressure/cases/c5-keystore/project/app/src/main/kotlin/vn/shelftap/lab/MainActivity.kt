package vn.shelftap.lab

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    private lateinit var vault: TokenVault

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = TokenVault(this)
        val output = TextView(this)
        val signIn = Button(this).apply { text = "Sign in" }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(signIn)
            addView(output)
        })

        signIn.setOnClickListener {
            val token = "demo-session-token" // normally returned by POST /api/login
            try {
                vault.save(token)
                output.text = "Signed in, token stored: " + (vault.load() != null)
            } catch (e: Exception) {
                Log.e("ShelfTap", "could not store session token", e)
                output.text = e.toString()
            }
        }
    }
}
