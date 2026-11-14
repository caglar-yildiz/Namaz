package com.namaz.app

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings

/**
 * Notification delivery foundation. Real prayer times must be supplied by a verified source.
 * The selected sound maps to a dedicated channel because channel sound cannot be
 * changed programmatically after channel creation.
 */
object ReminderNotifications {
    private const val BASE_ID = "namaz_reminders_v1"
    private const val TEST_REQUEST_CODE = 901
    private val modes = listOf("silent", "soft", "default", "vibrate")

    fun channelId(mode: String) = "${BASE_ID}_${mode.takeIf { it in modes } ?: "soft"}"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        modes.forEach { mode ->
            val channel = NotificationChannel(
                channelId(mode), "Namaz hatırlatmaları ($mode)",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            when (mode) {
                "silent" -> { channel.setSound(null, null); channel.enableVibration(false) }
                "vibrate" -> { channel.setSound(null, null); channel.enableVibration(true) }
                else -> {
                    // 'soft' uses the system notification sound until a licensed short tone exists.
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    channel.setSound(uri, AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
                    channel.enableVibration(false)
                }
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun send(context: Context, title: String, message: String) {
        if (!canNotify(context)) return
        createChannels(context)
        val channel = channelId(ReminderPreferences.sound(context))
        val notification = Notification.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    /** Inexact one-shot test, no exact-alarm permission required. */
    fun scheduleTest(context: Context) {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("test", true)
        val pending = PendingIntent.getBroadcast(
            context, TEST_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 60_000L, pending)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra("test", false)) {
            ReminderNotifications.send(context, "Namaz test bildirimi",
                "Bildirim altyapısı çalışıyor. Bu gerçek bir namaz vakti değildir.")
        }
    }
}
