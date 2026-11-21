package com.example.library.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.library.loans.data.InMemoryLoanRepository
import com.example.library.loans.ui.LoansRoutes
import com.example.library.loans.ui.loansGraph

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val repository = remember { InMemoryLoanRepository() }

                NavHost(
                    navController = navController,
                    startDestination = LoansRoutes.MY_LOANS,
                ) {
                    loansGraph(
                        navController = navController,
                        repository = repository,
                        memberId = CURRENT_MEMBER_ID,
                    )
                }
            }
        }
    }

    private companion object {
        // Stand-in for the signed-in member until a real login exists.
        const val CURRENT_MEMBER_ID = "member-1"
    }
}
