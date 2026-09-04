package com.inialpha.executiveai.ui.screens.upcoming

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.ExecutiveItemCard
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.components.SectionHeader
import com.inialpha.executiveai.ui.components.formatDueAt
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.UpcomingViewModel
import com.inialpha.executiveai.viewmodel.containerViewModelFactory
import com.inialpha.executiveai.viewmodel.executiveAIContainer

/** Unified timeline: real Google Calendar events + accepted proposals (events/deadlines/reminders/tasks). */
@Composable
fun UpcomingScreen() {
    val container = executiveAIContainer()
    val viewModel: UpcomingViewModel = viewModel(
        factory = containerViewModelFactory(container) { UpcomingViewModel(it) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) { LoadingState(); return }
    if (state.calendarEvents.isEmpty() && state.acceptedItems.isEmpty()) {
        EmptyState("Nothing upcoming", "Calendar events and accepted proposals will appear here.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.calendarEvents.isNotEmpty()) {
            item { SectionHeader("From your calendar") }
            items(state.calendarEvents) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(event.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        formatDueAt(event.startAtMillis)?.let { Text(it, color = TextSecondary, modifier = Modifier.padding(top = 4.dp)) }
                        event.location?.let { Text("📍 $it", color = TextSecondary) }
                    }
                }
            }
        }
        if (state.acceptedItems.isNotEmpty()) {
            item { SectionHeader("From Executive AI") }
            items(state.acceptedItems) { item -> ExecutiveItemCard(item) }
        }
    }
}
