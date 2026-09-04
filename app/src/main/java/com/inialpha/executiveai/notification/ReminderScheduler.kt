package com.inialpha.executiveai.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Foundation for turning an ACCEPTED reminder-type [com.inialpha.executiveai.domain.model.ExecutiveItem]
 * into a real device alarm. Uses `setExactAndAllowWhileIdle` where the app holds the exact-alarm
 * privilege, degrading to an inexact `set()` otherwise — Android 12+ restricts exact alarms, so
 * this never assumes the stronger guarantee is available.
 *
 * Persistence/recovery after reboot is handled by [BootRescheduleReceiver], which re-reads
 * ACCEPTED reminder items from Room and calls [schedule] again for each one still in the future.
 */
object ReminderScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(context: Context, itemId: String, title: String, body: String, triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_BODY, body)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, itemId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (canScheduleExactAlarms(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context, itemId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, itemId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }
}
