package com.example.libraryloans.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.libraryloans.model.Book

@Composable
fun BookCoverView(
    book: Book,
    modifier: Modifier = Modifier,
    width: Dp = 80.dp,
    height: Dp = 115.dp,
    isLarge: Boolean = false
) {
    val baseColor = Color(book.coverColorHex)
    val spineDarkColor = Color(
        red = (baseColor.red * 0.7f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.7f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.7f).coerceIn(0f, 1f),
        alpha = 1f
    )

    Surface(
        modifier = modifier
            .size(width = width, height = height)
            .shadow(4.dp, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)),
        shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
        color = baseColor
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Book spine shadow line
            Box(
                modifier = Modifier
                    .width(if (isLarge) 10.dp else 6.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(spineDarkColor, baseColor)
                        )
                    )
            )

            // Book front cover content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(if (isLarge) 8.dp else 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(if (isLarge) 28.dp else 16.dp)
                )

                Text(
                    text = book.title,
                    color = Color.White,
                    fontSize = if (isLarge) 13.sp else 9.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = if (isLarge) 4 else 3,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = book.author,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = if (isLarge) 10.sp else 7.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
