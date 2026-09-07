package com.inialpha.executiveai.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.ExecutiveItemCard
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.components.SectionHeader
import com.inialpha.executiveai.viewmodel.TasksViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/** Tasks/Actions screen: AI-proposed actions the user must Accept/Edit/Reject, accepted/deadline items, and manual "Add Task". */
@Composable
fun TasksScreen() {
    val viewModel: TasksViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val isEmpty = state.proposedTasks.isEmpty() && state.acceptedTasks.isEmpty() && state.deadlines.isEmpty()
        if (isEmpty) {
            EmptyState(
                "No tasks yet",
                "Actions the AI extracts from your emails appear here for review — or tap + to add one yourself.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.proposedTasks.isNotEmpty()) {
                item { SectionHeader("Needs your review") }
                items(state.proposedTasks) { item ->
                    ExecutiveItemCard(
                        item = item,
                        onAccept = { viewModel.accept(item.id) },
                        onReject = { viewModel.reject(item.id) },
                    )
                }
            }
            if (state.acceptedTasks.isNotEmpty()) {
                item { SectionHeader("In progress") }
                items(state.acceptedTasks) { item ->
                    ExecutiveItemCard(item = item, onComplete = { viewModel.complete(item.id) })
                }
            }
            if (state.deadlines.isNotEmpty()) {
                item { SectionHeader("Deadlines") }
                items(state.deadlines) { item ->
                    ExecutiveItemCard(
                        item = item,
                        onAccept = if (item.state.name in setOf("PROPOSED", "EDITED")) { { viewModel.accept(item.id) } } else null,
                        onReject = if (item.state.name in setOf("PROPOSED", "EDITED")) { { viewModel.reject(item.id) } } else null,
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            accounts = state.accounts,
            onDismiss = { showAddDialog = false },
            onSave = { accountId, title, description, dueAtMillis, wantsReminder ->
                viewModel.addTask(accountId, title, description, dueAtMillis, wantsReminder)
                showAddDialog = false
            },
        )
    }
}

private val taskDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

@Composable
private fun AddTaskDialog(
    accounts: List<com.inialpha.executiveai.domain.model.Account>,
    onDismiss: () -> Unit,
    onSave: (accountId: String, title: String, description: String?, dueAtMillis: Long?, wantsReminder: Boolean) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueText by remember { mutableStateOf("") }
    var wantsReminder by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add task") },
        text = {
            Column {
                if (accounts.size > 1) {
                    val selectedIndex = accounts.indexOfFirst { it.id == selectedAccountId }.coerceAtLeast(0)
                    ScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 0.dp) {
                        accounts.forEachIndexed { index, account ->
                            Tab(
                                selected = index == selectedIndex,
                                onClick = { selectedAccountId = account.id },
                                text = { Text(account.displayName ?: account.email, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(
                    value = dueText,
                    onValueChange = { dueText = it },
                    label = { Text("Due date/time (yyyy-MM-dd HH:mm, optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = wantsReminder, onCheckedChange = { wantsReminder = it }, enabled = dueText.isNotBlank())
                    Text("Remind me at the due time")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val accountId = selectedAccountId
                if (title.isBlank() || accountId == null) {
                    error = if (accountId == null) "Connect a Google account first." else "Enter a task title."
                    return@TextButton
                }
                val dueAtMillis = if (dueText.isBlank()) null else runCatching { taskDateFormat.parse(dueText)?.time }.getOrNull()
                if (dueText.isNotBlank() && dueAtMillis == null) {
                    error = "Enter a valid due date/time or leave it blank."
                    return@TextButton
                }
                onSave(accountId, title, description.ifBlank { null }, dueAtMillis, wantsReminder && dueAtMillis != null)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
