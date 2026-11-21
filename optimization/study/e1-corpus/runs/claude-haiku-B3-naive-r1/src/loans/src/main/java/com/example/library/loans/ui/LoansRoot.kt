package com.example.library.loans.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.library.loans.data.InMemoryLoanRepository
import com.example.library.loans.data.LoanRepository

internal object LoansRoutes {
    const val LOANS = "loans"
    const val BOOK = "book/{bookId}"
    fun book(bookId: String) = "book/$bookId"
}

/**
 * Entry point of the loans feature: current loans, reading history and book details.
 * Pass a [repository] to supply your own data source; defaults to an in-memory one.
 */
@Composable
fun LoansRoot(repository: LoanRepository? = null) {
    val repo = remember(repository) { repository ?: InMemoryLoanRepository() }
    val viewModel: LoansViewModel = viewModel(
        factory = viewModelFactory { initializer { LoansViewModel(repo) } },
    )
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = LoansRoutes.LOANS) {
        composable(LoansRoutes.LOANS) {
            LoansScreen(
                viewModel = viewModel,
                onBookClick = { bookId -> navController.navigate(LoansRoutes.book(bookId)) },
            )
        }
        composable(
            route = LoansRoutes.BOOK,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { entry ->
            val bookId = entry.arguments?.getString("bookId").orEmpty()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            BookDetailScreen(
                book = repo.findBook(bookId),
                loan = state.active.firstOrNull { it.book.id == bookId }
                    ?: state.history.firstOrNull { it.book.id == bookId },
                state = state,
                onMessageShown = viewModel::consumeMessage,
                onBack = { navController.popBackStack() },
                onRenew = viewModel::renew,
            )
        }
    }
}
