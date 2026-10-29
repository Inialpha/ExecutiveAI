package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.Account
import com.inialpha.executiveai.domain.model.ExecutiveItem
import com.inialpha.executiveai.domain.model.ExecutiveItemState
import com.inialpha.executiveai.domain.model.ExecutiveItemType
import com.inialpha.executiveai.notification.ReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val isLoading: Boolean = true,
    val connectedAccounts: List<Account> = emptyList(),
    /** AI-proposed events still awaiting Accept/Reject. */
    val eventsNeedingAction: Int = 0,
    /** AI-proposed tasks and deadlines still awaiting Accept/Reject. */
    val tasksNeedingAction: Int = 0,
    val remindersNeedingAction: Int = 0,
    /** ACCEPTED, not-yet-completed items whose due date falls today, earliest first. */
    val today: List<ExecutiveItem> = emptyList(),
)

/**
 * Home answers two questions only: "what is waiting on a decision from me?" (counts per type,
 * each a link to where the decision is made) and "what is on for today?" (accepted items whose
 * [ExecutiveItem.dueAtMillis] falls inside today's local day). No email list lives here.
 */
class DashboardViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    /** Re-evaluates "today" every minute so the list rolls over at midnight without a reopen. */
    private val minuteTick = flow {
        while (true) {
            emit(Unit)
            delay(60_000)
        }
    }

    init {
        combine(
            container.accountRepository.observeAccounts(),
            container.executiveItemRepository.observeAll(),
            minuteTick,
        ) { accounts, items, _ ->
            val needsDecision = items.filter { it.state == ExecutiveItemState.PROPOSED || it.state == ExecutiveItemState.EDITED }
            val (dayStart, dayEnd) = todayRangeMillis()
            DashboardUiState(
                isLoading = false,
                connectedAccounts = accounts,
                eventsNeedingAction = needsDecision.count { it.type == ExecutiveItemType.EVENT },
                tasksNeedingAction = needsDecision.count { it.type == ExecutiveItemType.TASK || it.type == ExecutiveItemType.DEADLINE },
                remindersNeedingAction = needsDecision.count { it.type == ExecutiveItemType.REMINDER },
                today = items
                    .filter { it.state == ExecutiveItemState.ACCEPTED }
                    .filter { val due = it.dueAtMillis; due != null && due >= dayStart && due < dayEnd }
                    .sortedBy { it.dueAtMillis },
            )
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    /** Marks a today-item done: cancels its alarm and keeps it in storage (hidden from active UI). */
    fun complete(itemId: String) = viewModelScope.launch {
        ReminderScheduler.cancel(container.appContext, itemId)
        container.executiveItemRepository.complete(itemId)
    }

    private fun todayRangeMillis(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to cal.timeInMillis
    }
}
