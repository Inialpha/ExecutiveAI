package com.inialpha.executiveai.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.inialpha.executiveai.ui.components.ExecutiveItemCard
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.components.SectionHeader
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.DashboardViewModel
import com.inialpha.executiveai.viewmodel.containerViewModelFactory
import com.inialpha.executiveai.viewmodel.executiveAIContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home ("Dashboard" internally). Two things only: cards showing how many events / tasks /
 * reminders are waiting on an Accept/Reject decision (each opens the screen where that decision
 * is made), and a "Today" list of accepted items due today, each completable in place.
 */
@Composable
fun DashboardScreen(
    onOpenCalendar: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenReminders: () -> Unit,
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

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCountCard("event", state.eventsNeedingAction, onOpenCalendar, Modifier.weight(1f))
                ActionCountCard("task", state.tasksNeedingAction, onOpenTasks, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCountCard("reminder", state.remindersNeedingAction, onOpenReminders, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
        }

        item {
            SectionHeader("Today")
            Text(
                SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        if (state.today.isEmpty()) {
            item { Text("Nothing scheduled for today.", color = TextSecondary) }
        } else {
            items(state.today, key = { it.id }) { item ->
                ExecutiveItemCard(item = item, onComplete = { viewModel.complete(item.id) })
            }
        }

        item {
            TextButton(onClick = onOpenAssistant) { Text("Ask the AI Assistant →") }
        }
    }
}

/** "2 events need action" — or a calm "No events pending" at zero. Always tappable. */
@Composable
private fun ActionCountCard(noun: String, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasWork = count > 0
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasWork) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (hasWork) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary,
            )
            Text(
                when {
                    count == 0 -> "No ${noun}s pending"
                    count == 1 -> "$noun needs action"
                    else -> "${noun}s need action"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasWork) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary,
            )
        }
    }
}
