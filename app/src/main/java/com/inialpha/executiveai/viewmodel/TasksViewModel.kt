package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.ExecutiveItem
import com.inialpha.executiveai.domain.model.ExecutiveItemType
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
)

/** Tasks/Actions screen. AI-extracted actions start PROPOSED; the user Accepts/Edits/Rejects/Completes. */
class TasksViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    init {
        combine(
            container.executiveItemRepository.observeByType(ExecutiveItemType.TASK),
            container.executiveItemRepository.observeByType(ExecutiveItemType.DEADLINE),
        ) { tasks, deadlines ->
            TasksUiState(
                isLoading = false,
                proposedTasks = tasks.filter { it.state.name == "PROPOSED" || it.state.name == "EDITED" },
                acceptedTasks = tasks.filter { it.state.name == "ACCEPTED" },
                deadlines = deadlines,
            )
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    fun accept(id: String) = viewModelScope.launch { container.executiveItemRepository.accept(id) }
    fun reject(id: String) = viewModelScope.launch { container.executiveItemRepository.reject(id) }
    fun complete(id: String) = viewModelScope.launch { container.executiveItemRepository.complete(id) }
    fun edit(id: String, title: String, description: String?) =
        viewModelScope.launch { container.executiveItemRepository.edit(id, title = title, description = description) }
}
