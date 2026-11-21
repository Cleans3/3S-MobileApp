package com.example.libraryloans.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.libraryloans.ui.components.RenewConfirmationDialog
import com.example.libraryloans.ui.theme.NavyPrimary
import com.example.libraryloans.ui.viewmodel.LoansViewModel
import com.example.libraryloans.ui.viewmodel.Screen
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LoansViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val activeLoans by viewModel.activeLoans.collectAsState()
    val loanHistory by viewModel.loanHistory.collectAsState()
    val historySearchQuery by viewModel.historySearchQuery.collectAsState()
    val pendingRenewalLoan by viewModel.pendingRenewalLoan.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Renewal Confirmation Dialog
    pendingRenewalLoan?.let { loan ->
        RenewConfirmationDialog(
            loan = loan,
            onConfirm = { loanId ->
                viewModel.confirmRenewal(loanId)
            },
            onDismiss = {
                viewModel.dismissRenewDialog()
            }
        )
    }

    when (val screen = currentScreen) {
        is Screen.BookDetails -> {
            BookDetailsScreen(
                book = screen.book,
                loan = screen.currentLoan,
                onBackClick = { viewModel.navigateBack() },
                onRenewLoan = { loan -> viewModel.promptRenewLoan(loan) }
            )
        }
        is Screen.Main -> {
            Scaffold(
                topBar = {
                    Column {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalLibrary,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "City Central Library",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            },
                            actions = {
                                Row(
                                    modifier = Modifier.padding(end = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "User Account",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Member details strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Member: Alex Morgan",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• Card #L-83921",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Tab Bar: Active Loans vs Borrowing History
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = NavyPrimary
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Active Loans")
                                        if (activeLoans.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge {
                                                Text(activeLoans.size.toString())
                                            }
                                        }
                                    }
                                }
                            )

                            Tab(
                                selected = selectedTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reading History")
                                    }
                                }
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (selectedTab == 0) {
                        ActiveLoansScreen(
                            loans = activeLoans,
                            onBookClick = { book, loan -> viewModel.openBookDetails(book, loan) },
                            onRenewClick = { loan -> viewModel.promptRenewLoan(loan) }
                        )
                    } else {
                        LoanHistoryScreen(
                            history = loanHistory,
                            searchQuery = historySearchQuery,
                            onSearchQueryChange = { viewModel.updateHistorySearchQuery(it) },
                            onBookClick = { book, loan -> viewModel.openBookDetails(book, loan) }
                        )
                    }
                }
            }
        }
    }
}
