package com.example.libraryaccount

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.libraryaccount.data.ApiResult
import com.example.libraryaccount.data.LibraryApi
import com.example.libraryaccount.data.MemberProfile
import com.example.libraryaccount.data.SessionStore
import com.example.libraryaccount.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionStore
    private val api by lazy { LibraryApi(BuildConfig.LIBRARY_API_URL) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionStore(this)
        binding.signInButton.setOnClickListener { signIn() }
        binding.signOutButton.setOnClickListener { signOut() }
        if (session.token.isNullOrBlank()) showSignIn() else loadProfile(session.token!!)
    }

    private fun signIn() {
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        binding.emailLayout.error = if (email.isBlank()) getString(R.string.email_required) else null
        binding.passwordLayout.error = if (password.isBlank()) getString(R.string.password_required) else null
        if (email.isBlank() || password.isBlank()) return
        setLoading(true)
        lifecycleScope.launch {
            when (val result = api.signIn(email, password)) {
                is ApiResult.Success -> { session.token = result.value; loadProfile(result.value) }
                is ApiResult.Failure -> { setLoading(false); showError(result.message) }
            }
        }
    }

    private fun loadProfile(token: String) {
        setLoading(true)
        lifecycleScope.launch {
            when (val result = api.profile(token)) {
                is ApiResult.Success -> showProfile(result.value)
                is ApiResult.Failure -> {
                    setLoading(false)
                    if (result.unauthorized) { session.clear(); showSignIn(); showError(getString(R.string.session_expired)) }
                    else showError(result.message)
                }
            }
        }
    }

    private fun showProfile(profile: MemberProfile) {
        setLoading(false)
        binding.signInPanel.visibility = View.GONE
        binding.profilePanel.visibility = View.VISIBLE
        binding.profileName.text = profile.name
        binding.profileEmail.text = profile.email
        binding.memberId.text = profile.memberId
        binding.membershipStatus.text = profile.membership
        binding.joinedDate.text = profile.joinedDate
    }

    private fun signOut() { session.clear(); binding.passwordInput.text?.clear(); showSignIn() }
    private fun showSignIn() { setLoading(false); binding.signInPanel.visibility = View.VISIBLE; binding.profilePanel.visibility = View.GONE }
    private fun setLoading(loading: Boolean) { binding.progress.visibility = if (loading) View.VISIBLE else View.GONE; binding.signInButton.isEnabled = !loading }
    private fun showError(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
