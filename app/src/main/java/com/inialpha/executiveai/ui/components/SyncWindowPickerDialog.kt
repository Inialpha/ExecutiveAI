package com.inialpha.executiveai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inialpha.executiveai.domain.model.SyncWindow

/**
 * Lets the user pick how far back email synchronization should look — one of [SyncWindow.PRESETS].
 * See REQUIREMENTS change request section 4. The preset list — rather than freeform number entry
 * — keeps this quick and unambiguous while [SyncWindow] itself stays a plain amount+unit pair, so
 * more presets/units can be added later without changing this dialog's shape.
 */
@Composable
fun SyncWindowPickerDialog(
    current: SyncWindow,
    onSelect: (SyncWindow) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync window") },
        text = {
            Column {
                SyncWindow.PRESETS.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == current, onClick = { onSelect(option) })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == current, onClick = { onSelect(option) })
                        Text(option.label(), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
