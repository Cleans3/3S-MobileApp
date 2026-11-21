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
import com.example.library.loans.data.SampleLoans
import com.example.library.loans.domain.LoanRepository
import java.time.LocalDate

private object LoansRoutes {
    const val LIST = "loans"
    const val DETAIL = "loans/{loanId}"
    const val LOAN_ID_ARG = "loanId"

    fun detail(loanId: String) = "loans/$loanId"
}

/**
 * Entry point of the loans feature. The host app only needs to place this composable;
 * pass a [repository] to supply real data, otherwise sample data is used.
 */
@Composable
fun LoansFeature(repository: LoanRepository? = null) {
    val loanRepository = repository
        ?: remember { InMemoryLoanRepository(SampleLoans.create(LocalDate.now())) }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = LoansRoutes.LIST) {
        composable(LoansRoutes.LIST) {
            val viewModel: LoansListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { LoansListViewModel(loanRepository) }
                },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LoansListScreen(
                state = state,
                onOpenLoan = { loanId -> navController.navigate(LoansRoutes.detail(loanId)) },
            )
        }

        composable(
            route = LoansRoutes.DETAIL,
            arguments = listOf(navArgument(LoansRoutes.LOAN_ID_ARG) { type = NavType.StringType }),
        ) { entry ->
            val loanId = entry.arguments?.getString(LoansRoutes.LOAN_ID_ARG).orEmpty()
            val viewModel: LoanDetailViewModel = viewModel(
                key = "loan-$loanId",
                factory = viewModelFactory {
                    initializer { LoanDetailViewModel(loanId, loanRepository) }
                },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LoanDetailScreen(
                state = state,
                onRenew = viewModel::renew,
                onResultShown = viewModel::onResultShown,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
