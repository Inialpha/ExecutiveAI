package com.inialpha.executiveai.notification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inialpha.executiveai.ui.theme.ExecutiveAITheme

/** Full-screen alarm screen: wakes the screen, shows over the lock screen, offers Snooze / Dismiss. */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()

        setContent {
            ExecutiveAITheme {
                AlarmScreen(
                    title = title,
                    body = body,
                    onDismiss = { startService(AlarmService.dismissIntent(this)); finish() },
                    onSnooze = { startService(AlarmService.snoozeIntent(this)); finish() },
                    onStopped = { finish() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_TITLE = "alarm_title"
        private const val EXTRA_BODY = "alarm_body"

        fun intent(context: Context, title: String, body: String): Intent =
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_BODY, body)
    }
}

@Composable
private fun AlarmScreen(title: String, body: String, onDismiss: () -> Unit, onSnooze: () -> Unit, onStopped: () -> Unit) {
    val ringing by AlarmService.isRinging.collectAsStateWithLifecycle()
    // Closes itself if the alarm was dismissed from the notification instead.
    var seenRinging by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(ringing) {
        if (ringing) seenRinging = true else if (seenRinging) onStopped()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Executive AI", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (body.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(48.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Dismiss") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Snooze 10 min") }
        }
    }
}
