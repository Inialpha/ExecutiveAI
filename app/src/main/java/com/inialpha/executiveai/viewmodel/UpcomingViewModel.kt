package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.CalendarEvent
import com.inialpha.executiveai.domain.model.ExecutiveItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class UpcomingUiState(
    val isLoading: Boolean = true,
    val calendarEvents: List<CalendarEvent> = emptyList(),
    /** Accepted events/deadlines/reminders/tasks with a due date, not yet mirrored into Calendar. */
    val acceptedItems: List<ExecutiveItem> = emptyList(),
)

/** Unified upcoming view: real Calendar events + accepted executive items, merged by time. */
class UpcomingViewModel(container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(UpcomingUiState())
    val state: StateFlow<UpcomingUiState> = _state.asStateFlow()

    init {
        combine(
            container.calendarRepository.observeUpcoming(),
            container.executiveItemRepository.observeUpcomingAccepted(),
        ) { events, items -> UpcomingUiState(isLoading = false, calendarEvents = events, acceptedItems = items) }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }
}
