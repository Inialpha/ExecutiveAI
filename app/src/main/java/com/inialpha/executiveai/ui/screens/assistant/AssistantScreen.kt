package com.inialpha.executiveai.ui.screens.assistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.ui.components.EmptyState
import com.inialpha.executiveai.ui.components.ExecutiveItemCard
import com.inialpha.executiveai.ui.components.SectionHeader
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.AssistantViewModel

/**
 * AI Assistant foundation: native speech recognition → transcript → PROPOSED voice-command item.
 * See AssistantViewModel for the current scope/limits of the intent-interpretation step.
 */
@Composable
fun AssistantScreen() {
    val viewModel: AssistantViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startListening()
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("AI Assistant", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Speak an instruction and Executive AI will turn it into a proposal you can review.",
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        Button(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) viewModel.startListening() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null)
            Text(if (state.isListening) "  Listening…" else "  Speak", modifier = Modifier.padding(start = 4.dp))
        }

        if (state.transcript.isNotBlank()) {
            Text(state.transcript, modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyLarge)
        }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }

        if (state.recentProposals.isNotEmpty()) {
            SectionHeader("Recent voice proposals", modifier = Modifier.padding(top = 24.dp))
            LazyColumn(contentPadding = PaddingValues(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.recentProposals) { item -> ExecutiveItemCard(item) }
            }
        } else if (!state.isListening) {
            EmptyState("Nothing said yet", "Tap Speak and try something like \"Remind me to call the bank tomorrow at 10am.\"")
        }
    }
}
