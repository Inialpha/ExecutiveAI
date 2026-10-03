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

/**
 * Executive command center — displayed as "Home" in the bottom nav (see
 * ui/navigation/ExecutiveDestinations.kt for why the underlying route/screen name is unchanged).
 *
 * Deliberately NOT a dump of everything the app knows. Priority order: (1) what needs a
 * decision right now — proposals, (2) what's coming next — upcoming accepted items, (3)
 * important emails worth a glance. Each section is capped (by the ViewModel) to a handful of
 * items with a link to see the rest, rather than growing the dashboard itself; when all three
 * are empty, that's shown as "all caught up," not as three separate empty-state blocks.
 */
@Composable
fun DashboardScreen(
    onOpenEmails: () -> Unit,
    onOpenEmail: (String) -> Unit,
    onOpenCalendar: () -> Unit,
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
                        Text("Add a Google account (from the Menu) to start syncing Gmail and Calendar.", color = TextSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                        TextButton(onClick = onOpenAccounts) { Text("Connect account") }
                    }
                }
            }
        }

        val allCaughtUp = state.itemsNeedingReview.isEmpty() && state.upcomingAccepted.isEmpty() && state.importantEmails.isEmpty()

        if (state.itemsNeedingReview.isNotEmpty()) {
            item { SectionHeader("Needs your decision") }
            items(state.itemsNeedingReview, key = { it.id }) { item -> ExecutiveItemCard(item) }
        }

        if (state.upcomingAccepted.isNotEmpty()) {
            item { SectionHeaderRow(title = "Coming up", actionLabel = "See calendar", onAction = onOpenCalendar) }
            items(state.upcomingAccepted, key = { it.id }) { item -> ExecutiveItemCard(item) }
        }

        if (state.importantEmails.isNotEmpty()) {
            item { SectionHeaderRow(title = "Important emails", actionLabel = "See all", onAction = onOpenEmails) }
            items(state.importantEmails, key = { it.id }) { email ->
                EmailSummaryCard(
                    email,
                    onClick = { onOpenEmail(email.id) },
                    onDelete = { viewModel.deleteEmail(email.id) },
                )
            }
        }

        if (allCaughtUp && state.connectedAccounts.isNotEmpty()) {
            item { EmptyState("You're all caught up", "New proposals, upcoming items, and important emails will show up here as they come in.") }
        }

        item {
            TextButton(onClick = onOpenAssistant) { Text("Ask the AI Assistant →") }
        }
    }
}

@Composable
private fun SectionHeaderRow(title: String, actionLabel: String, onAction: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SectionHeader(title)
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}
