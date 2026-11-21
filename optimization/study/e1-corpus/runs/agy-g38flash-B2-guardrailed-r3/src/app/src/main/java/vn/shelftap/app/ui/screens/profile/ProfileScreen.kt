package vn.shelftap.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.shelftap.app.R
import vn.shelftap.app.data.repository.ShelfTapRepository

@Composable
fun ProfileScreen(
    repository: ShelfTapRepository,
    onSignOutComplete: () -> Unit,
    onPasswordChangeRequired: () -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel(repository) }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCachedAndFetchProfile(
            onSessionExpired = onSignOutComplete,
            onPasswordChangeRequired = onPasswordChangeRequired
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header_item", contentType = "header") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            item(key = "error_item", contentType = "error") {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        val profile = uiState.profile
        if (profile != null) {
            item(key = "full_name_item", contentType = "info_card") {
                ProfileInfoCard(
                    label = stringResource(R.string.full_name_label),
                    value = profile.fullName
                )
            }

            item(key = "email_item", contentType = "info_card") {
                ProfileInfoCard(
                    label = stringResource(R.string.email_label),
                    value = profile.email
                )
            }

            item(key = "member_type_item", contentType = "info_card") {
                ProfileInfoCard(
                    label = stringResource(R.string.member_type_label),
                    value = profile.memberType
                )
            }

            item(key = "status_item", contentType = "info_card") {
                ProfileInfoCard(
                    label = stringResource(R.string.status_label),
                    value = profile.status
                )
            }
        } else if (uiState.isLoading) {
            item(key = "loading_item", contentType = "loading") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        item(key = "sign_out_item", contentType = "action") {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    viewModel.signOut(onComplete = onSignOutComplete)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text(
                        text = stringResource(R.string.sign_out_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInfoCard(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
