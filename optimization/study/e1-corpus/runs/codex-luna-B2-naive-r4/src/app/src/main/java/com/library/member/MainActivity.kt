package com.library.member

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    private lateinit var session: SessionManager
    private lateinit var api: LibraryApi
    private lateinit var content: LinearLayout
    private val navy by lazy { getColor(R.color.library_navy) }
    private val teal by lazy { getColor(R.color.library_teal) }
    private val cream by lazy { getColor(R.color.library_cream) }

    override fun onCreate(state: Bundle?) { super.onCreate(state); session = SessionManager(this); api = LibraryApi(this); showShell(); if (session.token == null) showSignIn() else loadProfile() }

    private fun showShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(cream) }
        val bar = LinearLayout(this).apply { setPadding(24, 18, 16, 18); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(navy) }
        val title = TextView(this).apply { text = "LIBRARY"; textSize = 18f; letterSpacing = .18f; setTextColor(Color.WHITE); setTypeface(null, 1) }
        bar.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val logout = TextView(this).apply { text = "SIGN OUT"; textSize = 12f; setTextColor(Color.WHITE); setOnClickListener { session.clear(); showSignIn() } }
        bar.addView(logout)
        root.addView(bar); content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 30, 24, 24) }; root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
    }

    private fun showSignIn() {
        content.removeAllViews(); val heading = TextView(this).apply { text = "Welcome back"; textSize = 30f; setTextColor(navy); setTypeface(null, 1) }; content.addView(heading)
        content.addView(label("Sign in to your library account"));
        val email = field("Email address", false); val password = field("Password", true); content.addView(email); content.addView(password)
        val error = TextView(this).apply { setTextColor(Color.rgb(190, 50, 50)); visibility = View.GONE; setPadding(0, 16, 0, 0) }; content.addView(error)
        val button = MaterialButton(this).apply { text = "SIGN IN"; setTextColor(Color.WHITE); setBackgroundColor(teal); setOnClickListener {
            val e = email.text.toString().trim(); val p = password.text.toString(); if (e.isBlank() || p.isBlank()) { error.text = "Enter your email and password."; error.visibility = View.VISIBLE; return@setOnClickListener }
            isEnabled = false; text = "SIGNING IN…"; error.visibility = View.GONE; api.signIn(e, p) { result -> runOnUiThread { if (result.value != null) { session.saveToken(result.value!!); loadProfile() } else { isEnabled = true; text = "SIGN IN"; error.text = result.error; error.visibility = View.VISIBLE } } }
        } }; val bp = LinearLayout.LayoutParams(-1, 56); bp.topMargin = 24; content.addView(button, bp)
    }

    private fun loadProfile() { content.removeAllViews(); content.addView(label("MEMBER ACCOUNT")); val loading = label("Loading your profile…"); content.addView(loading); api.member(session.token.orEmpty()) { result -> runOnUiThread { if (result.value != null) showProfile(result.value!!) else { loading.text = result.error ?: "Could not load profile."; val retry = MaterialButton(this).apply { text = "TRY AGAIN"; setOnClickListener { loadProfile() } }; content.addView(retry) } } } }

    private fun showProfile(member: Member) { content.removeAllViews(); val welcome = TextView(this).apply { text = "Your profile"; textSize = 30f; setTextColor(navy); setTypeface(null, 1) }; content.addView(welcome); content.addView(label("Everything looks good. Your account is active.")); val card = MaterialCardView(this).apply { radius = 22f; setCardBackgroundColor(Color.WHITE); setContentPadding(22, 22, 22, 22) }; val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(detail("NAME", member.name)); addView(detail("EMAIL", member.email)); if (member.memberId.isNotBlank()) addView(detail("MEMBER ID", member.memberId)); if (member.joined.isNotBlank()) addView(detail("MEMBER SINCE", member.joined)) }; card.addView(details); val cp = LinearLayout.LayoutParams(-1, -2); cp.topMargin = 26; content.addView(card, cp) }

    private fun detail(label: String, value: String) = TextView(this).apply { text = "$label\n$value"; textSize = 16f; setTextColor(Color.rgb(36,49,58)); setPadding(0, 0, 0, 22) }
    private fun label(text: String) = TextView(this).apply { this.text = text; textSize = 15f; setTextColor(Color.rgb(111,123,131)); setPadding(0, 10, 0, 18) }
    private fun field(hint: String, secret: Boolean) = EditText(this).apply { this.hint = hint; textSize = 16f; setSingleLine(); setPadding(0, 10, 0, 10); if (secret) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; val p = LinearLayout.LayoutParams(-1, 58); p.bottomMargin = 8; layoutParams = p }
}
