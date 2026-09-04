package com.inialpha.executiveai.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.ExecutiveItemCard
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.components.SectionHeader
import com.inialpha.executiveai.viewmodel.TasksViewModel
import com.inialpha.executiveai.viewmodel.containerViewModelFactory
import com.inialpha.executiveai.viewmodel.executiveAIContainer

/** Tasks/Actions screen: AI-proposed actions the user must Accept/Edit/Reject, plus accepted and deadline items. */
@Composable
fun TasksScreen() {
    val container = executiveAIContainer()
    val viewModel: TasksViewModel = viewModel(
        factory = containerViewModelFactory(container) { TasksViewModel(it) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) { LoadingState(); return }
    val isEmpty = state.proposedTasks.isEmpty() && state.acceptedTasks.isEmpty() && state.deadlines.isEmpty()
    if (isEmpty) {
        EmptyState("No tasks yet", "Actions the AI extracts from your emails will appear here for review.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
