package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.Account
import com.inialpha.executiveai.domain.model.EmailInsight
import com.inialpha.executiveai.domain.model.EmailMessage
import com.inialpha.executiveai.domain.model.EmailProcessingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class EmailWithInsight(val email: EmailMessage, val insight: EmailInsight?)

data class EmailsUiState(
    val isLoading: Boolean = true,
    /** Tab strip source — one tab per connected account, per REQUIREMENTS change request section 8. */
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: String? = null,
    /** Only emails belonging to [selectedAccountId] — accounts are never mixed in this list. */
    val emails: List<EmailWithInsight> = emptyList(),
)

/**
 * Emails screen: account-tabbed list of AI-processed emails. Each account's emails are kept
 * strictly separate — switching the selected tab swaps the entire list rather than filtering a
 * combined one, so there's no risk of a stray cross-account item leaking through.
 */
class EmailsViewModel(container: AppContainer) : ViewModel() {

    private val selectedAccountId = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(EmailsUiState())
    val state: StateFlow<EmailsUiState> = _state.asStateFlow()

    init {
        container.accountRepository.observeAccounts()
            .onEach { accounts ->
                // Default to the first connected account, per the change request, and keep the
                // selection stable if it's still present after the account list changes.
                val current = selectedAccountId.value
                if (current == null || accounts.none { it.id == current }) {
                    selectedAccountId.value = accounts.firstOrNull()?.id
                }
                _state.value = _state.value.copy(accounts = accounts, selectedAccountId = selectedAccountId.value)
            }
            .launchIn(viewModelScope)

        selectedAccountId
            .flatMapLatest { accountId ->
                if (accountId == null) {
                    flowOf<List<EmailWithInsight>>(emptyList())
                } else {
                    combine(
                        container.emailRepository.observeForAccount(accountId),
                        container.insightRepository.observeAll(),
                    ) { emails, insights ->
                        val insightByEmailId = insights.associateBy { it.emailId }
                        emails
                            .filter { it.processingStatus == EmailProcessingStatus.COMPLETED }
                            .map { EmailWithInsight(it, insightByEmailId[it.id]) }
                    }
                }
            }
            .onEach { emails -> _state.value = _state.value.copy(isLoading = false, emails = emails) }
            .launchIn(viewModelScope)
    }

    fun selectAccount(accountId: String) {
        selectedAccountId.value = accountId
        _state.value = _state.value.copy(selectedAccountId = accountId, isLoading = true)
    }
}
