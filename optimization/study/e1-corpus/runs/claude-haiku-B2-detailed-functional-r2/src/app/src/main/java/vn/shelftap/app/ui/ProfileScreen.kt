package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.shelftap.app.data.MemberDto

@Composable
fun ProfileScreen(
    member: MemberDto,
    notice: String?,
    error: String?,
    busy: Boolean,
    onChangePassword: () -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            if (notice != null) {
                Text(
                    notice,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
            }

            ProfileRow(label = "Full name", value = member.fullName)
            ProfileRow(label = "Email", value = member.email)
            ProfileRow(label = "Member type", value = member.memberType)
            ProfileRow(label = "Status", value = member.status)

            Spacer(Modifier.weight(1f))

            if (error != null) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = onChangePassword,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Change password")
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSignOut,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
