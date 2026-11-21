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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HistoryEdu
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
import com.example.libraryloans.ui.components.HistoryItemCard
import com.example.libraryloans.ui.theme.ReturnedPurple
import com.example.libraryloans.ui.theme.ReturnedPurpleLight
import com.example.libraryloans.ui.theme.Slate600
import com.example.libraryloans.ui.theme.Slate800
import com.example.libraryloans.ui.viewmodel.LoanWithBook

@Composable
fun LoanHistoryScreen(
    historyLoans: List<LoanWithBook>,
    totalHistoryCount: Int,
    onOpenBookDetails: (LoanWithBook) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Stats banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ReturnedPurpleLight)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ReturnedPurple,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$totalHistoryCount Books Read",
                        fontWeight = FontWeight.Bold,
                        color = ReturnedPurple,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Your completed reading journey and loan history with the library.",
                        color = ReturnedPurple,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "Past Loans (${historyLoans.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
        }

        if (historyLoans.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No reading history found",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate600
                        )
                        Text(
                            text = "Books you return will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )
                    }
                }
            }
        } else {
            items(historyLoans, key = { it.loan.id }) { item ->
                HistoryItemCard(
                    item = item,
                    onDetailsClick = { onOpenBookDetails(item) }
                )
            }
        }
    }
}
