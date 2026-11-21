package vn.shelftap.app.ui.catalogue.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.shelftap.app.R
import vn.shelftap.app.data.sync.SyncStatus

@Composable
fun SyncStatusBanner(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = syncStatus !is SyncStatus.Idle,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val backgroundColor = when (syncStatus) {
            is SyncStatus.Syncing -> MaterialTheme.colorScheme.secondaryContainer
            is SyncStatus.Completed -> MaterialTheme.colorScheme.surfaceVariant
            is SyncStatus.Error -> MaterialTheme.colorScheme.errorContainer
            SyncStatus.Idle -> MaterialTheme.colorScheme.surfaceVariant
        }

        val contentColor = when (syncStatus) {
            is SyncStatus.Syncing -> MaterialTheme.colorScheme.onSecondaryContainer
            is SyncStatus.Completed -> MaterialTheme.colorScheme.onSurfaceVariant
            is SyncStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
            SyncStatus.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (syncStatus) {
                    is SyncStatus.Syncing -> {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sync_downloading, syncStatus.current, syncStatus.total),
                            color = contentColor,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is SyncStatus.Completed -> {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sync_ready) + " (${syncStatus.total} titles)",
                            color = contentColor,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is SyncStatus.Error -> {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (syncStatus.cachedCount > 0) {
                                stringResource(R.string.sync_offline) + " (${syncStatus.cachedCount} titles)"
                            } else {
                                syncStatus.message
                            },
                            color = contentColor,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    SyncStatus.Idle -> {}
                }
            }
        }
    }
}
