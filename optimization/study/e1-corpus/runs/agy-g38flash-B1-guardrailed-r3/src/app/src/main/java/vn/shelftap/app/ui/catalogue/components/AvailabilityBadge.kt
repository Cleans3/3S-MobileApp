package vn.shelftap.app.ui.catalogue.components

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
import androidx.compose.ui.unit.dp
import vn.shelftap.app.R
import vn.shelftap.app.ui.theme.GreenAvailableContainer
import vn.shelftap.app.ui.theme.OnGreenAvailableContainer
import vn.shelftap.app.ui.theme.OnRedUnavailableContainer
import vn.shelftap.app.ui.theme.RedUnavailableContainer

@Composable
fun AvailabilityBadge(
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isAvailable) GreenAvailableContainer else RedUnavailableContainer
    val textColor = if (isAvailable) OnGreenAvailableContainer else OnRedUnavailableContainer
    val textRes = if (isAvailable) R.string.available else R.string.all_copies_out

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(textRes),
            color = textColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
