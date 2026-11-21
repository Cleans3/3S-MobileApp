package vn.shelftap.app.ui.catalogue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import vn.shelftap.app.R
import vn.shelftap.app.ui.theme.AvailableGreenBg
import vn.shelftap.app.ui.theme.AvailableGreenFg
import vn.shelftap.app.ui.theme.OutOfStockRedBg
import vn.shelftap.app.ui.theme.OutOfStockRedFg

@Composable
fun AvailabilityBadge(
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isAvailable) AvailableGreenBg else OutOfStockRedBg
    val textColor = if (isAvailable) AvailableGreenFg else OutOfStockRedFg
    val label = if (isAvailable) {
        stringResource(R.string.badge_available)
    } else {
        stringResource(R.string.badge_all_copies_out)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
