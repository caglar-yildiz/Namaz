package com.namaz.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Prototype source: AlAdhan method=13 (Diyanet calculation METHOD, not official
 * Diyanet-issued times). Compare with official district times before release.
 * Network calls must run off the main thread.
 */
object PrayerTimesService {
    private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val zone = ZoneId.of("Europe/Istanbul")
    private val names = linkedMapOf(
        "İmsak" to "Fajr", "Öğle" to "Dhuhr", "İkindi" to "Asr",
        "Akşam" to "Maghrib", "Yatsı" to "Isha"
    )

    fun fetch(date: LocalDate, city: String): Map<String, LocalDateTime> {
        require(city.isNotBlank())
        val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
        val endpoint = "https://api.aladhan.com/v1/timingsByCity/${date.format(formatter)}?city=$encodedCity&country=TR&method=13&school=1&timezonestring=Europe%2FIstanbul"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            require(connection.responseCode == 200) { "Prayer API HTTP ${connection.responseCode}" }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            require(json.getInt("code") == 200)
            val timings = json.getJSONObject("data").getJSONObject("timings")
            return names.mapValues { (_, key) ->
                val value = timings.getString(key).substringBefore(" ").trim()
                val time = java.time.LocalTime.parse(value)
                LocalDateTime.of(date, time)
            }
        } finally {
            connection.disconnect()
        }
    }

    fun cancelAll(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        for (dayOffset in 0..1) for (index in 0 until names.size) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context, dayOffset * 10 + index + 1000, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                manager.cancel(pending)
                pending.cancel()
            }
        }
    }

    fun scheduleNextDay(context: Context, city: String): Int {
        val today = LocalDate.now(zone)
        val prefs = context.getSharedPreferences("prayer_reminder_preferences", Context.MODE_PRIVATE)
        val manager = context.getSystemService(AlarmManager::class.java)
        val category = ReminderPreferences.enabled(context, "Namaz vakitleri")
        var scheduled = 0
        // Fetch first: on network failure, preserve existing alarms.
        val fetched = if (category) (0..1).associateWith { offset ->
            fetch(today.plusDays(offset.toLong()), city)
        } else emptyMap()
        // Cancel existing alarms for the same stable request IDs before rescheduling.
        for (dayOffset in 0..1) {
            val date = today.plusDays(dayOffset.toLong())
            val times = fetched[dayOffset] ?: emptyMap()
            names.keys.forEachIndexed { index, name ->
                val requestId = dayOffset * 10 + index + 1000
                val intent = Intent(context, ReminderReceiver::class.java)
                    .putExtra("prayer", name)
                    .putExtra("date", date.toString())
                val pending = PendingIntent.getBroadcast(
                    context, requestId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                manager.cancel(pending)
                if (category && prefs.getBoolean(name, false)) {
                    val at = times.getValue(name).minusMinutes(
                        prefs.getInt("lead_minutes", 0).toLong()
                    ).atZone(zone).toInstant().toEpochMilli()
                    if (at > System.currentTimeMillis()) {
                        // Inexact scheduling avoids special exact-alarm permission.
                        manager.set(AlarmManager.RTC_WAKEUP, at, pending)
                        scheduled++
                    }
                }
            }
        }
        return scheduled
    }
}
