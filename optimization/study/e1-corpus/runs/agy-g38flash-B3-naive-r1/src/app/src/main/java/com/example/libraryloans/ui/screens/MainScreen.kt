package com.example.libraryloans.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.libraryloans.ui.components.BookDetailSheet
import com.example.libraryloans.ui.components.RenewConfirmDialog
import com.example.libraryloans.ui.theme.NavyPrimary
import com.example.libraryloans.ui.theme.Slate200
import com.example.libraryloans.ui.theme.Slate600
import com.example.libraryloans.ui.viewmodel.LoanTab
import com.example.libraryloans.ui.viewmodel.LoansViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LoansViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "City Library Loans",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Alex Morgan • Card #LIB-884210",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search borrowed or past books...",
                            fontSize = 14.sp,
                            color = Slate600
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Slate600
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Slate600
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = NavyPrimary,
                        unfocusedIndicatorColor = Slate200
                    ),
                    singleLine = true
                )
            }

            // Tabs for Active Loans and Borrowing History
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NavyPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                        color = NavyPrimary
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == LoanTab.ACTIVE,
                    onClick = { viewModel.setTab(LoanTab.ACTIVE) },
                    text = {
                        BadgedBox(
                            badge = {
                                if (uiState.totalActiveCount > 0) {
                                    Badge(containerColor = NavyPrimary) {
                                        Text(uiState.totalActiveCount.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Active Loans",
                                fontWeight = if (uiState.selectedTab == LoanTab.ACTIVE) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                )

                Tab(
                    selected = uiState.selectedTab == LoanTab.HISTORY,
                    onClick = { viewModel.setTab(LoanTab.HISTORY) },
                    text = {
                        BadgedBox(
                            badge = {
                                if (uiState.totalHistoryCount > 0) {
                                    Badge(containerColor = Slate600) {
                                        Text(uiState.totalHistoryCount.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Read History",
                                fontWeight = if (uiState.selectedTab == LoanTab.HISTORY) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                )
            }

            // Tab Content
            when (uiState.selectedTab) {
                LoanTab.ACTIVE -> {
                    ActiveLoansScreen(
                        loans = uiState.activeLoans,
                        overdueCount = uiState.overdueCount,
                        dueSoonCount = uiState.dueSoonCount,
                        onRenewLoan = { viewModel.requestRenewal(it) },
                        onOpenBookDetails = { viewModel.openBookDetails(it.book) }
                    )
                }
                LoanTab.HISTORY -> {
                    LoanHistoryScreen(
                        historyLoans = uiState.loanHistory,
                        totalHistoryCount = uiState.totalHistoryCount,
                        onOpenBookDetails = { viewModel.openBookDetails(it.book) }
                    )
                }
            }
        }
    }

    // Renewal Confirmation Dialog
    uiState.pendingRenewalLoan?.let { loanWithBook ->
        RenewConfirmDialog(
            item = loanWithBook,
            onConfirm = { loanId -> viewModel.confirmRenewal(loanId) },
            onDismiss = { viewModel.dismissRenewalDialog() }
        )
    }

    // Book Details Bottom Sheet
    uiState.selectedBook?.let { book ->
        BookDetailSheet(
            book = book,
            loansForBook = viewModel.getLoansForBook(book.id),
            onDismiss = { viewModel.closeBookDetails() },
            onRenewLoan = { loanWithBook ->
                viewModel.requestRenewal(loanWithBook)
            }
        )
    }
}
