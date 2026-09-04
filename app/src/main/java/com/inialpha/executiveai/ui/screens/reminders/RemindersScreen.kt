package com.inialpha.executiveai.ui.screens.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.ExecutiveItemCard
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.viewmodel.RemindersViewModel

/** Reminders screen. Accepting here schedules a real Android alarm via ReminderScheduler. */
@Composable
fun RemindersScreen() {
    val viewModel: RemindersViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) { LoadingState(); return }

    Column(Modifier.fillMaxSize()) {
        if (!state.canScheduleExactAlarms) {
            Text(
                "Exact alarm scheduling isn't currently permitted — reminders may fire a little late. " +
                    "Enable it from system settings for on-time delivery.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
        if (state.reminders.isEmpty()) {
            EmptyState("No reminders yet", "Reminders the AI extracts from your emails will appear here.")
            return
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.reminders) { item ->
                ExecutiveItemCard(
                    item = item,
                    onAccept = if (item.state.name in setOf("PROPOSED", "EDITED")) { { viewModel.accept(item) } } else null,
                    onReject = if (item.state.name in setOf("PROPOSED", "EDITED")) { { viewModel.reject(item.id) } } else null,
                    onComplete = if (item.state.name == "ACCEPTED") { { viewModel.complete(item.id) } } else null,
                )
            }
        }
    }
}
