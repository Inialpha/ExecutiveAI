package com.inialpha.executiveai.ui.screens.calendar

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.domain.model.CalendarEvent
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.ExecutiveItemCard
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.components.SectionHeader
import com.inialpha.executiveai.ui.components.formatDueAt
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.CalendarViewModel
import com.inialpha.executiveai.viewmodel.containerViewModelFactory
import com.inialpha.executiveai.viewmodel.executiveAIContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The complete event area: calendar sync, upcoming events (real + AI-accepted), AI-generated
 * event proposals to review, and add/edit/delete for real calendar events. Folds in what used to
 * be the separate Upcoming screen.
 */
@Composable
fun CalendarScreen() {
    val container = executiveAIContainer()
    val viewModel: CalendarViewModel = viewModel(
        factory = containerViewModelFactory(container) { CalendarViewModel(it) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var eventBeingEdited by remember { mutableStateOf<CalendarEvent?>(null) }

    val defaultAccountId = state.calendarEvents.firstOrNull()?.accountId
        ?: state.acceptedUpcoming.firstOrNull()?.accountId

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add event")
            }
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val isEmpty = state.calendarEvents.isEmpty() && state.proposedEvents.isEmpty() && state.acceptedUpcoming.isEmpty()

        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Calendar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (state.isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sync calendar")
                    }
                }
            }
            state.statusMessage?.let {
                Text(it, color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp))
            }

            if (isEmpty) {
                EmptyState("Nothing on your calendar", "Sync an account or add an event to get started.")
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.proposedEvents.isNotEmpty()) {
                    item { SectionHeader("AI-suggested events") }
                    items(state.proposedEvents, key = { it.id }) { item ->
                        ExecutiveItemCard(
                            item = item,
                            onAccept = { viewModel.acceptProposedEvent(item) },
                            onReject = { viewModel.rejectProposedEvent(item.id) },
                        )
                    }
                }

                if (state.calendarEvents.isNotEmpty()) {
                    item { SectionHeader("Your calendar") }
                    items(state.calendarEvents, key = { it.id }) { event ->
                        CalendarEventRow(event, onClick = { eventBeingEdited = event })
                    }
                }

                if (state.acceptedUpcoming.isNotEmpty()) {
                    item { SectionHeader("Upcoming from Executive AI") }
                    items(state.acceptedUpcoming, key = { it.id }) { item ->
                        ExecutiveItemCard(item = item, onComplete = { viewModel.completeItem(item.id) })
                    }
                }
            }
        }
    }

    if (showAddDialog && defaultAccountId != null) {
        EventEditDialog(
            title = "Add event",
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { title, description, location, start, end ->
                viewModel.addEvent(defaultAccountId, title, description, location, start, end)
                showAddDialog = false
            },
        )
    }

    eventBeingEdited?.let { event ->
        EventEditDialog(
            title = "Edit event",
            initial = event,
            onDismiss = { eventBeingEdited = null },
            onSave = { title, description, location, start, end ->
                viewModel.updateEvent(event.accountId, event.id, title, description, location, start, end)
                eventBeingEdited = null
            },
            onDelete = {
                viewModel.deleteEvent(event.accountId, event.id)
                eventBeingEdited = null
            },
        )
    }
}

@Composable
private fun CalendarEventRow(event: CalendarEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            formatDueAt(event.startAtMillis)?.let { Text(it, color = TextSecondary, modifier = Modifier.padding(top = 4.dp)) }
            event.location?.let { Text("📍 $it", color = TextSecondary) }
        }
    }
}

private val editFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

@Composable
private fun EventEditDialog(
    title: String,
    initial: CalendarEvent?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?, location: String?, startMillis: Long, endMillis: Long) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var eventTitle by remember { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var location by remember { mutableStateOf(initial?.location.orEmpty()) }
    var startText by remember { mutableStateOf(initial?.let { editFormat.format(Date(it.startAtMillis)) } ?: nextHourDefault()) }
    var endText by remember { mutableStateOf(initial?.let { editFormat.format(Date(it.endAtMillis)) } ?: nextHourPlusOneDefault()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = eventTitle, onValueChange = { eventTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(value = startText, onValueChange = { startText = it }, label = { Text("Start (yyyy-MM-dd HH:mm)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(value = endText, onValueChange = { endText = it }, label = { Text("End (yyyy-MM-dd HH:mm)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) { Text("Delete event") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val start = runCatching { editFormat.parse(startText)?.time }.getOrNull()
                val end = runCatching { editFormat.parse(endText)?.time }.getOrNull()
                if (eventTitle.isBlank() || start == null || end == null) {
                    error = "Enter a title and valid start/end times."
                } else {
                    onSave(eventTitle, description.ifBlank { null }, location.ifBlank { null }, start, end)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun nextHourDefault(): String = editFormat.format(Date(System.currentTimeMillis() + 60 * 60 * 1000))
private fun nextHourPlusOneDefault(): String = editFormat.format(Date(System.currentTimeMillis() + 2 * 60 * 60 * 1000))
