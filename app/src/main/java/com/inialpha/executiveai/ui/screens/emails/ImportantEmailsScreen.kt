package com.inialpha.executiveai.ui.screens.emails

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
import com.inialpha.executiveai.ui.components.EmailSummaryCard
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.viewmodel.ImportantEmailsViewModel
import com.inialpha.executiveai.viewmodel.containerViewModelFactory
import com.inialpha.executiveai.viewmodel.executiveAIContainer

@Composable
fun ImportantEmailsScreen(onOpenEmail: (String) -> Unit) {
    val container = executiveAIContainer()
    val viewModel: ImportantEmailsViewModel = viewModel(
        factory = containerViewModelFactory(container) { ImportantEmailsViewModel(it) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) { LoadingState(); return }
    if (state.emails.isEmpty()) {
        EmptyState("No important emails", "Emails the AI flags as important will appear here after syncing.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.emails) { email -> EmailSummaryCard(email, onClick = { onOpenEmail(email.id) }) }
    }
}
