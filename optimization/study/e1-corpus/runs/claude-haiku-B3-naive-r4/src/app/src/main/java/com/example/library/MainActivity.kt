package com.example.library

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.library.loans.LoansApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as LibraryApplication).loanRepository

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    LoansApp(repository = repository, memberId = CURRENT_MEMBER_ID)
                }
            }
        }
    }

    companion object {
        // Demo member; a real app would take this from its login/session state.
        private const val CURRENT_MEMBER_ID = "member-1"
    }
}
