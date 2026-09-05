package com.inialpha.executiveai.ui.screens.emails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.LoadingState
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
        Text(
            "Emails",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

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
            EmptyState("No processed emails yet", "Sync this account from the Menu to let Executive AI summarize your inbox.")
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.emails, key = { it.email.id }) { item ->
                EmailWithInsightCard(item, onClick = { onOpenEmail(item.email.id) })
            }
        }
    }
}

@Composable
private fun EmailWithInsightCard(item: EmailWithInsight, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                item.email.senderName ?: item.email.sender,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
