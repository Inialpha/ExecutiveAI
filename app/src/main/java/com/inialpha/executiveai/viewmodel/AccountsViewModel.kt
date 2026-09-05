package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.data.auth.AccountAuthScopes
import com.inialpha.executiveai.data.auth.AuthorizationOutcome
import com.inialpha.executiveai.data.repository.ConnectAccountResult
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class AccountsUiState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val isConnecting: Boolean = false,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
)

/**
 * Connected Accounts screen: add / view / select-toggle / synchronize / disconnect, all backed
 * by real [com.inialpha.executiveai.data.auth.GoogleAuthManager] + repository calls — never mocked.
 */
class AccountsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(AccountsUiState())
    val state: StateFlow<AccountsUiState> = _state.asStateFlow()

    init {
        container.accountRepository.observeAccounts()
            .onEach { _state.value = _state.value.copy(isLoading = false, accounts = it) }
            .launchIn(viewModelScope)
    }

    fun addAccount() {
        val authManager = container.googleAuthManager ?: run {
            _state.value = _state.value.copy(statusMessage = "Sign-in isn't available right now.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isConnecting = true, statusMessage = null)
            when (val result = container.accountRepository.connectNewAccount(authManager)) {
                is ConnectAccountResult.Success ->
                    _state.value = _state.value.copy(isConnecting = false, statusMessage = "Connected ${result.account.email}")
                ConnectAccountResult.Cancelled ->
                    _state.value = _state.value.copy(isConnecting = false)
                is ConnectAccountResult.Failure ->
                    _state.value = _state.value.copy(isConnecting = false, statusMessage = result.message)
            }
        }
    }

    fun setGmailSyncEnabled(accountId: String, enabled: Boolean) =
        viewModelScope.launch { container.accountRepository.setGmailSyncEnabled(accountId, enabled) }

    fun setCalendarSyncEnabled(accountId: String, enabled: Boolean) =
        viewModelScope.launch { container.accountRepository.setCalendarSyncEnabled(accountId, enabled) }

    fun disconnect(accountId: String) = viewModelScope.launch {
        container.emailRepository.deleteForAccount(accountId)
        container.calendarRepository.deleteForAccount(accountId)
        container.accountRepository.disconnectAccount(accountId)
    }

    /**
     * Synchronizes Gmail + Calendar for every account with sync enabled. Per REQUIREMENTS.md,
     * gathering is decoupled from processing: for each account, Gmail messages are fetched and
     * persisted first (regardless of whether AI processing succeeds), then that account's
     * PENDING/FAILED emails are processed sequentially, one at a time, oldest first — see
     * [com.inialpha.executiveai.data.repository.InsightRepository.processAllPendingForAccount].
     * If processing is interrupted partway, whatever completed stays completed; the rest is
     * picked up again on the next call to this function.
     */
    fun syncAll() {
        val authManager = container.googleAuthManager ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true, statusMessage = null)
            val accounts = _state.value.accounts
            var anyFailure = false
            var totalProcessed = 0
            var totalFailed = 0
            for (account in accounts) {
                if (account.isGmailSyncEnabled) {
                    val outcome = authManager.authorize(listOf(AccountAuthScopes.GMAIL_READONLY), account.email)
                    if (outcome is AuthorizationOutcome.Success) {
                        container.emailRepository.syncAccount(outcome.accessToken, account.id)
                        container.accountRepository.markGmailSynced(account.id, System.currentTimeMillis())
                        // Process this account's queue sequentially before moving to the next
                        // account — gathering (above) already persisted every fetched email
                        // regardless of what happens here.
                        val summary = container.insightRepository.processAllPendingForAccount(account.id)
                        totalProcessed += summary.processedCount
                        totalFailed += summary.failedCount
                    } else {
                        anyFailure = true
                    }
                }
                if (account.isCalendarSyncEnabled) {
                    val outcome = authManager.authorize(listOf(AccountAuthScopes.CALENDAR_READONLY), account.email)
                    if (outcome is AuthorizationOutcome.Success) {
                        container.calendarRepository.syncAccount(outcome.accessToken, account.id)
                        container.accountRepository.markCalendarSynced(account.id, System.currentTimeMillis())
                    } else {
                        anyFailure = true
                    }
                }
            }
            val statusMessage = when {
                anyFailure -> "Some accounts need re-authorization."
                totalFailed > 0 -> "Synced. $totalProcessed email(s) processed, $totalFailed failed and will retry next sync."
                else -> "Synced."
            }
            _state.value = _state.value.copy(isSyncing = false, statusMessage = statusMessage)
        }
    }
}
