package com.inialpha.executiveai.ui.screens.emailinsight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
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
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.ExecutiveItemCard
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.components.SectionHeader
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.EmailInsightViewModel
import com.inialpha.executiveai.viewmodel.executiveAIContainer
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** Email detail: sender/subject/AI summary/importance, plus every extracted item, individually actionable. */
@Composable
fun EmailInsightScreen(emailId: String) {
    val container = executiveAIContainer()
    val viewModel: EmailInsightViewModel = composeViewModel(
        factory = viewModelFactory { initializer { EmailInsightViewModel(container, emailId) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) { LoadingState(); return }
    val email = state.email
    if (email == null) {
        EmptyState("Email not found", state.errorMessage ?: "This email may no longer be synced.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(email.subject, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${email.senderName ?: email.sender} · ${email.sender}", color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
            if (state.insight?.isImportant == true) {
                AssistChip(onClick = {}, label = { Text("Important") }, modifier = Modifier.padding(top = 8.dp))
            }
        }

        state.insight?.let { insight ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Text(insight.summary.ifBlank { "No summary available." }, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        } ?: item {
            EmptyState("No AI insight yet", "This email hasn't been analyzed. Sync insights from the Dashboard.")
        }

        if (state.proposedItems.isNotEmpty()) {
            item { SectionHeader("Extracted for your review") }
            items(state.proposedItems) { item ->
                ExecutiveItemCard(
                    item = item,
                    onAccept = { viewModel.accept(item.id) },
                    onReject = { viewModel.reject(item.id) },
                )
            }
        }
    }
}
