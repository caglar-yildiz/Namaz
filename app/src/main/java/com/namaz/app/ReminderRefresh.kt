package com.namaz.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.util.concurrent.Executors

/** Reconciliation runs off the main thread and is retried at the next daily refresh. */
object ReminderRefresh {
    private const val REQUEST_ID = 3001
    private const val PREFS = "prayer_reminder_preferences"
    private const val CITY = "city"
    private val executor = Executors.newSingleThreadExecutor()

    fun saveCity(context: Context, city: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(CITY, city.trim()).apply()
    }

    fun city(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CITY, null)?.takeIf { it.isNotBlank() }

    fun refreshAsync(context: Context, finished: (() -> Unit)? = null) {
        val app = context.applicationContext
        executor.execute {
            try {
                val city = city(app)
                if (city == null || !ReminderPreferences.enabled(app, "Namaz vakitleri")) {
                    PrayerTimesService.cancelAll(app)
                } else {
                    PrayerTimesService.scheduleNextDay(app, city)
                }
            } catch (_: Exception) {
                // Preserve previously scheduled alarms if network refresh fails.
            } finally {
                scheduleDaily(app)
                finished?.invoke()
            }
        }
    }

    fun scheduleDaily(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderRefreshReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Inexact refresh: a two-day scheduling horizon limits gaps when delayed.
        manager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 6L * 60 * 60 * 1000,
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }
}

class ReminderRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED ||
            intent.component?.className == ReminderRefreshReceiver::class.java.name) {
            val pending = goAsync()
            try {
                PrayerRefreshWork.refreshNow(context)
                WorshipReminder.reconcile(context)
                ReligiousDayReminders.reconcile(context)
                RoutineAlarm.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }
}
