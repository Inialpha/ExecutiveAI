package com.inialpha.executiveai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.inialpha.executiveai.notification.ReminderScheduler

data class SettingsUiState(
    val canScheduleExactAlarms: Boolean,
    val appVersion: String = "0.1.0",
)

/** Foundation settings screen: surfaces system permission state the rest of the app depends on. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val state = SettingsUiState(
        canScheduleExactAlarms = ReminderScheduler.canScheduleExactAlarms(application),
    )
}
