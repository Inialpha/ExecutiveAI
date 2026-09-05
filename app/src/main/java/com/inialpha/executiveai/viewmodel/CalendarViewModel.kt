package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.data.auth.AccountAuthScopes
import com.inialpha.executiveai.data.auth.AuthorizationOutcome
import com.inialpha.executiveai.data.repository.CalendarWriteResult
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.CalendarEvent
import com.inialpha.executiveai.domain.model.ExecutiveItem
import com.inialpha.executiveai.domain.model.ExecutiveItemState
import com.inialpha.executiveai.domain.model.ExecutiveItemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class CalendarUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
    /** Real synced calendar events (any provider — see CalendarRepository doc comment). */
    val calendarEvents: List<CalendarEvent> = emptyList(),
    /** AI-extracted EVENT proposals awaiting Accept/Edit/Reject — "AI-generated events". */
    val proposedEvents: List<ExecutiveItem> = emptyList(),
    /** Accepted events/deadlines/reminders/tasks with a due date — the former "Upcoming" list, now folded in here. */
    val acceptedUpcoming: List<ExecutiveItem> = emptyList(),
)

/**
 * Calendar is now the single, central place for everything schedule-related: calendar view,
 * upcoming events, add/edit/delete, AI-generated event proposals, and calendar synchronization —
 * the former standalone Upcoming screen's data is folded directly into this ViewModel.
 */
class CalendarViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        combine(
            container.calendarRepository.observeUpcoming(),
            container.executiveItemRepository.observeByType(ExecutiveItemType.EVENT),
            container.executiveItemRepository.observeUpcomingAccepted(),
        ) { events, eventItems, accepted ->
            Triple(events, eventItems.filter { it.state == ExecutiveItemState.PROPOSED || it.state == ExecutiveItemState.EDITED }, accepted)
        }.onEach { (events, proposed, accepted) ->
            _state.value = _state.value.copy(
                isLoading = false,
                calendarEvents = events,
                proposedEvents = proposed,
                acceptedUpcoming = accepted,
            )
        }.launchIn(viewModelScope)
    }

    /** Calendar synchronization — extensible to future providers; see CalendarRepository doc comment. */
    fun syncNow() {
        val authManager = container.googleAuthManager ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true, statusMessage = null)
            val accounts = container.accountRepository.observeAccounts().first()
                .filter { it.isCalendarSyncEnabled }

            var anyFailure = false
            for (account in accounts) {
                val outcome = authManager.authorize(listOf(AccountAuthScopes.CALENDAR_READONLY), account.email)
                if (outcome is AuthorizationOutcome.Success) {
                    container.calendarRepository.syncAccount(outcome.accessToken, account.id)
                    container.accountRepository.markCalendarSynced(account.id, System.currentTimeMillis())
                } else {
                    anyFailure = true
                }
            }
            _state.value = _state.value.copy(
                isSyncing = false,
                statusMessage = if (anyFailure) "Some accounts need re-authorization." else "Calendar synced.",
            )
        }
    }

    fun addEvent(accountId: String, title: String, description: String?, location: String?, startMillis: Long, endMillis: Long) {
        viewModelScope.launch {
            withCalendarEventsAccess(accountId) { token ->
                container.calendarRepository.createEvent(token, accountId, title, description, location, startMillis, endMillis)
            }
        }
    }

    fun updateEvent(accountId: String, eventId: String, title: String, description: String?, location: String?, startMillis: Long, endMillis: Long) {
        viewModelScope.launch {
            withCalendarEventsAccess(accountId) { token ->
                container.calendarRepository.updateEvent(token, accountId, eventId, title, description, location, startMillis, endMillis)
            }
        }
    }

    fun deleteEvent(accountId: String, eventId: String) {
        viewModelScope.launch {
            withCalendarEventsAccess(accountId) { token ->
                container.calendarRepository.deleteEvent(token, eventId)
            }
        }
    }

    /** Accept a proposed AI event: transitions it (Commitment step) and mirrors it into the real calendar. */
    fun acceptProposedEvent(item: ExecutiveItem) {
        viewModelScope.launch {
            val accepted = container.executiveItemRepository.accept(item.id) ?: return@launch
            val start = accepted.dueAtMillis ?: return@launch
            val end = start + 60 * 60 * 1000 // default 1-hour duration when the AI didn't specify one
            withCalendarEventsAccess(accepted.accountId) { token ->
                container.calendarRepository.createEvent(token, accepted.accountId, accepted.title, accepted.description, accepted.location, start, end)
            }?.let { result ->
                if (result is CalendarWriteResult.Success) {
                    container.executiveItemRepository.markExecuted(accepted.id, result.eventId)
                }
            }
        }
    }

    fun rejectProposedEvent(itemId: String) = viewModelScope.launch { container.executiveItemRepository.reject(itemId) }

    fun completeItem(itemId: String) = viewModelScope.launch { container.executiveItemRepository.complete(itemId) }

    private suspend fun withCalendarEventsAccess(accountId: String, block: suspend (String) -> CalendarWriteResult): CalendarWriteResult? {
        val authManager = container.googleAuthManager ?: return null
        val account = container.accountRepository.getAccount(accountId) ?: return null
        val outcome = authManager.authorize(listOf(AccountAuthScopes.CALENDAR_EVENTS), account.email)
        if (outcome !is AuthorizationOutcome.Success) {
            _state.value = _state.value.copy(statusMessage = "Calendar access needs re-authorization.")
            return null
        }
        val result = block(outcome.accessToken)
        if (result is CalendarWriteResult.Failure) {
            _state.value = _state.value.copy(statusMessage = result.message)
        }
        return result
    }
}
