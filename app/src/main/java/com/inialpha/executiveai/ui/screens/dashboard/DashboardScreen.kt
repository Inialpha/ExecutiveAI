package com.inialpha.executiveai.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.ui.components.EmailSummaryCard
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.ExecutiveItemCard
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.components.SectionHeader
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.DashboardViewModel
import com.inialpha.executiveai.viewmodel.containerViewModelFactory
import com.inialpha.executiveai.viewmodel.executiveAIContainer

/** Executive command center: today's priorities, important mail, pending proposals, upcoming items. */
@Composable
fun DashboardScreen(
    onOpenImportantEmails: () -> Unit,
    onOpenEmail: (String) -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenAccounts: () -> Unit,
) {
    val container = executiveAIContainer()
    val viewModel: DashboardViewModel = viewModel(
        factory = containerViewModelFactory(container) { DashboardViewModel(it) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        LoadingState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Good day.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (state.connectedAccounts.isEmpty()) "Connect a Google account to get started."
                else "Here's what needs your attention.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        if (state.connectedAccounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No connected accounts yet", fontWeight = FontWeight.Bold)
                        Text("Add a Google account to start syncing Gmail and Calendar.", color = TextSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                        TextButton(onClick = onOpenAccounts) { Text("Connect account") }
                    }
                }
            }
        }

        if (state.itemsNeedingReview.isNotEmpty()) {
            item {
                SectionHeader("${state.itemsNeedingReview.size} proposals need your review")
            }
            items(state.itemsNeedingReview.take(3)) { item -> ExecutiveItemCard(item) }
        }

        item {
            Row(onOpenImportantEmails, state.importantEmails.size)
        }
        items(state.importantEmails) { email -> EmailSummaryCard(email, onClick = { onOpenEmail(email.id) }) }
        if (state.importantEmails.isEmpty()) {
            item { EmptyState("No important emails yet", "Sync an account to let Executive AI find what matters.") }
        }

        item { SectionHeader("Upcoming") }
        if (state.upcomingAccepted.isEmpty()) {
            item { EmptyState("Nothing scheduled", "Accepted events, deadlines, and reminders will show up here.") }
        }
        items(state.upcomingAccepted) { item -> ExecutiveItemCard(item) }

        item {
            TextButton(onClick = onOpenAssistant) { Text("Ask the AI Assistant →") }
        }
    }
}

@Composable
private fun Row(onOpenImportantEmails: () -> Unit, count: Int) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SectionHeader("Important emails")
        TextButton(onClick = onOpenImportantEmails) { Text(if (count > 0) "See all ($count)" else "See all") }
    }
}
