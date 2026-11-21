package com.example.library

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.content.Context
import android.text.InputType
import android.widget.*
import com.example.library.data.ApiException
import com.example.library.data.LibraryApi
import com.example.library.data.MemberProfile
import com.example.library.data.SessionStore
import java.io.IOException
import java.util.concurrent.Executors

class MainActivity : android.app.Activity() {
    private lateinit var container: FrameLayout
    private lateinit var session: SessionStore
    private val api = LibraryApi()
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.screen_container)
        session = SessionStore(this)
        if (session.token == null) showSignIn() else loadProfile(session.token!!)
    }

    private fun showSignIn(message: String? = null) {
        container.removeAllViews()
        val root = vertical(28, 32, 28, 24)
        root.gravity = Gravity.CENTER_VERTICAL

        val brand = text("THE CITY LIBRARY", 13f, R.color.gold).apply { letterSpacing = .18f }
        root.addView(brand, lpWrap(0))
        root.addView(space(12), lpWrap(0))
        root.addView(text(getString(R.string.sign_in_title), 30f, R.color.ink, true), lpWrap(0))
        root.addView(text(getString(R.string.sign_in_subtitle), 16f, R.color.muted_ink), lp(0, 8, -1, -2))

        val email = field(getString(R.string.email), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password = field(getString(R.string.password), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        root.addView(email, lp(0, 36, -1, 54))
        root.addView(password, lp(0, 12, -1, 54))
        val error = text(message.orEmpty(), 13f, R.color.danger).apply { visibility = if (message == null) View.GONE else View.VISIBLE }
        root.addView(error, lp(0, 10, -1, -2))
        val button = Button(this).apply {
            text = getString(R.string.sign_in)
            textSize = 15f
            setTextColor(Color.WHITE)
            background = resources.getDrawable(R.drawable.bg_primary_button, theme)
            isAllCaps = false
            setOnClickListener {
                val emailValue = email.text.toString().trim()
                val passwordValue = password.text.toString()
                if (emailValue.isBlank()) { email.error = getString(R.string.required_email); return@setOnClickListener }
                if (passwordValue.isBlank()) { password.error = getString(R.string.required_password); return@setOnClickListener }
                button.isEnabled = false
                button.text = getString(R.string.loading)
                error.visibility = View.GONE
                signIn(emailValue, passwordValue) { success, failure ->
                    if (success != null) loadProfile(success) else {
                        button.isEnabled = true
                        button.text = getString(R.string.sign_in)
                        error.text = failure
                        error.visibility = View.VISIBLE
                    }
                }
            }
        }
        root.addView(button, lp(0, 24, -1, 52))
        container.addView(root, FrameLayout.LayoutParams(-1, -1))
    }

    private fun signIn(email: String, password: String, done: (String?, String?) -> Unit) {
        executor.execute {
            try {
                val token = api.signIn(email, password)
                session.save(token)
                runOnUiThread { done(token, null) }
            } catch (e: ApiException) {
                runOnUiThread { done(null, if (e.statusCode == 401) getString(R.string.invalid_credentials) else getString(R.string.network_error)) }
            } catch (_: Exception) { runOnUiThread { done(null, getString(R.string.network_error)) } }
        }
    }

    private fun loadProfile(token: String) {
        showLoading()
        executor.execute {
            try {
                val profile = api.profile(token)
                runOnUiThread { showProfile(profile) }
            } catch (e: ApiException) {
                session.clear()
                runOnUiThread { showSignIn(getString(R.string.profile_error)) }
            } catch (_: Exception) {
                runOnUiThread { showSignIn(getString(R.string.profile_error)) }
            }
        }
    }

    private fun showLoading() {
        container.removeAllViews()
        val progress = ProgressBar(this)
        container.addView(progress, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
    }

    private fun showProfile(profile: MemberProfile) {
        container.removeAllViews()
        val root = vertical(24, 30, 24, 24)
        root.addView(text(getString(R.string.profile_title), 30f, R.color.ink, true), lpWrap(0))
        root.addView(text(getString(R.string.profile_subtitle), 16f, R.color.muted_ink), lp(0, 6, -1, -2))
        val card = vertical(20, 20, 20, 20).apply { background = resources.getDrawable(R.drawable.bg_card, theme) }
        card.addView(text(profile.name, 23f, R.color.ink, true), lpWrap(0))
        card.addView(text(profile.email, 15f, R.color.muted_ink), lp(0, 5, -1, -2))
        card.addView(text(getString(R.string.member_details), 13f, R.color.navy, true), lp(0, 28, -1, -2))
        card.addView(text(getString(R.string.membership_number), 12f, R.color.muted_ink), lp(0, 16, -1, -2))
        card.addView(text(profile.membershipNumber.ifBlank { "—" }, 16f, R.color.ink), lp(0, 2, -1, -2))
        card.addView(text(getString(R.string.member_since), 12f, R.color.muted_ink), lp(0, 16, -1, -2))
        card.addView(text(profile.memberSince.ifBlank { "—" }, 16f, R.color.ink), lp(0, 2, -1, -2))
        root.addView(card, lp(0, 28, -1, -2))
        val signOut = Button(this).apply {
            text = getString(R.string.sign_out); isAllCaps = false; textSize = 15f
            setTextColor(resources.getColor(R.color.navy, theme))
            setOnClickListener { session.clear(); showSignIn() }
        }
        root.addView(signOut, lp(0, 18, -1, 52))
        container.addView(root, FrameLayout.LayoutParams(-1, -1))
    }

    private fun field(hint: String, type: Int) = EditText(this).apply {
        this.hint = hint; textSize = 16f; inputType = type; setSingleLine(true)
        background = resources.getDrawable(R.drawable.bg_field, theme)
        setPadding(14, 0, 14, 0)
    }
    private fun vertical(l: Int, t: Int, r: Int, b: Int) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(l), dp(t), dp(r), dp(b)) }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply { text = value; textSize = size; setTextColor(resources.getColor(color, theme)); if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD) }
    private fun space(height: Int) = Space(this).apply { minimumHeight = dp(height) }
    private fun lp(w: Int, top: Int, width: Int, height: Int) = LinearLayout.LayoutParams(if (width == -1) -1 else w, height).apply { topMargin = dp(top) }
    private fun lpWrap(top: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
