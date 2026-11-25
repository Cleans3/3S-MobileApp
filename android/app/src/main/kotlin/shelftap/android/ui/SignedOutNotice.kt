package shelftap.android.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import shelftap.android.BuildConfig
import shelftap.android.R
import shelftap.android.session.LogoutReason
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.shelfTap

/** What a notice says and how it looks for one [LogoutReason]: a title, a body, an icon and the colours of its card (the board `SignedOut`). */
private data class Notice(@StringRes val title: Int, @StringRes val body: Int, val icon: String, val tone: Tone)

private enum class Tone { PLAIN, DANGER, WARN }

private fun LogoutReason.notice() = when (this) {
    LogoutReason.SESSION_EXPIRED -> Notice(R.string.signedout_expired_title, R.string.signedout_expired_body, LucideIcons.Clock, Tone.PLAIN)
    // The same token used twice means another device holds the session: the one message that asks for a password change.
    LogoutReason.TOKEN_REUSED -> Notice(R.string.signedout_reused_title, R.string.signedout_reused_body, LucideIcons.ShieldAlert, Tone.DANGER)
    LogoutReason.INACTIVITY -> Notice(R.string.signedout_idle_title, R.string.signedout_idle_body, LucideIcons.ClockBack, Tone.PLAIN)
    LogoutReason.LOCK_KEY_INVALIDATED -> Notice(R.string.signedout_lockkey_title, R.string.signedout_lockkey_body, LucideIcons.Fingerprint, Tone.WARN)
}

private val ICON_TILE = 36.dp
private val ICON_TILE_RADIUS = 12.dp
private val ICON = 20.dp
private const val TILE_FILL = 0.7f
private const val CARD_EDGE = 0.08f
private const val CARD_SHADOW = 0.08f

/**
 * Why the member is signed out, as a card above the login form: one message per [LogoutReason], with its own icon and colour (plain for an
 * expired session or inactivity, red for a reused token, amber for a changed fingerprint). It is a live region, so a screen reader says it
 * when the login appears.
 */
@Composable
fun SignedOutNotice(reason: LogoutReason, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    val notice = reason.notice()
    val (fill, accent) = when (notice.tone) {
        Tone.PLAIN -> MaterialTheme.colorScheme.surface to c.ink
        Tone.DANGER -> c.dangerBg to c.danger
        Tone.WARN -> c.warnBg to c.warn
    }
    val shape = RoundedCornerShape(Radius.card)
    val minutes = (BuildConfig.INACTIVITY_LIMIT_MS / 60_000L).toInt()
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .shadow(Spacing.l, shape, ambientColor = c.ink.copy(alpha = CARD_SHADOW), spotColor = c.ink.copy(alpha = CARD_SHADOW))
            .clip(shape).background(fill).border(BorderStroke(Radius.hairline, c.ink.copy(alpha = CARD_EDGE)), shape)
            .padding(horizontal = Spacing.l, vertical = Spacing.m + Spacing.xs / 2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            Modifier.size(ICON_TILE).clip(RoundedCornerShape(ICON_TILE_RADIUS)).background(c.glass.copy(alpha = TILE_FILL)),
            contentAlignment = Alignment.Center,
        ) { LucideIcon(notice.icon, tint = accent, size = ICON) }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs - 1.dp)) {
            Text(stringResource(notice.title), style = MaterialTheme.typography.titleSmall, color = accent)
            Text(
                if (reason == LogoutReason.INACTIVITY) stringResource(notice.body, minutes) else stringResource(notice.body),
                style = MaterialTheme.typography.bodyMedium, color = c.ink2,
            )
        }
    }
}
