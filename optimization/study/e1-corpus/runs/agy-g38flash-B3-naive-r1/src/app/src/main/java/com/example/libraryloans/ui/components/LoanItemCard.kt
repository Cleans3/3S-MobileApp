package com.example.libraryloans.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.libraryloans.model.LoanStatus
import com.example.libraryloans.ui.theme.ActiveGreen
import com.example.libraryloans.ui.theme.ActiveGreenLight
import com.example.libraryloans.ui.theme.DueSoonAmber
import com.example.libraryloans.ui.theme.DueSoonAmberLight
import com.example.libraryloans.ui.theme.NavyPrimary
import com.example.libraryloans.ui.theme.OverdueRed
import com.example.libraryloans.ui.theme.OverdueRedLight
import com.example.libraryloans.ui.theme.Slate600
import com.example.libraryloans.ui.theme.Slate800
import com.example.libraryloans.ui.viewmodel.LoanWithBook
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun LoanItemCard(
    item: LoanWithBook,
    onRenewClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loan = item.loan
    val book = item.book
    val status = loan.getEffectiveStatus()
    val days = loan.daysRemaining()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    val (badgeBg, badgeFg, statusLabel, statusIcon) = when (status) {
        LoanStatus.OVERDUE -> Quad(
            OverdueRedLight,
            OverdueRed,
            "Overdue by ${abs(days)} ${if (abs(days) == 1L) "day" else "days"}",
            Icons.Default.Warning
        )
        LoanStatus.DUE_SOON -> Quad(
            DueSoonAmberLight,
            DueSoonAmber,
            if (days == 0L) "Due today!" else "Due in $days ${if (days == 1L) "day" else "days"}",
            Icons.Default.Schedule
        )
        LoanStatus.ACTIVE -> Quad(
            ActiveGreenLight,
            ActiveGreen,
            "$days days remaining",
            Icons.Default.Schedule
        )
        LoanStatus.RETURNED -> Quad(
            Color.LightGray,
            Color.DarkGray,
            "Returned",
            Icons.Default.Book
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Book spine / avatar indicator
                Box(
                    modifier = Modifier
                        .size(48.dp, 64.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(book.coverAccentHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Book Cover",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Title and Author
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Due Date & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = badgeFg,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            color = badgeFg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Due date string
                Text(
                    text = "Due: ${loan.dueDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate800,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Renewal status bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Renewals: ${loan.renewalCount} of ${loan.maxRenewals} used",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (loan.renewalCount >= loan.maxRenewals) OverdueRed else Slate600
                )

                Text(
                    text = "Borrowed: ${loan.borrowDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate600
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons: Details and Renew
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDetailsClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Details", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRenewClick,
                    enabled = loan.canRenew,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyPrimary,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Renew Loan",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (loan.canRenew) "Renew Loan" else "Max Renewed",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
