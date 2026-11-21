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
import vn.shelftap.app.ui.theme.AvailableBadgeGreen
import vn.shelftap.app.ui.theme.AvailableBadgeGreenBg
import vn.shelftap.app.ui.theme.OutBadgeRed
import vn.shelftap.app.ui.theme.OutBadgeRedBg

@Composable
fun AvailabilityBadge(
    copiesAvailable: Int,
    modifier: Modifier = Modifier
) {
    val isAvailable = copiesAvailable > 0
    val text = if (isAvailable) {
        stringResource(R.string.badge_available)
    } else {
        stringResource(R.string.badge_out)
    }

    val backgroundColor = if (isAvailable) AvailableBadgeGreenBg else OutBadgeRedBg
    val textColor = if (isAvailable) AvailableBadgeGreen else OutBadgeRed

    Text(
        text = text,
        color = textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
