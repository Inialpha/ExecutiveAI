package com.inialpha.executiveai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.ExecutiveAIApplication
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.ExecutiveItem
import com.inialpha.executiveai.voice.VoiceRecognitionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AssistantUiState(
    val isListening: Boolean = false,
    val transcript: String = "",
    val recentProposals: List<ExecutiveItem> = emptyList(),
    val errorMessage: String? = null,
)

/**
 * AI Assistant foundation. Voice → recognized text → (future NLU) → proposed executive action →
 * user confirmation → execution, per REQUIREMENTS.md section 12. Today this wires the full path
 * up to "proposed executive action": recognized speech becomes a PROPOSED VOICE_COMMAND item the
 * user can review like any other proposal, rather than a real structured intent — true
 * intent/entity extraction from the transcript is the next layer to add here.
 */
class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val container: AppContainer get() = (getApplication<Application>() as ExecutiveAIApplication).container

    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    fun startListening() {
        if (!container.speechRecognizerManager.isRecognitionAvailable()) {
            _state.value = _state.value.copy(errorMessage = "Speech recognition isn't available on this device.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isListening = true, errorMessage = null, transcript = "")
            container.speechRecognizerManager.listen().collect { recognitionState ->
                when (recognitionState) {
                    is VoiceRecognitionState.PartialResult ->
                        _state.value = _state.value.copy(transcript = recognitionState.text)
                    is VoiceRecognitionState.FinalResult -> {
                        _state.value = _state.value.copy(isListening = false, transcript = recognitionState.text)
                        if (recognitionState.text.isNotBlank()) proposeFromTranscript(recognitionState.text)
                    }
                    is VoiceRecognitionState.Error ->
                        _state.value = _state.value.copy(isListening = false, errorMessage = recognitionState.message)
                    VoiceRecognitionState.Listening ->
                        _state.value = _state.value.copy(isListening = true)
                    VoiceRecognitionState.Idle -> Unit
                }
            }
        }
    }

    private suspend fun proposeFromTranscript(text: String) {
        // Best-effort: attribute the voice item to the first connected account, if any.
        val firstAccountId = container.accountRepository.observeAccounts().first().firstOrNull()?.id ?: ""
        val item = container.executiveItemRepository.createVoiceProposal(
            accountId = firstAccountId,
            title = text,
            description = "Captured from voice input — review and confirm before it becomes an action.",
        )
        _state.value = _state.value.copy(recentProposals = _state.value.recentProposals + item)
    }
}
