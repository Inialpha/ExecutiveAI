package com.inialpha.executiveai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inialpha.executiveai.data.repository.EmailProcessingDebugInfo
import com.inialpha.executiveai.data.repository.EmailProcessingPhase
import com.inialpha.executiveai.data.repository.EmailProcessingProgress
import com.inialpha.executiveai.data.repository.FailureStage
import com.inialpha.executiveai.ui.theme.TextSecondary

/**
 * A polished, live progress view for a running email synchronization — replaces a generic
 * "Synchronizing…" spinner with the actual state of the operation: how many emails were found,
 * which one is currently being processed, running success/failure counts, and a per-item
 * success/failure acknowledgment before moving on. See REQUIREMENTS change request sections 5-6.
 *
 * When debug info is present (debug builds only — see EmailDebugConfig), two additive sections
 * appear below the normal user-facing progress:
 *  - "Current request" from [progress]'s own debugInfo — the request just sent for whichever
 *    email is currently in flight, plus that email's failure stage/reason if it failed.
 *  - "Last response received" from [lastResponseDebugInfo] — deliberately a *separate* value
 *    from progress.debugInfo, because progress.debugInfo gets reset the instant the next email's
 *    request starts preparing. This section only changes when a genuinely new response arrives,
 *    so the previous response stays fully visible for inspection right up until that happens —
 *    it does not get blanked out just because the next request has started.
 */
@Composable
fun SyncProgressDialog(
    progress: EmailProcessingProgress,
    accountLabel: String?,
    lastResponseDebugInfo: EmailProcessingDebugInfo?,
    onDismiss: () -> Unit,
) {
    val isComplete = progress.phase == EmailProcessingPhase.COMPLETE
    val fraction = if (progress.totalCount == 0) 1f else progress.currentIndex.toFloat() / progress.totalCount.toFloat()

    AlertDialog(
        onDismissRequest = { if (isComplete) onDismiss() },
        title = { Text("Email Synchronization") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
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
                        val (ackText, ackColor) = phaseAcknowledgment(progress.phase)
                        Text(ackText, color = ackColor, fontWeight = FontWeight.Bold)
                    }

                    progress.debugInfo?.let { debug ->
                        Spacer(Modifier.height(20.dp))
                        Text("Technical debug information", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "(development-only trace — see EmailDebugConfig)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        CurrentRequestSection(debug)
                    }

                    lastResponseDebugInfo?.let { response ->
                        Spacer(Modifier.height(16.dp))
                        LastResponseSection(response)
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

@Composable
private fun phaseAcknowledgment(phase: EmailProcessingPhase): Pair<String, Color> = when (phase) {
    EmailProcessingPhase.REQUEST_PREPARED -> "Preparing request…" to TextSecondary
    EmailProcessingPhase.REQUEST_SENT -> "Waiting for backend response…" to TextSecondary
    EmailProcessingPhase.RESPONSE_RECEIVED -> "Response received…" to TextSecondary
    EmailProcessingPhase.PARSING_RESPONSE -> "Parsing response…" to TextSecondary
    EmailProcessingPhase.SAVING_RESULT -> "Saving result…" to TextSecondary
    EmailProcessingPhase.SUCCEEDED -> "Successfully processed" to Color(0xFF22C55E)
    EmailProcessingPhase.FAILED -> "Processing failed" to MaterialTheme.colorScheme.error
    else -> "Processing…" to TextSecondary
}

/** The request just sent for whichever email is currently in flight, plus its failure info if any. */
@Composable
private fun CurrentRequestSection(debug: EmailProcessingDebugInfo) {
    if (debug.failureStage != FailureStage.NONE) {
        DebugRow("FAILED AT", debug.failureStage.name.replace('_', ' '), color = MaterialTheme.colorScheme.error)
        debug.failureReason?.let { DebugRow("REASON", it, color = MaterialTheme.colorScheme.error) }
    }
    if (debug.requestBodyJson.isNotBlank()) {
        Text("Request sent:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        MonospaceBlock(debug.requestBodyJson)
    }
}

/**
 * Sticky: shows whatever the *last actually-received* backend response was. Stays on screen,
 * unchanged, through the next email's request-prepared/request-sent phases — only replaced when
 * a new response genuinely arrives (see AccountsViewModel.syncAll's collect block).
 */
@Composable
private fun LastResponseSection(response: EmailProcessingDebugInfo) {
    Text("Last response received", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    Text(
        "(stays visible until the next response arrives)",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    response.httpStatusCode?.let { DebugRow("HTTP STATUS", it.toString()) }
    response.httpSuccess?.let { DebugRow("HTTP SUCCESS", it.toString()) }

    response.rawResponseBody?.let {
        Text("Raw response body:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        MonospaceBlock(it)
    }
    response.parsedResultJson?.let {
        Text("Parsed result:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        MonospaceBlock(it)
    }
    response.parseErrorMessage?.let {
        Text("Parse error:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        MonospaceBlock(it)
    }
}

@Composable
private fun DebugRow(label: String, value: String, color: Color = TextSecondary) {
    Row(modifier = Modifier.padding(top = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun MonospaceBlock(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = TextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp),
    )
}
