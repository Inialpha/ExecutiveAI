package com.inialpha.executiveai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.ExecutiveAIApplication
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.ExecutiveItem
import com.inialpha.executiveai.domain.model.ExecutiveItemType
import com.inialpha.executiveai.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class RemindersUiState(
    val isLoading: Boolean = true,
    val reminders: List<ExecutiveItem> = emptyList(),
    val canScheduleExactAlarms: Boolean = true,
)

/**
 * Reminders screen. Accepting a reminder both transitions its state (ExecutiveItemRepository)
 * and — the "Execution" step of the product workflow — schedules the real Android alarm via
 * [ReminderScheduler].
 */
class RemindersViewModel(application: Application) : AndroidViewModel(application) {
    private val container: AppContainer get() = (getApplication<Application>() as ExecutiveAIApplication).container

    private val _state = MutableStateFlow(RemindersUiState())
    val state: StateFlow<RemindersUiState> = _state.asStateFlow()

    init {
        container.executiveItemRepository.observeByType(ExecutiveItemType.REMINDER)
            .onEach {
                _state.value = RemindersUiState(
                    isLoading = false,
                    reminders = it,
                    canScheduleExactAlarms = ReminderScheduler.canScheduleExactAlarms(getApplication()),
                )
            }
            .launchIn(viewModelScope)
    }

    fun accept(item: ExecutiveItem) = viewModelScope.launch {
        val accepted = container.executiveItemRepository.accept(item.id) ?: return@launch
        val dueAt = accepted.dueAtMillis ?: return@launch
        ReminderScheduler.schedule(
            context = getApplication(),
            itemId = accepted.id,
            title = accepted.title,
            body = accepted.description ?: "",
            triggerAtMillis = dueAt,
        )
    }

    fun reject(id: String) = viewModelScope.launch {
        container.executiveItemRepository.reject(id)
        ReminderScheduler.cancel(getApplication(), id)
    }

    fun complete(id: String) = viewModelScope.launch {
        container.executiveItemRepository.complete(id)
        ReminderScheduler.cancel(getApplication(), id)
    }
}
