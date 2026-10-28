package com.inialpha.executiveai.notification

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.inialpha.executiveai.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Rings like a real alarm: plays the phone's own alarm tone on the ALARM audio stream (so it is
 * audibly different from a message/notification sound, and plays at alarm volume), loops it,
 * vibrates, and shows a full-screen alarm screen with Dismiss / Snooze. It keeps ringing until
 * the user acts, or [AUTO_STOP_MILLIS] passes.
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopAlarm() }

    private var itemId = ""
    private var title = ""
    private var body = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> stopAlarm()
            ACTION_SNOOZE -> {
                ReminderScheduler.schedule(this, itemId, title, body, System.currentTimeMillis() + SNOOZE_MILLIS)
                stopAlarm()
            }
            else -> startAlarm(intent)
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(intent: Intent?) {
        itemId = intent?.getStringExtra(ReminderAlarmReceiver.EXTRA_ITEM_ID).orEmpty()
        title = intent?.getStringExtra(ReminderAlarmReceiver.EXTRA_TITLE).orEmpty()
        body = intent?.getStringExtra(ReminderAlarmReceiver.EXTRA_BODY).orEmpty()

        NotificationHelper.ensureChannels(this)
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        releasePlayer()
        startSound()
        startVibration()
        _isRinging.value = true
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, AUTO_STOP_MILLIS)
    }

    private fun buildNotification(): android.app.Notification {
        val alarmScreen = PendingIntent.getActivity(
            this, 0, AlarmActivity.intent(this, title, body),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ALARMS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body.ifBlank { getString(R.string.alarm_default_body) })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(alarmScreen)
            .setFullScreenIntent(alarmScreen, true)
            .addAction(0, getString(R.string.alarm_snooze), serviceAction(ACTION_SNOOZE))
            .addAction(0, getString(R.string.alarm_dismiss), serviceAction(ACTION_DISMISS))
            .build()
    }

    private fun serviceAction(action: String): PendingIntent = PendingIntent.getService(
        this, action.hashCode(), Intent(this, AlarmService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun startSound() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private fun startVibration() {
        vibrator()?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 700), 0))
    }

    private fun releasePlayer() {
        player?.runCatching { stop(); release() }
        player = null
    }

    private fun stopAlarm() {
        handler.removeCallbacks(autoStop)
        releasePlayer()
        vibrator()?.cancel()
        _isRinging.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        releasePlayer()
        vibrator()?.cancel()
        _isRinging.value = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_DISMISS = "com.inialpha.executiveai.alarm.DISMISS"
        const val ACTION_SNOOZE = "com.inialpha.executiveai.alarm.SNOOZE"
        private const val NOTIFICATION_ID = 4242
        private const val SNOOZE_MILLIS = 10 * 60 * 1000L
        private const val AUTO_STOP_MILLIS = 5 * 60 * 1000L

        private val _isRinging = MutableStateFlow(false)
        /** True while an alarm is ringing; [AlarmActivity] closes itself when this turns false. */
        val isRinging: StateFlow<Boolean> = _isRinging.asStateFlow()

        fun startIntent(context: Context, itemId: String, title: String, body: String): Intent =
            Intent(context, AlarmService::class.java)
                .putExtra(ReminderAlarmReceiver.EXTRA_ITEM_ID, itemId)
                .putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
                .putExtra(ReminderAlarmReceiver.EXTRA_BODY, body)

        fun dismissIntent(context: Context): Intent = Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS)
        fun snoozeIntent(context: Context): Intent = Intent(context, AlarmService::class.java).setAction(ACTION_SNOOZE)
    }
}
