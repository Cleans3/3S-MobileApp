package com.example.library.loans.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.library.loans.R
import com.example.library.loans.domain.LoanRules
import com.example.library.loans.domain.LoanStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookDetailScreen(
    viewModel: BookDetailViewModel,
    onBack: () -> Unit,
) {
    val loan by viewModel.loan.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    RenewResultEffect(viewModel.renewResults, snackbarHostState)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loans_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.loans_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val current = loan
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.loans_renew_not_found))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(current.book.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.loans_author, current.book.author),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.loans_isbn, current.book.isbn),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.loans_pages, current.book.pages),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(8.dp))
                Text(current.book.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))

                Text(
                    stringResource(R.string.loans_borrowed_on, current.borrowedOn.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val returnedOn = current.returnedOn
                if (returnedOn != null) {
                    Text(
                        stringResource(R.string.loans_returned_on, returnedOn.toString()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        stringResource(R.string.loans_due, current.dueDate.toString()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    stringResource(R.string.loans_renewals, current.renewCount, LoanRules.MAX_RENEWALS),
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (current.status == LoanStatus.ACTIVE) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.renew() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.loans_renew))
                    }
                }
            }
        }
    }
}
