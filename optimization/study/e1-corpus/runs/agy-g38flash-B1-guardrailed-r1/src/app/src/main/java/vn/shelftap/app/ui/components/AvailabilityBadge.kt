package vn.shelftap.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.shelftap.app.R
import vn.shelftap.app.ui.theme.AvailableGreen
import vn.shelftap.app.ui.theme.AvailableGreenContainer
import vn.shelftap.app.ui.theme.Typography
import vn.shelftap.app.ui.theme.UnavailableRed
import vn.shelftap.app.ui.theme.UnavailableRedContainer

@Composable
fun AvailabilityBadge(
    copiesAvailable: Int,
    modifier: Modifier = Modifier
) {
    val isAvailable = copiesAvailable > 0
    val text = if (isAvailable) {
        stringResource(R.string.badge_available)
    } else {
        stringResource(R.string.badge_all_copies_out)
    }

    val backgroundColor = if (isAvailable) AvailableGreenContainer else UnavailableRedContainer
    val textColor = if (isAvailable) AvailableGreen else UnavailableRed

    Text(
        text = text,
        color = textColor,
        style = Typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
