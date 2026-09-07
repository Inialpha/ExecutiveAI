package com.inialpha.executiveai.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.ui.components.SyncWindowPickerDialog
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(onOpenAccounts: () -> Unit, onOpenReminders: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showWindowPicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Executive AI v${state.appVersion}", color = TextSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        TextButton(onClick = onOpenAccounts) { Text("Connected accounts") }
        TextButton(onClick = onOpenReminders) { Text("Reminders") }
        TextButton(onClick = { showWindowPicker = true }) { Text("Email sync window: ${state.syncWindow.label()}") }

        if (!state.canScheduleExactAlarms) {
            Text(
                "Exact alarms aren't currently permitted for Executive AI, so reminders may be delayed. " +
                    "Grant this from the device's app settings for on-time delivery.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    if (showWindowPicker) {
        SyncWindowPickerDialog(
            current = state.syncWindow,
            onSelect = { viewModel.setSyncWindow(it) },
            onDismiss = { showWindowPicker = false },
        )
    }
}
