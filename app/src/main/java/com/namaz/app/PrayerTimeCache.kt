package com.namaz.app

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Stores only successfully parsed daily times, keyed by city and date. */
object PrayerTimeCache {
    private const val STORE = "prayer_time_cache"
    fun save(context: Context, city: String, date: LocalDate, times: Map<String, LocalDateTime>) {
        val json = JSONObject()
        times.forEach { (name, value) -> json.put(name, value.toLocalTime().toString()) }
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .putString("${city.lowercase(java.util.Locale.ROOT)}_$date", json.toString()).apply()
    }

    fun read(context: Context, city: String, date: LocalDate): Map<String, LocalDateTime>? {
        val raw = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .getString("${city.lowercase(java.util.Locale.ROOT)}_$date", null) ?: return null
        return try {
            val json = JSONObject(raw)
            ReminderPreferences.prayers.associateWith { name ->
                LocalDateTime.of(date, LocalTime.parse(json.getString(name)))
            }
        } catch (_: Exception) {
            null
        }
    }
}
