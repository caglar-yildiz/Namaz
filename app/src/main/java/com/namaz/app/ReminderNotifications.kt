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

    fun send(context: Context, title: String, message: String, destination: String? = null) {
        if (!canNotify(context)) return
        createChannels(context)
        val channel = channelId(ReminderPreferences.sound(context))
        val notification = build(context, title, message, channel, destination).build()
        context.getSystemService(NotificationManager::class.java)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    fun build(context: Context, title: String, message: String, channel: String, destination: String?): Notification.Builder {
        val intent = Intent(context, MainActivity::class.java)
            .setData(destination?.let { android.net.Uri.parse("namaz://open/$it") })
        val open = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setContentIntent(open)
    }

    /** One-shot test about a minute later, using the same exact/fallback path as prayer alarms. */
    fun scheduleTest(context: Context) {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("test", true)
        val pending = PendingIntent.getBroadcast(
            context, TEST_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        ExactAlarms.schedule(context, System.currentTimeMillis() + 60_000L, pending)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra("test", false)) {
            PrayerAnnouncer.announce(context, "Namaz test bildirimi",
                "Bildirim altyapısı çalışıyor. Bu gerçek bir namaz vakti değildir.",
                "Bu bir denemedir. Namaz vakitlerinde size böyle sesli haber vereceğim.")
            return
        }
        val prayer = intent.getStringExtra("prayer") ?: return
        val date = intent.getStringExtra("date") ?: return
        val prefs = context.getSharedPreferences("prayer_reminder_preferences", Context.MODE_PRIVATE)
        if (!ReminderPreferences.enabled(context, "Namaz vakitleri") ||
            !prefs.getBoolean(prayer, false)) return
        AlarmHealth.recordFired(context, date, prayer)
        if (date != java.time.LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")).toString()) return
        val (title, message, speech) = prayerAlarmTexts(prayer, intent.getStringExtra("time"),
            intent.getIntExtra("lead", 0), ReminderRefresh.city(context))
        PrayerAnnouncer.announce(context, title, message, speech)
        // The last prayer of the day extends the two-day scheduling horizon.
        if (prayer == "Yatsı") PrayerRefreshWork.refreshNow(context)
    }
}
