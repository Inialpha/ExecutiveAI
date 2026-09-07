package com.inialpha.executiveai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.ExecutiveAIApplication
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.SyncWindow
import com.inialpha.executiveai.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsUiState(
    val canScheduleExactAlarms: Boolean,
    val syncWindow: SyncWindow = SyncWindow.DEFAULT,
    val appVersion: String = "0.1.0",
)

/** Settings screen: system permission state the rest of the app depends on, plus the email sync window. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container: AppContainer get() = (getApplication<Application>() as ExecutiveAIApplication).container

    private val _state = MutableStateFlow(
        SettingsUiState(canScheduleExactAlarms = ReminderScheduler.canScheduleExactAlarms(application)),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        container.syncSettingsRepository.observeSyncWindow()
            .onEach { _state.value = _state.value.copy(syncWindow = it) }
            .launchIn(viewModelScope)
    }

    fun setSyncWindow(window: SyncWindow) = viewModelScope.launch { container.syncSettingsRepository.setSyncWindow(window) }
}
