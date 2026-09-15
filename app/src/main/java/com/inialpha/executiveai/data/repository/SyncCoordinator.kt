package com.inialpha.executiveai.data.repository

import com.inialpha.executiveai.data.auth.AccountAuthScopes
import com.inialpha.executiveai.data.auth.AuthorizationOutcome
import com.inialpha.executiveai.data.auth.GoogleAuthManager
import com.inialpha.executiveai.data.settings.SyncSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/** One event from a running [SyncCoordinator.syncAll] call — see that function's doc comment. */
sealed class SyncEvent {
    data class EmailProgress(val accountLabel: String, val progress: EmailProcessingProgress) : SyncEvent()
    data class Finished(val anyFailure: Boolean, val totalProcessed: Int, val totalFailed: Int) : SyncEvent()
}

/**
 * The single implementation of "synchronize Gmail + Calendar for every enabled account, then
 * process each account's pending emails sequentially" — used by both the Accounts screen and the
 * Emails screen's "Sync Emails" action, so the two never duplicate this orchestration (per
 * REQUIREMENTS change request section 3: "Reuse the existing synchronization logic and expose it
 * from the appropriate UI"). Each caller (AccountsViewModel, EmailsViewModel) just collects
 * [syncAll] into its own local UI state.
 */
class SyncCoordinator(
    private val accountRepository: AccountRepository,
    private val emailRepository: EmailRepository,
    private val calendarRepository: CalendarRepository,
    private val insightRepository: InsightRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
) {
    /**
     * Synchronizes Gmail + Calendar for every account with sync enabled, within the user's
     * currently selected sync window (see Settings). Gathering is decoupled from processing: for
     * each account, Gmail messages received within the window are fetched and persisted first
     * (regardless of whether AI processing succeeds), then that account's PENDING/FAILED emails
     * *within the window* are processed sequentially, one at a time, oldest first — each result
     * saved immediately. If processing is interrupted partway, whatever completed stays
     * completed; the rest is picked up again on the next call.
     */
    fun syncAll(authManager: GoogleAuthManager): Flow<SyncEvent> = flow {
        val accounts = accountRepository.observeAccounts().first()
        val window = syncSettingsRepository.observeSyncWindow().first()
        val sinceMillis = System.currentTimeMillis() - window.toMillis()

        var anyFailure = false
        var totalProcessed = 0
        var totalFailed = 0

        for (account in accounts) {
            val accountLabel = account.displayName ?: account.email
            if (account.isGmailSyncEnabled) {
                val outcome = authManager.authorize(listOf(AccountAuthScopes.GMAIL_READONLY), account.email)
                if (outcome is AuthorizationOutcome.Success) {
                    emailRepository.syncAccount(outcome.accessToken, account.id, sinceMillis)
                    accountRepository.markGmailSynced(account.id, System.currentTimeMillis())
                    // Process this account's queue sequentially before moving to the next
                    // account — gathering (above) already persisted every fetched email
                    // regardless of what happens here.
                    insightRepository.processAllPendingForAccount(account.id, accountLabel, sinceMillis)
                        .collect { progress ->
                            emit(SyncEvent.EmailProgress(accountLabel, progress))
                            if (progress.phase == EmailProcessingPhase.COMPLETE) {
                                totalProcessed += progress.succeededCount
                                totalFailed += progress.failedCount
                            }
                        }
                } else {
                    anyFailure = true
                }
            }
            if (account.isCalendarSyncEnabled) {
                val outcome = authManager.authorize(listOf(AccountAuthScopes.CALENDAR_READONLY), account.email)
                if (outcome is AuthorizationOutcome.Success) {
                    calendarRepository.syncAccount(outcome.accessToken, account.id)
                    accountRepository.markCalendarSynced(account.id, System.currentTimeMillis())
                } else {
                    anyFailure = true
                }
            }
        }

        emit(SyncEvent.Finished(anyFailure, totalProcessed, totalFailed))
    }
}
