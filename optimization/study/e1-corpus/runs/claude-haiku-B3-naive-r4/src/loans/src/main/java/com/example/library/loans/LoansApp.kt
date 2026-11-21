package com.example.library.loans

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.library.loans.domain.LoanRepository
import com.example.library.loans.ui.ActiveLoansScreen
import com.example.library.loans.ui.BookDetailScreen
import com.example.library.loans.ui.BookDetailViewModel
import com.example.library.loans.ui.HistoryScreen
import com.example.library.loans.ui.LoansViewModel

private object LoansRoutes {
    const val ACTIVE = "loans/active"
    const val HISTORY = "loans/history"
    const val ARG_LOAN_ID = "loanId"
    const val DETAIL = "loans/detail/{$ARG_LOAN_ID}"

    fun detail(loanId: String) = "loans/detail/$loanId"
}

/**
 * Entry point of the loans feature. The host app supplies the repository and the
 * signed-in member; everything else (navigation, screens, view models) is owned here.
 */
@Composable
fun LoansApp(
    repository: LoanRepository,
    memberId: String,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val loansViewModel: LoansViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LoansViewModel(repository, memberId) }
        },
    )

    NavHost(
        navController = navController,
        startDestination = LoansRoutes.ACTIVE,
        modifier = modifier,
    ) {
        composable(LoansRoutes.ACTIVE) {
            ActiveLoansScreen(
                viewModel = loansViewModel,
                onBookClick = { loanId -> navController.navigate(LoansRoutes.detail(loanId)) },
                onHistoryClick = { navController.navigate(LoansRoutes.HISTORY) },
            )
        }

        composable(LoansRoutes.HISTORY) {
            HistoryScreen(
                viewModel = loansViewModel,
                onBack = { navController.popBackStack() },
                onBookClick = { loanId -> navController.navigate(LoansRoutes.detail(loanId)) },
            )
        }

        composable(
            route = LoansRoutes.DETAIL,
            arguments = listOf(navArgument(LoansRoutes.ARG_LOAN_ID) { type = NavType.StringType }),
        ) { entry ->
            val loanId = entry.arguments?.getString(LoansRoutes.ARG_LOAN_ID).orEmpty()
            val detailViewModel: BookDetailViewModel = viewModel(
                key = loanId,
                factory = viewModelFactory {
                    initializer { BookDetailViewModel(repository, loanId) }
                },
            )
            BookDetailScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
