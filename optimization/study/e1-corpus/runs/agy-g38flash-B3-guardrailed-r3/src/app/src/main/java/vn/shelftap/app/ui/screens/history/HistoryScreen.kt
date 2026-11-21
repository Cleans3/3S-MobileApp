package vn.shelftap.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vn.shelftap.app.R
import vn.shelftap.app.ui.components.OfflineBanner

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Confirmation dialog for turning off reading history
    if (uiState.showTurnOffConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            title = {
                Text(text = stringResource(R.string.dialog_confirm_history_title))
            },
            text = {
                Text(text = stringResource(R.string.dialog_confirm_history_message))
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmTurnOffHistory() },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text(text = stringResource(R.string.dialog_btn_confirm_turn_off))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissConfirmDialog() },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text(text = stringResource(R.string.dialog_btn_cancel))
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.historyItems.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.errorMessage != null && uiState.historyItems.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.errorMessage ?: stringResource(R.string.error_generic),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadData() },
                        modifier = Modifier.sizeIn(minWidth = 120.dp, minHeight = 48.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_retry))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Privacy switch header
                    item(key = "history_privacy_switch", contentType = "privacy_header") {
                        HistoryPrivacyCard(
                            historyEnabled = uiState.historyEnabled,
                            isUpdating = uiState.isUpdatingPrivacy,
                            onToggle = { viewModel.onPrivacySwitchToggled(it) }
                        )
                    }

                    // Offline banner
                    if (uiState.isOffline) {
                        item(key = "offline_banner", contentType = "banner") {
                            OfflineBanner(modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }

                    if (!uiState.historyEnabled) {
                        item(key = "history_disabled", contentType = "notice") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.history_disabled_notice),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (uiState.historyItems.isEmpty()) {
                        item(key = "empty_history", contentType = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_history),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(
                            items = uiState.historyItems,
                            key = { "${it.item.titleId}_${it.item.outAt}" },
                            contentType = { "history_row" }
                        ) { historyUiState ->
                            HistoryRowCard(
                                historyUiState = historyUiState,
                                onClick = { onNavigateToDetail(historyUiState.item.titleId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryPrivacyCard(
    historyEnabled: Boolean,
    isUpdating: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.keep_history_switch),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            // Switch meets 48dp touch target
            Switch(
                checked = historyEnabled,
                onCheckedChange = onToggle,
                enabled = !isUpdating,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            )
        }
    }
}

@Composable
fun HistoryRowCard(
    historyUiState: HistoryItemUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = historyUiState.item
    val rowDescription = stringResource(R.string.cd_history_row, item.title)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = rowDescription },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.borrowed_date_label, historyUiState.formattedBorrowedDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val returnedText = if (historyUiState.formattedReturnedDate.isNotEmpty()) {
                stringResource(R.string.returned_date_label, historyUiState.formattedReturnedDate)
            } else {
                stringResource(R.string.not_returned_yet)
            }
            Text(
                text = returnedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
