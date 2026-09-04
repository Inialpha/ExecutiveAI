package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.EmailInsight
import com.inialpha.executiveai.domain.model.EmailMessage
import com.inialpha.executiveai.domain.model.ExecutiveItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EmailInsightUiState(
    val isLoading: Boolean = true,
    val email: EmailMessage? = null,
    val insight: EmailInsight? = null,
    val proposedItems: List<ExecutiveItem> = emptyList(),
    val errorMessage: String? = null,
)

/**
 * Detail screen for one email: sender/subject/summary/importance plus the events/actions/
 * deadlines/reminders the AI extracted, each still individually Accept/Edit/Reject-able.
 */
class EmailInsightViewModel(
    private val container: AppContainer,
    private val emailId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(EmailInsightUiState())
    val state: StateFlow<EmailInsightUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val email = container.emailRepository.getById(emailId)
            val insight = container.insightRepository.getForEmail(emailId)
            val items = container.executiveItemRepository.observeAll()
            _state.value = _state.value.copy(
                isLoading = false,
                email = email,
                insight = insight,
                proposedItems = emptyList(),
                errorMessage = if (email == null) "Email not found" else null,
            )
            items.collect { all ->
                _state.value = _state.value.copy(proposedItems = all.filter { it.sourceEmailId == emailId })
            }
        }
    }

    fun accept(itemId: String) = viewModelScope.launch { container.executiveItemRepository.accept(itemId) }
    fun reject(itemId: String) = viewModelScope.launch { container.executiveItemRepository.reject(itemId) }
    fun edit(itemId: String, title: String, description: String?) =
        viewModelScope.launch { container.executiveItemRepository.edit(itemId, title = title, description = description) }
}
