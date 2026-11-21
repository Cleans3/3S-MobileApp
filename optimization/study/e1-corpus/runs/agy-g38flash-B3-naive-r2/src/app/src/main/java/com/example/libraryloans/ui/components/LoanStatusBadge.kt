package com.example.libraryloans.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.libraryloans.model.LoanStatus
import com.example.libraryloans.ui.theme.StatusAmber
import com.example.libraryloans.ui.theme.StatusAmberBg
import com.example.libraryloans.ui.theme.StatusBlue
import com.example.libraryloans.ui.theme.StatusBlueBg
import com.example.libraryloans.ui.theme.StatusGreen
import com.example.libraryloans.ui.theme.StatusGreenBg
import com.example.libraryloans.ui.theme.StatusRed
import com.example.libraryloans.ui.theme.StatusRedBg

@Composable
fun LoanStatusBadge(
    status: LoanStatus,
    daysRemaining: Long? = null,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        LoanStatus.ACTIVE -> {
            val text = if (daysRemaining != null) "$daysRemaining days left" else "Active"
            Triple(StatusGreenBg, StatusGreen, text)
        }
        LoanStatus.DUE_SOON -> {
            val text = when (daysRemaining) {
                0L -> "Due Today"
                1L -> "Due Tomorrow"
                else -> "Due in $daysRemaining days"
            }
            Triple(StatusAmberBg, StatusAmber, text)
        }
        LoanStatus.OVERDUE -> {
            val daysOverdue = daysRemaining?.let { kotlin.math.abs(it) } ?: 0
            val text = if (daysOverdue > 0) "Overdue by $daysOverdue d" else "Overdue"
            Triple(StatusRedBg, StatusRed, text)
        }
        LoanStatus.RETURNED -> Triple(StatusBlueBg, StatusBlue, "Returned")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
