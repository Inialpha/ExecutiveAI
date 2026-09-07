package com.inialpha.executiveai.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.inialpha.executiveai.domain.model.SyncWindow
import com.inialpha.executiveai.domain.model.SyncWindowUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.syncSettingsDataStore by preferencesDataStore(name = "sync_settings")

/**
 * Persists the user's chosen email-synchronization window (e.g. "last 24 hours") across app
 * restarts. This window governs two things — see [com.inialpha.executiveai.data.repository.EmailRepository]
 * and [com.inialpha.executiveai.data.repository.InsightRepository]: how far back Gmail is
 * queried, and which PENDING/FAILED emails are still eligible for (re)processing.
 */
class SyncSettingsRepository(private val context: Context) {
    private object Keys {
        val AMOUNT = intPreferencesKey("sync_window_amount")
        val UNIT = stringPreferencesKey("sync_window_unit")
    }

    fun observeSyncWindow(): Flow<SyncWindow> = context.syncSettingsDataStore.data.map { prefs -> prefs.toSyncWindow() }

    suspend fun setSyncWindow(window: SyncWindow) {
        context.syncSettingsDataStore.edit { prefs ->
            prefs[Keys.AMOUNT] = window.amount
            prefs[Keys.UNIT] = window.unit.name
        }
    }

    private fun Preferences.toSyncWindow(): SyncWindow {
        val amount = this[Keys.AMOUNT] ?: return SyncWindow.DEFAULT
        val unit = this[Keys.UNIT]?.let { runCatching { SyncWindowUnit.valueOf(it) }.getOrNull() } ?: return SyncWindow.DEFAULT
        return SyncWindow(amount, unit)
    }
}
