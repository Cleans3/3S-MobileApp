package com.example.librarycatalogue.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.librarycatalogue.model.Book
import com.example.librarycatalogue.ui.theme.CoverPalettes
import kotlin.math.abs

@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val palette = remember(book.coverColorSeed) {
        val index = (abs(book.coverColorSeed) % CoverPalettes.size).toInt()
        CoverPalettes[index]
    }

    val primaryBg = palette.first
    val accentColor = palette.second

    Box(
        modifier = modifier
            .aspectRatio(0.68f)
            .shadow(
                elevation = if (isLarge) 10.dp else 5.dp,
                shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp, topStart = 2.dp, bottomStart = 2.dp)
            )
            .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp, topStart = 2.dp, bottomStart = 2.dp))
            .background(primaryBg)
    ) {
        if (!book.coverImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(book.coverImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover for ${book.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Procedurally generated high-aesthetic book cover
            ProceduralCoverCanvas(accentColor = accentColor, seed = book.coverColorSeed)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (isLarge) 24.dp else 14.dp,
                        end = if (isLarge) 18.dp else 10.dp,
                        top = if (isLarge) 22.dp else 12.dp,
                        bottom = if (isLarge) 18.dp else 10.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Genre watermark / header
                Text(
                    text = book.genre.uppercase(),
                    color = accentColor.copy(alpha = 0.8f),
                    fontSize = if (isLarge) 11.sp else 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(0.7f))

                // Book Title
                Text(
                    text = book.title,
                    color = Color.White,
                    fontSize = if (isLarge) 20.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isLarge) 24.sp else 16.sp,
                    maxLines = if (isLarge) 4 else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(if (isLarge) 8.dp else 4.dp))

                // Author Name
                Text(
                    text = book.author,
                    color = accentColor,
                    fontSize = if (isLarge) 13.sp else 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                // Vintage library insignia / Year
                Text(
                    text = "EST. ${book.publicationYear}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = if (isLarge) 10.sp else 7.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        // Spine shadow & embossing overlay
        BookSpineOverlay()
    }
}

@Composable
private fun ProceduralCoverCanvas(accentColor: Color, seed: Long) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Outer decorative border
        val strokeWidth = 2.dp.toPx()
        val margin = 8.dp.toPx()

        drawRect(
            color = accentColor.copy(alpha = 0.35f),
            topLeft = Offset(margin + 4.dp.toPx(), margin),
            size = androidx.compose.ui.geometry.Size(w - margin * 2 - 4.dp.toPx(), h - margin * 2),
            style = Stroke(width = strokeWidth)
        )

        // Geometric emblem according to seed pattern
        val patternChoice = (abs(seed) % 3).toInt()
        val center = Offset(w / 2f + 2.dp.toPx(), h * 0.42f)
        val radius = w * 0.18f

        when (patternChoice) {
            0 -> {
                // Concentric circles
                drawCircle(
                    color = accentColor.copy(alpha = 0.25f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.15f),
                    radius = radius * 0.6f,
                    center = center
                )
            }
            1 -> {
                // Diamond emblem
                drawRect(
                    color = accentColor.copy(alpha = 0.2f),
                    topLeft = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
                    size = androidx.compose.ui.geometry.Size(radius * 1.6f, radius * 1.6f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            else -> {
                // Elegant horizontal divider lines
                drawLine(
                    color = accentColor.copy(alpha = 0.4f),
                    start = Offset(w * 0.25f, center.y),
                    end = Offset(w * 0.75f, center.y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun BookSpineOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val spineWidth = w * 0.08f

        // Spine shadow gradient
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.45f),
                    Color.Black.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                startX = 0f,
                endX = spineWidth * 2.5f
            ),
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(spineWidth * 2.5f, h)
        )

        // Spine vertical crease line
        drawLine(
            color = Color.Black.copy(alpha = 0.3f),
            start = Offset(spineWidth, 0f),
            end = Offset(spineWidth, h),
            strokeWidth = 1.dp.toPx()
        )
    }
}
