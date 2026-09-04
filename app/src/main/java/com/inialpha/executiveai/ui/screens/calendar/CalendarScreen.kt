package com.inialpha.executiveai.ui.screens.calendar

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
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.components.formatDueAt
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.CalendarViewModel
import com.inialpha.executiveai.viewmodel.containerViewModelFactory
import com.inialpha.executiveai.viewmodel.executiveAIContainer

/**
 * Google Calendar screen — read-only sync foundation today (write access + conflict detection
 * against proposed events is scaffolded in CalendarRepository/ExecutiveItemRepository but not
 * yet surfaced in this screen's UI).
 */
@Composable
fun CalendarScreen() {
    val container = executiveAIContainer()
    val viewModel: CalendarViewModel = viewModel(
        factory = containerViewModelFactory(container) { CalendarViewModel(it) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) { LoadingState(); return }
    if (state.events.isEmpty()) {
        EmptyState("No calendar events synced", "Sync a connected account from Settings to see your schedule here.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.events) { event ->
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
}
