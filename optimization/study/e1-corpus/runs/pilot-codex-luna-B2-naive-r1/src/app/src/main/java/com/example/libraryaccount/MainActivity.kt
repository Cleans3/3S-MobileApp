package com.example.libraryaccount

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.IOException
import java.util.concurrent.Executors

class MainActivity : android.app.Activity() {
    private lateinit var session: SessionStore
    private val api = LibraryApi()
    private val executor = Executors.newSingleThreadExecutor()
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var content: LinearLayout
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionStore(this)
        showShell()
        if (session.token() == null) showSignIn() else loadProfile(session.token()!!)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun showShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(255, 253, 249))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(18), dp(24), dp(18))
            setBackgroundColor(getColor(com.example.libraryaccount.R.color.library_navy))
        }
        val mark = TextView(this).apply {
            text = "▥"
            textSize = 30f
            setTextColor(getColor(R.color.library_gold))
        }
        header.addView(mark, LinearLayout.LayoutParams(dp(42), dp(48)))
        header.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 21f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(30), dp(24), dp(36))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun showSignIn() {
        content.removeAllViews()
        title(getString(R.string.welcome))
        label(getString(R.string.sign_in_subtitle), 16f, R.color.muted_ink)
        spacer(28)
        val email = field(getString(R.string.email), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password = field(getString(R.string.password), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        spacer(8)
        val signIn = button(getString(R.string.sign_in))
        signIn.setOnClickListener {
            val emailValue = email.text.toString().trim()
            val passwordValue = password.text.toString()
            email.error = null
            password.error = null
            when {
                emailValue.isBlank() -> email.error = getString(R.string.required_email)
                !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches() -> email.error = getString(R.string.invalid_email)
                passwordValue.isBlank() -> password.error = getString(R.string.required_password)
                else -> signIn(emailValue, passwordValue, signIn)
            }
        }
        content.addView(signIn)
    }

    private fun signIn(email: String, password: String, button: Button) {
        setBusy(button, true)
        runRequest({ api.signIn(email, password) }, { token ->
            session.saveToken(token)
            loadProfile(token)
        }, { showError(it, button) })
    }

    private fun loadProfile(token: String) {
        content.removeAllViews()
        val progress = ProgressBar(this).apply { isIndeterminate = true }
        content.addView(progress, LinearLayout.LayoutParams(-1, dp(48)))
        label(getString(R.string.loading), 16f, R.color.muted_ink)
        runRequest({ api.profile(token) }, { showProfile(it) }, {
            if ((it as? ApiException)?.statusCode == 401) {
                session.clear()
                showSignIn()
            } else {
                showError(it, null)
            }
        })
    }

    private fun showProfile(profile: MemberProfile) {
        content.removeAllViews()
        title(getString(R.string.profile))
        label(profile.name.ifBlank { "Library member" }, 24f, R.color.ink)
        spacer(24)
        profileRow(getString(R.string.account_email), profile.email)
        profileRow(getString(R.string.member_id), profile.memberId)
        profileRow(getString(R.string.member_since), profile.memberSince)
        spacer(30)
        val signOut = button(getString(R.string.sign_out))
        signOut.setOnClickListener { session.clear(); showSignIn() }
        content.addView(signOut)
    }

    private fun <T> runRequest(request: () -> T, success: (T) -> Unit, failure: (Exception) -> Unit) {
        executor.execute {
            try {
                val result = request()
                main.post { success(result) }
            } catch (exception: Exception) {
                main.post { failure(exception) }
            }
        }
    }

    private fun showError(exception: Exception, button: Button?) {
        button?.let { setBusy(it, false) }
        val message = when {
            exception is ApiException && exception.statusCode in 400..499 -> exception.message ?: getString(R.string.unexpected_error)
            exception is IOException -> getString(R.string.network_error)
            else -> getString(R.string.unexpected_error)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setBusy(button: Button, value: Boolean) {
        busy = value
        button.isEnabled = !value
        button.text = if (value) getString(R.string.loading) else getString(R.string.sign_in)
    }

    private fun title(text: String) = label(text, 30f, R.color.ink, true)
    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(getColor(color))
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
            content.addView(this)
        }
    }

    private fun field(hint: String, type: Int): EditText = EditText(this).apply {
        this.hint = hint
        inputType = type
        setSingleLine(true)
        setPadding(dp(14), 0, dp(14), 0)
        content.addView(this, LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(14) })
    }

    private fun button(text: String): Button = Button(this).apply {
        this.text = text
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(getColor(R.color.library_blue))
        content.addView(this, LinearLayout.LayoutParams(-1, dp(52)))
    }

    private fun profileRow(caption: String, value: String) {
        label(caption, 13f, R.color.muted_ink, true)
        label(value.ifBlank { "—" }, 17f, R.color.ink)
        spacer(10)
    }

    private fun spacer(height: Int) { content.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
