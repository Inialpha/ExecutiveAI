package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.CalendarEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class CalendarUiState(
    val isLoading: Boolean = true,
    val events: List<CalendarEvent> = emptyList(),
)

/** Read-only Calendar screen. Sync is triggered from Connected Accounts / Dashboard "Sync now". */
class CalendarViewModel(container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        container.calendarRepository.observeUpcoming()
            .onEach { _state.value = CalendarUiState(isLoading = false, events = it) }
            .launchIn(viewModelScope)
    }
}
