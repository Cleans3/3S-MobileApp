package vn.shelftap.app.ui.catalogue.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.shelftap.app.R
import vn.shelftap.app.ui.theme.GreenAvailable
import vn.shelftap.app.ui.theme.GreenAvailableContainer
import vn.shelftap.app.ui.theme.RedOutOfStock
import vn.shelftap.app.ui.theme.RedOutOfStockContainer

@Composable
fun AvailabilityBadge(
    copiesAvailable: Int,
    modifier: Modifier = Modifier
) {
    val isAvailable = copiesAvailable > 0
    val text = if (isAvailable) {
        stringResource(R.string.available)
    } else {
        stringResource(R.string.all_copies_out)
    }

    val backgroundColor = if (isAvailable) GreenAvailableContainer else RedOutOfStockContainer
    val textColor = if (isAvailable) GreenAvailable else RedOutOfStock

    Text(
        text = text,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
