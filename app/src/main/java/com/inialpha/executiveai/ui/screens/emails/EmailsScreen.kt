package com.inialpha.executiveai.ui.screens.emails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.components.SyncProgressDialog
import com.inialpha.executiveai.ui.components.formatDueAt
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.EmailWithInsight
import com.inialpha.executiveai.viewmodel.EmailsViewModel
import com.inialpha.executiveai.viewmodel.containerViewModelFactory
import com.inialpha.executiveai.viewmodel.executiveAIContainer

/**
 * Primary Emails destination: account tabs at the top, then only that account's AI-processed
 * emails below — accounts are never combined into one list (REQUIREMENTS change request #8).
 */
@Composable
fun EmailsScreen(onOpenEmail: (String) -> Unit) {
    val container = executiveAIContainer()
    val viewModel: EmailsViewModel = viewModel(
        factory = containerViewModelFactory(container) { EmailsViewModel(it) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Emails", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (state.accounts.isNotEmpty()) {
                if (state.isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sync emails")
                    }
                }
            }
        }
        state.statusMessage?.let {
            Text(it, color = TextSecondary, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
        }

        if (state.accounts.isEmpty()) {
            EmptyState("No connected accounts", "Connect a Google account from the Menu to see your emails.")
            return
        }

        val selectedIndex = state.accounts.indexOfFirst { it.id == state.selectedAccountId }.coerceAtLeast(0)
        ScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 16.dp) {
            state.accounts.forEachIndexed { index, account ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { viewModel.selectAccount(account.id) },
                    text = { Text(account.displayName ?: account.email, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }

        if (state.isLoading) {
            LoadingState()
            return
        }
        if (state.emails.isEmpty()) {
            EmptyState("No processed emails yet", "Tap the sync icon above to let Executive AI summarize this account's inbox.")
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.emails, key = { it.email.id }) { item ->
                EmailWithInsightCard(
                    item,
                    onClick = { onOpenEmail(item.email.id) },
                    onDelete = { viewModel.deleteEmail(item.email.id) },
                )
            }
        }
    }

    state.syncProgress?.let { progress ->
        SyncProgressDialog(
            progress = progress,
            accountLabel = state.syncingAccountLabel,
            onDismiss = { viewModel.dismissSyncProgress() },
        )
    }
}

@Composable
private fun EmailWithInsightCard(item: EmailWithInsight, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.email.senderName ?: item.email.sender,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete")
                }
            }
            Text(
                item.email.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            formatDueAt(item.email.receivedAt)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            if (item.email.isImportant) {
                AssistChip(onClick = {}, label = { Text("Important") }, modifier = Modifier.padding(top = 6.dp))
            }
            val summary = item.insight?.summary
            if (!summary.isNullOrBlank()) {
                Text(
                    "Summary:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
