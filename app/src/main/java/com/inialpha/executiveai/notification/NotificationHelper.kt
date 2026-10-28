package com.inialpha.executiveai.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.inialpha.executiveai.MainActivity
import com.inialpha.executiveai.R

object NotificationHelper {
    const val CHANNEL_REMINDERS = "executive_reminders"
    const val CHANNEL_ALARMS = "executive_alarms"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notification_channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
        }
        manager.createNotificationChannel(channel)

        // Alarm channel: deliberately silent itself — AlarmService plays the phone's alarm tone on
        // the ALARM audio stream (looping), which a channel sound cannot do.
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARMS,
            context.getString(R.string.notification_channel_alarms_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_alarms_description)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(alarmChannel)
    }

    /** Requires POST_NOTIFICATIONS to already be granted on API 33+ — callers check before invoking. */
    fun showReminder(context: Context, notificationId: Int, title: String, body: String) {
        val launchIntent = android.content.Intent(context, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, notificationId, launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: replace with a proper Executive AI notification icon asset
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
