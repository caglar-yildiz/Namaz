package com.namaz.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Local-only religious day reminders; event IDs are stable across refreshes. */
object ReligiousDayReminders {
    private const val PREFS = "prayer_reminder_preferences"
    private const val CATEGORY = "Kandil ve bayramlar"
    private const val WORK_NAME = "religious_day_daily_refresh"
    private const val MAX_EVENTS = 100
    private val zone = ZoneId.of("Europe/Istanbul")

    fun leadDays(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("religious_day_lead", 1).coerceIn(0, 1)

    fun setLeadDays(context: Context, days: Int) {
        require(days in 0..1)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt("religious_day_lead", days).apply()
        reconcile(context)
    }

    private fun intent(context: Context, id: Int): PendingIntent {
        val broadcast = Intent(context, ReligiousDayReceiver::class.java)
            .setAction("com.namaz.app.RELIGIOUS_DAY_$id")
        return PendingIntent.getBroadcast(context, 6000 + id, broadcast,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun reconcile(context: Context) {
        val app = context.applicationContext
        val alarms = app.getSystemService(AlarmManager::class.java)
        // Cancel old pending intents first so switching lead-day preferences is safe.
        repeat(MAX_EVENTS) { alarms.cancel(intent(app, it)) }
        if (!ReminderPreferences.enabled(app, CATEGORY)) {
            WorkManager.getInstance(app).cancelUniqueWork(WORK_NAME)
            return
        }
        val now = ZonedDateTime.now(zone)
        ReligiousDaysData.days.sortedWith(compareBy({ it.date }, { it.name }))
            .take(MAX_EVENTS).forEachIndexed { index, event ->
                val fireAt = event.date.minusDays(leadDays(app).toLong()).atTime(9, 0).atZone(zone)
                if (fireAt.isAfter(now) && fireAt.isBefore(now.plusDays(40))) {
                    val pending = intent(app, index)
                    alarms.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, fireAt.toInstant().toEpochMilli(), pending)
                }
            }
        val request = PeriodicWorkRequestBuilder<ReligiousDayRefreshWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun deliver(context: Context, id: Int) {
        if (!ReminderPreferences.enabled(context, CATEGORY)) return
        val event = ReligiousDaysData.days.sortedWith(compareBy({ it.date }, { it.name }))
            .getOrNull(id) ?: return
        val date = LocalDate.now(zone)
        if (date != event.date.minusDays(leadDays(context).toLong())) return
        val message = if (leadDays(context) == 1)
            "Yarın: ${event.name}" else "Bugün: ${event.name}"
        ReminderNotifications.send(context, "Dini gün hatırlatması", message)
    }
}

class ReligiousDayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.action?.substringAfterLast("_")?.toIntOrNull() ?: return
        ReligiousDayReminders.deliver(context, id)
    }
}

class ReligiousDayRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        ReligiousDayReminders.reconcile(applicationContext)
        Result.success()
    } catch (_: Exception) { Result.retry() }
}
