package com.inialpha.executiveai.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.inialpha.executiveai.MainActivity

/**
 * Turns an ACCEPTED item into a real device alarm. Uses `AlarmManager.setAlarmClock`, the same
 * mechanism the system Clock app uses: it fires exactly on time even in Doze, shows the alarm
 * icon in the status bar, and — unlike setExact* — needs no special exact-alarm permission.
 * When it fires, [ReminderAlarmReceiver] starts [AlarmService], which rings with the phone's
 * alarm tone until dismissed or snoozed.
 *
 * Persistence/recovery after reboot is handled by [BootRescheduleReceiver], which re-reads
 * ACCEPTED reminder items from Room and calls [schedule] again for each one still in the future.
 */
object ReminderScheduler {

    /** Always true: setAlarmClock alarms are exempt from the Android 12+ exact-alarm restriction. */
    @Suppress("UNUSED_PARAMETER")
    fun canScheduleExactAlarms(context: Context): Boolean = true

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

        // Tapping the status-bar alarm icon opens the app.
        val showIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), pendingIntent)
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
