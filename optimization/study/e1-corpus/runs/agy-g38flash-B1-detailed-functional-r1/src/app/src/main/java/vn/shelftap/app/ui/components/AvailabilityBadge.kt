package vn.shelftap.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.shelftap.app.R
import vn.shelftap.app.ui.theme.AvailableGreenBorder
import vn.shelftap.app.ui.theme.AvailableGreenBg
import vn.shelftap.app.ui.theme.AvailableGreenText
import vn.shelftap.app.ui.theme.UnavailableRedBorder
import vn.shelftap.app.ui.theme.UnavailableRedBg
import vn.shelftap.app.ui.theme.UnavailableRedText

/**
 * Badge indicating whether physical copies are available on the shelves.
 * Displays "Available" (copiesAvailable > 0) or "All copies out".
 */
@Composable
fun AvailabilityBadge(
    copiesAvailable: Int,
    modifier: Modifier = Modifier
) {
    val isAvailable = copiesAvailable > 0
    val bgColor = if (isAvailable) AvailableGreenBg else UnavailableRedBg
    val textColor = if (isAvailable) AvailableGreenText else UnavailableRedText
    val borderColor = if (isAvailable) AvailableGreenBorder else UnavailableRedBorder
    val text = if (isAvailable) {
        stringResource(R.string.badge_available)
    } else {
        stringResource(R.string.badge_unavailable)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
