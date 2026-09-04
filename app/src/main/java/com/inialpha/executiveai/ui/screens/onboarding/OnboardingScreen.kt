package com.inialpha.executiveai.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.executiveai.ui.components.ErrorBanner
import com.inialpha.executiveai.ui.theme.TextSecondary
import com.inialpha.executiveai.viewmodel.OnboardingViewModel
import com.inialpha.executiveai.viewmodel.containerViewModelFactory
import com.inialpha.executiveai.viewmodel.executiveAIContainer

/**
 * First screen a new user sees: explains what Executive AI does, then lets them connect their
 * Google account (the trigger for the whole Information → ... → Execution pipeline).
 */
@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val container = executiveAIContainer()
    val viewModel: OnboardingViewModel = viewModel(
        factory = containerViewModelFactory(container) { OnboardingViewModel(it) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.connected) {
        if (state.connected) onOnboardingComplete()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Executive AI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Your personal executive assistant",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Text(
            "Executive AI reads what matters in your Gmail, turns it into clear priorities, " +
                "and proposes events, tasks, deadlines, and reminders for you to review. Nothing " +
                "becomes a real commitment until you say so.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        state.errorMessage?.let {
            ErrorBanner(it, modifier = Modifier.padding(bottom = 16.dp))
        }

        if (state.isConnecting) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            Button(
                onClick = { viewModel.connectGoogleAccount() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Connect your Google account")
            }
        }
    }
}
