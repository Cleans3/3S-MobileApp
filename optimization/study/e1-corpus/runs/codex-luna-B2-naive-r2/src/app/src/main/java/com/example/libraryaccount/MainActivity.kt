package com.example.libraryaccount

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.libraryaccount.data.AuthRepository
import com.example.libraryaccount.data.LibraryApi
import com.example.libraryaccount.data.Profile
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: AuthRepository
    private lateinit var signInPanel: View
    private lateinit var profilePanel: View
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var submitButton: Button
    private lateinit var progress: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var nameText: TextView
    private lateinit var memberSinceText: TextView
    private lateinit var memberIdText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repository = AuthRepository(this, LibraryApi(BuildConfig.LIBRARY_API_BASE_URL))
        bindViews()
        submitButton.setOnClickListener { signIn() }
        findViewById<Button>(R.id.sign_out_button).setOnClickListener { signOut() }
        if (repository.isSignedIn) loadProfile() else showSignIn()
    }

    private fun bindViews() {
        signInPanel = findViewById(R.id.sign_in_panel)
        profilePanel = findViewById(R.id.profile_panel)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        submitButton = findViewById(R.id.sign_in_button)
        progress = findViewById(R.id.progress)
        errorText = findViewById(R.id.error_text)
        nameText = findViewById(R.id.profile_name)
        memberSinceText = findViewById(R.id.member_since)
        memberIdText = findViewById(R.id.member_id)
    }

    private fun signIn() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.invalid_email)); return
        }
        if (password.isBlank()) { showError(getString(R.string.password_required)); return }
        setLoading(true)
        lifecycleScope.launch {
            try { repository.signIn(email, password); loadProfile() }
            catch (e: Exception) { setLoading(false); showError(e.message ?: getString(R.string.sign_in_failed)) }
        }
    }

    private fun loadProfile() {
        setLoading(true)
        lifecycleScope.launch {
            try { showProfile(repository.profile()) }
            catch (e: Exception) { repository.signOut(); showSignIn(); showError(e.message ?: getString(R.string.profile_failed)) }
            finally { setLoading(false) }
        }
    }

    private fun showProfile(profile: Profile) {
        errorText.visibility = View.GONE
        signInPanel.visibility = View.GONE
        profilePanel.visibility = View.VISIBLE
        nameText.text = profile.name.ifBlank { profile.email }
        memberSinceText.text = profile.memberSince?.let { getString(R.string.member_since_value, it) }
            ?: getString(R.string.member_since_unknown)
        memberIdText.text = getString(R.string.member_id_value, profile.memberId)
    }

    private fun showSignIn() {
        signInPanel.visibility = View.VISIBLE
        profilePanel.visibility = View.GONE
    }

    private fun signOut() { repository.signOut(); passwordInput.text.clear(); showSignIn() }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        submitButton.isEnabled = !loading
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
