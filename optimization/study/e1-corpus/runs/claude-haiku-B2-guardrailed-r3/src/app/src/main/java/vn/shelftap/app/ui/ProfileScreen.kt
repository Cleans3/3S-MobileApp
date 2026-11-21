package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.shelftap.app.R

@Composable
fun ProfileScreen(
    state: AppUiState,
    onChangePassword: () -> Unit,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
) {
    val member = state.member

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            if (member != null) {
                ProfileField(stringResource(R.string.field_full_name), member.fullName)
                ProfileField(stringResource(R.string.field_email), member.email)
                ProfileField(stringResource(R.string.field_member_type), member.memberType)
                ProfileField(stringResource(R.string.field_status), member.status)
            } else if (state.profileLoading) {
                CircularProgressIndicator()
            }

            state.profileError?.let { err ->
                Text(
                    text = err.message(),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (member == null && !state.profileLoading) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.retry_button))
                }
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = onChangePassword,
                enabled = member != null && !state.profileLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.change_password_button))
            }

            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.sign_out_button))
            }
        }
    }
}
