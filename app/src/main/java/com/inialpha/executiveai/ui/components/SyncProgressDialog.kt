package com.inialpha.executiveai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inialpha.executiveai.data.repository.EmailProcessingPhase
import com.inialpha.executiveai.data.repository.EmailProcessingProgress
import com.inialpha.executiveai.ui.theme.TextSecondary

/**
 * A polished, live progress view for a running email synchronization — replaces a generic
 * "Synchronizing…" spinner with the actual state of the operation: how many emails were found,
 * which one is currently being processed, running success/failure counts, and a per-item
 * success/failure acknowledgment before moving on.
 *
 * (The verbose request/response/HTTP debug trace that used to appear here has been removed now
 * that it served its purpose diagnosing the original backend/Android schema mismatch — see git
 * history if that level of detail is ever needed again. Error handling itself is unaffected;
 * failures are still logged via Logcat in InsightRepository.)
 */
@Composable
fun SyncProgressDialog(
    progress: EmailProcessingProgress,
    accountLabel: String?,
    onDismiss: () -> Unit,
) {
    val isComplete = progress.phase == EmailProcessingPhase.COMPLETE
    val fraction = if (progress.totalCount == 0) 1f else progress.currentIndex.toFloat() / progress.totalCount.toFloat()

    AlertDialog(
        onDismissRequest = { if (isComplete) onDismiss() },
        title = { Text("Email Synchronization") },
        text = {
            Column {
                accountLabel?.let {
                    Text(it, color = TextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                }

                if (progress.totalCount == 0) {
                    Text("No new emails to process.", color = TextSecondary)
                } else {
                    Text(
                        "${progress.totalCount} email${if (progress.totalCount == 1) "" else "s"} found",
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Text(
                        "${progress.currentIndex} / ${progress.totalCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isComplete) "Done" else "Processing email ${progress.currentIndex} of ${progress.totalCount}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(Modifier.height(12.dp))
                    Row {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.padding(end = 4.dp))
                        Text("${progress.succeededCount} successfully processed")
                    }
                    if (progress.failedCount > 0) {
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 4.dp))
                            Text("${progress.failedCount} failed")
                        }
                    }

                    if (!isComplete && progress.currentEmailLabel.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Current:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(progress.currentEmailLabel, color = TextSecondary, maxLines = 2)
                        Spacer(Modifier.height(4.dp))
                        val (ackText, ackColor) = when (progress.phase) {
                            EmailProcessingPhase.SUCCEEDED -> "Successfully processed" to Color(0xFF22C55E)
                            EmailProcessingPhase.FAILED -> "Processing failed" to MaterialTheme.colorScheme.error
                            else -> "Processing…" to TextSecondary
                        }
                        Text(ackText, color = ackColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            if (isComplete) {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
    )
}
