package com.inialpha.executiveai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inialpha.executiveai.domain.model.EmailMessage
import com.inialpha.executiveai.domain.model.ExecutiveItem
import com.inialpha.executiveai.domain.model.ExecutiveItemState
import com.inialpha.executiveai.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())

fun formatDueAt(millis: Long?): String? = millis?.let { dateFormat.format(Date(it)) }

@Composable
fun EmailSummaryCard(
    email: EmailMessage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    email.senderName ?: email.sender,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!email.isRead) {
                    AssistChip(
                        onClick = {},
                        label = { Text("New") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primary),
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete")
                    }
                }
            }
            Text(
                email.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                email.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Renders one [ExecutiveItem] with state-appropriate actions:
 * PROPOSED/EDITED → Accept / Edit / Reject. ACCEPTED → Mark complete / Delete. Everything else
 * is read-only (a COMPLETED item is filtered out of every active-item list before this card ever
 * sees it, so there is no "completed" action row to render here).
 */
@Composable
fun ExecutiveItemCard(
    item: ExecutiveItem,
    onAccept: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(item.type.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                StateBadge(item.state)
            }
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            item.description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
            formatDueAt(item.dueAtMillis)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(top = 6.dp))
            }
            item.location?.let {
                Text("📍 $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            val showProposalActions = (item.state == ExecutiveItemState.PROPOSED || item.state == ExecutiveItemState.EDITED) &&
                (onAccept != null || onReject != null || onEdit != null)
            if (showProposalActions) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    onAccept?.let { Button(onClick = it) { Text("Accept") } }
                    onEdit?.let { OutlinedButton(onClick = it) { Text("Edit") } }
                    onReject?.let { TextButton(onClick = it) { Text("Reject") } }
                }
            } else if (item.state == ExecutiveItemState.ACCEPTED && (onComplete != null || onDelete != null)) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    onComplete?.let { Button(onClick = it) { Text("Mark complete") } }
                    onDelete?.let { OutlinedButton(onClick = it) { Text("Delete") } }
                }
            }
        }
    }
}

@Composable
private fun StateBadge(state: ExecutiveItemState) {
    val label = when (state) {
        ExecutiveItemState.PROPOSED -> "Proposed"
        ExecutiveItemState.EDITED -> "Edited"
        ExecutiveItemState.ACCEPTED -> "Accepted"
        ExecutiveItemState.REJECTED -> "Rejected"
        ExecutiveItemState.COMPLETED -> "Completed"
    }
    AssistChip(onClick = {}, label = { Text(label) })
}
