package com.example.libraryloans.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.example.libraryloans.model.Book
import com.example.libraryloans.model.Loan
import com.example.libraryloans.model.LoanHistory
import com.example.libraryloans.ui.components.HistoryItemCard
import com.example.libraryloans.ui.components.LoanItemCard
import com.example.libraryloans.ui.components.RenewConfirmDialog
import com.example.libraryloans.ui.theme.AmberSecondary
import com.example.libraryloans.ui.theme.NavyPrimary
import com.example.libraryloans.ui.theme.StatusRed
import com.example.libraryloans.ui.theme.TextMuted
import com.example.libraryloans.ui.theme.TextSecondary
import com.example.libraryloans.ui.viewmodel.LoanFilter
import com.example.libraryloans.ui.viewmodel.LoansViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLoansScreen(
    viewModel: LoansViewModel
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val member by viewModel.member.collectAsState()
    val activeLoans by viewModel.filteredActiveLoans.collectAsState()
    val history by viewModel.filteredHistory.collectAsState()
    val totalActiveCount by viewModel.totalActiveLoansCount.collectAsState()
    val dueSoonCount by viewModel.dueSoonCount.collectAsState()
    val historyCount by viewModel.historyCount.collectAsState()
    val loanPendingRenewal by viewModel.loanPendingRenewal.collectAsState()
    val selectedBook by viewModel.selectedBook.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // If book detail is selected, display detail screen
    if (selectedBook != null) {
        val currentBook = selectedBook!!
        BookDetailScreen(
            book = currentBook,
            activeLoan = viewModel.getActiveLoanForBook(currentBook.id),
            history = viewModel.getHistoryForBook(currentBook.id),
            onBackClick = { viewModel.closeBookDetail() },
            onRenewClick = { loan -> viewModel.requestRenewal(loan) }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "City Central Library",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Loans & Reading History",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Member Card Header
                MemberHeaderCard(
                    memberName = member.name,
                    cardNumber = member.cardNumber,
                    activeCount = totalActiveCount,
                    dueSoonCount = dueSoonCount,
                    historyCount = historyCount
                )

                // Tab selection: Active Loans vs Loan History
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Borrowed ($totalActiveCount)",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HistoryEdu,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "History ($historyCount)",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }

                // Search & Filter controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by title, author, or genre...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Filter chips for active loans
                    if (selectedTab == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = activeFilter == LoanFilter.ALL,
                                onClick = { viewModel.onFilterChanged(LoanFilter.ALL) },
                                label = { Text("All Active") }
                            )
                            FilterChip(
                                selected = activeFilter == LoanFilter.DUE_SOON,
                                onClick = { viewModel.onFilterChanged(LoanFilter.DUE_SOON) },
                                label = { Text("Due Soon") }
                            )
                            FilterChip(
                                selected = activeFilter == LoanFilter.OVERDUE,
                                onClick = { viewModel.onFilterChanged(LoanFilter.OVERDUE) },
                                label = { Text("Overdue") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatusRed.copy(alpha = 0.15f),
                                    selectedLabelColor = StatusRed
                                )
                            )
                        }
                    }
                }

                // List content
                if (selectedTab == 0) {
                    ActiveLoansList(
                        loans = activeLoans,
                        onBookClick = { loan -> viewModel.openBookDetail(loan.book) },
                        onRenewClick = { loan -> viewModel.requestRenewal(loan) }
                    )
                } else {
                    HistoryList(
                        history = history,
                        onBookClick = { hist -> viewModel.openBookDetail(hist.book) }
                    )
                }
            }
        }
    }

    // Renewal Dialog
    if (loanPendingRenewal != null) {
        RenewConfirmDialog(
            loan = loanPendingRenewal!!,
            onConfirm = { viewModel.confirmRenewal() },
            onDismiss = { viewModel.dismissRenewalDialog() }
        )
    }
}

@Composable
fun MemberHeaderCard(
    memberName: String,
    cardNumber: String,
    activeCount: Int,
    dueSoonCount: Int,
    historyCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = memberName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Card: $cardNumber",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick stats banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    label = "Active",
                    value = activeCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Due/Overdue",
                    value = dueSoonCount.toString(),
                    alert = dueSoonCount > 0,
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Read Before",
                    value = historyCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatBadge(
    label: String,
    value: String,
    alert: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (alert) Color(0xFFEF4444).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun ActiveLoansList(
    loans: List<Loan>,
    onBookClick: (Loan) -> Unit,
    onRenewClick: (Loan) -> Unit
) {
    if (loans.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No borrowed books found",
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(loans, key = { it.id }) { loan ->
                LoanItemCard(
                    loan = loan,
                    onBookClick = onBookClick,
                    onRenewClick = onRenewClick
                )
            }
        }
    }
}

@Composable
fun HistoryList(
    history: List<LoanHistory>,
    onBookClick: (LoanHistory) -> Unit
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.HistoryEdu,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No reading history found",
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(history, key = { it.id }) { historyItem ->
                HistoryItemCard(
                    history = historyItem,
                    onBookClick = onBookClick
                )
            }
        }
    }
}
