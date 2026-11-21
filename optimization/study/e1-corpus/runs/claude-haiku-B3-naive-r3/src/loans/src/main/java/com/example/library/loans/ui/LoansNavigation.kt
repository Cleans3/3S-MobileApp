package com.example.library.loans.ui

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.library.loans.data.LoanRepository

object LoansRoutes {
    const val MY_LOANS = "loans"
    const val HISTORY = "loans/history"
    const val ARG_BOOK_ID = "bookId"
    const val BOOK_DETAILS = "loans/book/{$ARG_BOOK_ID}"

    fun bookDetails(bookId: String): String = "loans/book/$bookId"
}

/**
 * Registers the loans screens in the host app's navigation graph.
 * The host supplies the repository and the id of the signed-in member.
 */
fun NavGraphBuilder.loansGraph(
    navController: NavController,
    repository: LoanRepository,
    memberId: String,
) {
    composable(LoansRoutes.MY_LOANS) {
        val viewModel: MyLoansViewModel = viewModel(
            factory = viewModelFactory {
                initializer { MyLoansViewModel(repository, memberId) }
            },
        )
        MyLoansScreen(
            viewModel = viewModel,
            onOpenBook = { navController.navigate(LoansRoutes.bookDetails(it)) },
            onOpenHistory = { navController.navigate(LoansRoutes.HISTORY) },
        )
    }

    composable(LoansRoutes.HISTORY) {
        val viewModel: HistoryViewModel = viewModel(
            factory = viewModelFactory {
                initializer { HistoryViewModel(repository, memberId) }
            },
        )
        LoanHistoryScreen(
            viewModel = viewModel,
            onOpenBook = { navController.navigate(LoansRoutes.bookDetails(it)) },
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = LoansRoutes.BOOK_DETAILS,
        arguments = listOf(navArgument(LoansRoutes.ARG_BOOK_ID) { type = NavType.StringType }),
    ) { entry ->
        val bookId = requireNotNull(entry.arguments?.getString(LoansRoutes.ARG_BOOK_ID))
        val viewModel: BookDetailsViewModel = viewModel(
            key = bookId,
            factory = viewModelFactory {
                initializer { BookDetailsViewModel(repository, bookId) }
            },
        )
        BookDetailsScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
        )
    }
}
