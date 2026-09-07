package com.inialpha.executiveai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.ExecutiveAIApplication
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.Account
import com.inialpha.executiveai.domain.model.ExecutiveItem
import com.inialpha.executiveai.domain.model.ExecutiveItemType
import com.inialpha.executiveai.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class TasksUiState(
    val isLoading: Boolean = true,
    val proposedTasks: List<ExecutiveItem> = emptyList(),
    val acceptedTasks: List<ExecutiveItem> = emptyList(),
    val deadlines: List<ExecutiveItem> = emptyList(),
    val accounts: List<Account> = emptyList(),
)

/**
 * Tasks/Actions screen. AI-extracted actions start PROPOSED; the user Accepts/Edits/Rejects/
 * Completes. Manually created tasks (see [addTask]) skip the proposal step entirely — they're
 * created ACCEPTED, since the user authored them directly.
 */
class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val container: AppContainer get() = (getApplication<Application>() as ExecutiveAIApplication).container

    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    init {
        combine(
            container.executiveItemRepository.observeByType(ExecutiveItemType.TASK),
            container.executiveItemRepository.observeByType(ExecutiveItemType.DEADLINE),
            container.accountRepository.observeAccounts(),
        ) { tasks, deadlines, accounts ->
            TasksUiState(
                isLoading = false,
                proposedTasks = tasks.filter { it.state.name == "PROPOSED" || it.state.name == "EDITED" },
                acceptedTasks = tasks.filter { it.state.name == "ACCEPTED" },
                deadlines = deadlines,
                accounts = accounts,
            )
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    fun accept(id: String) = viewModelScope.launch { container.executiveItemRepository.accept(id) }
    fun reject(id: String) = viewModelScope.launch { container.executiveItemRepository.reject(id) }
    fun complete(id: String) = viewModelScope.launch { container.executiveItemRepository.complete(id) }
    fun edit(id: String, title: String, description: String?) =
        viewModelScope.launch { container.executiveItemRepository.edit(id, title = title, description = description) }

    /**
     * "Add Task": creates the task directly (ACCEPTED — see ExecutiveItemRepository.createManualItem)
     * and, if a due date/time was given and a reminder was requested, schedules a real Android
     * reminder alarm for it — the same execution path an AI-proposed reminder uses once accepted.
     */
    fun addTask(accountId: String, title: String, description: String?, dueAtMillis: Long?, wantsReminder: Boolean) {
        viewModelScope.launch {
            val item = container.executiveItemRepository.createManualItem(
                accountId = accountId,
                type = ExecutiveItemType.TASK,
                title = title,
                description = description,
                dueAtMillis = dueAtMillis,
            )
            if (wantsReminder && dueAtMillis != null) {
                ReminderScheduler.schedule(
                    context = getApplication(),
                    itemId = item.id,
                    title = item.title,
                    body = item.description ?: "",
                    triggerAtMillis = dueAtMillis,
                )
            }
        }
    }
}
