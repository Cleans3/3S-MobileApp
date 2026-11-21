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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.libraryloans.ui.components.LoanItemCard
import com.example.libraryloans.ui.theme.DueSoonAmber
import com.example.libraryloans.ui.theme.DueSoonAmberLight
import com.example.libraryloans.ui.theme.NavyPrimary
import com.example.libraryloans.ui.theme.OverdueRed
import com.example.libraryloans.ui.theme.OverdueRedLight
import com.example.libraryloans.ui.theme.Slate600
import com.example.libraryloans.ui.theme.Slate800
import com.example.libraryloans.ui.viewmodel.LoanWithBook

@Composable
fun ActiveLoansScreen(
    loans: List<LoanWithBook>,
    overdueCount: Int,
    dueSoonCount: Int,
    onRenewLoan: (LoanWithBook) -> Unit,
    onOpenBookDetails: (LoanWithBook) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Alert Banners
        if (overdueCount > 0) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OverdueRedLight)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = OverdueRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Overdue Books ($overdueCount)",
                            fontWeight = FontWeight.Bold,
                            color = OverdueRed,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Please return or renew overdue items promptly to avoid library fines.",
                            color = OverdueRed,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (dueSoonCount > 0) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DueSoonAmberLight)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = DueSoonAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "$dueSoonCount book(s) due soon",
                            fontWeight = FontWeight.Bold,
                            color = DueSoonAmber,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "You can renew eligible loans below to extend your lending period.",
                            color = DueSoonAmber,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Section header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Borrowed Items (${loans.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
            }
        }

        if (loans.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No active loans found",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate600
                        )
                        Text(
                            text = "Visit the catalog to borrow your next book!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )
                    }
                }
            }
        } else {
            items(loans, key = { it.loan.id }) { item ->
                LoanItemCard(
                    item = item,
                    onRenewClick = { onRenewLoan(item) },
                    onDetailsClick = { onOpenBookDetails(item) }
                )
            }
        }
    }
}
