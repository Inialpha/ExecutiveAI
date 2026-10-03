package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.Account
import com.inialpha.executiveai.domain.model.EmailMessage
import com.inialpha.executiveai.domain.model.ExecutiveItem
import com.inialpha.executiveai.domain.model.ExecutiveItemState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val connectedAccounts: List<Account> = emptyList(),
    val importantEmails: List<EmailMessage> = emptyList(),
    val itemsNeedingReview: List<ExecutiveItem> = emptyList(),
    val upcomingAccepted: List<ExecutiveItem> = emptyList(),
)

/**
 * Executive dashboard: aggregates real state from every subsystem — never hard-coded mock data.
 * Deliberately minimal (see DashboardScreen's doc comment): this exposes only what's needed to
 * answer "what matters now", each list already capped before the UI ever sees it, not a full
 * dump of everything the app knows.
 */
class DashboardViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        combine(
            container.accountRepository.observeAccounts(),
            container.emailRepository.observeImportant(),
            container.executiveItemRepository.observeByState(ExecutiveItemState.PROPOSED),
            container.executiveItemRepository.observeUpcomingAccepted(),
        ) { accounts, emails, proposed, upcoming ->
            DashboardUiState(
                isLoading = false,
                connectedAccounts = accounts,
                importantEmails = emails.take(3),
                itemsNeedingReview = proposed.take(3),
                upcomingAccepted = upcoming.take(3),
            )
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    /** Same cascading delete as the Emails screen — every email shown here must have a visible Delete. */
    fun deleteEmail(emailId: String) = viewModelScope.launch {
        container.executiveItemRepository.deleteAllForEmail(emailId)
        container.insightRepository.deleteForEmail(emailId)
        container.emailRepository.deleteEmail(emailId)
    }
}
