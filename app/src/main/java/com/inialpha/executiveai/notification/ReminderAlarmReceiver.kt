package com.inialpha.executiveai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Fires when a scheduled alarm goes off; hands over to [AlarmService], which rings until dismissed. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Executive AI reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        ContextCompat.startForegroundService(context, AlarmService.startIntent(context, itemId, title, body))
    }

    companion object {
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
    }
}
