package com.example.libraryapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.libraryapp.R
import com.example.libraryapp.data.AccountRepository
import com.example.libraryapp.data.api.MemberProfile

@Composable
fun ProfileScreen(repository: AccountRepository) {
    val viewModel: ProfileViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ProfileViewModel(repository) }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Reload every time the profile screen appears, so a new sign-in never shows stale data.
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                actions = {
                    TextButton(onClick = viewModel::signOut) {
                        Text(stringResource(R.string.sign_out))
                    }
                }
            )
        }
    ) { padding ->
        val profile = state.profile
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                profile != null -> ProfileDetails(profile)
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error ?: stringResource(R.string.generic_error),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = viewModel::load) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDetails(profile: MemberProfile) {
    val notSet = stringResource(R.string.value_not_set)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = profile.fullName, style = MaterialTheme.typography.headlineSmall)
        Text(text = profile.email, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
        ProfileRow(stringResource(R.string.membership_number), profile.membershipNumber ?: notSet)
        ProfileRow(stringResource(R.string.membership_type), profile.membershipType ?: notSet)
        ProfileRow(stringResource(R.string.member_since), profile.memberSince ?: notSet)
        ProfileRow(stringResource(R.string.active_loans), profile.activeLoans?.toString() ?: notSet)
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
