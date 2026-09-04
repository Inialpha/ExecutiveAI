package com.inialpha.executiveai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * On device reboot, exact alarms scheduled via [ReminderScheduler] are cleared by the OS. This
 * receiver kicks off a short-lived worker that re-reads ACCEPTED reminder [ExecutiveItem]s from
 * Room and re-schedules any that are still in the future, so reminders survive a restart.
 */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val request = OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
