package com.example.libraryloans.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.libraryloans.model.Book

@Composable
fun BookCoverView(
    book: Book,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(book.coverAccentHex)
    val darkGradient = Brush.verticalGradient(
        colors = listOf(
            baseColor,
            baseColor.copy(alpha = 0.85f),
            Color(0xFF0F172A).copy(alpha = 0.95f)
        )
    )

    Box(
        modifier = modifier
            .width(80.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(darkGradient)
            .shadow(4.dp, RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        // Spine accent
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(3.dp)
                .background(Color.White.copy(alpha = 0.25f))
        )

        // Book title & author
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp)
                .align(Alignment.Center)
        ) {
            Text(
                text = book.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = book.author,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}
