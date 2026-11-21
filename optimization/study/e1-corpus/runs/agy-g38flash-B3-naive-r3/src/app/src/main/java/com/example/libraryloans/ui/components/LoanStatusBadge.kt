package com.example.libraryloans.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.libraryloans.model.Loan
import com.example.libraryloans.ui.theme.StatusAmber
import com.example.libraryloans.ui.theme.StatusAmberBg
import com.example.libraryloans.ui.theme.StatusGreen
import com.example.libraryloans.ui.theme.StatusGreenBg
import com.example.libraryloans.ui.theme.StatusRed
import com.example.libraryloans.ui.theme.StatusRedBg

@Composable
fun LoanStatusBadge(loan: Loan, modifier: Modifier = Modifier) {
    val (bgColor, textColor, text) = when {
        loan.isOverdue -> {
            val days = kotlin.math.abs(loan.daysRemaining)
            Triple(StatusRedBg, StatusRed, if (days == 1L) "1 day overdue" else "$days days overdue")
        }
        loan.daysRemaining == 0L -> {
            Triple(StatusAmberBg, StatusAmber, "Due today")
        }
        loan.isDueSoon -> {
            val days = loan.daysRemaining
            Triple(StatusAmberBg, StatusAmber, if (days == 1L) "Due tomorrow" else "Due in $days days")
        }
        else -> {
            val days = loan.daysRemaining
            Triple(StatusGreenBg, StatusGreen, "$days days left")
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
