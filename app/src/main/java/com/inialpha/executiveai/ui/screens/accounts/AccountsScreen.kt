package com.inialpha.executiveai.ui.screens.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.domain.model.Account
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.AccountsViewModel
import com.inialpha.executiveai.viewmodel.containerViewModelFactory
import com.inialpha.executiveai.viewmodel.executiveAIContainer

/** Connected Accounts: add, view, toggle sync per-source, synchronize now, disconnect. Every account is independently identifiable by email. */
@Composable
fun AccountsScreen() {
    val container = executiveAIContainer()
    val viewModel: AccountsViewModel = viewModel(
        factory = containerViewModelFactory(container) { AccountsViewModel(it) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) { LoadingState(); return }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Connected accounts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.addAccount() }, enabled = !state.isConnecting) {
                Text(if (state.isConnecting) "Connecting…" else "Add account")
            }
        }

        state.statusMessage?.let {
            Text(it, color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        if (state.accounts.isEmpty()) {
            EmptyState("No accounts connected", "Add a Google account to start syncing Gmail and Calendar.")
            return
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            TextButton(onClick = { viewModel.syncAll() }, enabled = !state.isSyncing) {
                Text(if (state.isSyncing) "Synchronizing…" else "Synchronize all now")
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.accounts) { account ->
                AccountRow(
                    account = account,
                    onToggleGmail = { viewModel.setGmailSyncEnabled(account.id, it) },
                    onToggleCalendar = { viewModel.setCalendarSyncEnabled(account.id, it) },
                    onDisconnect = { viewModel.disconnect(account.id) },
                )
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    onToggleGmail: (Boolean) -> Unit,
    onToggleCalendar: (Boolean) -> Unit,
    onDisconnect: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(account.displayName ?: account.email, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(account.email, color = TextSecondary, style = MaterialTheme.typography.bodySmall)

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Gmail sync")
                Switch(checked = account.isGmailSyncEnabled, onCheckedChange = onToggleGmail)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Calendar sync")
                Switch(checked = account.isCalendarSyncEnabled, onCheckedChange = onToggleCalendar)
            }

            OutlinedButton(onClick = onDisconnect, modifier = Modifier.padding(top = 12.dp)) { Text("Disconnect") }
        }
    }
}
