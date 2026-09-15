package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.data.repository.ConnectAccountResult
import com.inialpha.executiveai.data.repository.EmailProcessingProgress
import com.inialpha.executiveai.data.repository.SyncEvent
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.Account
import com.inialpha.executiveai.domain.model.SyncWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class AccountsUiState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val isConnecting: Boolean = false,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
    val syncWindow: SyncWindow = SyncWindow.DEFAULT,
    /** Live per-email progress for the account currently being processed — drives the sync progress dialog. */
    val syncProgress: EmailProcessingProgress? = null,
    /** Which account's progress is currently shown, for a label alongside [syncProgress]. */
    val syncingAccountLabel: String? = null,
)

/**
 * Connected Accounts screen: add / view / select-toggle / synchronize / disconnect, all backed
 * by real [com.inialpha.executiveai.data.auth.GoogleAuthManager] + repository calls — never
 * mocked. Account management only; the actual sync orchestration lives in
 * [com.inialpha.executiveai.data.repository.SyncCoordinator], shared with the Emails screen's
 * own "Sync Emails" action so the two never duplicate that logic.
 */
class AccountsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(AccountsUiState())
    val state: StateFlow<AccountsUiState> = _state.asStateFlow()

    init {
        container.accountRepository.observeAccounts()
            .onEach { _state.value = _state.value.copy(isLoading = false, accounts = it) }
            .launchIn(viewModelScope)

        container.syncSettingsRepository.observeSyncWindow()
            .onEach { _state.value = _state.value.copy(syncWindow = it) }
            .launchIn(viewModelScope)
    }

    fun setSyncWindow(window: SyncWindow) = viewModelScope.launch { container.syncSettingsRepository.setSyncWindow(window) }

    /**
     * "Add account": always shows the Google account chooser and never silently reuses whichever
     * account was connected previously — see AccountRepository.connectNewAccount /
     * GoogleAuthManager.clearCredentialState. Existing connected accounts are untouched.
     */
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

    fun dismissSyncProgress() {
        _state.value = _state.value.copy(syncProgress = null, syncingAccountLabel = null)
    }

    fun syncAll() {
        val authManager = container.googleAuthManager ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true, statusMessage = null)
            container.syncCoordinator.syncAll(authManager).collect { event ->
                when (event) {
                    is SyncEvent.EmailProgress ->
                        _state.value = _state.value.copy(syncProgress = event.progress, syncingAccountLabel = event.accountLabel)
                    is SyncEvent.Finished -> {
                        val statusMessage = when {
                            event.anyFailure -> "Some accounts need re-authorization."
                            event.totalFailed > 0 -> "Synced. ${event.totalProcessed} email(s) processed, ${event.totalFailed} failed and will retry next sync."
                            else -> "Synced."
                        }
                        _state.value = _state.value.copy(isSyncing = false, statusMessage = statusMessage)
                    }
                }
            }
        }
    }
}
