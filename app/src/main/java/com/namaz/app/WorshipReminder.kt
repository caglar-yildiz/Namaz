package com.namaz.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Optional, inexact evening check-in. No reminders are enabled by default. */
object WorshipReminder {
    private const val REQUEST_ID = 4301
    private val zone = ZoneId.of("Europe/Istanbul")
    private fun pending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, REQUEST_ID, Intent(context, WorshipReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun reconcile(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val intent = pending(context)
        manager.cancel(intent)
        if (!ReminderPreferences.enabled(context, "İbadet takibi")) return
        val now = java.time.ZonedDateTime.now(zone)
        var next = LocalDate.now(zone).atTime(LocalTime.of(21, 0)).atZone(zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        manager.set(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), intent)
    }
}

class WorshipReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (!ReminderPreferences.enabled(context, "İbadet takibi")) return
            val today = WorshipStore.today()
            val marked = WorshipStore.prayers.count {
                WorshipStore.completed(context, today, it)
            }
            if (marked < WorshipStore.prayers.size) {
                ReminderNotifications.send(
                    context,
                    "İbadet takibi",
                    "Bugün $marked / 5 namaz kaydınız var. Dilerseniz günlüğünüzü güncelleyin.",
                    "worship"
                )
            }
        } finally {
            WorshipReminder.reconcile(context)
        }
    }
}
