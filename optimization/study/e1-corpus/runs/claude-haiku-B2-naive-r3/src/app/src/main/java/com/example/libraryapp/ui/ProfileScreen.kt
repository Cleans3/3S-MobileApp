package com.example.libraryapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.libraryapp.R

@Composable
fun ProfileScreen(
    state: AuthUiState,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(16.dp))

        val profile = state.profile
        if (profile != null) {
            ProfileField(stringResource(R.string.label_name), profile.fullName)
            ProfileField(stringResource(R.string.label_email), profile.email)
            ProfileField(stringResource(R.string.label_member_number), profile.memberNumber)
            profile.membershipType?.let { ProfileField(stringResource(R.string.label_membership), it) }
            profile.joinedOn?.let { ProfileField(stringResource(R.string.label_joined), it) }
        } else if (state.isLoadingProfile) {
            CircularProgressIndicator()
        }

        state.profileError?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onRefresh,
            enabled = !state.isLoadingProfile,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.refresh))
        }

        Spacer(Modifier.height(12.dp))

        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sign_out))
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
